package com.coolkid.coolkidrss.service.impl;


import com.coolkid.coolkidrss.dao.RssDlInfoRepository;
import com.coolkid.coolkidrss.dao.RssRuleInfoRepository;
import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.model.task.DownloadParam;
import com.coolkid.coolkidrss.service.BittorrentService;
import com.coolkid.coolkidrss.service.DownloadService;
import com.coolkid.coolkidrss.service.RssFeedRecordService;
import com.google.common.collect.Maps;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.function.Function;

@Slf4j
@Service
@Data
public class DownloadServiceImpl implements DownloadService {
    private final List<BittorrentService> bittorrentServices;
    private final RssRuleInfoRepository rssRuleInfoRepository;
    private final RssFeedRecordService rssFeedRecordService;
    private final RssDlInfoRepository rssDlInfoRepository;
    private final HashMap<Integer, Function<DownloadParam, Mono<Void>>> functionMap = Maps.newHashMap();


    @PostConstruct
    public void init() {
        for (BittorrentService item : bittorrentServices) {
            Function<DownloadParam, Mono<Void>> t = item::download;
            functionMap.put(item.type(), t);
        }
    }

    private Mono<Void> sendToDownload(DownloadParam downloadParam) {
        RssDlInfo rssDlInfo = downloadParam.getRssDlInfo();
        Function<DownloadParam, Mono<Void>> download = functionMap.get(rssDlInfo.getDlType());
        if (download == null) {
            return Mono.error(new IllegalArgumentException("不支持的下载器类型: " + rssDlInfo.getDlType()));
        }
        return download.apply(downloadParam);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> download() {
        return rssRuleInfoRepository.findByStatus(1)
                .flatMap(rssRuleInfo -> rssDlInfoRepository.findById(rssRuleInfo.getDlId())
                        .switchIfEmpty(Mono.error(new IllegalArgumentException("下载配置不存在: " + rssRuleInfo.getDlId())))
                        .flatMapMany(downloadInfo -> rssFeedRecordService.query(rssRuleInfo)
                                .flatMap(record -> {
                                    DownloadParam downloadParam = new DownloadParam();
                                    downloadParam.setRssRuleInfo(rssRuleInfo);
                                    downloadParam.setRssFeedRecord(record);
                                    downloadParam.setRssDlInfo(downloadInfo);
                                    log.info("规则:{}。下载:{}", rssRuleInfo.getRuleTitle(), record.getRecordTitle());
                                    return sendToDownload(downloadParam);
                                })))
                .then();
    }
}
