package com.coolkid.coolkidrss.dao;

import com.coolkid.coolkidrss.entity.RssDlInfo;
import org.springframework.data.mongodb.repository.ReactiveMongoRepository;

/**
 * @author Coolkid
 * @version 1.0
 * @date 2024/8/12
 */
public interface RssDlInfoRepository extends ReactiveMongoRepository<RssDlInfo, String> {

}
