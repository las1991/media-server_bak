package com.sengled.media.bootstrap.cache;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.interceptor.KeyGenerator;

/**
 * 从 Mybatis 的接口定义中，提取注解 @Param("deviceId") 指定的 device id
 * @author chenxh
 */
public class RedisKeyGenerator implements KeyGenerator {
    private static final Logger LOGGER = LoggerFactory.getLogger(RedisKeyGenerator.class);
    
    public static final String BEAN_NAME = "RedisKeyGenerator";

    private final String version;
    
    public RedisKeyGenerator(String version) {
        this.version = version;
    }
    
    @Override
    public Object generate(Object target, Method method, Object... params) {
        Annotation[][] annons = method.getParameterAnnotations();
        for (int i = 0; i < annons.length; i++) {
            for (int j = 0; j < annons[i].length; j++) {
                if (annons[i][j].annotationType().isAssignableFrom(CacheModel.class)) {
                    CacheModel annotation = (CacheModel) annons[i][j];
                    String key = String.valueOf(params[i]);
                    
                    String model = getModelName(annotation, method.getName());
                    
                    String generated;
                    if ("*".equals(model)) {
                        generated = key + ":v*";
                    } else {
                        generated = key + ":v" + version + ":" + model;
                    }
                    
                    LOGGER.debug("key [{}], {}", generated, method);
                    return generated;
                }
            }
        }

        CacheModel key = method.getAnnotation(CacheModel.class);
        if (null == key) {
            throw new IllegalArgumentException("@CacheKey NOT found");
        }
        return key.value();
    }



    private String getModelName(CacheModel annotation, String defaultName) {
        return StringUtils.isNotEmpty(annotation.value()) ? annotation.value() : defaultName;
    }

}
