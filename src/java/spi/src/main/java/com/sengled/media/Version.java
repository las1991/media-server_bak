package com.sengled.media;

import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Version {
	private static final Logger LOGGER = LoggerFactory.getLogger(Version.class);
	
	private static final String NAME;
	private static final String CURRENT;
	static {
		// 通过环境变量获取
		final String key = "media.version";
		String version = System.getProperty(key, "3.0");
		
		Pattern pattern = Pattern.compile("sengled-(.*)-((\\d+)\\.(\\d+)\\.(\\d+))");
		String classPath = System.getProperty("java.class.path");
		for (String jarPath : StringUtils.split(classPath, ":")) {
		    Matcher matcher = pattern.matcher(jarPath);
		    if (matcher.find() && !StringUtils.contains(jarPath, "api")) {
		        version =  matcher.group(2) ;
		    }
        }
		
		CURRENT = version;
		NAME = Sengled.getProperty("server.name", "Sengled Media Server") + " v" + version;
		LOGGER.info("use version '{}'", CURRENT);
	}
	
	public static String currentVersion() {
	    return CURRENT;
	}
	
	public static String server() {
	    return NAME;
	}
}
