package com.coolkid.coolkidrss.service;


import reactor.core.publisher.Mono;

public interface DownloadService {
    Mono<Void> download();
}
