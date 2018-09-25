package com.sengled.media.bootstrap.timer;

import java.util.TimerTask;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 把 Runnable 接口包装成一个 TimerTask
 * 
 * @author chenxh
 */
public class RunnableTimerTask extends TimerTask {
	private static final Logger LOGGER = LoggerFactory.getLogger(RunnableTimerTask.class);

	private Runnable run;
	
	public RunnableTimerTask(Runnable run) {
		super();
		this.run = run;
	}

	@Override
	public void run() {
		try {
			if (null != run) {
				run.run();
			}
		} catch (Exception ex) {
			handleException(ex);
		}
	}

	protected void handleException(Exception ex) {
		LOGGER.error("timer task exception:{}", ex.getMessage(), ex);
	}
}
