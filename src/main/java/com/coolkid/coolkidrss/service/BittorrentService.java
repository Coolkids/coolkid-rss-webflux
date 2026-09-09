package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.dao.RssDlInfoRepository;
import com.coolkid.coolkidrss.dao.RssDlLogRepository;
import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.dao.RssFeedRecordRepository;
import com.coolkid.coolkidrss.download.ClientSecret;
import com.coolkid.coolkidrss.download.DownloadClientApi;
import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssDlLog;
import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.request.DownloadRecordReq;
import com.coolkid.coolkidrss.model.task.DownloadParam;
import com.coolkid.coolkidrss.util.RssIdUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Date;

@Slf4j
public abstract class BittorrentService {
    protected RssFeedRecordRepository rssFeedRecordRepository;
    protected RssFeedInfoRepository rssFeedInfoRepository;
    protected RssDlInfoRepository rssDlInfoRepository;
    protected RssDlLogRepository rssDlLogRepository;
    protected DownloadClientApi downloadClientApi;

    public abstract int type();

    public abstract String name();

    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> download(DownloadParam downloadParam) {
        RssDlInfo rssDlInfo = downloadParam.getRssDlInfo();
        RssFeedRecord rssFeedRecord = downloadParam.getRssFeedRecord();
        RssRuleInfo rssRuleInfo = downloadParam.getRssRuleInfo();
        String dlUrl = rssDlInfo.getDlUrl();
        String dlUser = rssDlInfo.getDlUser();
        String dlPasswd = rssDlInfo.getDlPasswd();
        String ruleSavePath = rssRuleInfo.getRuleSavePath();

        return Mono.fromRunnable(() -> {
                    ClientSecret clientSecret = downloadClientApi.getClientSecret(dlUrl, dlUser, dlPasswd);
                    downloadClientApi.addTorrent(clientSecret, ruleSavePath, rssFeedRecord.getRecordDlurl());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then(updateDatabase(rssFeedRecord, rssRuleInfo));
    }

    public Mono<Void> download(RssDlInfo rssDlInfo, DownloadRecordReq downloadRecordReq) {
        String dlUrl = rssDlInfo.getDlUrl();
        String dlUser = rssDlInfo.getDlUser();
        String dlPasswd = rssDlInfo.getDlPasswd();
        String ruleSavePath = downloadRecordReq.getRuleSavePath();

        return Mono.fromRunnable(() -> {
                    ClientSecret clientSecret = downloadClientApi.getClientSecret(dlUrl, dlUser, dlPasswd);
                    downloadClientApi.addTorrent(clientSecret, ruleSavePath, downloadRecordReq.getDownUrl());
                })
                .subscribeOn(Schedulers.boundedElastic())
                .then();
    }

    public Mono<Void> updateDatabase(RssFeedRecord rssFeedRecords, RssRuleInfo rssRuleInfo) {
        log.info("写入下载日志");

        RssDlLog rssDlLog = new RssDlLog();
        rssDlLog.setLogId(RssIdUtil.nextIdStr());
        rssDlLog.setRecordTitle(rssFeedRecords.getRecordTitle());
        rssDlLog.setDlDate(new Date());
        rssDlLog.setFeedId(rssFeedRecords.getFeedId());
        return Mono.zip(
                        rssFeedInfoRepository.findById(rssFeedRecords.getFeedId()).defaultIfEmpty(new RssFeedInfo()),
                        rssDlInfoRepository.findById(rssRuleInfo.getDlId()).defaultIfEmpty(new RssDlInfo())
                )
                .flatMap(values -> {
                    rssDlLog.setFeedName(values.getT1().getFeedName());
                    rssDlLog.setRuleId(rssRuleInfo.getRuleId());
                    rssDlLog.setDlId(rssRuleInfo.getDlId());
                    rssDlLog.setRuleInfo(rssRuleInfo.getRuleTitle());
                    rssDlLog.setDlInfo(values.getT2().getDlName());
                    return Mono.when(
                            rssDlLogRepository.save(rssDlLog),
                            rssFeedRecordRepository.updateDlbyId(rssFeedRecords.getRecordId(), 1, new Date())
                    );
                })
                .then();
    }
}
