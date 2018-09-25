package com.sengled.media.file.segment;

import java.io.Closeable;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSource;
import com.sengled.media.clock.Rational;
import com.sengled.media.file.flv.FlvOutputEncoder;
import com.sengled.media.server.MutableFramePacket;
import io.netty.buffer.ByteBuf;
import io.netty.util.ReferenceCountUtil;

public class Segment implements Closeable {
	private static final int NO_VALUE = -1;

    private static final Logger LOGGER = LoggerFactory.getLogger(Segment.class);

	private final long created;
	private final MediaSource source;
	private final File tmpFile;
	
	private FlvOutputEncoder encoder;
	private long firstFrameTime = NO_VALUE;
	private long nextFrameTime = NO_VALUE;
	
	private AtomicBoolean closed = new AtomicBoolean(false);
	private FileOutputStream channel;
	private File saveAsFile;

	/**
	 * flvTag.pts = frame.pts - created
	 * @param alloc 
	 * 
	 * @param src
	 * @param out
	 * @param created  
	 * @throws IOException 
	 */
	public Segment(MediaSource src, final long created) {
	    
	    this.source = src;
		this.created = created;
        this.tmpFile = Directory.newTempFile(src.getToken());
		try {
		    this.channel = new FileOutputStream(tmpFile);
            LOGGER.debug("createTempFile {}", tmpFile.getAbsolutePath());
		} catch (IOException e) {
            throw new IllegalArgumentException("Fail open tmp file " + tmpFile.getAbsolutePath());
        }
	}

    @Override
    public void close() throws IOException {
        if (closed.compareAndSet(false, true)) {
            doClose();
        }
    }
    
    @Override
    protected void finalize() throws Throwable {
        close(); // 二次关闭
    }
    
	protected void doClose() throws IOException {
        try {
            if(channel.getChannel().isOpen()) {
                channel.close();
            }
        } catch(IOException ex) {
            LOGGER.error("Fail close output channel {}", tmpFile.getAbsolutePath());
            throw ex;
        } finally {
            try {
                if (tmpFile.exists() && saveAsFile != null) {
                    saveAs(tmpFile, saveAsFile);
                } else if (null == saveAsFile) {
                    LOGGER.error("[{}] save as file is NULL", getToken());
                }
            } finally {
                if (tmpFile.exists()) {
                    boolean deleted = tmpFile.delete();

                    // 源文件应该删除的,实际上可能没删除
                    if (!deleted && tmpFile.exists()) {
                        LOGGER.error("Fail delete src {}, length = {}", tmpFile.getAbsolutePath(), tmpFile.length());
                    } else {
                        LOGGER.debug("{} delete = {}, existed = {}", tmpFile.getAbsolutePath(), deleted, tmpFile.exists());
                    }
                }
            }
        }
	}


    private void saveAs(File srcFile, File saveAsFile) {
       try {
           if (saveAsFile.exists() && saveAsFile.length() > 0) {
               LOGGER.error("exist {}, length = {}", saveAsFile.getAbsolutePath(), saveAsFile.length());
           } else {
               saveAsFile.delete();
               FileUtils.moveFile(srcFile, saveAsFile);
               LOGGER.debug("createNewFile {}, length = {}, src = {}", saveAsFile.getAbsolutePath(), saveAsFile.length(), srcFile.getName());
           }
       } catch (Exception e) {
           LOGGER.error("Fail move {} to {}", tmpFile.getAbsolutePath(), saveAsFile.getAbsolutePath(), e);
       }
    }

	
	/**
	 * @param src  这个 frame 将会被主动释放
	 * @throws IOException
	 */
	public void appendFrame(MutableFramePacket src) throws IOException {
	    ByteBuf out = src.content().alloc().buffer(src.content().readableBytes() + 16);
	    try {
	        // eoncode as flv tag
            writeInterleaveFrame(src, out);
	        
            // save as file
            int count = 0;
            while(out.isReadable() && count ++ < 32) {
                out.readBytes(channel.getChannel(), out.readableBytes());
            }
	    } finally {
	        out.release();
	        ReferenceCountUtil.release(src);
        }
	}

	private void writeInterleaveFrame(MutableFramePacket frame, ByteBuf out) {
		long time = frame.getTime(Rational.MILLISECONDS);
		if (firstFrameTime < 0) {
			firstFrameTime = time;
			nextFrameTime = time + Math.max(frame.getDuration(Rational.MILLISECONDS), 0);
		}
		
		if (null == encoder) {
            encoder = new FlvOutputEncoder(source.getStreamContexts());

            // flv 头
            encoder.setCreated(created);
            encoder.writeFlvHeader(out);
		}

		// flv 内容
		frame.setTime(Math.max(time - created, 0), Rational.MILLISECONDS);
		encoder.encode(frame.retain(), out);
	}

	
	public long getDuration() {
	    if (firstFrameTime == NO_VALUE) {
	        LOGGER.debug("[{}], duration = {}.", getToken(), nextFrameTime - firstFrameTime);
	    }

		return firstFrameTime != NO_VALUE ? nextFrameTime - firstFrameTime : 0;
	}

	public void setNextFrameTime(long nextFrameTime) {
		this.nextFrameTime = nextFrameTime;
	}
	
	public long getFirstFrameTime() {
		return firstFrameTime;
	}
	
    public String getToken() {
        return source.getToken();
    }

    public boolean isClosed() {
        return closed.get();
    }
    
    public void setSaveAsFile(File saveAsFile) {
        this.saveAsFile = saveAsFile;
    }
}
