package com.sengled.media.storage.repository.impl;

import java.util.List;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.sengled.media.storage.dynamodb.Dynamodb;
import com.sengled.media.storage.repository.M3StorageItemRepository;
import com.sengled.media.storage.repository.domain.M3StorageItem;

@Component
public class M3StorageItemRepositoryImpl extends BaseRepository implements M3StorageItemRepository, InitializingBean {

    @Value("${MEDIA_DYNAMO_M3_STORAGE_ITEM}")
    private String tableName;
    
    private DynamoDBMapper dynamoDBMapperWithTableName;

    @Autowired
    Dynamodb dynamoDB;

    @Override
    public void afterPropertiesSet() throws Exception {
        dynamoDBMapperWithTableName = new DynamoDBMapper(dynamoDB.getAmazonDynamoDB(), getDynamoDBMapperConfig(tableName, DynamoDBMapperConfig.SaveBehavior.CLOBBER));
    }

    @Override
    public void save(M3StorageItem m3StorageItem) throws Exception {
        dynamoDBMapperWithTableName.save(m3StorageItem);
    }

    @Override
    public void batchSave(List<M3StorageItem> m3StorageList) throws Exception {
        dynamoDBMapperWithTableName.batchSave(m3StorageList);

    }

}
