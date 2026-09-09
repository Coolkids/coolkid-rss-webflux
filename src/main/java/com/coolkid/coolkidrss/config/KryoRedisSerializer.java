package com.coolkid.coolkidrss.config;

import com.coolkid.coolkidrss.util.KryoUtil;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.serializer.RedisSerializer;
import org.springframework.data.redis.serializer.SerializationException;

import java.io.Serializable;

@Slf4j
public class KryoRedisSerializer<T extends Serializable> implements RedisSerializer<T> {
    public byte[] serialize(T object) {
        return KryoUtil.serialize(object);
    }

    public T deserialize(byte[] bytes) throws SerializationException {
        return KryoUtil.deserialize(bytes);
    }
}
