package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.FeedType;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import jakarta.annotation.PreDestroy;
import java.util.List;

/** RSS 基础记录保存后的异步类型补充服务。 */
@Slf4j
@Service
public class FeedEnrichmentService {
    private final FeedTypeProcessor feedTypeProcessor;
    private final RssFeedRecordService rssFeedRecordService;
    private final Scheduler scheduler;
    private final int concurrency;

    public FeedEnrichmentService(
            FeedTypeProcessor feedTypeProcessor,
            RssFeedRecordService rssFeedRecordService,
            @Value("${coolkidrss.rss.enrichment.concurrency:3}") int concurrency) {
        this.feedTypeProcessor = feedTypeProcessor;
        this.rssFeedRecordService = rssFeedRecordService;
        int actualConcurrency = Math.max(1, concurrency);
        this.concurrency = actualConcurrency;
        this.scheduler = Schedulers.newBoundedElastic(
                actualConcurrency, 10_000, "rss-enrichment");
    }

    /** 提交异步补充任务，不等待外部 API 完成。 */
    public void submit(List<RssFeedRecord> records, FeedType feedType) {
        if (CollectionUtils.isEmpty(records) || !needsEnrichment(feedType)) {
            return;
        }
        enrich(records, feedType)
                .subscribe(
                        ignored -> { },
                        error -> log.error("RSS 异步补充任务失败，feedType:{}", feedType, error),
                        () -> log.debug("RSS 异步补充任务完成，feedType:{}，数量:{}", feedType, records.size())
                );
    }

    private Mono<Void> enrich(List<RssFeedRecord> records, FeedType feedType) {
        return Flux.fromIterable(records)
                .filter(record -> needsRecordEnrichment(record, feedType))
                .flatMap(record -> Mono.fromCallable(() -> {
                            feedTypeProcessor.process(feedType, record, null);
                            return record;
                        })
                        .subscribeOn(scheduler)
                        .flatMap(rssFeedRecordService::updateEnrichment)
                        .onErrorResume(error -> {
                            log.warn("RSS 记录异步补充失败，recordId:{}，feedType:{}",
                                    record.getRecordId(), feedType, error);
                            return Mono.empty();
                        }), concurrency)
                .then();
    }

    private boolean needsEnrichment(FeedType feedType) {
        return FeedType.MOVIE == feedType || FeedType.CODE == feedType;
    }

    private boolean needsRecordEnrichment(RssFeedRecord record, FeedType feedType) {
        if (record == null) {
            return false;
        }
        if (FeedType.MOVIE == feedType) {
            return record.getRecordMediaInfo() == null
                    || !record.getRecordMediaInfo().containsKey("tmdb");
        }
        return record.getRecordPatch() == null;
    }

    @PreDestroy
    public void shutdown() {
        scheduler.dispose();
    }
}
