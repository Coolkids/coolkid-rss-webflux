package com.coolkid.coolkidrss.service;

import com.coolkid.coolkidrss.entity.AnitopyTmdbMiss;
import com.coolkid.coolkidrss.entity.RssFeedRecord;
import com.coolkid.coolkidrss.model.request.AnitopyTmdbMissReq;
import com.coolkid.coolkidrss.model.response.Page;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;

import java.util.Date;
import java.util.Map;
import java.util.Objects;
import java.util.regex.Pattern;

import com.coolkid.coolkidrss.util.RssIdUtil;

/** 保存和查询 anitopy-ml 解析后未匹配到 TMDB 的标题。 */
@Slf4j
@Service
public class AnitopyTmdbMissService {
    private static final int MAX_PAGE_SIZE = 100;

    private final ReactiveMongoTemplate mongoTemplate;

    public AnitopyTmdbMissService(ReactiveMongoTemplate mongoTemplate) {
        this.mongoTemplate = mongoTemplate;
    }

    /**
     * 按 feed 和原始标题幂等保存一次未匹配结果，并更新最近解析值和出现次数。
     * 该方法运行在影视 Feed 的 boundedElastic 线程中，可以安全等待 Mongo 写入完成。
     */
    public void record(RssFeedRecord record, Map<String, Object> anitopyResult) {
        if (record == null || StringUtils.isBlank(record.getRecordTitle())) {
            return;
        }
        Date now = new Date();
        Query query = Query.query(new Criteria()
                .andOperator(
                        Criteria.where("feed_id").is(record.getFeedId()),
                        Criteria.where("record_title").is(record.getRecordTitle())
                ));
        Update update = new Update()
                .set("record_id", record.getRecordId())
                .set("anitopy_result", anitopyResult)
                .set("last_seen_at", now)
                .inc("seen_count", 1)
                .setOnInsert("_id", RssIdUtil.nextIdStr())
                .setOnInsert("feed_id", record.getFeedId())
                .setOnInsert("record_title", record.getRecordTitle())
                .setOnInsert("first_seen_at", now);
        try {
            mongoTemplate.findAndModify(
                            query,
                            update,
                            FindAndModifyOptions.options().upsert(true).returnNew(true),
                            AnitopyTmdbMiss.class)
                    .block();
        } catch (RuntimeException e) {
            // 训练数据记录失败不能阻断 RSS 记录保存。
            log.warn("保存 TMDB 未匹配标题失败：{}", record.getRecordTitle(), e);
        }
    }

    public Mono<Page<AnitopyTmdbMiss>> page(AnitopyTmdbMissReq request) {
        AnitopyTmdbMissReq actual = Objects.requireNonNullElseGet(request, AnitopyTmdbMissReq::new);
        int current = Math.max(1, actual.getPage());
        int size = Math.min(MAX_PAGE_SIZE, Math.max(1, actual.getPageSize()));
        Query countQuery = buildQuery(actual);
        Query pageQuery = buildQuery(actual).with(PageRequest.of(
                current - 1,
                size,
                Sort.by(Sort.Direction.DESC, "last_seen_at")
        ));
        return mongoTemplate.count(countQuery, AnitopyTmdbMiss.class)
                .zipWith(mongoTemplate.find(
                                pageQuery,
                                AnitopyTmdbMiss.class).collectList())
                .map(result -> {
                    Page<AnitopyTmdbMiss> page = new Page<>();
                    page.setTotal(result.getT1());
                    page.setRecords(result.getT2());
                    page.setCurrent(current);
                    page.setSize(size);
                    return page;
                });
    }

    private Query buildQuery(AnitopyTmdbMissReq request) {
        Query query = new Query();
        if (StringUtils.isNotBlank(request.getFeedId())) {
            query.addCriteria(Criteria.where("feed_id").is(request.getFeedId().trim()));
        }
        if (StringUtils.isNotBlank(request.getTitle())) {
            query.addCriteria(Criteria.where("record_title")
                    .regex(Pattern.quote(request.getTitle().trim()), "i"));
        }
        return query;
    }
}
