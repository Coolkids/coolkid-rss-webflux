package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.util.RedisUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;

/**
 * MongoDB 保存 Feed 的真实配置；Redis ZSet 仅维护按到期时间排序的运行时索引。
 */
@Service
@Slf4j
@RequiredArgsConstructor
public class RssUpdateScheduleService {
    public static final String SCHEDULE_KEY = "coolkidrss:rss:update:schedule";

    private final RssFeedInfoRepository rssFeedInfoRepository;
    private final RedisUtil<?> redisUtil;

    public Mono<Void> schedule(RssFeedInfo feed) {
        if (!Integer.valueOf(1).equals(feed.getStatus())) {
            return cancel(feed.getFeedId());
        }
        Date nextUpdate = feed.getFeedNextUpdate();
        Instant executeAt = nextUpdate == null ? Instant.now() : nextUpdate.toInstant();
        return schedule(feed.getFeedId(), executeAt);
    }

    public Mono<Void> schedule(String feedId, Instant executeAt) {
        return redisUtil.schedule(SCHEDULE_KEY, feedId, executeAt);
    }

    public Mono<Void> cancel(String feedId) {
        return redisUtil.cancelSchedule(SCHEDULE_KEY, feedId);
    }

    /** 应用启动时从 MongoDB 恢复调度索引。 */
    public Mono<Void> restore() {
        return rssFeedInfoRepository.findByStatus(1)
                .flatMap(this::schedule)
                .then();
    }

    public Flux<String> claimDue(Instant now, int limit) {
        return redisUtil.claimDueSchedules(SCHEDULE_KEY, now, limit);
    }
}
