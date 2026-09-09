package com.coolkid.coolkidrss.service.impl;

import cn.hutool.core.bean.BeanUtil;
import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.dao.RssFeedRecordRepository;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.request.RssFeedSortReq;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.model.response.RssFeedInfoList;
import com.coolkid.coolkidrss.service.FeedRecordService;
import com.coolkid.coolkidrss.service.FeedService;
import com.coolkid.coolkidrss.service.RssFeedRecordService;
import com.coolkid.coolkidrss.service.RssUpdateScheduleService;
import com.coolkid.coolkidrss.util.EasyUtil;
import com.coolkid.coolkidrss.util.FeedUtil;
import com.coolkid.coolkidrss.util.RssIdUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Schedulers;

import java.util.Date;
import java.util.Map;
import java.util.Objects;

@Service
@Slf4j
@Data
public class FeedServiceImpl implements FeedService {
    private final RssFeedInfoRepository rssFeedInfoRepository;
    private final RssFeedRecordRepository rssFeedRecordRepository;
    private final FeedUtil feedUtil;
    private final RssFeedRecordService rssFeedRecordService;
    private final FeedRecordService feedRecordService;
    private final RssUpdateScheduleService rssUpdateScheduleService;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<RssFeedInfo> save(RssFeedInfo rssFeedInfo) {
        if (StringUtils.isNotBlank(rssFeedInfo.getFeedId())) {
            return rssFeedInfoRepository.findById(rssFeedInfo.getFeedId())
                    .switchIfEmpty(Mono.error(new IllegalArgumentException("feed_id非法")))
                    .flatMap(t -> doUpdate(rssFeedInfo, t))
                    .flatMap(this::schedule);
        }

        String feedUrl = rssFeedInfo.getFeedUrl();
        String feedName = rssFeedInfo.getFeedName();
        if (StringUtils.isBlank(feedName)) {
            return Mono.fromCallable(() -> feedUtil.getFeedTitle(feedUrl))
                    .subscribeOn(Schedulers.boundedElastic())
                    .defaultIfEmpty("")
                    .flatMap(feedTitle -> {
                        rssFeedInfo.setFeedName(feedTitle);
                        return savePrepared(rssFeedInfo);
                    });
        }
        return savePrepared(rssFeedInfo);
    }

    private Mono<RssFeedInfo> savePrepared(RssFeedInfo rssFeedInfo) {
        if (StringUtils.isBlank(rssFeedInfo.getFeedName())) {
            log.error("feed名称为空");
            return Mono.error(new IllegalArgumentException("feed名称为空"));
        }

        Date now = Objects.isNull(rssFeedInfo.getFeedLastUpdate()) ? new Date() : rssFeedInfo.getFeedLastUpdate();

        rssFeedInfo.setFeedId(RssIdUtil.nextIdStr());
        rssFeedInfo.setFeedNextUpdate(DateUtils.addMinutes(now, -1));

        Mono<RssFeedInfo> prepared = rssFeedInfo.getSortOn() > 0
                ? Mono.just(rssFeedInfo)
                : rssFeedInfoRepository.findTopByOrderBySortOnDesc()
                .defaultIfEmpty(new RssFeedInfo())
                .map(lastFeed -> {
                    rssFeedInfo.setSortOn(Math.max(1, lastFeed.getSortOn() + 1));
                    return rssFeedInfo;
                });

        return prepared.flatMap(feed -> {
            log.info("添加新feed到延迟队列 name:{}, 时间:{}", feed.getFeedName(), EasyUtil.getNowDateStr());
            return rssFeedInfoRepository.save(feed).flatMap(this::schedule);
        });
    }

    private Mono<RssFeedInfo> schedule(RssFeedInfo rssFeedInfo) {
        return rssUpdateScheduleService.schedule(rssFeedInfo).thenReturn(rssFeedInfo);
    }

    private Mono<RssFeedInfo> doUpdate(RssFeedInfo rssFeedInfo, RssFeedInfo t) {
        if (Objects.isNull(t)) {
            log.error("feed_id非法");
            return Mono.error(new IllegalArgumentException("feed_id非法"));
        }

        Map<String, Object> stringObjectMap = BeanUtil.beanToMap(rssFeedInfo, false, true);
        if (rssFeedInfo.getSortOn() <= 0) {
            stringObjectMap.remove("sortOn");
        }
        BeanUtil.copyProperties(stringObjectMap, t, "feedId");
        return rssFeedInfoRepository.save(t);
    }

    @Override
    public Flux<RssFeedInfo> get(boolean full) {
        if (full) {
            return rssFeedInfoRepository.findAll();
        }
        return rssFeedInfoRepository.findByStatus(1);
    }

    @Override
    public Flux<RssFeedInfoList> getRessFeedInfoList(boolean full) {
        return get(full).flatMap(feed -> rssFeedRecordRepository.countUnRead(feed.getFeedId())
                .defaultIfEmpty(0L)
                .map(unread -> {
                    RssFeedInfoList result = new RssFeedInfoList(feed);
                    result.setUnRead(unread.intValue());
                    return result;
                }));
    }

    @Override
    public Mono<Page<RssFeedRecord>> getRecord(FeedRecordReq feedRecordReq) {
        return rssFeedRecordService.page(feedRecordReq);
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Mono<Void> readRecord(String recordId) {
        return rssFeedRecordRepository.updateRecordReadateById(recordId, new Date()).then();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> allRead(String feedId) {
        return rssFeedRecordRepository.updateRecordReadateByFeedId(feedId, new Date()).then();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> remove(String feedId) {
        return rssUpdateScheduleService.cancel(feedId)
                .then(rssFeedInfoRepository.deleteById(feedId));
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> sortOn(RssFeedSortReq rssFeedSortReq) {
        if (rssFeedSortReq == null || rssFeedSortReq.getData() == null) {
            return Mono.empty();
        }
        return Flux.fromIterable(rssFeedSortReq.getData())
                .concatMap(item -> {
                    if (StringUtils.isBlank(item.getFeedId())) {
                        return Mono.error(new IllegalArgumentException("feed_id非法"));
                    }
                    if (item.getSortOn() < 1) {
                        return Mono.error(new IllegalArgumentException("sort_on必须从1开始"));
                    }
                    return rssFeedInfoRepository.updateSortOnById(item.getFeedId(), item.getSortOn());
                })
                .then();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> flush(RssFeedInfo item) {
        return rssFeedInfoRepository.findById(item.getFeedId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("feed_id非法")))
                .flatMap(feed -> rssUpdateScheduleService.cancel(feed.getFeedId())
                        .then(feedRecordService.save(feed)))
                .then();
    }

    @Override
    public Mono<Void> favRecord(String recordId, Integer fav) {
        return rssFeedRecordRepository.updateRecordFavById(recordId, fav).then();
    }
}
