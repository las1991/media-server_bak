package com.sengled.media.server.rtsp;

import java.io.File;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.netty.util.internal.SystemPropertyUtil;

public class RTSP {
    private static final Logger LOGGER = LoggerFactory.getLogger(RTSP.class);

    /**
     * openssl 的证书
     **/
    public static final String OPENSSL_PEM_FILE;


    /**
     * 工作线程实例数量
     */
    public static final int SERVER_MAX_WORKER_THREADS;


    /**
     * 为了实现秒开，在服务器段缓存的 RTP 数据大小
     */
    public static final int DISPATCHER_BUFFER_BYTES;


    /**
     * 如果服务端 send-Q 超过这个大小，则开始 flush
     **/
    public static final int PLAYER_MIN_BUFFER_BYTES;
    /**
     * 如果服务端 send-Q 超过这个大小，则开始丢包
     */
    public static final int PLAYER_MAX_BUFFER_BYTES;

    static {
        String key;
        final int availableProcessors = Runtime.getRuntime().availableProcessors();


        key = "rtsp.server.ssl.pem";
        // 生产环境的证书
        String openSslPemFile = SystemPropertyUtil.get(key, "/opt/sengled/apps/haproxy/server.pem");
        if (!new File(openSslPemFile).exists()) {
            // 测试环境的证书
            openSslPemFile = "/opt/sengled/apps/content/server.pem";
        }

        OPENSSL_PEM_FILE = openSslPemFile;
        LOGGER.info("-D{}={}", key, OPENSSL_PEM_FILE);


        // 线程数太多， 会造成直播 FLV 卡顿
        key = "rtsp.server.worker.maxThreads";
        SERVER_MAX_WORKER_THREADS = SystemPropertyUtil.getInt(key, 2 * availableProcessors);
        LOGGER.info("-D{}={}", key, SERVER_MAX_WORKER_THREADS);

        key = "rtsp.dispatcher.buffer.bytes";
        String bufferBytes = SystemPropertyUtil.get(key, "256k").toLowerCase();
        bufferBytes = bufferBytes.toLowerCase();
        if (bufferBytes.endsWith("m")) {
            DISPATCHER_BUFFER_BYTES = 1024 * 1024 * Integer.valueOf(bufferBytes.substring(0, bufferBytes.length() - 1));
        } else if (bufferBytes.endsWith("k")) {
            DISPATCHER_BUFFER_BYTES = 1024 * Integer.valueOf(bufferBytes.substring(0, bufferBytes.length() - 1));
        } else {
            DISPATCHER_BUFFER_BYTES = Integer.valueOf(bufferBytes.substring(0, bufferBytes.length() - 1));
        }
        LOGGER.info("-D{}={}k", key, DISPATCHER_BUFFER_BYTES / 1024);



        key = "rtsp.player.min.buffer.bytes";
        PLAYER_MIN_BUFFER_BYTES = SystemPropertyUtil.getInt(key, DISPATCHER_BUFFER_BYTES / 2);
        LOGGER.info("-D{}={}", key, PLAYER_MIN_BUFFER_BYTES);

        key = "rtsp.player.max.buffer.bytes";
        PLAYER_MAX_BUFFER_BYTES = SystemPropertyUtil.getInt(key, DISPATCHER_BUFFER_BYTES * 2);
        LOGGER.info("-D{}={}", key, PLAYER_MAX_BUFFER_BYTES);
    }

    private RTSP() {
    }
}
