package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.dao.RssDlLogRepository;
import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.dao.RssFeedRecordRepository;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.Date;

@Service
@Data
@Slf4j
public class RssLittleJobService {
    @Value("${coolkidrss.keep.data.month}")
    private int month;
    private final RssDlLogRepository rssDlLogRepository;
    private final RssFeedRecordRepository rssFeedRecordRepository;
    private final RssFeedInfoRepository rssFeedInfoRepository;

    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> cleanRssOldData(){
        Date date = DateUtils.addMonths(new Date(), -month);
        return rssDlLogRepository.countByDlDateBefore(date)
                .flatMap(size -> rssDlLogRepository.deleteByDlDateBefore(date)
                        .doOnSuccess(ignored -> log.info("删除下载日志数据:{}条", size)))
                .then(rssFeedRecordRepository.countByTsBefore(date)
                        .flatMap(size -> rssFeedRecordRepository.deleteByTsBefore(date)
                                .doOnSuccess(ignored -> log.info("删除RSS记录:{}条", size))))
                .then();
    }
}
