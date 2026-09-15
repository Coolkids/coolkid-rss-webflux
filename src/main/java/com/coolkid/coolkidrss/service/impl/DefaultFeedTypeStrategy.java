package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.service.FeedTypeStrategy;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.stereotype.Component;

import java.util.Set;

/** 新闻、音乐和其他 Feed 的默认策略：保留 RSS 原始内容。 */
@Component
public class DefaultFeedTypeStrategy implements FeedTypeStrategy {

    @Override
    public Set<FeedType> supportedTypes() {
        return Set.of(FeedType.NEWS, FeedType.MUSIC, FeedType.OTHER);
    }

    @Override
    public void process(RssFeedRecord record, SyndEntry entry) {
        // 保留 RSS 原始内容，后续类型可以增加独立策略。
    }
}
