package com.sengled.media.resourcemanager;

import org.springframework.scheduling.annotation.Scheduled;

/**
 * 用于声明 spring-boot 的定时任务
 * @author chenxh
 */
class ScheduledWorker  {
    private Runnable run;
    
    /**
     * 7s 执行一次
     * @param run
     * @return
     */
    public static ScheduledWorker minDelay(Runnable run) {
        return new ScheduledWorker(run){
            @Scheduled(fixedDelay=15000, initialDelay=5000)
            public void scheduled() {
                run();
            }
        };
    }
    
    /**
     * 30s 执行一次
     * @param run
     * @return
     */
    public static ScheduledWorker simpleDelay(Runnable run) {
        return new ScheduledWorker(run){
            @Scheduled(fixedDelay=29000, initialDelay=5000)
            public void scheduled() {
                run();
            }
        };
    }

    /**
     * 180s 执行一次 (约 3min)
     * 
     * @param run
     * @return
     */
    public static ScheduledWorker maxDelay(Runnable run) {
        return new ScheduledWorker(run){
            @Scheduled(fixedDelay=180000, initialDelay=5000)
            public void scheduled() {
                run();
            }
        };
    }
    
    private ScheduledWorker(Runnable run) {
        this.run = run;
    }
    
    
    protected void run() {
        try {
            run.run();
        } catch (Exception ex) {
            ResourceManagerConfiguration.LOGGER.error("{}", ex.getMessage(), ex);
        }
    }
    

}