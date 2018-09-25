package com.sengled.media.storage.services;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.TimeZone;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import com.sengled.media.device.MediaDeviceService;
import com.sengled.media.storage.repository.M3StorageItemRepository;
import com.sengled.media.storage.repository.M3StorageRepository;
import com.sengled.media.storage.repository.domain.M3Storage;
import com.sengled.media.storage.repository.domain.M3StorageItem;
import com.sengled.media.storage.services.exception.AwsTransferException;

@Service
public class SyncDynamodb {
    private static final Logger LOGGER = LoggerFactory.getLogger(SyncDynamodb.class);
    
    private ConcurrentHashMap<String, M3Storage> contexts = new ConcurrentHashMap<>();

    private static final String[] timePointcut = new String[]{"00","04","08","12","16","20"}; // 时间切点，4个小时一条记录
    
    @Autowired
    M3StorageRepository m3StorageRepository;
    
    @Autowired
    M3StorageItemRepository m3StorageItemRepository;

    @Autowired
    MediaDeviceService mediaDeviceService;
    
    public void putDynamodb(List<StorageMessage> smgs) throws Exception {
        List<M3Storage> m3StorageList = new ArrayList<M3Storage>();
        List<M3Storage> m3StorageUpdateList = new ArrayList<M3Storage>();
        List<M3StorageItem> m3StorageItemList = new ArrayList<M3StorageItem>();
        
        for (StorageMessage storageMsg : smgs) {
            String token = storageMsg.getToken();
            long startTime = storageMsg.getStartTime();
            long endTime = storageMsg.getEndTime();
            int liveHours = storageMsg.getLiveHours();
            
            M3Storage m3storage = getOrCreateM3Storage(storageMsg);
            m3storage.setToken(token);
            m3storage.setExpiredTimestampSec(getExpiredTimestampSec(liveHours));
            m3storage.setIndex(token+ "_" + DateFormatUtils.format(startTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
            m3storage.setUtcDate(DateFormatUtils.format(startTime, M3Storage.DATE_FORMAT, TimeZone.getTimeZone("UTC")));
            m3storage.setEndTime(DateFormatUtils.format(endTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
            String partitionID = setM3StoragePartitionXX(m3storage,startTime,endTime,storageMsg.getLiveHours());

            if (m3storage.isNeatBool()){
                m3StorageUpdateList.add(m3storage);
            }else {
                m3StorageList.add(m3storage);
            }

            M3StorageItem item = new M3StorageItem();
            item.setUserID(String.valueOf(storageMsg.getUserId()));
            item.setToken(token);
            item.setStorageID(m3storage.getStorageID());
            item.setStartTime(DateFormatUtils.format(startTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
            item.setEndTime(DateFormatUtils.format(endTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
            item.setPartitionID(partitionID);
            item.setIndex(token+ "_" + DateFormatUtils.format(startTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
            item.setFile(getFileUrl(storageMsg));
            item.setExpiredTimestampSec(getExpiredTimestampSec(liveHours));
            m3StorageItemList.add(item);
            
            LOGGER.debug("[{}], {}, {}", item.getToken(), item.getStartTime(), item.getEndTime());
        }
        try {
            m3StorageItemRepository.batchSave(m3StorageItemList);
            
            if (!m3StorageList.isEmpty()){
                m3StorageRepository.batchSave(m3StorageList);
                LOGGER.debug("save {}", m3StorageList.size());
            }
            if (!m3StorageUpdateList.isEmpty()){
                m3StorageRepository.batchUpdate(m3StorageUpdateList);
                LOGGER.debug("update {}", m3StorageUpdateList.size());
            }
            
        } catch (Exception e) {
            throw new AwsTransferException(e.getMessage(),e);
        }
    }

    private String getFileUrl(StorageMessage storageMsg) {
            long p0 = storageMsg.getPosition();
            long p1 = p0 + storageMsg.getLength() - 1;
            return new StringBuffer().append(storageMsg.getVideoFile()).append("?range=").append(p0).append("-")
                    .append(p1).append("&").append("index=").append(storageMsg.getVideoIndex()).toString();
    }


    private String setM3StoragePartitionXX(M3Storage m3storage, long startTime, long endTime, int liveHours) throws Exception{
        String partitionID = getPartitionId(m3storage, startTime);
        setPartitionXX(m3storage, startTime, endTime, liveHours,partitionID);
        return partitionID;
    }

    private String getPartitionId(M3Storage m3storage, long startTime) {
        return m3storage.getTaskID() + "_" + DateFormatUtils.format(startTime, M3Storage.PARTITION_ID_FORMAT);
    }

    private M3Storage getOrCreateM3Storage(StorageMessage storageMsg) {
        String token = storageMsg.getToken();
        M3Storage m3storage = contexts.get(token);
        long startTime = storageMsg.getStartTime();
        long storageIndex = storageMsg.getVideoIndex();

        String userId = String.valueOf(storageMsg.getUserId());
        
        
        if ( storageIndex == 0 || null == m3storage) {// new video
            m3storage = buildM3Storage(userId, startTime);
            contexts.put(token, m3storage);
        }else{
            String originalMMdd = m3storage.getStorageID().split("_")[1];
            String currMMdd = DateFormatUtils.format(startTime, M3Storage.STORAGE_ID_FORMAT, TimeZone.getTimeZone("UTC"));
            if( ! originalMMdd.equals(currMMdd) || isCreateStorage(m3storage, startTime)){// new  day, A new record of 4 hours
                m3storage = buildM3Storage(userId, startTime);
                contexts.put(token, m3storage);
            }else {
                m3storage = buildNiceStorage(m3storage, startTime);
                contexts.put(token, m3storage);
            }
        }
        return m3storage;
    }

    /**
     * 是否要创建一条新的storage
     * @param startTime
     * @param storage
     * @return
     */
    private boolean isCreateStorage(M3Storage storage, long startTime){
        String hh = DateFormatUtils.format(startTime, "HH");
        if (storage == null) return true;

        if (ArrayUtils.contains(timePointcut, hh)){

            Method getMethod = BeanUtils.findMethod(storage.getClass(), "getPartition"+hh);
            try {
                Object partition = getMethod.invoke(storage);

                if (partition == null){
                    return true;
                }else {
                    return false;
                }
            } catch (Exception e) {
                LOGGER.error("isCreateStorage Error, return true, cause: {}", e.getMessage());
                return true;
            }
        }
        return false;
    }

    /**
     * 构建实例，清楚冗余数据， 避免wcu 过高
     * @param m3storage
     * @param startTime
     * @return
     */
    private M3Storage buildNiceStorage(M3Storage m3storage,  long startTime){

        M3Storage niceStorage = new M3Storage();
        niceStorage.setToken(m3storage.getToken());
        niceStorage.setStorageID(m3storage.getStorageID());
        niceStorage.setExpiredTimestampSec(m3storage.getExpiredTimestampSec());
        niceStorage.setIndex(m3storage.getIndex());
        niceStorage.setStartTime(m3storage.getStartTime());
        niceStorage.setEndTime(m3storage.getEndTime());
        niceStorage.setUtcDate(m3storage.getUtcDate());
        niceStorage.setTaskID(m3storage.getTaskID());
        niceStorage.setUserID(m3storage.getUserID());
        niceStorage.setNeatBool(true);

        String hh = DateFormatUtils.format(startTime, "HH");
        String setMethodString = "setPartition" + hh;
        String getMethodString = "getPartition" + hh;

        try {
            Method getMethod = BeanUtils.findMethod(m3storage.getClass(), getMethodString);
            Method setMethod = BeanUtils.findMethod(niceStorage.getClass(), setMethodString, M3Storage.M3StoragePartition.class);
            M3Storage.M3StoragePartition partition = (M3Storage.M3StoragePartition)getMethod.invoke(m3storage);
            setMethod.invoke(niceStorage, partition);
        } catch (Exception e) {
            LOGGER.error("buildNiceStorage fail, {}", e);
        }
        return niceStorage;
    };

    private long getExpiredTimestampSec(int fileExpiresHours) {
        Calendar calendar;
        try {
            calendar = Calendar.getInstance();
            calendar.add(Calendar.HOUR, fileExpiresHours+1);//多存1小时
            calendar.set(Calendar.MINUTE, 0);
            calendar.set(Calendar.SECOND, 0);
            calendar.set(Calendar.MILLISECOND, 000);
            return TimeUnit.MILLISECONDS.toSeconds(calendar.getTimeInMillis());
        } catch (Exception e) {
            return 0;
        }
    }
    

    private void setPartitionXX(M3Storage m3storage, long startTime, long endTime, int liveHours, String partitionID) throws Exception {
        Class<? extends M3Storage> clazz =  M3Storage.class;
        
        String hh = DateFormatUtils.format(startTime, "HH");
        String getMethodString = "getPartition" + hh;
        String setMethodString = "setPartition" + hh;
        
        Method getMethod = clazz.getMethod(getMethodString);
        Method setMethod = clazz.getMethod(setMethodString, new Class[] {M3Storage.M3StoragePartition.class});
        
        M3Storage.M3StoragePartition partition = (M3Storage.M3StoragePartition)getMethod.invoke(m3storage);
        if( null == partition ){
            partition = new M3Storage.M3StoragePartition();
            partition.setStartTime(DateFormatUtils.format(startTime, M3Storage.START_TIME_FORMAT));
        }
        partition.setEndTime(DateFormatUtils.format(endTime, M3Storage.START_TIME_FORMAT));
        partition.setPartitionID(partitionID);
        partition.setExpiredUtcSec(getExpiredTimestampSec(liveHours));
        setMethod.invoke(m3storage, new Object[] {partition});
        
    }

    private M3Storage buildM3Storage(String userId, long startTime) {
        M3Storage m3Storage = new M3Storage();
        String uuid = UUID.randomUUID().toString().toUpperCase();
        String mmdd = DateFormatUtils.format(startTime, M3Storage.STORAGE_ID_FORMAT, TimeZone.getTimeZone("UTC"));
        String storageID = uuid + "_" + mmdd;
        
        m3Storage.setTaskID(uuid);
        m3Storage.setStorageID(storageID);
        m3Storage.setUserID(userId);
        m3Storage.setStartTime(DateFormatUtils.format(startTime, M3Storage.START_TIME_FORMAT, TimeZone.getTimeZone("UTC")));
        return m3Storage;
    }
}
