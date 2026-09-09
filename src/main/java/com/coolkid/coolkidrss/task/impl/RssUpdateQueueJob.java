package com.coolkid.coolkidrss.task.impl;

import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.service.FeedRecordService;
import com.coolkid.coolkidrss.service.RssUpdateScheduleService;
import com.coolkid.coolkidrss.task.ScheduledTask;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.concurrent.TimeUnit;

@Service
@Slf4j
@Data
public class RssUpdateQueueJob implements ScheduledTask {
    private final RssFeedInfoRepository rssFeedInfoRepository;
    private final FeedRecordService feedRecordService;
    private final RssUpdateScheduleService rssUpdateScheduleService;
    private static final String TASK_NAME = "RssUpdateQueueJob";

    @Value("${coolkidrss.devmode}")
    private boolean devMode;

    @PostConstruct
    public void init(){
        if (!devMode) {
            rssUpdateScheduleService.restore()
                    .subscribe(ignored -> { }, error -> log.error("恢复 RSS 更新调度失败", error));
        }
    }

    @Override
    @Scheduled(fixedDelay = 1, timeUnit = TimeUnit.SECONDS)
    public void mainJob() {
        if (devMode) {
            return;
        }
        Instant now = Instant.now();
        rssUpdateScheduleService.claimDue(now, 32)
                .flatMap(feedId -> updateFeed(feedId, now), 32)
                .subscribe(ignored -> { }, error -> log.error("领取 RSS 更新任务失败", error));
    }

    private reactor.core.publisher.Mono<Void> updateFeed(String feedId, Instant now) {
        return rssFeedInfoRepository.findById(feedId)
                .filter(feed -> Integer.valueOf(1).equals(feed.getStatus()))
                .filter(feed -> feed.getFeedNextUpdate() == null || !feed.getFeedNextUpdate().toInstant().isAfter(now))
                .flatMap(feed -> {
                    log.info("开始更新 rss name:{}", feed.getFeedName());
                    return feedRecordService.save(feed);
                })
                .onErrorResume(error -> {
                    log.error("更新 RSS 失败，稍后重试，feedId:{}", feedId, error);
                    return rssUpdateScheduleService.schedule(feedId, now.plus(Duration.ofMinutes(1)));
                })
                .then();
    }

    @Override
    public String taskName() {
        return TASK_NAME;
    }
}
