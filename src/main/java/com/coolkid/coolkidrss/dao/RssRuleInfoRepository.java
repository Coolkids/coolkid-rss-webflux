package com.coolkid.coolkidrss.dao;

import com.coolkid.coolkidrss.entity.RssRuleInfo;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Flux;

/**
 * @author Coolkid
 * @version 1.0
 * @date 2024/8/12
 */
public interface RssRuleInfoRepository extends ReactiveMongoRepository<RssRuleInfo, String> {
    Flux<RssRuleInfo> findByStatus(int status);

    Flux<RssRuleInfo> findByRuleTitleLike(String ruleTitle, Sort sort);
    Flux<RssRuleInfo> findByRuleTitleLike(String ruleTitle);
}
