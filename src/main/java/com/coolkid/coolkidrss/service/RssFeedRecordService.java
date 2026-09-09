package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.entity.RssRuleInfo;
import com.coolkid.coolkidrss.model.RuleFilter;
import com.coolkid.coolkidrss.model.request.FeedRecordReq;
import com.coolkid.coolkidrss.model.request.TestRuleReq;
import com.coolkid.coolkidrss.model.response.Page;
import com.coolkid.coolkidrss.util.EasyUtil;
import com.google.common.collect.Lists;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.*;

/**
 * @author coolk
 * @version 1.0
 * @date 2024/8/12
 */

@Service
@Slf4j
@Data
public class RssFeedRecordService {
    private final ReactiveMongoTemplate mongoTemplate;

    public Mono<Page<RssFeedRecord>> page(FeedRecordReq feedRecordReq) {
        return count(feedRecordReq).zipWhen(
                t -> page(feedRecordReq,
                        PageRequest.of(
                                feedRecordReq.getPage() - 1,
                                feedRecordReq.getPageSize(),
                                Sort.by(Sort.Direction.DESC, "record_pubdate")
                        )
                ).collectList(), (t1, t2) -> {
                    Page<RssFeedRecord> page = new Page<>();
                    page.setTotal(t1);
                    page.setRecords(t2);
                    page.setCurrent(feedRecordReq.getPage());
                    page.setSize(feedRecordReq.getPageSize());
                    return page;
                });
    }

    public Mono<Long> count(FeedRecordReq feedRecordReq) {
        Query query = buildQuery(feedRecordReq);
        return mongoTemplate.count(query, RssFeedRecord.class);
    }

    public Flux<RssFeedRecord> query(FeedRecordReq feedRecordReq) {
        Query query = buildQuery(feedRecordReq);
        query.with(Sort.by(Sort.Direction.DESC, "record_pubdate"));
        return mongoTemplate.find(query, RssFeedRecord.class);
    }

    public Flux<RssFeedRecord> query(RssRuleInfo rssRuleInfo) {
        Query query = buildQuery(rssRuleInfo);
        query.with(Sort.by(Sort.Direction.DESC, "record_pubdate"));
        return mongoTemplate.find(query, RssFeedRecord.class);
    }

    public Flux<RssFeedRecord> page(FeedRecordReq feedRecordReq, Pageable pageable) {
        Query query = buildQuery(feedRecordReq);
        query.with(pageable);
        return mongoTemplate.find(query, RssFeedRecord.class);
    }

    public Query buildQuery(RssRuleInfo rssRuleInfo) {
        List<Criteria> criterias = new ArrayList<>();
        criterias.add(buildFeedIdCriteria(rssRuleInfo));
        criterias.add(Criteria.where("record_isdl").is(0));
        Integer ruleType = rssRuleInfo.getRuleType();
        if(Integer.valueOf(0).equals(ruleType)) {
            criterias.addAll(buildNeedCriteria(rssRuleInfo));
            criterias.add(buildNeedOrCriteria(rssRuleInfo));
            criterias.addAll(buildNotNeedCriteria(rssRuleInfo));
            criterias.add(buildNotNeedOrCriteria(rssRuleInfo));
        }else {
            criterias.add(Criteria.where("record_title").regex(rssRuleInfo.getRuleParam()));
        }

        Query query = new Query();
        Criteria total = new Criteria();
        total.andOperator(criterias.stream().filter(Objects::nonNull).toList());
        query.addCriteria(total);
        return query;
    }

    public Query buildQuery(FeedRecordReq feedRecordReq) {
        List<Criteria> criterias = new ArrayList<>();
        criterias.add(buildFeedIdCriteria(feedRecordReq));
        criterias.add(buildTimeRangeCriteria(feedRecordReq, "record_pubdate"));
        criterias.add(buildUnReadCriteria(feedRecordReq));
        criterias.add(buildFavCriteria(feedRecordReq));
        criterias.addAll(buildNeedCriteria(feedRecordReq));
        criterias.add(buildNeedOrCriteria(feedRecordReq));
        criterias.addAll(buildNotNeedCriteria(feedRecordReq));
        criterias.add(buildNotNeedOrCriteria(feedRecordReq));
        Query query = new Query();
        Criteria total = new Criteria();
        total.andOperator(criterias.stream().filter(Objects::nonNull).toList());
        query.addCriteria(total);
        return query;
    }


