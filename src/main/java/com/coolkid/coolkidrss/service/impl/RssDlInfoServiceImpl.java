package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.dao.RssDlInfoRepository;
import com.coolkid.coolkidrss.dao.RssDlLogRepository;
import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssDlLog;
import com.coolkid.coolkidrss.model.request.DownloadRecordReq;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.service.BittorrentService;
import com.coolkid.coolkidrss.service.RssDlInfoService;
import com.coolkid.coolkidrss.service.RssFeedRecordService;
import com.coolkid.coolkidrss.util.RssIdUtil;
import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;
import java.util.function.BiFunction;

@Service
@Data
public class RssDlInfoServiceImpl implements RssDlInfoService {
    private final RssDlLogRepository rssDlLogRepository;
    private final RssDlInfoRepository rssDlInfoRepository;
    private final ReactiveMongoTemplate mongoTemplate;
    private final List<BittorrentService> bittorrentServices;
    private final RssFeedRecordService rssFeedRecordService;
    private final HashMap<Integer, BiFunction<RssDlInfo, DownloadRecordReq, Mono<Void>>> functionMap = Maps.newHashMap();

    @PostConstruct
    public void init() {
        for (BittorrentService item : bittorrentServices) {
            BiFunction<RssDlInfo, DownloadRecordReq, Mono<Void>> t = item::download;
            functionMap.put(item.type(), t);
        }
    }

    @Override
    public Mono<Page<RssDlLog>> getLogs(FeedRecordReq feedRecordReq) {
        return page(feedRecordReq);
    }

    public Mono<Page<RssDlLog>> page(FeedRecordReq feedRecordReq) {
        return count(feedRecordReq).zipWhen(
                t -> page(feedRecordReq,
                        PageRequest.of(
                                feedRecordReq.getPage() - 1,
                                feedRecordReq.getPageSize(),
                                Sort.by(Sort.Direction.DESC, "dl_date")
                        )
                ).collectList(), (t1, t2) -> {
                    Page<RssDlLog> page = new Page<>();
                    page.setTotal(t1);
                    page.setRecords(t2);
                    page.setCurrent(feedRecordReq.getPage());
                    page.setSize(feedRecordReq.getPageSize());
                    return page;
                }
        );
    }

    public Mono<Long> count(FeedRecordReq feedRecordReq) {
        Query query = buildQuery(feedRecordReq);
        return mongoTemplate.count(query, RssDlLog.class);
    }

    public Flux<RssDlLog> query(FeedRecordReq feedRecordReq) {
        Query query = buildQuery(feedRecordReq);
        return mongoTemplate.find(query, RssDlLog.class);
    }

    public Flux<RssDlLog> page(FeedRecordReq feedRecordReq, Pageable pageable) {
        Query query = buildQuery(feedRecordReq);
        query.with(pageable);
        return mongoTemplate.find(query, RssDlLog.class);
    }


    private Query buildQuery(FeedRecordReq feedRecordReq) {
        List<Criteria> criterias = new ArrayList<>();
        criterias.add(rssFeedRecordService.buildFeedIdCriteria(feedRecordReq));
        criterias.add(rssFeedRecordService.buildTimeRangeCriteria(feedRecordReq,"dl_date"));
        criterias.addAll(rssFeedRecordService.buildNeedCriteria(feedRecordReq));
        criterias.add(rssFeedRecordService.buildNeedOrCriteria(feedRecordReq));
        criterias.addAll(rssFeedRecordService.buildNotNeedCriteria(feedRecordReq));
        criterias.add(rssFeedRecordService.buildNotNeedOrCriteria(feedRecordReq));
        Query query = new Query();
        Criteria total = new Criteria();
        total.andOperator(criterias.stream().filter(Objects::nonNull).toList());
        query.addCriteria(total);
        return query;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> download(DownloadRecordReq downloadRecordReq) {
        String recordTitle = downloadRecordReq.getRecordTitle();
        if (Objects.nonNull(recordTitle)) {
            return rssFeedRecordService.updateDownStatusByRecordTitle(recordTitle, 1)
                    .then(executeDownload(downloadRecordReq));
        }
        return executeDownload(downloadRecordReq);
    }

    private Mono<Void> executeDownload(DownloadRecordReq downloadRecordReq) {
        return rssDlInfoRepository.findById(downloadRecordReq.getDlId())
                .switchIfEmpty(Mono.error(new IllegalArgumentException("下载配置不存在: " + downloadRecordReq.getDlId())))
                .flatMap(downloadInfo -> {
                    BiFunction<RssDlInfo, DownloadRecordReq, Mono<Void>> download = functionMap.get(downloadInfo.getDlType());
                    if (download == null) {
                        return Mono.error(new IllegalArgumentException("不支持的下载器类型: " + downloadInfo.getDlType()));
                    }
                    return download.apply(downloadInfo, downloadRecordReq);
                });
    }

    @Override
    public Mono<Void> remove(String dlid) {
        return rssDlInfoRepository.deleteById(dlid);
    }

    @Override
    public Flux<RssDlInfo> findAll() {
        return rssDlInfoRepository.findAll(Sort.by(Sort.Direction.DESC, "_id"));
    }

    @Override
    public Mono<RssDlInfo> save(RssDlInfo rssDlInfo) {
        if (Objects.isNull(rssDlInfo.getDlId())) {
            rssDlInfo.setDlId(RssIdUtil.nextIdStr());
            return rssDlInfoRepository.save(rssDlInfo);
        }

        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(rssDlInfo.getDlId()));
        Update update = new Update();
        update.set("dl_name", rssDlInfo.getDlName());
        update.set("dl_url", rssDlInfo.getDlUrl());
        update.set("dl_type", rssDlInfo.getDlType());
        update.set("dl_user", rssDlInfo.getDlUser());
        update.set("dl_passwd", rssDlInfo.getDlPasswd());
        update.set("status", rssDlInfo.getStatus());
        update.set("ts", rssDlInfo.getTs());
        return mongoTemplate.findAndModify(query, update, RssDlInfo.class);
    }
}
