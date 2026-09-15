package com.coolkid.coolkidrss.service.impl;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.coolkid.coolkidrss.service.TmdbService;
import com.coolkid.coolkidrss.model.tmdb.TmdbMediaInfo;
import com.coolkid.coolkidrss.util.EasyUtil;
import com.coolkid.coolkidrss.util.OkHttpUtil;
import com.coolkid.coolkidrss.util.RedisUtil;
import lombok.extern.slf4j.Slf4j;
import okhttp3.HttpUrl;
import okhttp3.Request;
import okhttp3.Response;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/** TMDB v3 简易客户端：搜索条目并读取电影/剧集详情。 */
@Slf4j
@Service
public class TmdbServiceImpl implements TmdbService {
    private static final String CACHE_KEY_PREFIX = "coolkidrss:tmdb:media:";
    private static final Duration CACHE_TTL = Duration.ofDays(31);
    private static final Duration CACHE_OPERATION_TIMEOUT = Duration.ofSeconds(3);

    private final ObjectMapper objectMapper;
    private final RedisUtil<TmdbMediaInfo> redisUtil;

    @Value("${coolkidrss.tmdb.api-token:}")
    private String apiToken;

    @Value("${coolkidrss.tmdb.base-url:https://api.themoviedb.org/3}")
    private String baseUrl;

    @Value("${coolkidrss.tmdb.language:zh-CN}")
    private String language;

    @Value("${coolkidrss.tmdb.image-base-url:https://image.tmdb.org/t/p/w500}")
    private String imageBaseUrl;

    private final AtomicInteger tokenCursor = new AtomicInteger();

    public TmdbServiceImpl(ObjectMapper objectMapper, RedisUtil<TmdbMediaInfo> redisUtil) {
        this.objectMapper = objectMapper;
        this.redisUtil = redisUtil;
    }

    @Override
    public Optional<TmdbMediaInfo> findByName(String name, Integer releaseYear) {
        if (StringUtils.isBlank(name)) {
            return Optional.empty();
        }
        String cacheKey = cacheKey(name, releaseYear);
        TmdbMediaInfo cached = readCache(cacheKey);
        if (cached != null) {
            return Optional.of(cached);
        }

        List<String> tokens = apiTokens();
        if (tokens.isEmpty()) {
            return Optional.empty();
        }
        int start = Math.floorMod(tokenCursor.getAndIncrement(), tokens.size());
        for (int i = 0; i < tokens.size(); i++) {
            String token = tokens.get((start + i) % tokens.size());
            try {
                JsonNode searchResult = request(searchUrl(name), token);
                JsonNode matched = selectMatch(searchResult.path("results"), name, releaseYear);
                if (matched == null || !matched.hasNonNull("id")) {
                    return Optional.empty();
                }
                String mediaType = matched.path("media_type").asText();
                if (!"movie".equals(mediaType) && !"tv".equals(mediaType)) {
                    return Optional.empty();
                }
                JsonNode details = request(detailsUrl(mediaType, matched.path("id").asLong()), token);
                TmdbMediaInfo mediaInfo = toMediaInfo(details, mediaType, matched);
                writeCache(cacheKey, mediaInfo);
                return Optional.of(mediaInfo);
            } catch (IOException | RuntimeException e) {
                // 当前 Token 失败时尝试下一个；所有 Token 都失败才放弃本次补全。
                log.warn("TMDB 查询失败，将尝试下一个 Token，名称：{}", name, e);
            }
        }
        return Optional.empty();
    }

    private String cacheKey(String name, Integer releaseYear) {
        String keySource = language + "|" + normalize(name) + "|" + (releaseYear == null ? "" : releaseYear);
        return CACHE_KEY_PREFIX + EasyUtil.sha256(keySource);
    }

    private TmdbMediaInfo readCache(String cacheKey) {
        try {
            return redisUtil.get(cacheKey).block(CACHE_OPERATION_TIMEOUT);
        } catch (RuntimeException e) {
            log.debug("读取 TMDB 缓存失败：{}", cacheKey, e);
            return null;
        }
    }

    private void writeCache(String cacheKey, TmdbMediaInfo mediaInfo) {
        try {
            redisUtil.set(cacheKey, mediaInfo, CACHE_TTL).block(CACHE_OPERATION_TIMEOUT);
        } catch (RuntimeException e) {
            log.debug("写入 TMDB 缓存失败：{}", cacheKey, e);
        }
    }

