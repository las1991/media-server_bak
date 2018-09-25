package com.sengled.media.storage.services;

import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 文件名称所包含的信息
 * 
 * @author chenxh
 */
public class FileNameMetadata {
    public static final String FILE_SUFFIX = "flv";
    private static final Logger LOGGER = LoggerFactory.getLogger(FileNameMetadata.class);
    private static final String  FIELD_SEPARATOR = "_FS_";
    
    private String token;
    private Long userId;
    private long startTime;
    private long endTime;
    private int storageTTLHours;
    private String timeZoneCity;
    private long videoIndex;
    
    private FileNameMetadata() {}

    public FileNameMetadata(String token, Long userId, long startTime, long endTime, int storageTTLHours,
            String timeZoneCity, long videoIndex) {
        super();
        this.token = token;
        this.userId = userId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.storageTTLHours = storageTTLHours;
        this.timeZoneCity = timeZoneCity;
        this.videoIndex = videoIndex;
    }

    public static FileNameMetadata parse(String fileName) {
        try {
            FileNameMetadata metadata = new FileNameMetadata();
            
            String[] fileNameFields = FilenameUtils.getBaseName(fileName).split(FIELD_SEPARATOR);
            metadata.token = fileNameFields[0];
            metadata.startTime = Long.valueOf(fileNameFields[1]);
            metadata.endTime = Long.valueOf(fileNameFields[2]);
            metadata.storageTTLHours = Integer.valueOf(fileNameFields[3]);
            metadata.timeZoneCity = fileNameFields[4].replace("$", "/");
            metadata.videoIndex = Integer.valueOf(fileNameFields[5]);
            if (fileNameFields.length > 6) {
                metadata.userId = Long.valueOf(fileNameFields[6]);
            }
            return metadata;
        } catch (Exception e) {
            LOGGER.error("Fail read name {} for {}", fileName, e.getMessage(), e);
        }
        
        return null;
    }
    
    public String encode() {
        String timezoneCity = timeZoneCity.replace("/", "$");
        StringBuilder builder = new StringBuilder()
                .append(token).append(FIELD_SEPARATOR)
                .append(startTime).append(FIELD_SEPARATOR)
                .append(endTime).append(FIELD_SEPARATOR)
                .append(storageTTLHours).append(FIELD_SEPARATOR)
                .append(timezoneCity).append(FIELD_SEPARATOR)
                .append(videoIndex);
        
        if (null != userId) {
            builder.append(FIELD_SEPARATOR).append(userId);
        }
        
        return builder.append(".").append(FILE_SUFFIX).toString();
    }

    public String getToken() {
        return token;
    }

    public void setToken(String token) {
        this.token = token;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public long getStartTime() {
        return startTime;
    }

    public void setStartTime(long startTime) {
        this.startTime = startTime;
    }

    public long getEndTime() {
        return endTime;
    }

    public void setEndTime(long endTime) {
        this.endTime = endTime;
    }

    public int getStorageTTLHours() {
        return storageTTLHours;
    }

    public void setStorageTTLHours(int storageTTLHours) {
        this.storageTTLHours = storageTTLHours;
    }

    public String getTimeZoneCity() {
        return timeZoneCity;
    }

    public void setTimeZoneCity(String timeZoneCity) {
        this.timeZoneCity = timeZoneCity;
    }

    public long getVideoIndex() {
        return videoIndex;
    }

    public void setVideoIndex(long videoIndex) {
        this.videoIndex = videoIndex;
    }
}
