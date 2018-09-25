package com.sengled.media.storage.webapp;

import java.io.File;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSource;
import com.sengled.media.device.MediaDeviceProfile;
import com.sengled.media.file.segment.DefaultMediaSinkSpliterator;
import com.sengled.media.file.segment.Segment;
import com.sengled.media.storage.services.FileNameMetadata;

/**
 * 存储服务需要实现的接口
 * 
 * @author chenxh
 */
public class StorageMediaSinkSpliterator extends DefaultMediaSinkSpliterator {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageMediaSinkSpliterator.class);
    
    
    private String timeZoneCity;
    private int storageTime;
    private int videoIndex = 0;
    private Long userId;
    
	public StorageMediaSinkSpliterator(MediaSource source, File dir,String timeZoneCity,int storageTime, int flvSegmentDurationInSeconds,long userId) {
		super(source, dir, flvSegmentDurationInSeconds);
		this.timeZoneCity = timeZoneCity;
		this.storageTime = storageTime;
		this.userId = userId;
	}
	
	@Override
	public boolean isFromStorage() {
		return true; // 标记为存储用的 media sink
	}
	
	public boolean isModified(int storageTime, MediaDeviceProfile profile) {
		return storageTime != this.storageTime 
		        || !StringUtils.equals(profile.getTimeZone(), this.timeZoneCity) 
		        || !userId.equals(profile.getUserId());
	}
	
	@Override
	protected void onStarted() {
        LOGGER.info("[{}] storage started, storageTime:{} hours, timeZoneCity:{} ", getToken(), storageTime, timeZoneCity);
	}
	
	@Override
	protected void onClosed0() {
	    LOGGER.info("[{}] storage stopped, storageTime:{} hours, timeZoneCity:{} ", getToken(), storageTime, timeZoneCity);
	}
	
	@Override
	protected File createNewFile(File dir, Segment segment) {
	    final int nextVideoIndex = videoIndex++;
        final String token = segment.getToken();
        final long startTime = segment.getFirstFrameTime();
        final long endTime = startTime + segment.getDuration();
        final FileNameMetadata metadata = new FileNameMetadata(token, userId, startTime, endTime, storageTime, timeZoneCity, nextVideoIndex);
        String dstFlvFileName = metadata.encode();
        LOGGER.debug("create file:{} ", dstFlvFileName);
        return new File(dir,dstFlvFileName);
	}
}
