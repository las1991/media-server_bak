package com.sengled.cloud.devops.mdflags;

import java.io.IOException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import com.sengled.media.MediaSink;
import com.sengled.media.MediaSource;
import com.sengled.media.SingletonMediaSink;
import com.sengled.media.server.MutableFramePacket;

public class MdFlagsMediaSink extends SingletonMediaSink {
    public static final String SESSION_KEY = MdFlagsMediaSink.class.getName();

    private static final Logger LOGGER = LoggerFactory.getLogger(MdFlagsMediaSink.class);

    private final WebSocketSession ws;

    public static void close(WebSocketSession ws) {
        MediaSink removed = (MediaSink) ws.getAttributes().remove(SESSION_KEY);
        if (null != removed) {
            removed.close();
        }
    }

    public MdFlagsMediaSink(MediaSource source, WebSocketSession ws) {
        super(source);
        this.ws = ws;
    }

    @Override
    protected void onStarted() {
        final String token = source.getToken();
        try {
            ws.sendMessage(new TextMessage("[" + token + "] started " + MdFlagsMediaSink.this));
        } catch (IOException e) {
            LOGGER.error("[{}] can't send data to websocket", token, e);
        }
        // 把上一个关掉
        MediaSink removed = (MediaSink) ws.getAttributes().put(SESSION_KEY, MdFlagsMediaSink.this);
        if (null != removed) {
            removed.close();
        }
    }
    
    @Override
    protected void onClosed() {
        final String token = source.getToken();
        if (ws.isOpen()) {
            try {
                ws.sendMessage(new TextMessage("[" + token + "] removed with " + MdFlagsMediaSink.this));
            } catch (IOException e) {
                LOGGER.error("close Websocket failed", e);
            }
        }
        LOGGER.info("[{}] removed {}", token, MdFlagsMediaSink.this);
    }
    
    @Override
    public void onFrame(MutableFramePacket frame) throws IOException {
        try {
            if (frame.getFlags() > 0 && ws.isOpen()) {
                ws.sendMessage(new TextMessage(frame.toString()));
            }
        } finally {
            frame.release();
        }
    }

}
