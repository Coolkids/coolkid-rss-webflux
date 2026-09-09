package com.coolkid.coolkidrss.service.impl;

import cn.hutool.core.date.DateUtil;
import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.service.FeedRecordService;
import com.coolkid.coolkidrss.service.RssFeedRecordService;
import com.coolkid.coolkidrss.service.RssUpdateScheduleService;
import com.coolkid.coolkidrss.util.FeedUtil;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Date;


@Slf4j
@Data
@Service
public class FeedRecordServiceImpl implements FeedRecordService {
    private final RssFeedRecordService rssFeedRecordService;
    private final RssFeedInfoRepository rssFeedInfoRepository;
    private final FeedUtil feedUtil;
    private final RssUpdateScheduleService rssUpdateScheduleService;

    @Transactional(rollbackFor = Exception.class)
    @Override
    public Mono<Void> save(RssFeedInfo rssFeedInfo) {
        Date now = new Date();
        Date nextUpdate = DateUtils.addMinutes(now, rssFeedInfo.getFeedCrontab());
        return Mono.fromCallable(() -> feedUtil.getItems(rssFeedInfo.getFeedUrl(), rssFeedInfo.getFeedId()))
                .subscribeOn(Schedulers.boundedElastic())
                .flatMap(items -> CollectionUtils.isEmpty(items)
                        ? Mono.<Void>empty()
                        : rssFeedRecordService.saveOrUpdate(items))
                .then(rssFeedInfoRepository.updateDateById(
                        rssFeedInfo.getFeedId(),
                        now,
                        nextUpdate,
                        now
                ))
                .then(rssUpdateScheduleService.schedule(rssFeedInfo.getFeedId(), nextUpdate.toInstant()))
                .doOnSuccess(ignored -> log.info("更新完毕：{}, 时间：{}", rssFeedInfo.getFeedUrl(),
                        DateUtil.format(new Date(), "yyyy-MM-dd HH:mm:ss")))
                .then();
    }
}
