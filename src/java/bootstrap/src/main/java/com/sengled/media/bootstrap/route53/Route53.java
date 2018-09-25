package com.sengled.media.bootstrap.route53;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.amazonaws.services.route53.AmazonRoute53;
import com.amazonaws.services.route53.AmazonRoute53ClientBuilder;
import com.amazonaws.services.route53.model.Change;
import com.amazonaws.services.route53.model.ChangeAction;
import com.amazonaws.services.route53.model.ChangeBatch;
import com.amazonaws.services.route53.model.ChangeInfo;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsRequest;
import com.amazonaws.services.route53.model.ChangeResourceRecordSetsResult;
import com.amazonaws.services.route53.model.RRType;
import com.amazonaws.services.route53.model.ResourceRecord;
import com.amazonaws.services.route53.model.ResourceRecordSet;
import com.sengled.media.bootstrap.AmazonAwsConfig;

@Configuration
@ConfigurationProperties()
public class Route53 implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(Route53.class);
    
    @Value("${PUBLIC_IPV4}")
    private String publicIP;
    
    @Autowired
    private AmazonAwsConfig config;

    
    @Value("${AWS_ROUTE53_HOSTED_ZONE_ID}")
    private String hostedZoneId;
    

    @Value("${domain.suffix:.cloud.sengled.com}")
    private String domainSuffix;
    

    @Value("${domain.prefix:m3-}")
    private String domainPrefix;
    
    
    private final ProxyRoute53Template proxyTemplate = new ProxyRoute53Template();
    
    @Override
    public void afterPropertiesSet() throws Exception {
        try {
            // 独立线程注册域名
            new Thread(new Runnable() {
                @Override
                public void run() {
                    try {
                        AmazonRoute53 route53 = AmazonRoute53ClientBuilder
                                .standard()
                                .withCredentials(config.getCredentialsProvider())
                                .withRegion(config.getRegion())
                                .build();

                        registDomain(route53);
                    } catch(Exception ex) {
                        LOGGER.warn("Fail regist domain for {}", ex.getMessage());
                    }
                }
            }).start();
            
        } catch (Exception ex) {
            LOGGER.warn("Route53 NOT supported", ex);
        }
    }


    private void registDomain(AmazonRoute53 route53) {
        Route53Template route53Template = new Route53Template(domainPrefix, domainSuffix);
        
        // 没有得到合法的域名地址， 则注册一个
        String domain = route53Template.getDomainString(publicIP);
        
        final Change change = new Change(ChangeAction.UPSERT,
                new ResourceRecordSet()
                    .withTTL(300L)
                    .withName(domain)
                    .withType(RRType.A)
                    .withResourceRecords(
                        new ResourceRecord().withValue(publicIP)));
        
        final ChangeBatch withChanges = new ChangeBatch().withChanges(change);
        final ChangeResourceRecordSetsRequest withChangeBatch;
        withChangeBatch = new ChangeResourceRecordSetsRequest()
                                .withHostedZoneId(hostedZoneId)
                                .withChangeBatch(withChanges);
        
        final ChangeResourceRecordSetsResult changeResourceRecordSets = route53.changeResourceRecordSets(withChangeBatch);
        ChangeInfo info = changeResourceRecordSets.getChangeInfo();
        LOGGER.info("set domain {}={}, status={}", domain, publicIP, info.getStatus());
        
        // 成功使用了 route53
        proxyTemplate.setProxy(route53Template);
        LOGGER.info("{} used", route53Template);
    }
    
    @Bean
    public DomainTemplate geRoute53Template() {
        return proxyTemplate;
    }
}
