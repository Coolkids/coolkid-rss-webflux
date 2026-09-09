package com.coolkid.coolkidrss.service;


import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.response.RssRuleFeed;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface RssRuleInfoService {
    Flux<RssRuleInfo> findByRuleTitleLike(String ruleTitle);

    Flux<RssRuleFeed> getRuleFeed(String ruleId);

    Mono<RssRuleInfo> save(RssRuleInfo rssRuleInfo);

    Mono<Void> remove(String ruleId);
}
