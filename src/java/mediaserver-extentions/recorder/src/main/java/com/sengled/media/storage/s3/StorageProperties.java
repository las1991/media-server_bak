package com.sengled.media.storage.s3;

import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import com.amazonaws.services.s3.model.Tag;

@Component
@ConfigurationProperties(prefix = "storage")
public class StorageProperties implements InitializingBean {
    private static final Logger LOGGER = LoggerFactory.getLogger(StorageProperties.class);
    public static final String EXPIRED_KEY = "expired";

    // @see storage.properties storage.expired array
    private Map<String, String> expired = new HashMap<String, String>();

    @Override
    public void afterPropertiesSet() throws Exception {

        try {
            initialize();
        } catch (Exception e) {
            LOGGER.error(e.getMessage(), e);
            System.exit(1);
        }
    }

    private void initialize() {
        for (Entry<String, String> entry : expired.entrySet()) {
            String ruleName = entry.getKey();
            int days = Integer.valueOf(entry.getValue().trim());
            LOGGER.info("read  S3 Storage Expired Config ruleName :{},expiredDays:{}", ruleName,
                    days);
        }
    }

    /**
     * 
     * @param key eg:1 2 7 30
     * @return eg: expired|1
     */
    public Tag getTag(String day) {
        for (Entry<String, String> obj : expired.entrySet()) {
            String tagKey = obj.getKey();
            if (day.equals(tagKey)) {
                return new Tag(StorageProperties.EXPIRED_KEY, tagKey);
            }
        }
        return null;
    }


    public Map<String, String> getExpired() {
        return expired;
    }

    public void setExpired(Map<String, String> expired) {
        this.expired = expired;
    }
}
