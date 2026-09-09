package com.coolkid.coolkidrss.aop;

import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.reflect.MethodSignature;
import org.redisson.api.RLock;
import org.redisson.api.RedissonClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.expression.Expression;
import org.springframework.expression.ExpressionParser;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.expression.spel.support.StandardEvaluationContext;
import org.springframework.stereotype.Component;

import java.util.concurrent.TimeUnit;

@Aspect
@Component
@Slf4j
public class DistributedLockAspect {

    private final RedissonClient redissonClient;

    @Autowired
    public DistributedLockAspect(RedissonClient redissonClient) {
        this.redissonClient = redissonClient;
    }

    @Around("@annotation(distributedLock)")
    public Object around(ProceedingJoinPoint joinPoint, DistributedLock distributedLock) throws Throwable {
        String lockName = distributedLock.name();
        boolean wait = distributedLock.isWait();
        TimeUnit timeUnit = distributedLock.timeUnit();
        int secound = distributedLock.secound();

        // Evaluate SpEL expression
        if (lockName.contains("#")) {
            lockName = getExpression(lockName, joinPoint);
        }

        lockName = "DistributedLock_" + lockName;

        String name = joinPoint.getSignature().getName();
        RLock lock = redissonClient.getLock(lockName);
        try {
            log.debug("进入到分布式锁:{}, 方法: {}", lockName, name);
            boolean locked = lock.isLocked();
            if (locked) {
                log.error("{} task is locked 方法: {}", lockName, name);
                return null;
            }
            boolean isLocked;
            if (wait) {
                isLocked = lock.tryLock(0, secound, timeUnit); // 尝试加锁，最多等待10秒
            } else {
                isLocked = lock.tryLock();
            }
            if (isLocked) {
                log.debug("获取到分布式锁:{}, 方法: {}", lockName, name);
                return joinPoint.proceed(); // 成功获取到锁，执行业务方法
            } else {
                // 加锁失败，记录日志并跳过执行
                log.error("Failed to acquire lock for method: {}", name);
                return null;
            }
        } catch (InterruptedException e) {
            log.warn("Interrupted!", e);
            // Restore interrupted state...
            Thread.currentThread().interrupt();
            return null;
        } catch (Exception e) {
            log.error("Failed to acquire lock for method: {}", name);
            log.error(e.getMessage(), e);
            return null;
        } finally {
            if (lock.isLocked() && lock.isHeldByCurrentThread()) {
                lock.unlock(); // 释放锁
            }
        }
    }

    private String resolveSpELExpression(String expression, ProceedingJoinPoint joinPoint) {
        StandardEvaluationContext context = new StandardEvaluationContext();

        Object[] args = joinPoint.getArgs();
        MethodSignature methodSignature = (MethodSignature) joinPoint.getSignature();
        String[] parameterNames = methodSignature.getParameterNames();

        for (int i = 0; i < args.length; i++) {
            String paramName = parameterNames[i];
            Object paramValue = args[i];
            context.setVariable(paramName, paramValue);
        }

        // Use Spring's SpEL parser to evaluate the expression
        ExpressionParser parser = new SpelExpressionParser();
        Expression exp = parser.parseExpression(expression);
        return exp.getValue(context, String.class);
    }

    private String getExpression(String expression, ProceedingJoinPoint joinPoint) {
        try {
            return resolveSpELExpression(expression, joinPoint);
        } catch (Exception e) {
            log.error(e.getMessage(), e);
            return expression;
        }
    }
}


