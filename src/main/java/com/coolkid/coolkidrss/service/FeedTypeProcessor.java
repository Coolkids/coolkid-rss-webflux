package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.rometools.rome.feed.synd.SyndEntry;
import org.springframework.stereotype.Component;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** 类型处理策略上下文：根据 Feed 类型选择对应策略。 */
@Component
public class FeedTypeProcessor {
    private final Map<FeedType, FeedTypeStrategy> strategies;

    public FeedTypeProcessor(List<FeedTypeStrategy> strategyList) {
        EnumMap<FeedType, FeedTypeStrategy> strategyMap = new EnumMap<>(FeedType.class);
        for (FeedTypeStrategy strategy : strategyList) {
            for (FeedType type : strategy.supportedTypes()) {
                FeedTypeStrategy previous = strategyMap.put(type, strategy);
                if (previous != null) {
                    throw new IllegalStateException("Feed 类型策略重复：" + type);
                }
            }
        }
        this.strategies = Map.copyOf(strategyMap);
    }

    public void process(FeedType type, RssFeedRecord record, SyndEntry entry) {
        FeedType actualType = type == null ? FeedType.OTHER : type;
        FeedTypeStrategy strategy = strategies.get(actualType);
        if (strategy == null) {
            strategy = strategies.get(FeedType.OTHER);
        }
        if (strategy == null) {
            throw new IllegalStateException("未配置 Feed 类型默认策略：" + FeedType.OTHER);
        }
        strategy.process(record, entry);
    }
}
