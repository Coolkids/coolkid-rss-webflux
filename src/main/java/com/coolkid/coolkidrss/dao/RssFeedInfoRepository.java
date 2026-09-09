package com.coolkid.coolkidrss.dao;

import com.coolkid.coolkidrss.entity.RssFeedInfo;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * @author Coolkid
 * @version 1.0
 * @date 2024/8/12
 */
public interface RssFeedInfoRepository extends ReactiveMongoRepository<RssFeedInfo, String> {

    Flux<RssFeedInfo> findByStatus(int status);

    Mono<RssFeedInfo> findTopByOrderBySortOnDesc();

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : {'sort_on': ?1} }")
    Mono<Long> updateSortOnById(String feedId, int sortOn);

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : {'status': ?1} }")
    Mono<Long> updateStatusByEmail(String feedId, int status);

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : {'feed_last_update': ?1,'feed_next_update': ?2,'ts': ?3} }")
    Mono<Long> updateDateById(String feedId, Date feedLastUpdate, Date feedNextUpdate, Date ts);

    Mono<RssFeedInfo> findByFeedIdAndStatus(String feedId, int status);
}
