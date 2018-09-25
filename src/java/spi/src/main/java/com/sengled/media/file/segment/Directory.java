package com.sengled.media.file.segment;

import java.io.File;
import java.util.Collection;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBufUtil;
import io.netty.util.internal.SystemPropertyUtil;

class Directory {
    private static final String SUFFIX_FLVTMP = "flvtmp";

    private static final Logger LOGGER = LoggerFactory.getLogger(Directory.class);

    private static final File ROOT;
    private static final File[] ROOT_SUBS = new File[16];
    static {
        // 读取缓存文件
        String key = "rtsp.recorder.dir";
        ROOT = new File(SystemPropertyUtil.get(key, "/dev/shm/mediav3"));
        LOGGER.info("-D{}={} used", key, ROOT.getAbsolutePath());
        
        // 子目录，访问更快一些
        for (int i = 0; i < ROOT_SUBS.length; i++) {
            ROOT_SUBS[i] = new File(ROOT, ByteBufUtil.hexDump(new byte[] {(byte)i}).toUpperCase());
        }

        try { 
            // 创建空目录
            for (int i = 0; i < ROOT_SUBS.length; i++) {
                ROOT_SUBS[i].mkdirs();
            }

            // 清理过期的临时文件
            deleteExpiredFiles(0);
        } catch (Exception e) {
            LOGGER.error("Fail clean {}", ROOT, e);
        }
        
        final long peroid = TimeUnit.MINUTES.toMillis(5);
        new Timer(true).scheduleAtFixedRate(new TimerTask() {
            
            @Override
            public void run() {
                try {
                    deleteExpiredFiles(peroid);
                } catch (Exception e) {
                    LOGGER.error("{}", e.getMessage(), e);
                }
            }
        }, 30000, peroid);
    }

    private static void deleteExpiredFiles(long timeout) {
        Collection<File> files = FileUtils.listFiles(ROOT, new String[] {SUFFIX_FLVTMP}, true);
        
        final long now = System.currentTimeMillis();
        for (File file : files) {
            try {
                if (now - file.lastModified() > timeout) {
                    boolean deleted = file.delete();
                    LOGGER.error("{} is expired, delete it {}", file.getAbsolutePath(), deleted);
                } 
            } catch (Exception e) {
                LOGGER.error("File delete {}", file.getAbsolutePath(), e);
            }
        }
    }
    
    public static File newTempFile(String name) {
        String random = DateFormatUtils.format(System.currentTimeMillis(), "yyyyMMddHHmmss");
        return new File(nextDir(name), name + "-" + random + "." + SUFFIX_FLVTMP);
    }
    
    private static File nextDir(String token) {
        final int abs = Math.abs(token.hashCode());
        
        File nextDir = ROOT_SUBS[abs % ROOT_SUBS.length];

        if (!nextDir.exists()) {
            nextDir.mkdirs();
        }

        return nextDir;
    }
    
}
