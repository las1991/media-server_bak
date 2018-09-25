package com.sengled.media.bootstrap.cache;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * 
 * @author chenxh
 * @see RedisKeyGenerator
 */
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.PARAMETER, ElementType.METHOD})
public @interface CacheModel {
    /**
     * 
     * @return '*' 代指所有模块，通常用于 EVIT操作
     * @author chenxh
     */
    String value();

}
