package com.sengled.media.file.segment;

import java.io.File;
import java.io.IOException;
import org.apache.commons.lang.time.DateFormatUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.MediaSource;
import com.sengled.media.server.MutableFramePacket;
import io.netty.buffer.ByteBufAllocator;

/**
 * 把录像切片保存到一个指定目录下面
 * 
 * @author chenxh
 */
public class DefaultMediaSinkSpliterator extends MediaSinkSpliterator {
	private static final Logger LOGGER = LoggerFactory.getLogger(DefaultMediaSinkSpliterator.class);
	
	private final MediaSource source;
	private final SplitStrategy  strategy;
	private final File dir;
	public DefaultMediaSinkSpliterator(MediaSource source, File dir, int flvSegmentDurationInSeconds) {
		super(source);
		
		this.source = source;
		this.strategy = new DefaultSplitStrategy(source, flvSegmentDurationInSeconds);
		this.dir = dir;
	}

	@Override
	protected SplitStrategy strategy() {
		return strategy;
	}
	
	protected File createNewFile(File dir, Segment segment) {
		String name = DateFormatUtils.format(segment.getFirstFrameTime(), "yyyyMMdd-HHmmss-SSS");
		File flvFile = new File(dir, source.getToken() + name + ".flv");
		
		return flvFile;
	}
	
	protected void onSegmentClosed(File flvFile, Segment segment) {
		if (LOGGER.isDebugEnabled()) {
			LOGGER.debug("duration = {}, t = {}, at file {}", 
					DateFormatUtils.formatUTC(segment.getDuration(), "HH:mm:ss.SSS"), 
					DateFormatUtils.formatUTC(segment.getFirstFrameTime(), "yyyy-MM-dd HH:mm:ss.SSS"), 
					flvFile.getAbsolutePath());
		}
	}
	
	class DefaultSplitStrategy implements SplitStrategy {
		private static final int ONE_SECOND = 1000;
		private static final int ONE_MINUTE = 60 * ONE_SECOND;
		private static final int   ONE_HOUR = 60 * ONE_MINUTE;
		private static final int    ONE_DAY = 24 * ONE_HOUR;
		private static final int MAX_FLV_FILE_DURATION = 15 * ONE_DAY;

		private final MediaSource src;
		private final long maxVideoDuration;
		private final long maxDuration;
		
		private long started = -1;
		
		/**
		 * @param src
		 * @param dir 已知的存在的目录
		 */
		public DefaultSplitStrategy(MediaSource src, int flvSegmentDurationInSeconds) {
		    // (50s, 70s)
            int seconds = (int)(flvSegmentDurationInSeconds - 10 + 20 * Math.random());

			this.src = src;
			this.maxVideoDuration = seconds * ONE_SECOND;
			this.maxDuration = maxVideoDuration * 3;
			
			LOGGER.debug("[{}] segment file duration is {}s", src.getToken(), seconds);
		}
		
		@Override
		public boolean isNeedClose(Segment segment, MutableFramePacket next) {
			try {
				boolean needClose = false;
				if (segment.getFirstFrameTime() > 0) {
					// 遇到关键帧，但是时长不够
					if (next.isKeyFrame()) {
						if (segment.getDuration() > maxVideoDuration) {
							needClose = true;
						}
					} else {
						if (segment.getDuration()  > maxDuration) {
							needClose = true;
						}
					}
				}
				return needClose;
			} finally {
				next.release();
			}
		}

		@Override
		public void close(Segment segment) throws IOException {
			try {
			    File flvFile = createNewFile(dir, segment);
			    segment.setSaveAsFile(flvFile);
			} finally {
                segment.close();
            }
		}

		@Override
		public Segment nextSegment(ByteBufAllocator alloc, long nextFrameTime) {
			// flv 用 4 字节保存时间，所以 pts - started 不能太大了
			if (started < 0 || nextFrameTime - started  > MAX_FLV_FILE_DURATION) {
				if(started > 0) {
				    LOGGER.info("[{}] record too long", src.getToken());
				}
				started = nextFrameTime - 5000; 
			}

			return new Segment(src, started);
		}
	}
}
