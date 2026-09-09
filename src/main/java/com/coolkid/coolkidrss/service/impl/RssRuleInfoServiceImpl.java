package com.coolkid.coolkidrss.service.impl;

import com.coolkid.coolkidrss.dao.RssRuleInfoRepository;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.response.RssRuleFeed;
import com.coolkid.coolkidrss.service.RssRuleInfoService;
import com.coolkid.coolkidrss.util.RssIdUtil;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.List;

@Service
@Data
public class RssRuleInfoServiceImpl implements RssRuleInfoService {
    private final RssRuleInfoRepository ruleInfoRepository;
    private final ReactiveMongoTemplate mongoTemplate;


    @Override
    public Flux<RssRuleInfo> findByRuleTitleLike(String ruleTitle) {
        Sort sort = Sort.by(Sort.Direction.DESC, "ts");
        if (StringUtils.isNotBlank(ruleTitle)) {
            return ruleInfoRepository.findByRuleTitleLike(ruleTitle, sort);
        }
        return ruleInfoRepository.findAll(sort);
    }

    @Override
    public Flux<RssRuleFeed> getRuleFeed(String ruleId) {
        return ruleInfoRepository.findById(ruleId).map(t -> {
            List<String> feedIds = t.getFeedIds();
            return feedIds.stream().map(a -> {
                RssRuleFeed feed = new RssRuleFeed();
                feed.setRuleId(ruleId);
                feed.setFeedId(a);
                return feed;
            }).toList();
        }).flatMapMany(Flux::fromIterable);
    }


    @Override
    public Mono<RssRuleInfo> save(RssRuleInfo rssRuleInfo) {
        String ruleId = rssRuleInfo.getRuleId();
        if(StringUtils.isBlank(ruleId)){
            rssRuleInfo.setRuleId(RssIdUtil.nextIdStr());
            return ruleInfoRepository.save(rssRuleInfo);
        }
        return update(rssRuleInfo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public Mono<Void> remove(String ruleId) {
        return ruleInfoRepository.deleteById(ruleId);
    }

    private Mono<RssRuleInfo> update(RssRuleInfo rssRuleInfo){
        Query query = new Query();
        query.addCriteria(Criteria.where("_id").is(rssRuleInfo.getRuleId()));

        Update update = new Update();
        update.set("rule_title", rssRuleInfo.getRuleTitle());
        update.set("rule_param", rssRuleInfo.getRuleParam());
        update.set("rule_type", rssRuleInfo.getRuleType());
        update.set("rule_save_path", rssRuleInfo.getRuleSavePath());
        update.set("rule_save_param", rssRuleInfo.getRuleSaveParam());
        update.set("dl_id", rssRuleInfo.getDlId());
        update.set("status", rssRuleInfo.getStatus());
        update.set("ts", rssRuleInfo.getTs());
        update.set("feed_id", rssRuleInfo.getFeedIds());
        return mongoTemplate.findAndModify(query, update, RssRuleInfo.class)
                .switchIfEmpty(mongoTemplate.save(rssRuleInfo));
    }
}
