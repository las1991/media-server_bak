package com.sengled.media.clock;

import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.mobicents.media.server.scheduler.Clock;

/**
 * 通过单线程轮训时间的
 * 
 * @author chenxh
 */
public class SystemClock implements Clock {
    private static final SystemClock DEFAULT = new SystemClock();
    static {
        Executors.newScheduledThreadPool(1).scheduleWithFixedDelay(new Runnable() {
            
            @Override
            public void run() {
                DEFAULT.nanoTime = System.nanoTime();
                DEFAULT.currentTimeMillis = System.currentTimeMillis();
            }
        }, 1, 1, TimeUnit.MILLISECONDS);
    }
    
    
    private volatile long nanoTime = System.nanoTime();
    private volatile long currentTimeMillis = System.currentTimeMillis();

    public static Clock standard() {
        return DEFAULT;
    }

    /** 
     * 获取不太准确的当前时间 
     * 
     * @return
     */
    public static long currentTimeMillis() {
        return standard().getCurrentTime();
    }
    
    private SystemClock(){}
    
    
    
    /**
     * The default time unit: nanoseconds.
     */
    private TimeUnit timeUnit = TimeUnit.NANOSECONDS;

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.scheduler.Clock.getTime().
     */
    public long getTime() {
        return nanoTime;
    }
    
    public long getCurrentTime() {
        return currentTimeMillis;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.scheduler.Clock.getTimeUnit().
     */
    public TimeUnit getTimeUnit() {
        return timeUnit;
    }

    /**
     * (Non Java-doc.)
     *
     * @see org.mobicents.media.server.scheduler.Clock.getTime().
     */
    public long getTime(TimeUnit timeUnit) {
        return timeUnit.convert(nanoTime, this.timeUnit);
    }
    
    public static void main(String[] args) {
        System.out.println(TimeUnit.MILLISECONDS.toNanos(1));
        System.out.println(TimeUnit.MICROSECONDS.toNanos(1));
    }
}
