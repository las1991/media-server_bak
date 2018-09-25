package com.sengled.media.bootstrap.cache;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.data.redis.connection.RedisConnection;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.serializer.Jackson2JsonRedisSerializer;
import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.PropertyAccessor;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * 专门用于 spring cache 缓存的 redis template 
 * @author las
 * @author chenxh
 */
public class SpringCacheRedisTemplate extends StringRedisTemplate {
    private static final boolean TRANSACTION_SUPPORTED = Boolean.valueOf(System.getProperty("spring.cache.transaction.supported", "false"));
    final Long deleteNone = 0L;
    
    public SpringCacheRedisTemplate(RedisConnectionFactory factory) {
        super(factory);
        
        // 必须支持事务，否则同一个线程执行多次删除会出错
        setEnableTransactionSupport(TRANSACTION_SUPPORTED);
        
        Jackson2JsonRedisSerializer jackson2JsonRedisSerializer = new Jackson2JsonRedisSerializer(Object.class);
        ObjectMapper om = new ObjectMapper();
        om.setVisibility(PropertyAccessor.ALL, JsonAutoDetect.Visibility.ANY);
        om.configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        om.enableDefaultTyping(ObjectMapper.DefaultTyping.NON_FINAL);
        jackson2JsonRedisSerializer.setObjectMapper(om);
        setValueSerializer(jackson2JsonRedisSerializer);
    }
    
    protected RedisConnection createRedisConnectionProxy(RedisConnection pm) {
        RedisConnection conn = super.createRedisConnectionProxy(pm);
        
        return (RedisConnection) Proxy.newProxyInstance(getClass().getClassLoader(), new Class[]{RedisConnection.class}, new InvocationHandler() {
            
            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {

                // conn.del(keys):Long
                if (method.getName().equals("del") && args.length == 1) {
                    byte[][] keys = (byte[][]) (args[0]);
                    
                    List<byte[]> strKeys = new ArrayList<>();
                    for (int i = 0; i < keys.length; i++) {
                        byte[] pattern = keys[i];
                        if (new String(pattern).endsWith("*")) {
                            Set<byte[]> keySet = conn.keys(pattern);
                            if (null != keySet){
                                strKeys.addAll(keySet);
                            }
                        } else {
                            strKeys.add(pattern);
                        }
                    }
                    
                    if (strKeys.isEmpty()) {
                        return deleteNone; // 返回 long 类型
                    }
                    return conn.del(strKeys.toArray(new byte[strKeys.size()][]));
                }
                
                Object result = method.invoke(conn, args);
                
                // conn.exists 返回 null
                if (null == result && method.getReturnType().equals(Boolean.class)) {
                    return false;
                }

                return result;
            }
        });
        
    }

}
