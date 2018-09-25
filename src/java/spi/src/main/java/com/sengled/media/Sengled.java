package com.sengled.media;

import java.net.URL;
import java.util.Properties;
import org.apache.commons.io.input.AutoCloseInputStream;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Sengled {
	final static Logger logger = LoggerFactory.getLogger(Sengled.class);

	private static final Properties props;
	static {
		props = new Properties();

		String configs = System.getProperty("spring.config.location");
		if (StringUtils.isNotEmpty(configs)) {
		    load(configs);
		} else {
		    load("file:/etc/sengled/sengled.properties");
		}
	}

	public static void load(String locations) {
		Properties p = props;
		String[] urls = StringUtils.split(locations, ",");
		for (String location : urls) {
		    location = StringUtils.trim(location);
		    
		    URL url = null;
			try {
			    if (location.startsWith("classpath:")) {
			        url = Sengled.class.getResource(location.substring("classpath:".length()));
			    } else {
			        url = new URL(location);
			    }
			    
			    p.load(new AutoCloseInputStream(url.openStream()));
			    logger.info("{} loaded", url);
			} catch (Exception e) {
				logger.error("{} load failed", location, e);
			}
		}
	}


	private Sengled() {
	}

	public static void setProperty(String key, String value) {
		props.setProperty(key, value);
	}

	/**
	 * @param key
	 * @return
	 * @throws IllegalArgumentException if no value found
	 */
	public static String getProperty(String key) {
		String value = props.getProperty(key);
		if (null == value) {
			throw new IllegalArgumentException("config [" + key + "] not found");
		}

		return value;
	}

	public static String getProperty(String key, String defaultValue) {
		return props.getProperty(key, defaultValue);
	}

	public static int getIntProperty(String key, int defaultValue) {
		String value = props.getProperty(key);
		return null != value ? Integer.parseInt(value) : defaultValue;
	}
}
