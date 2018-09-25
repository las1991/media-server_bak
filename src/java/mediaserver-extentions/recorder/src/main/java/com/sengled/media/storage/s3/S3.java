package com.sengled.media.storage.s3;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.CollectionUtils;
import com.alibaba.fastjson.JSONObject;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.ClientConfigurationFactory;
import com.amazonaws.Protocol;
import com.amazonaws.auth.AWSCredentialsProvider;
import com.amazonaws.auth.AWSStaticCredentialsProvider;
import com.amazonaws.auth.BasicAWSCredentials;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.client.builder.AwsClientBuilder.EndpointConfiguration;
import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.AmazonS3ClientBuilder;
import com.amazonaws.services.s3.model.Bucket;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration;
import com.amazonaws.services.s3.model.BucketLifecycleConfiguration.Rule;
import com.amazonaws.services.s3.model.Tag;
import com.amazonaws.services.s3.model.lifecycle.LifecycleFilter;
import com.amazonaws.services.s3.model.lifecycle.LifecycleTagPredicate;

@Configuration
@ConfigurationProperties
public class S3 implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(S3.class);

    @Value("${MEDIA_AWS_REGION}")
    private String regionName;

    @Value("${aws_video_bucket}")
    private String awsVideoBucket;

    @Value("${deployment.target:prod}")
    private String deploymentTarget;

    private AmazonS3 s3;

    @Autowired
    StorageProperties storageProperties;

    @Override
    public void afterPropertiesSet() {
        LOGGER.info("Initializing...");
        try {
            initialize();
        } catch (Exception e) {
            LOGGER.error("Fail connect with S3 for '{}'.", e.getMessage(), e);
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void initialize() {
        ClientConfiguration conf = new ClientConfiguration();
        conf.setMaxErrorRetry(3);
        conf.setConnectionTimeout(15 * 1000);
        conf.setSocketTimeout(60 * 1000);
        conf.setProtocol(Protocol.HTTPS);
        conf.setMaxConnections(3 * ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        conf.setUseTcpKeepAlive(true);
        LOGGER.info("connect with S3,region = {}.", regionName);

        if (deploymentTarget.equals("local")) {
            String signingRegion = "regionName";
            String serviceEndpoint = "http://10.100.102.121:9000";
            EndpointConfiguration endpointConfiguration =
                    new EndpointConfiguration(serviceEndpoint, signingRegion);
            s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .withEndpointConfiguration(endpointConfiguration).withClientConfiguration(conf)
                    .build();
        } else {
            s3 = AmazonS3ClientBuilder.standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .withClientConfiguration(conf).withRegion(regionName).build();
            // 查看都有哪些桶
            List<Bucket> buckets = s3.listBuckets();
            LOGGER.info("[{}] S3 {} buckets", s3.getRegion(), buckets.size());
 
            // 配置过期规则
            configExpiredTag(storageProperties.getExpired(), s3, awsVideoBucket);
        }
    }

    private void configExpiredTag(Map<String, String> expired, AmazonS3 s3Client,
            String bucketName) {

        BucketLifecycleConfiguration configuration =
                s3Client.getBucketLifecycleConfiguration(bucketName);

        List<Rule> rules = new ArrayList<>();
        for (Entry<String, String> entry : expired.entrySet()) {
            String ruleName = "RULE_" + entry.getKey();

            int days = Integer.valueOf(entry.getValue().trim());
            LifecycleFilter filter = new LifecycleFilter(new LifecycleTagPredicate(
                    new Tag(StorageProperties.EXPIRED_KEY, entry.getKey())));
            BucketLifecycleConfiguration.Rule rule = new BucketLifecycleConfiguration.Rule()
                    .withId(ruleName).withExpirationInDays(days).withFilter(filter)
                    .withStatus(BucketLifecycleConfiguration.ENABLED.toString());
            rules.add(rule);
        }

        BucketLifecycleConfiguration newConfiguration = null;
        // 根据ruleid,没有则新增
        if (null == configuration || CollectionUtils.isEmpty(configuration.getRules())) {
            newConfiguration = new BucketLifecycleConfiguration().withRules(rules);
        } else {
            Map<String, Rule> ruleMap = new HashMap<>();
            List<Rule> currentRules = configuration.getRules();

            for (Rule rule : rules) {
                ruleMap.put(rule.getId(), rule);
            }
            for (Rule rule : currentRules) {
                ruleMap.put(rule.getId(), rule);
            }
            List<Rule> newRules = new ArrayList<>(ruleMap.values());
            newConfiguration = new BucketLifecycleConfiguration().withRules(newRules);
        }

        // Save configuration.
        s3Client.setBucketLifecycleConfiguration(bucketName, newConfiguration);

        // Verify there are now three rules.
        newConfiguration = s3Client.getBucketLifecycleConfiguration(bucketName);
        for (Rule rule : newConfiguration.getRules()) {
            LOGGER.info("bucketName:{} rule:{}", bucketName, JSONObject.toJSONString(rule));
        }
    }

    @Bean()
    public AmazonS3Template getS3Template() {
        return new AmazonS3Template(s3);
    }

    public void setRegion(String region) {
        this.regionName = region;
    }
}
