package com.sengled.media.server;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.PooledByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.internal.RecyclableArrayList;
import io.netty.util.internal.SystemPropertyUtil;

public class NettySupport {
    private static final Logger LOGGER = LoggerFactory.getLogger(NettySupport.class);
    
    private static final ByteBufAllocator DEFAULT_ALLOCATOR;

    static {
        String allocType = SystemPropertyUtil.get("io.netty.allocator.type", "unpooled").toLowerCase(Locale.US).trim();
        ByteBufAllocator alloc;
        if ("unpooled".equals(allocType)) {
            alloc = UnpooledByteBufAllocator.DEFAULT;
            LOGGER.debug("-Dio.netty.allocator.type: {}", allocType);
        } else if ("pooled".equals(allocType)) {
            alloc = PooledByteBufAllocator.DEFAULT;
            LOGGER.debug("-Dio.netty.allocator.type: {}", allocType);
        } else {
            alloc = UnpooledByteBufAllocator.DEFAULT;
            LOGGER.debug("-Dio.netty.allocator.type: unpooled (unknown: {})", allocType);
        }

        DEFAULT_ALLOCATOR = alloc;
    }
    
    
    public ByteBufAllocator alloc() {
        return DEFAULT_ALLOCATOR;
    }
    
    public RecyclableArrayList newArrayList() {
        return RecyclableArrayList.newInstance();
    }
    
    public void recycle(RecyclableArrayList list) {
        if (null != list) {
            try {
                release(list);
            } finally {
                list.recycle();
            }
        }
    }

    
    public void release(List<?> list) {
        if (list instanceof ArrayList) {
            while(null != list && !list.isEmpty()) {
                int lastIndex = list.size() - 1;
                ReferenceCountUtil.release(list.remove(lastIndex));
            }
        } else {
            while(null != list && !list.isEmpty()) {
                ReferenceCountUtil.release(list.remove(0));
            }
        }
    }
}
