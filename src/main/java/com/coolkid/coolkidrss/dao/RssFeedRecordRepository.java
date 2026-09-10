package com.coolkid.coolkidrss.dao;

import com.coolkid.coolkidrss.entity.RssFeedRecord;
import org.springframework.data.mongodb.repository.Aggregation;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import org.springframework.data.mongodb.repository.Update;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.Date;

/**
 * @author Coolkid
 * @version 1.0
 * @date 2024/8/12
 */
public interface RssFeedRecordRepository extends ReactiveMongoRepository<RssFeedRecord, String> {
    Mono<Void> deleteByTsBefore(Date date);

    Mono<Long> countByTsBefore(Date date);

    Mono<Void> deleteByFeedIdNotIn(Collection<String> feedIds);

    Mono<Long> countByFeedIdNotIn(Collection<String> feedIds);

    Flux<RssFeedRecord> findByFeedId(String feedId);

    Flux<RssFeedRecord> findByFeedIdAndRecordIsdl(String feedId, int recordIsdl);

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : { 'record_isdl' : ?1, 'record_readate' : ?2 } }")
    Mono<Long> updateDlbyId(String recordId, int isDl, Date readate);

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : { 'record_readate' : ?1 } }")
    Mono<Long> updateRecordReadateById(String recordId, Date readate);

    @Query("{ '_id' : ?0 }")
    @Update("{ '$set' : { 'record_fav' : ?1 } }")
    Mono<Long> updateRecordFavById(String recordId, int fav);


    @Query("{ 'feed_id' : ?0 }")
    @Update("{ '$set' : { 'record_readate' : ?1 } }")
    Mono<Long> updateRecordReadateByFeedId(String feedId, Date readate);

    @Aggregation(pipeline = {
            "{ $match: { 'record_readate' : null, 'feed_id' : ?0 } }",
            "{ $count: 'total' }"
    })
    Mono<Long> countUnRead(String feedId);
}