    public Criteria buildTimeRangeCriteria(FeedRecordReq feedRecordReq, String key) {
        Date startDate = feedRecordReq.getStartDate();
        Date endDate = feedRecordReq.getEndDate();
        if (Objects.nonNull(startDate) && Objects.nonNull(endDate)) {
            startDate = DateUtils.setHours(startDate, 0);
            startDate = DateUtils.setMinutes(startDate, 0);
            startDate = DateUtils.setSeconds(startDate, 0);
            endDate = DateUtils.setHours(endDate, 23);
            endDate = DateUtils.setMinutes(endDate, 59);
            endDate = DateUtils.setSeconds(endDate, 59);
            return Criteria.where(key).gte(startDate).lte(endDate);
        } else {
            return null;
        }
    }

    public Criteria buildUnReadCriteria(FeedRecordReq feedRecordReq) {
        final boolean unread = feedRecordReq.isUnread();
        if (unread) {
            return new Criteria().orOperator(
                    Criteria.where("record_readate").exists(false),
                    Criteria.where("record_readate").isNull()
            );
        }
        return null;
    }

    public Criteria buildFavCriteria(FeedRecordReq feedRecordReq) {
        final boolean fav = feedRecordReq.isFav();
        if (fav) {
            return Criteria.where("record_fav").is(1);
        }
        return null;
    }

    public Criteria buildFeedIdCriteria(FeedRecordReq feedRecordReq) {
        final String feedId = feedRecordReq.getFeedId();
        if (StringUtils.isNotBlank(feedId)) {
            return Criteria.where("feed_id").is(feedId);
        } else {
            return null;
        }
    }
    public Criteria buildFeedIdCriteria(RssRuleInfo rssRuleInfo) {
        final List<String> feedIds = rssRuleInfo.getFeedIds();
        if (CollectionUtils.isNotEmpty(feedIds)) {
            return Criteria.where("feed_id").in(feedIds);
        } else {
            return null;
        }
    }

    public List<Criteria> buildNeedCriteria(FeedRecordReq feedRecordReq) {
        String keywords = feedRecordReq.getKeywords();
        return buildNeedCriteriaStr(keywords);
    }

    public List<Criteria> buildNeedCriteriaStr(String keywords){
        if (StringUtils.isBlank(keywords)) {
            return Collections.emptyList();
        }
        RuleFilter ruleFilter = EasyUtil.expressionRulePattern(keywords);
        List<String> need = ruleFilter.getNeed();
        if (CollectionUtils.isEmpty(need)) {
            return Collections.emptyList();
        }
        Criteria[] includeCriteriaArray = need.stream()
                .map(cond -> Criteria.where("record_title").regex(cond, "i"))
                .toArray(Criteria[]::new);
        return Arrays.stream(includeCriteriaArray).toList();
    }


    public Criteria buildNeedOrCriteria(FeedRecordReq feedRecordReq) {
        String keywords = feedRecordReq.getKeywords();
        return buildNeedOrCriteriaStr(keywords);
    }

    public Criteria buildNeedOrCriteriaStr(String keywords){
        if (StringUtils.isBlank(keywords)) {
            return null;
        }
        RuleFilter ruleFilter = EasyUtil.expressionRulePattern(keywords);
        List<String> needor = ruleFilter.getNeedor();
        if (CollectionUtils.isEmpty(needor)) {
            return null;
        }
        Criteria[] includeCriteriaArray = needor.stream()
                .map(cond -> Criteria.where("record_title").regex(cond, "i"))
                .toArray(Criteria[]::new);
        return new Criteria().orOperator(includeCriteriaArray);
    }

    public List<Criteria> buildNotNeedCriteria(FeedRecordReq feedRecordReq) {
        String keywords = feedRecordReq.getKeywords();
        return buildNotNeedCriteriaStr(keywords);
    }

    public List<Criteria> buildNotNeedCriteriaStr(String keywords){
        if (StringUtils.isBlank(keywords)) {
            return Lists.newArrayList();
        }
        RuleFilter ruleFilter = EasyUtil.expressionRulePattern(keywords);
        List<String> notNeed = ruleFilter.getNotneed();
        if (CollectionUtils.isEmpty(notNeed)) {
            return Lists.newArrayList();
        }
        Criteria[] mustNotContainCriteriaArray = notNeed.stream()
                .map(cond -> Criteria.where("record_title").not().regex(cond, "i"))
                .toArray(Criteria[]::new);
        return Arrays.stream(mustNotContainCriteriaArray).toList();
    }

