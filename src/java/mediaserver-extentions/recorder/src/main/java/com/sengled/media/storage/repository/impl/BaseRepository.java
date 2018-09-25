package com.sengled.media.storage.repository.impl;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.SaveBehavior;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapperConfig.TableNameOverride;

public class BaseRepository {

    DynamoDBMapperConfig getDynamoDBMapperConfig(String tableName, SaveBehavior behavior){
        TableNameOverride value = new TableNameOverride(tableName);
        DynamoDBMapperConfig config = new  DynamoDBMapperConfig.Builder()
                .withTableNameOverride(value)
                .withSaveBehavior(behavior)
                .build();
        return config;
    }
}
