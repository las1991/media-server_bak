package com.sengled.media.storage.metrics.custom;

import java.util.concurrent.ConcurrentHashMap;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.stereotype.Component;

@Component
public class ServicesMetrics implements InitializingBean {
	private static final Logger LOGGER = LoggerFactory.getLogger(ServicesMetrics.class);
	
	private final ConcurrentHashMap<String, LastMinuteCount> minuteCountMap = new ConcurrentHashMap<String, LastMinuteCount>();
	
	public final static String DYNAMODB_FAILURE = "storage.dynamodbFailure.lastMinute";
	public final static String DYNAMODB_SUCCESS = "storage.dynamodbSuccess.lastMinute";
	
	
	@Override
	public void afterPropertiesSet() throws Exception {
		add();
	}
	
	private void add(){
		minuteCountMap.put(DYNAMODB_FAILURE, new LastMinuteCount());
		minuteCountMap.put(DYNAMODB_SUCCESS, new LastMinuteCount());
	}
	public void mark(String key,long num){
		LastMinuteCount mc = minuteCountMap.get(key);
		if(mc == null){
			LOGGER.error("minuteCountMap hasnot key:{}",key);
			return;
		}
		mc.markCount(num);
	}
	public long getValue(String key){
		LastMinuteCount mc = minuteCountMap.get(key);
		if(null == mc){
			LOGGER.error("minuteCountMap hasnot key:{}",key);
			return 0L;
		}
		return mc.getPreMinuteCount();
	}

}
