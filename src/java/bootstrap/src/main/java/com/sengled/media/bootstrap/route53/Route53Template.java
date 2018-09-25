package com.sengled.media.bootstrap.route53;

import java.net.InetAddress;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;

class Route53Template implements DomainTemplate {
    private static final Logger LOGGER = LoggerFactory.getLogger(Route53Template.class);
    private final Cache<String, String> cache = CacheBuilder.newBuilder().maximumSize(8 * 1024).expireAfterWrite(0, TimeUnit.DAYS).build();
    

    private String domainPrefix = "m3-";
    private String domainSuffix = ".cloud.sengled.com";
    
    public Route53Template(String domainPrefix, String domainSuffix) {
        this.domainPrefix = domainPrefix;
        this.domainSuffix = domainSuffix;
    }

    /* (non-Javadoc)
     * @see com.sengled.media.bootstrap.route53.DomainTemplate#getDomain(java.lang.String)
     */
    @Override
    public final String getDomain(final String ip) {
        final String domain = getDomainString(ip);
        
        try {
            // 对 host 作二次验证
            String hostAddress =
            cache.get(domain, new Callable<String>() {
                @Override
                public String call() throws Exception {
                    InetAddress address = InetAddress.getByName(domain);
                    
                    if (StringUtils.equals(ip, address.getHostAddress())) {
                        return ip;
                    }
                    
                    throw new IllegalArgumentException( domain + " address is " + address.getHostAddress() + ", excepted is " + ip);
                }
            });
            
            // ip 和 域名匹配
            if (StringUtils.equals(hostAddress, ip)) {
                return domain;
            }

        } catch (ExecutionException ex) {
            LOGGER.warn("{}", ex.getCause().getMessage());
        }catch (Throwable ex) {
            LOGGER.error("{}", ex.getCause().getMessage());
        }
        
        // ip 和 域名不匹配
        return ip;
    }

    final String getDomainString(String ip) {
        return domainPrefix + StringUtils.replaceChars(ip, '.', '-') + domainSuffix;
    }
}
