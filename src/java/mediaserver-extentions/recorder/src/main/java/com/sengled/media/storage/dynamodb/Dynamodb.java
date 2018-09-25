package com.sengled.media.storage.dynamodb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import com.amazonaws.ClientConfiguration;
import com.amazonaws.Protocol;
import com.amazonaws.auth.DefaultAWSCredentialsProviderChain;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsync;
import com.amazonaws.services.dynamodbv2.AmazonDynamoDBAsyncClientBuilder;
import com.amazonaws.services.dynamodbv2.document.DynamoDB;
import com.amazonaws.services.dynamodbv2.document.Page;
import com.amazonaws.services.dynamodbv2.document.Table;
import com.amazonaws.services.dynamodbv2.model.ListTablesResult;

@Configuration
public class Dynamodb implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(Dynamodb.class);

    @Value("${MEDIA_AWS_REGION}")
    private String region;

    private DynamoDB dynamoDB;
    private AmazonDynamoDBAsync amazonDynamoDB;

    @Override
    public void afterPropertiesSet() throws Exception {
        LOGGER.info("Initializing...");
        try {
            initialize();
        } catch (Exception e) {
            LOGGER.error("Fail connect with DynamoDB for '{}'.", e.getMessage(), e);
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void initialize() {
        amazonDynamoDB = AmazonDynamoDBAsyncClientBuilder.standard()
                    .withCredentials(DefaultAWSCredentialsProviderChain.getInstance())
                    .withRegion(region)
                    .withClientConfiguration(getConfig()).build();
        dynamoDB = new DynamoDB(amazonDynamoDB);

        new Thread(new Runnable() {
            
            @Override
            public void run() {
                try {
                    Page<Table, ListTablesResult> page = dynamoDB.listTables().firstPage();
                    LOGGER.info("[{}] Dynamo {} found", region, page.size());
                } catch (Exception e) {
                    LOGGER.error("Fail list dynamo tables", e);
                }
            }
        }).start();
        
    }

    private ClientConfiguration getConfig() {
        ClientConfiguration conf = new ClientConfiguration();
        conf.setUseTcpKeepAlive(true);
        conf.setMaxErrorRetry(3);
        conf.setConnectionTimeout(15 * 1000);
        conf.setSocketTimeout(60 * 1000);
        conf.setProtocol(Protocol.HTTPS);
        conf.setMaxConnections(3 * ClientConfiguration.DEFAULT_MAX_CONNECTIONS);
        conf.setUseTcpKeepAlive(true);
        return conf;
    }
    
    @Bean
    public DynamodbTemplate getDynamodbTemplate() {
        return new DynamodbTemplate(dynamoDB);
    }

    public AmazonDynamoDBAsync getAmazonDynamoDB() {
        return amazonDynamoDB;
    }
    
    
}
