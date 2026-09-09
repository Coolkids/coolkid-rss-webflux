package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.RssFeedInfo;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.request.RssFeedSortReq;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.model.response.RssFeedInfoList;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface FeedService {
    Mono<RssFeedInfo> save(RssFeedInfo rssFeedInfo);

    Flux<RssFeedInfo> get(boolean full);

    Flux<RssFeedInfoList> getRessFeedInfoList(boolean full);

    Mono<Page<RssFeedRecord>> getRecord(FeedRecordReq feedRecordReq);

    Mono<Void> readRecord(String recordId);

    Mono<Void> allRead(String feedId);

    Mono<Void> remove(String feedId);

    Mono<Void> sortOn(RssFeedSortReq rssFeedSortReq);

    Mono<Void> flush(RssFeedInfo rssFeedInfo);

    Mono<Void> favRecord(String recordId, Integer fav);
}
