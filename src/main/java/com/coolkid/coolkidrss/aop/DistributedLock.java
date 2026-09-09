package com.coolkid.coolkidrss.aop;

import org.intellij.lang.annotations.Language;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

/**
 * 分布式锁注解
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface DistributedLock {
    /**
     * 锁的名称
     * @return
     */
    @Language("SpEL")
    String name(); // 可以用来指定锁的名称

    /**
     * 是否等待获取锁
     * @return
     */
    boolean isWait() default false;

    /**
     * 等待时间单位
     * @return
     */
    TimeUnit timeUnit() default TimeUnit.SECONDS;

    /**
     * 等待时间
     * @return
     */
    int secound() default 5;
}
