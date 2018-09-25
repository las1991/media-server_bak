package com.sengled.media.storage.repository.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * 安全概要
 * 
 * @author media-liwei
 *
 */

@DynamoDBTable(tableName = "REPLACED_BY_VALUE_IN_PROPERTIES_FILE")
public class M3StorageItem {

    @DynamoDBHashKey(attributeName = "TOKEN")
    private String token;
    
    @DynamoDBRangeKey(attributeName = "START_TIME")
    private String startTime;
    
    @DynamoDBAttribute(attributeName = "END_TIME")
    private String endTime;
    
    @DynamoDBAttribute(attributeName = "USER_ID")
    private String userID;
    
    @DynamoDBAttribute(attributeName = "EXPIRED_UTC")
    private long expiredTimestampSec;
    
    @DynamoDBAttribute(attributeName = "INDEX")
    private String index;   //<TOKEN, 时间> 组成的冗余索引
    
    @DynamoDBAttribute(attributeName = "STORAGE_ID")
    private String storageID;// C_TASK_ID   + FORMAT(C_DATE, “MMDD”)
    
    @DynamoDBAttribute(attributeName = "PARTITION_ID")
    private String partitionID;
    
    @DynamoDBAttribute(attributeName = "FILE")
    private String file;

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public String getStartTime() {
        return startTime;
    }

    public void setStartTime(String startTime) {
        this.startTime = startTime;
    }

    public String getEndTime() {
        return endTime;
    }

    public void setEndTime(String endTime) {
        this.endTime = endTime;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public long getExpiredTimestampSec() {
        return expiredTimestampSec;
    }

    public void setExpiredTimestampSec(long expiredTimestampSec) {
        this.expiredTimestampSec = expiredTimestampSec;
    }

    public String getIndex() {
        return index;
    }

    public void setIndex(String index) {
        this.index = index;
    }

    public String getStorageID() {
        return storageID;
    }

    public void setStorageID(String storageID) {
        this.storageID = storageID;
    }

    public String getPartitionID() {
        return partitionID;
    }

    public void setPartitionID(String partitionID) {
        this.partitionID = partitionID;
    }

    public String getFile() {
        return file;
    }

    public void setFile(String file) {
        this.file = file;
    }

    
    

}
