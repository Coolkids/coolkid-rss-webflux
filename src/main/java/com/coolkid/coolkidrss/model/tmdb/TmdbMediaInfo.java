package com.coolkid.coolkidrss.model.tmdb;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.io.Serializable;

/** 返回给 RSS 记录的 TMDB 影视信息。 */
public record TmdbMediaInfo(
        Long id,
        String mediaType,
        String name,
        String originalName,
        Integer releaseYear,
        String posterUrl,
        String backdropUrl,
        String overview,
        String originalLanguage,
        Double voteAverage,
        Integer voteCount,
        List<String> genres,
        Integer runtimeMinutes,
        String status
) implements Serializable {

    /** 转换为可直接放入 RssFeedRecord.recordMediaInfo 的结构。 */
    public Map<String, Object> toMap() {
        Map<String, Object> result = new LinkedHashMap<>();
        putIfNotNull(result, "id", id);
        putIfNotNull(result, "mediaType", mediaType);
        putIfNotNull(result, "name", name);
        putIfNotNull(result, "originalName", originalName);
        putIfNotNull(result, "releaseYear", releaseYear);
        putIfNotNull(result, "posterUrl", posterUrl);
        putIfNotNull(result, "backdropUrl", backdropUrl);
        putIfNotNull(result, "overview", overview);
        putIfNotNull(result, "originalLanguage", originalLanguage);
        putIfNotNull(result, "voteAverage", voteAverage);
        putIfNotNull(result, "voteCount", voteCount);
        if (genres != null && !genres.isEmpty()) {
            result.put("genres", genres);
        }
        putIfNotNull(result, "runtimeMinutes", runtimeMinutes);
        putIfNotNull(result, "status", status);
        return result;
    }

    private static void putIfNotNull(Map<String, Object> target, String key, Object value) {
        if (value != null) {
            target.put(key, value);
        }
    }
}
