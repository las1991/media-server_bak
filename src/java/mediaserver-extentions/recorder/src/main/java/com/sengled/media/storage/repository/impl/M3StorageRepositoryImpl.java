package com.sengled.media.storage.repository.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.sengled.media.storage.dynamodb.Dynamodb;
import com.sengled.media.storage.repository.M3StorageRepository;
import com.sengled.media.storage.repository.domain.M3Storage;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class M3StorageRepositoryImpl extends BaseRepository implements M3StorageRepository,InitializingBean {
    
    @Value("${MEDIA_DYNAMO_M3_STORAGE}")
    private String tableName;
    
    private DynamoDBMapper dynamoDBMapperWithTableName;

    private DynamoDBMapper dynamoDBMapperUpdate;

    @Autowired
    Dynamodb dynamoDB;
    

    @Override
    public void afterPropertiesSet() throws Exception {
        dynamoDBMapperWithTableName = new DynamoDBMapper(dynamoDB.getAmazonDynamoDB(), getDynamoDBMapperConfig(tableName, DynamoDBMapperConfig.SaveBehavior.CLOBBER));
        dynamoDBMapperUpdate = new DynamoDBMapper(dynamoDB.getAmazonDynamoDB(), getDynamoDBMapperConfig(tableName, DynamoDBMapperConfig.SaveBehavior.UPDATE_SKIP_NULL_ATTRIBUTES));
    }
    
    @Override
    public void save(M3Storage m3Storage) throws Exception {
        dynamoDBMapperWithTableName.save(m3Storage);
    }

    @Override
    public void batchSave(List<M3Storage> m3StorageList) throws Exception {
        dynamoDBMapperWithTableName.batchSave(m3StorageList);
    }

    @Override
    public void update(M3Storage m3Storage) throws Exception {
        dynamoDBMapperUpdate.save(m3Storage);

    }

    @Override
    public void batchUpdate(List<M3Storage> m3StorageList) throws Exception {
        for (M3Storage m3Storage : m3StorageList) {
            dynamoDBMapperUpdate.save(m3Storage);
        }
    }
}
