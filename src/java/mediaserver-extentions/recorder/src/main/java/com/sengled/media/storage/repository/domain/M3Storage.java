package com.sengled.media.storage.repository.domain;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBAttribute;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDocument;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBHashKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBIgnore;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBRangeKey;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBTable;

/**
 * 安全概要
 * 
 * @author media-liwei
 *
 */

@DynamoDBTable(tableName = "REPLACED_BY_VALUE_IN_PROPERTIES_FILE")
public class M3Storage {

    public static String START_TIME_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'";
    public static String STORAGE_ID_FORMAT = "MMdd";
    public static String DATE_FORMAT = "yyyyMMdd";
    public static String PARTITION_ID_FORMAT = "MMddHH";

    @DynamoDBHashKey(attributeName = "TOKEN")
    private String token;
    
    @DynamoDBRangeKey(attributeName = "STORAGE_ID")
    private String storageID;// C_TASK_ID + FORMAT(C_DATE, STORAGE_ID_FORMAT)

    @DynamoDBAttribute(attributeName = "TASK_ID")
    private String taskID;// 录像服务生成/UUID

    @DynamoDBAttribute(attributeName = "USER_ID")
    private String userID;

    @DynamoDBAttribute(attributeName = "EXPIRED_UTC")
    private long expiredTimestampSec;

    @DynamoDBAttribute(attributeName = "INDEX")
    private String index; // <TOKEN, 时间> 组成的冗余索引

    @DynamoDBAttribute(attributeName = "DATE")
    private String utcDate;// yyy-MM-dd

    @DynamoDBAttribute(attributeName = "START_TIME")
    private String startTime;
    @DynamoDBAttribute(attributeName = "END_TIME")
    private String endTime;
    
    @DynamoDBAttribute(attributeName = "PARTITION_00")
    private M3StoragePartition partition00;
    @DynamoDBAttribute(attributeName = "PARTITION_01")
    private M3StoragePartition partition01;
    @DynamoDBAttribute(attributeName = "PARTITION_02")
    private M3StoragePartition partition02;
    @DynamoDBAttribute(attributeName = "PARTITION_03")
    private M3StoragePartition partition03;
    @DynamoDBAttribute(attributeName = "PARTITION_04")
    private M3StoragePartition partition04;
    @DynamoDBAttribute(attributeName = "PARTITION_05")
    private M3StoragePartition partition05;
    @DynamoDBAttribute(attributeName = "PARTITION_06")
    private M3StoragePartition partition06;
    @DynamoDBAttribute(attributeName = "PARTITION_07")
    private M3StoragePartition partition07;
    @DynamoDBAttribute(attributeName = "PARTITION_08")
    private M3StoragePartition partition08;
    @DynamoDBAttribute(attributeName = "PARTITION_09")
    private M3StoragePartition partition09;
    @DynamoDBAttribute(attributeName = "PARTITION_10")
    private M3StoragePartition partition10;
    @DynamoDBAttribute(attributeName = "PARTITION_11")
    private M3StoragePartition partition11;
    @DynamoDBAttribute(attributeName = "PARTITION_12")
    private M3StoragePartition partition12;
    @DynamoDBAttribute(attributeName = "PARTITION_13")
    private M3StoragePartition partition13;
    @DynamoDBAttribute(attributeName = "PARTITION_14")
    private M3StoragePartition partition14;
    @DynamoDBAttribute(attributeName = "PARTITION_15")
    private M3StoragePartition partition15;
    @DynamoDBAttribute(attributeName = "PARTITION_16")
    private M3StoragePartition partition16;
    @DynamoDBAttribute(attributeName = "PARTITION_17")
    private M3StoragePartition partition17;
    @DynamoDBAttribute(attributeName = "PARTITION_18")
    private M3StoragePartition partition18;
    @DynamoDBAttribute(attributeName = "PARTITION_19")
    private M3StoragePartition partition19;
    @DynamoDBAttribute(attributeName = "PARTITION_20")
    private M3StoragePartition partition20;
    @DynamoDBAttribute(attributeName = "PARTITION_21")
    private M3StoragePartition partition21;
    @DynamoDBAttribute(attributeName = "PARTITION_22")
    private M3StoragePartition partition22;
    @DynamoDBAttribute(attributeName = "PARTITION_23")
    private M3StoragePartition partition23;

    @DynamoDBIgnore
    private boolean neatBool; // partition 数据是否整理过


    public boolean isNeatBool() {
        return neatBool;
    }

    public void setNeatBool(boolean neatBool) {
        this.neatBool = neatBool;
    }

    public String getUserID() {
        return userID;
    }

