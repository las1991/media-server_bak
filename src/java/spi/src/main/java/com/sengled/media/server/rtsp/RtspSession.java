package com.sengled.media.server.rtsp;

import com.sengled.media.MediaSession;
import com.sengled.media.url.URLObject;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.lang.RandomStringUtils;
import org.apache.commons.lang.time.DateFormatUtils;

/**
 * 一个 rtsp 会话。 <p> <p> <ul> <li>1、通过 {@link #onRtpEvent(RtpPkt)} 把流数据分发给他的监听者</li> <li>2、通过 {@link
 * #getSessionDescription()} 获取 SDP 信息</li> </ul>
 *
 * @author 陈修恒
 * @date 2016年4月15日
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
        StringBuilder buf = new StringBuilder();
        buf.append("{").append(getClass().getSimpleName());
        buf.append(" ");
        buf.append(", created=").append(DateFormatUtils.format(lastModified, "yyyy-MM-dd HH:mm:ss.SSS"));

        buf.append("}");
        return buf.toString();
    }


}