    public Criteria buildNotNeedOrCriteria(FeedRecordReq feedRecordReq) {
        String keywords = feedRecordReq.getKeywords();
        return buildNotNeedOrCriteriaStr(keywords);
    }

    public List<Criteria> buildNeedCriteria(RssRuleInfo rssRuleInfo) {
        String keywords = rssRuleInfo.getRuleParam();
        return buildNeedCriteriaStr(keywords);
    }


    public Criteria buildNeedOrCriteria(RssRuleInfo rssRuleInfo) {
        String keywords = rssRuleInfo.getRuleParam();
        return buildNeedOrCriteriaStr(keywords);
    }

    public List<Criteria> buildNotNeedCriteria(RssRuleInfo rssRuleInfo) {
        String keywords = rssRuleInfo.getRuleParam();
        return buildNotNeedCriteriaStr(keywords);
    }

    public Criteria buildNotNeedOrCriteria(RssRuleInfo rssRuleInfo) {
        String keywords = rssRuleInfo.getRuleParam();
        return buildNotNeedOrCriteriaStr(keywords);
    }

    public Criteria buildNotNeedOrCriteriaStr(String keywords) {
        if (StringUtils.isBlank(keywords)) {
            return null;
        }
        RuleFilter ruleFilter = EasyUtil.expressionRulePattern(keywords);
        List<String> notneedor = ruleFilter.getNotneedor();
        if (CollectionUtils.isEmpty(notneedor)) {
            return null;
        }
        Criteria[] includeCriteriaArray = notneedor.stream()
                .map(cond -> Criteria.where("record_title").regex(cond, "i"))
                .toArray(Criteria[]::new);
        return new Criteria().norOperator(includeCriteriaArray);
    }

    public Mono<Void> insertIgnore(List<RssFeedRecord> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Mono.empty();
        }
        return Flux.fromIterable(items)
                .flatMap(this::saveNotUpdate)
                .then();
    }


    public Mono<RssFeedRecord> saveNotUpdate(RssFeedRecord entity) {
        Query query = new Query();

        query.addCriteria(Criteria.where("record_sha256").is(entity.getRecordSha256()));
        return mongoTemplate.exists(query, RssFeedRecord.class)
                .flatMap(exists -> Boolean.TRUE.equals(exists) ? Mono.empty() : mongoTemplate.save(entity));
    }

    public Mono<RssFeedRecord> saveOrUpdate(RssFeedRecord entity) {
        Query query = new Query();

        query.addCriteria(Criteria.where("record_sha256").is(entity.getRecordSha256()));

        Update update = new Update();
        update.set("ts", new Date());

        return mongoTemplate.findAndModify(query, update, RssFeedRecord.class)
                .switchIfEmpty(mongoTemplate.save(entity));
    }

    public Mono<Void> saveOrUpdate(List<RssFeedRecord> items) {
        if (CollectionUtils.isEmpty(items)) {
            return Mono.empty();
        }
        return Flux.fromIterable(items)
                .flatMap(this::saveOrUpdate)
                .then();
    }

    public Flux<RssFeedRecord> queryByFilter(TestRuleReq testRuleReq){
        List<String> feedIds = testRuleReq.getFeedIds();
        String ruleParam = testRuleReq.getRuleParam();
        if(CollectionUtils.isEmpty(feedIds)){
            return Flux.empty();
        }
        return Flux.fromIterable(feedIds)
        .flatMap(t -> {
            FeedRecordReq feedRecordReq = new FeedRecordReq();
            feedRecordReq.setFeedId(t);
            feedRecordReq.setKeywords(ruleParam);
            return query(feedRecordReq);
        }).collectList().flatMapMany(Flux::fromIterable);
    }

    public Mono<Void> updateDownStatusByRecordTitle(String recordTitle, int i) {
        Query query = new Query();
        query.addCriteria(Criteria.where("record_title").is(recordTitle));
        Update update = new Update();
        update.set("record_readate", new Date());
        update.set("record_isdl", i);
        return mongoTemplate.updateMulti(query, update, RssFeedRecord.class).then();
    }
}
