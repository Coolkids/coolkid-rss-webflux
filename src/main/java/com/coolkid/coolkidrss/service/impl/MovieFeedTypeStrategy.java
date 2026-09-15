package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.service.FeedTypeStrategy;
import com.rometools.rome.feed.synd.SyndEntry;
import io.github.igorcmoura.anitopy4j.Anitopy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** 影视 Feed 策略：从标题中提取媒体元数据。 */
@Slf4j
@Component
public class MovieFeedTypeStrategy implements FeedTypeStrategy {

    @Override
    public Set<FeedType> supportedTypes() {
        return Set.of(FeedType.MOVIE);
    }

    @Override
    public void process(RssFeedRecord record, SyndEntry entry) {
        try {
            Map<String, Object> parsed = Anitopy.parse(record.getRecordTitle());
            if (parsed != null && !parsed.isEmpty()) {
                record.setRecordMediaInfo(new LinkedHashMap<>(parsed));
            }
        } catch (RuntimeException e) {
            // 单条标题解析失败不应导致整个 feed 更新失败。
            log.debug("影视标题解析失败：{}", record.getRecordTitle(), e);
        }
    }
}
