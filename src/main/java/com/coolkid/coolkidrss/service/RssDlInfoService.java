package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.RssDlInfo;
import com.coolkid.coolkidrss.entity.RssDlLog;
import com.coolkid.coolkidrss.model.request.DownloadRecordReq;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.response.Page;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RssDlInfoService {
    Mono<Page<RssDlLog>> getLogs(FeedRecordReq feedRecordReq);

    Mono<Void> download(DownloadRecordReq downloadRecordReq);

    Mono<Void> remove(String dlid);

    Flux<RssDlInfo> findAll();

    Mono<RssDlInfo> save(RssDlInfo rssDlInfo);
}
