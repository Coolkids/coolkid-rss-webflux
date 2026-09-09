package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.dao.RssDlInfoRepository;
import com.coolkid.coolkidrss.dao.RssDlLogRepository;
import com.coolkid.coolkidrss.dao.RssFeedInfoRepository;
import com.coolkid.coolkidrss.dao.RssFeedRecordRepository;
import com.coolkid.coolkidrss.download.qbittorrent.QbDownloadServer;
import com.coolkid.coolkidrss.service.BittorrentService;
import com.coolkid.coolkidrss.util.DLTOOLS;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class QbittorrentServiceImpl extends BittorrentService {

    public QbittorrentServiceImpl(
            RssFeedRecordRepository rssFeedRecordRepository,
            RssFeedInfoRepository rssFeedInfoRepository,
            RssDlInfoRepository rssDlInfoRepository,
            RssDlLogRepository rssDlLogRepository,
            QbDownloadServer downloadClientApi
    ) {
        this.rssFeedRecordRepository = rssFeedRecordRepository;
        this.rssFeedInfoRepository = rssFeedInfoRepository;
        this.rssDlInfoRepository = rssDlInfoRepository;
        this.rssDlLogRepository = rssDlLogRepository;
        this.downloadClientApi = downloadClientApi;
    }

    @Override
    public int type() {
        return DLTOOLS.QBT.getType();
    }

    @Override
    public String name() {
        return DLTOOLS.QBT.getName();
    }
}
