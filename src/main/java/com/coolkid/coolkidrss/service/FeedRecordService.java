package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.RssFeedInfo;
import reactor.core.publisher.Mono;

public interface FeedRecordService {
    Mono<Void> save(RssFeedInfo rssFeedInfo);
}
