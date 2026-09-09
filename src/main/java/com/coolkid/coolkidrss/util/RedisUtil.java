package com.coolkid.coolkidrss.util;

import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.redisson.api.RedissonClient;
import org.redisson.api.RScript;
import org.redisson.api.RScoredSortedSet;
import org.redisson.client.codec.StringCodec;
import org.springframework.data.redis.core.ReactiveRedisTemplate;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.Serializable;
import java.time.Duration;
import java.time.Instant;
import java.util.Collection;
import java.util.List;

@Component
@Slf4j
@Data
public class RedisUtil<T extends Serializable> {
    private static final String CLAIM_DUE_TASKS_SCRIPT = """
            local members = redis.call('ZRANGEBYSCORE', KEYS[1], '-inf', ARGV[1], 'LIMIT', 0, ARGV[2])
            if #members > 0 then
                redis.call('ZREM', KEYS[1], unpack(members))
            end
            return members
            """;

    private final ReactiveRedisTemplate<String, T> reactiveRedisTemplate;
    private final RedissonClient redissonClient;

    /**
     * 删除key
     *
     * @param key
     */
    public Mono<Boolean> delete(String key) {
        return reactiveRedisTemplate.delete(key).map(Boolean.TRUE::equals);
    }

    /**
     * 批量删除key
     *
     * @param keys
     */
    public Mono<Long> delete(Collection<String> keys) {
        if (CollectionUtils.isEmpty(keys)) {
            return Mono.just(0L);
        }
        return reactiveRedisTemplate.delete(Flux.fromIterable(keys));
    }

    /**
     * Set a value in Redis with no expiration time.
     *
     * @param key   the key
     * @param value the value
     * @return a Mono that completes when the operation is done
     */
    public Mono<Boolean> set(String key, T value) {
        return reactiveRedisTemplate.opsForValue().set(key, value);
    }

    /**
     * Set a value in Redis with an expiration time.
     *
     * @param key      the key
     * @param value    the value
     * @param duration the expiration time
     * @return a Mono that completes when the operation is done
     */
    public Mono<Boolean> set(String key, T value, Duration duration) {
        return reactiveRedisTemplate.opsForValue().set(key, value, duration);
    }

    public Mono<T> get(String key) {
        return reactiveRedisTemplate.opsForValue().get(key);
    }

    /**
     * 是否存在key
     */
    public Mono<Boolean> hasKey(String key) {
        return reactiveRedisTemplate.hasKey(key);
    }

    /**
     * 设置过期时间
     */
    public Mono<Boolean> expire(String key, Duration duration) {
        return reactiveRedisTemplate.expire(key, duration);
    }

    /**
     * 设置过期时间
     *
     */
    public Mono<Boolean> expireAt(String key, Instant date) {
        return reactiveRedisTemplate.expireAt(key, date);
    }

    /**
     * 查找匹配的key
     */
    public Flux<String> keys(String pattern) {
        return reactiveRedisTemplate.keys(pattern);
    }

    /**
     * 将任务加入延迟队列。同一 member 重复加入会更新 score，因此可用于修改更新时间。
     */
    public Mono<Void> schedule(String key, String member, Instant executeAt) {
        RScoredSortedSet<String> schedule = redissonClient.getScoredSortedSet(key, StringCodec.INSTANCE);
        return Mono.fromCompletionStage(() -> schedule.addAsync(executeAt.toEpochMilli(), member)).then();
    }

    /**
     * 从延迟队列移除任务，例如 Feed 被停用或删除时。
     */
    public Mono<Void> cancelSchedule(String key, String member) {
        RScoredSortedSet<String> schedule = redissonClient.getScoredSortedSet(key, StringCodec.INSTANCE);
        return Mono.fromCompletionStage(() -> schedule.removeAsync(member)).then();
    }

    /**
     * 原子领取最多 {@code limit} 个已到期任务。
     *
     * <p>读取和删除在同一个 Lua 脚本内完成，多个应用实例不会领取到相同任务。</p>
     */
    public Flux<String> claimDueSchedules(String key, Instant now, int limit) {
        if (limit <= 0) {
            return Flux.error(new IllegalArgumentException("limit 必须大于 0"));
        }
        return Mono.<List<String>>fromCompletionStage(() -> redissonClient.getScript(StringCodec.INSTANCE)
                        .<List<String>>evalAsync(
                                RScript.Mode.READ_WRITE,
                                CLAIM_DUE_TASKS_SCRIPT,
                                RScript.ReturnType.LIST,
                                List.of((Object) key),
                                String.valueOf(now.toEpochMilli()),
                                String.valueOf(limit)
                        ))
                .flatMapMany(Flux::fromIterable);
    }

}
