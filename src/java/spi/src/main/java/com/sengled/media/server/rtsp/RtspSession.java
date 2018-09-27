package com.sengled.media.server.rtsp;

import com.sengled.media.MediaSession;
import com.sengled.media.url.URLObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * 一个 rtsp 会话
 */
public class RtspSession implements MediaSession {
    final private long lastModified = System.currentTimeMillis();

    private String token;
    private final URLObject url;
    private String sessionId = RandomStringUtils.random(6, true, true).toUpperCase();

    public RtspSession(URLObject url) {
        this.url = url;
        this.token = FilenameUtils.getBaseName(url.getUri()).split("[&?]+")[0];
    }

    public String getSessionId() {
        return sessionId;
    }

    public long getLastModified() {
        return lastModified;
    }

    @Override
    public String getToken() {
        return token;
    }

    public URLObject getUrl() {
        return url;
    }

    @Override
    public String toString() {
        return "RtspSession{" +
                "lastModified='" + DateFormatUtils.format(lastModified, "yyyy-MM-dd HH:mm:ss.SSS") + '\'' +
                ", token='" + token + '\'' +
                ", url=" + url +
                ", sessionId='" + sessionId + '\'' +
                '}';
    }
}
