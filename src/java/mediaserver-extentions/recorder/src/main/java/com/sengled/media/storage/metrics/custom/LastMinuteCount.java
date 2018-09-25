package com.sengled.media.storage.metrics.custom;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
/**
 * 最后一分钟计数
 * @author media-liwei
 *
 */
public class LastMinuteCount {
	private AtomicLong  lastMinuteCount = new AtomicLong();
	private AtomicLong  currentCount = new AtomicLong(); 
	
	private static final Logger LOGGER = LoggerFactory.getLogger(LastMinuteCount.class);
	
	private long recordMinutes;
	
	public LastMinuteCount(){
		recordMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
	}
	public void markCount(long num){
		LOGGER.debug("markCount:{}",num);
		tick();
		currentCount.addAndGet(num);
	}
	public long getPreMinuteCount(){
		long currentMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
		long age = currentMinutes - recordMinutes;
		if(age > 0){
			tick();
		}
		return lastMinuteCount.get();
	}
	private synchronized void tick(){
		long currentMinutes = TimeUnit.MILLISECONDS.toMinutes(System.currentTimeMillis());
		LOGGER.debug("recordMinutes:{},currentMinutes:{},",recordMinutes,currentMinutes);
		long age = currentMinutes - recordMinutes;
		if(age > 0){
			if(age > 1){
				lastMinuteCount.set(0);
			}else{
				lastMinuteCount.set(currentCount.get());	
			}
			currentCount.set(0);
			recordMinutes = currentMinutes;
		}
	}
}
