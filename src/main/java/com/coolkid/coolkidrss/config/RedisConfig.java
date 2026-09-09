package com.coolkid.coolkidrss.config;

import com.coolkid.coolkidrss.aop.RedisExpireCacheResolver;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheResolver;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.cache.RedisCacheWriter;
import org.springframework.data.redis.connection.ReactiveRedisConnectionFactory;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.serializer.RedisSerializationContext;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.io.Serializable;
import java.time.Duration;
import java.util.Objects;

@Configuration
public class RedisConfig {
    @Bean
    public <T extends Serializable> ReactiveRedisTemplate<String, T> reactiveRedisTemplate(ReactiveRedisConnectionFactory connectionFactory) {
        RedisSerializationContext<String, T> serializationContext = RedisSerializationContext
                .<String, T>newSerializationContext(new StringRedisSerializer())
                .value(new KryoRedisSerializer<>())
                .build();

        return new ReactiveRedisTemplate<>(connectionFactory, serializationContext);
    }

    @Bean
    public <T extends Serializable> RedisTemplate<String, T> redisTemplate(RedisConnectionFactory redisConnectionFactory) {
        RedisTemplate<String, T> template = new RedisTemplate<>();
        template.setConnectionFactory(redisConnectionFactory);
        // redis value使用的序列化器
        template.setValueSerializer(new KryoRedisSerializer<>());
        // redis key使用的序列化器
        template.setKeySerializer(new StringRedisSerializer());
        template.afterPropertiesSet();
        return template;
    }

    @Bean
    public <T extends Serializable> RedisCacheManager redisCacheManager(RedisTemplate<String, T> redisTemplate) {
        RedisCacheWriter redisCacheWriter = RedisCacheWriter.nonLockingRedisCacheWriter(Objects.requireNonNull(redisTemplate.getConnectionFactory()));
        RedisCacheConfiguration redisCacheConfiguration = RedisCacheConfiguration.defaultCacheConfig()
                .serializeValuesWith(RedisSerializationContext.SerializationPair.fromSerializer(redisTemplate.getValueSerializer()))
                .entryTtl(Duration.ofMinutes(3L));

        return new RedisCacheManager(redisCacheWriter, redisCacheConfiguration);
    }
    @Bean
    public CacheResolver redisExpireCacheResolver(CacheManager cacheManager) {
        return new RedisExpireCacheResolver(cacheManager);
    }
}
