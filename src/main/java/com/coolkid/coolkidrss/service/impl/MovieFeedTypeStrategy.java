package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.service.AnitopyMlService;
import com.coolkid.coolkidrss.service.AnitopyTmdbMissService;
import com.coolkid.coolkidrss.service.FeedTypeStrategy;
import com.coolkid.coolkidrss.service.TmdbService;
import com.coolkid.coolkidrss.model.tmdb.TmdbMediaInfo;
import com.rometools.rome.feed.synd.SyndEntry;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 影视 Feed 策略：从标题中提取媒体元数据。 */
@Slf4j
@Component
public class MovieFeedTypeStrategy implements FeedTypeStrategy {
    private final AnitopyMlService anitopyMlService;
    private final AnitopyTmdbMissService anitopyTmdbMissService;
    private final TmdbService tmdbService;

    public MovieFeedTypeStrategy(
            AnitopyMlService anitopyMlService,
            AnitopyTmdbMissService anitopyTmdbMissService,
            TmdbService tmdbService) {
        this.anitopyMlService = anitopyMlService;
        this.anitopyTmdbMissService = anitopyTmdbMissService;
        this.tmdbService = tmdbService;
    }

    @Override
    public Set<FeedType> supportedTypes() {
        return Set.of(FeedType.MOVIE);
    }

    @Override
    public void process(RssFeedRecord record, SyndEntry entry) {
        try {
            anitopyMlService.parse(record.getRecordTitle()).ifPresent(parsed -> {
                Map<String, Object> mediaInfo = new LinkedHashMap<>(parsed);
                String mediaName = textValue(mediaInfo.get("title"));
                if (mediaName == null) {
                    // 兼容历史解析结果，新的 anitopy-ml 字段为 title。
                    mediaName = textValue(mediaInfo.get("anime_title"));
                }
                Integer releaseYear = yearValue(mediaInfo.get("year"));
                if (releaseYear == null) {
                    releaseYear = yearValue(mediaInfo.get("anime_year"));
                }
                if (mediaName != null) {
                    tmdbService.findByName(mediaName, releaseYear)
                            .map(TmdbMediaInfo::toMap)
                            .ifPresentOrElse(
                                    tmdb -> mediaInfo.put("tmdb", tmdb),
                                    () -> anitopyTmdbMissService.record(record, mediaInfo));
                } else {
                    anitopyTmdbMissService.record(record, mediaInfo);
                }
                record.setRecordMediaInfo(mediaInfo);
            });
        } catch (RuntimeException e) {
            // 单条标题解析失败不应导致整个 feed 更新失败。
            log.debug("影视标题解析失败：{}", record.getRecordTitle(), e);
        }
    }

    private String textValue(Object value) {
        if (value == null) {
            return null;
        }
        String text = String.valueOf(value).trim();
        return text.isEmpty() ? null : text;
    }

    private Integer yearValue(Object value) {
        String text = textValue(value);
        if (text == null) {
            return null;
        }
        try {
            return Integer.valueOf(text.length() >= 4 ? text.substring(0, 4) : text);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