    private JsonNode request(HttpUrl url, String token) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .header("Authorization", "Bearer " + token)
                .header("accept", "application/json")
                .get()
                .build();
        try (Response response = OkHttpUtil.build(5, 15).newCall(request).execute()) {
            if (!response.isSuccessful() || response.body() == null) {
                throw new IllegalStateException("TMDB HTTP " + response.code());
            }
            return objectMapper.readTree(response.body().string());
        }
    }

    private List<String> apiTokens() {
        if (StringUtils.isBlank(apiToken)) {
            return List.of();
        }
        return java.util.Arrays.stream(apiToken.split(";"))
                .map(String::trim)
                .filter(StringUtils::isNotBlank)
                .distinct()
                .toList();
    }

    private HttpUrl searchUrl(String name) {
        return HttpUrl.parse(baseUrl + "/search/multi").newBuilder()
                .addQueryParameter("query", name.trim())
                .addQueryParameter("include_adult", "true")
                .addQueryParameter("language", language)
                .addQueryParameter("page", "1")
                .build();
    }

    private HttpUrl detailsUrl(String mediaType, long id) {
        return HttpUrl.parse(baseUrl + "/" + mediaType + "/" + id).newBuilder()
                .addQueryParameter("language", language)
                .build();
    }

    private JsonNode selectMatch(JsonNode results, String name, Integer releaseYear) {
        if (!results.isArray()) {
            return null;
        }
        String normalizedName = normalize(name);
        List<JsonNode> candidates = new ArrayList<>();
        results.forEach(candidate -> {
            String mediaType = candidate.path("media_type").asText();
            if ("movie".equals(mediaType) || "tv".equals(mediaType)) {
                candidates.add(candidate);
            }
        });
        JsonNode best = null;
        int bestScore = -1;
        for (JsonNode candidate : candidates) {
            int score = matchScore(candidate, normalizedName, releaseYear);
            if (best == null || score > bestScore) {
                best = candidate;
                bestScore = score;
            }
        }
        return best;
    }

    private int matchScore(JsonNode candidate, String normalizedName, Integer releaseYear) {
        int score = 0;
        String title = candidate.path("title").asText(candidate.path("name").asText());
        String originalTitle = candidate.path("original_title")
                .asText(candidate.path("original_name").asText());
        if (normalizedName.equals(normalize(title))) {
            score += 100;
        }
        if (normalizedName.equals(normalize(originalTitle))) {
            score += 80;
        }
        if (releaseYear != null && releaseYear.equals(extractYear(candidate))) {
            score += 50;
        }
        // 分数相同保留 TMDB 返回的先后顺序。
        return score;
    }

    private TmdbMediaInfo toMediaInfo(JsonNode details, String mediaType, JsonNode searchResult) {
        String name = text(details, "title", "name");
        String originalName = text(details, "original_title", "original_name");
        Integer releaseYear = extractYear(details);
        if (releaseYear == null) {
            releaseYear = extractYear(searchResult);
        }
        return new TmdbMediaInfo(
                details.path("id").asLong(searchResult.path("id").asLong()),
                mediaType,
                name,
                originalName,
                releaseYear,
                imageUrl(details.path("poster_path").asText(null)),
                imageUrl(details.path("backdrop_path").asText(null)),
                details.path("overview").asText(null),
                details.path("original_language").asText(null),
                details.hasNonNull("vote_average") ? details.path("vote_average").asDouble() : null,
                details.hasNonNull("vote_count") ? details.path("vote_count").asInt() : null,
                genres(details.path("genres")),
                runtimeMinutes(details, mediaType),
                details.path("status").asText(null)
        );
    }

    private List<String> genres(JsonNode genres) {
        if (!genres.isArray()) {
            return List.of();
        }
        List<String> result = new ArrayList<>();
        genres.forEach(genre -> {
            if (StringUtils.isNotBlank(genre.path("name").asText())) {
                result.add(genre.path("name").asText());
            }
        });
        return result;
    }

    private Integer runtimeMinutes(JsonNode details, String mediaType) {
        if ("movie".equals(mediaType) && details.hasNonNull("runtime")) {
            return details.path("runtime").asInt();
        }
        JsonNode runtimes = details.path("episode_run_time");
        return runtimes.isArray() && !runtimes.isEmpty() ? runtimes.get(0).asInt() : null;
    }

    private String imageUrl(String path) {
        if (StringUtils.isBlank(path)) {
            return null;
        }
        return imageBaseUrl.replaceAll("/+$", "") + "/" + path.replaceFirst("^/+", "");
    }

    private Integer extractYear(JsonNode node) {
        String date = text(node, "release_date", "first_air_date");
        if (StringUtils.isBlank(date) || date.length() < 4) {
            return null;
        }
        try {
            return Integer.valueOf(date.substring(0, 4));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private String text(JsonNode node, String first, String second) {
        String firstValue = node.path(first).asText();
        return StringUtils.isNotBlank(firstValue) ? firstValue : node.path(second).asText(null);
    }

    private String normalize(String value) {
        if (value == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        value.codePoints()
                .filter(Character::isLetterOrDigit)
                .forEach(result::appendCodePoint);
        return result.toString().toLowerCase(java.util.Locale.ROOT);
    }
}
