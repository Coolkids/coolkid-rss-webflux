package com.coolkid.coolkidrss.aop;

import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.Cache;
import org.springframework.cache.CacheManager;
import org.springframework.cache.interceptor.CacheOperationInvocationContext;
import org.springframework.cache.interceptor.SimpleCacheResolver;
import org.springframework.core.annotation.AnnotatedElementUtils;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.redis.cache.RedisCache;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.util.ReflectionUtils;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

@Slf4j
public class RedisExpireCacheResolver extends SimpleCacheResolver {
    public RedisExpireCacheResolver(CacheManager cacheManager){
        super(cacheManager);
    }
    @Override
    public Collection<? extends Cache> resolveCaches(CacheOperationInvocationContext<?> context) {
        Collection<String> cacheNames = getCacheNames(context);
        if (cacheNames == null) {
            return Collections.emptyList();
        }
        Collection<Cache> result = new ArrayList<>(cacheNames.size());
        for (String cacheName : cacheNames) {
            Cache cache = getCacheManager().getCache(cacheName);
            if (cache == null) {
                throw new IllegalArgumentException("Cannot find cache named '" +
                        cacheName + "' for " + context.getOperation());
            }
            // 获取到Cache对象后，开始解析 @CacheExpire
            parseCacheExpire(cache,context);
            result.add(cache);
        }
        return result;
    }

    private void parseCacheExpire(Cache cache,CacheOperationInvocationContext<?> context){
        Method method= context.getMethod();
        // 方法上是否标注了CacheExpire
        if(AnnotatedElementUtils.isAnnotated(method,CacheExpire.class)){
            // 获取对象
            CacheExpire cacheExpire= AnnotationUtils.getAnnotation(method,CacheExpire.class);
            log.info("CacheExpire ttl:{}, CacheExpire unit:{}",cacheExpire.ttl(), cacheExpire.unit());
            // 将 cache强制转换成 RedisCache，准备替换掉 配置
            RedisCache redisCache=(RedisCache) cache;
            Duration duration=Duration.ofMillis(cacheExpire.unit().toMillis(cacheExpire.ttl()));
            // 替换RedisCacheConfiguration 对象
            setRedisCacheConfiguration(redisCache,duration);
        }
    }

    // 替换RedisCacheConfiguration 对象
    private void setRedisCacheConfiguration(RedisCache redisCache, Duration duration){
        RedisCacheConfiguration defaultConfiguration=redisCache.getCacheConfiguration();
        RedisCacheConfiguration configuration = RedisCacheConfiguration.defaultCacheConfig();
        configuration = configuration.serializeValuesWith
                        (defaultConfiguration.getValueSerializationPair())
                .entryTtl(duration);
        //反射设置新的值
        Field configField = ReflectionUtils.findField(RedisCache.class,"cacheConfiguration", RedisCacheConfiguration.class);
        if(Objects.isNull(configField)){
            return;
        }
        ReflectionUtils.makeAccessible(configField);
        ReflectionUtils.setField(configField,redisCache,configuration);
    }
}
