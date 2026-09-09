package com.coolkid.coolkidrss.dao;

import com.coolkid.coolkidrss.entity.RssDlLog;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;
import reactor.core.publisher.Mono;

import java.util.Date;

/**
 * @author Coolkid
 * @version 1.0
 * @date 2024/8/12
 */
public interface RssDlLogRepository extends ReactiveMongoRepository<RssDlLog, String> {
    /**
     * 删除数据
     * @param date
     * @return
     */
    Mono<Void> deleteByDlDateBefore(Date date);

    /**
     * 查询删除数据总数
     * @param date
     * @return
     */
    Mono<Long> countByDlDateBefore(Date date);
}
