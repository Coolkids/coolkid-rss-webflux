package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.rometools.rome.feed.synd.SyndEntry;

import java.util.Set;

/** RSS Feed 类型处理策略。 */
public interface FeedTypeStrategy {

    /** 返回当前策略支持的 Feed 类型。 */
    Set<FeedType> supportedTypes();

    /** 对 RSS 记录执行类型专用处理。 */
    void process(RssFeedRecord record, SyndEntry entry);
}
