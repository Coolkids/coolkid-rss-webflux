package com.coolkid.coolkidrss.aop;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.TimeUnit;

@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface CacheExpire {
    /**
     * 失效时间，默认是60
     */
    long ttl() default 60L;
    /**
     * 单位，默认是秒
     */
    TimeUnit unit() default TimeUnit.SECONDS;
}