    public void setUserID(String userID) {
        this.userID = userID;
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
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

    public String getTaskID() {
        return taskID;
    }

    public void setTaskID(String taskID) {
        this.taskID = taskID;
    }

    public String getUtcDate() {
        return utcDate;
    }

    public void setUtcDate(String utcDate) {
        this.utcDate = utcDate;
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

    public M3StoragePartition getPartition00() {
        return partition00;
    }

    public void setPartition00(M3StoragePartition partition00) {
        this.partition00 = partition00;
    }

    public M3StoragePartition getPartition01() {
        return partition01;
    }

    public void setPartition01(M3StoragePartition partition01) {
        this.partition01 = partition01;
    }

    public M3StoragePartition getPartition02() {
        return partition02;
    }

    public void setPartition02(M3StoragePartition partition02) {
        this.partition02 = partition02;
    }

    public M3StoragePartition getPartition03() {
        return partition03;
    }

    public void setPartition03(M3StoragePartition partition03) {
        this.partition03 = partition03;
    }

    public M3StoragePartition getPartition04() {
        return partition04;
    }

    public void setPartition04(M3StoragePartition partition04) {
        this.partition04 = partition04;
    }

    public M3StoragePartition getPartition05() {
        return partition05;
    }

    public void setPartition05(M3StoragePartition partition05) {
        this.partition05 = partition05;
    }

    public M3StoragePartition getPartition06() {
        return partition06;
    }

    public void setPartition06(M3StoragePartition partition06) {
        this.partition06 = partition06;
    }

    public M3StoragePartition getPartition07() {
        return partition07;
    }

    public void setPartition07(M3StoragePartition partition07) {
        this.partition07 = partition07;
    }

    public M3StoragePartition getPartition08() {
        return partition08;
    }

    public void setPartition08(M3StoragePartition partition08) {
        this.partition08 = partition08;
    }

    public M3StoragePartition getPartition09() {
        return partition09;
    }

    public void setPartition09(M3StoragePartition partition09) {
        this.partition09 = partition09;
    }

    public M3StoragePartition getPartition10() {
        return partition10;
    }

    public void setPartition10(M3StoragePartition partition10) {
        this.partition10 = partition10;
    }

    public M3StoragePartition getPartition11() {
        return partition11;
    }

    public void setPartition11(M3StoragePartition partition11) {
        this.partition11 = partition11;
    }

    public M3StoragePartition getPartition12() {
        return partition12;
    }

    public void setPartition12(M3StoragePartition partition12) {
        this.partition12 = partition12;
    }

    public M3StoragePartition getPartition13() {
        return partition13;
    }

    public void setPartition13(M3StoragePartition partition13) {
        this.partition13 = partition13;
    }

    public M3StoragePartition getPartition14() {
        return partition14;
    }

    public void setPartition14(M3StoragePartition partition14) {
        this.partition14 = partition14;
    }

    public M3StoragePartition getPartition15() {
        return partition15;
    }

    public void setPartition15(M3StoragePartition partition15) {
        this.partition15 = partition15;
    }

    public M3StoragePartition getPartition16() {
        return partition16;
    }

    public void setPartition16(M3StoragePartition partition16) {
        this.partition16 = partition16;
    }

    public M3StoragePartition getPartition17() {
        return partition17;
    }

    public void setPartition17(M3StoragePartition partition17) {
        this.partition17 = partition17;
    }

    public M3StoragePartition getPartition18() {
        return partition18;
    }

    public void setPartition18(M3StoragePartition partition18) {
        this.partition18 = partition18;
    }

    public M3StoragePartition getPartition19() {
        return partition19;
    }

    public void setPartition19(M3StoragePartition partition19) {
        this.partition19 = partition19;
    }

    public M3StoragePartition getPartition20() {
        return partition20;
    }

    public void setPartition20(M3StoragePartition partition20) {
        this.partition20 = partition20;
    }

    public M3StoragePartition getPartition21() {
        return partition21;
    }

    public void setPartition21(M3StoragePartition partition21) {
        this.partition21 = partition21;
    }

    public M3StoragePartition getPartition22() {
        return partition22;
    }

    public void setPartition22(M3StoragePartition partition22) {
        this.partition22 = partition22;
    }

    public M3StoragePartition getPartition23() {
        return partition23;
    }

    public void setPartition23(M3StoragePartition partition23) {
        this.partition23 = partition23;
    }

    @Override
    public String toString() {
        return "M3Storage{" +
                "partition00=" + partition00 +
                ", partition01=" + partition01 +
                ", partition02=" + partition02 +
                ", partition03=" + partition03 +
                ", partition04=" + partition04 +
                ", partition05=" + partition05 +
                ", partition06=" + partition06 +
                ", partition07=" + partition07 +
                ", partition08=" + partition08 +
                ", partition09=" + partition09 +
                ", partition10=" + partition10 +
                ", partition11=" + partition11 +
                ", partition12=" + partition12 +
                ", partition13=" + partition13 +
                ", partition14=" + partition14 +
                ", partition15=" + partition15 +
                ", partition16=" + partition16 +
                ", partition17=" + partition17 +
                ", partition18=" + partition18 +
                ", partition19=" + partition19 +
                ", partition20=" + partition20 +
                ", partition21=" + partition21 +
                ", partition22=" + partition22 +
                ", partition23=" + partition23 +
                ", neatBool=" + neatBool +
                '}';
    }

    @DynamoDBDocument
    public static class M3StoragePartition {
        
        
        @DynamoDBAttribute(attributeName = "PARTITION_ID")
        private String partitionID;
        @DynamoDBAttribute(attributeName = "START_TIME")
        private String startTime;
        @DynamoDBAttribute(attributeName = "END_TIME")
        private String endTime;
        @DynamoDBAttribute(attributeName = "EXPIRED_UTC")
        private long expiredUtcSec;

        public String getPartitionID() {
            return partitionID;
        }

        public void setPartitionID(String partitionID) {
            this.partitionID = partitionID;
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

        public long getExpiredUtcSec() {
            return expiredUtcSec;
        }

        public void setExpiredUtcSec(long expiredUtcSec) {
            this.expiredUtcSec = expiredUtcSec;
        }



    }

}
