package com.sengled.media.server.rtsp;

import com.codahale.metrics.Counter;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.google.common.eventbus.EventBus;
import com.sengled.media.event.Event;
import com.sengled.media.server.rtsp.codec.RtspObjectDecoder;
import com.sengled.media.ssl.SSL;
import io.netty.bootstrap.ServerBootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.epoll.Epoll;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.concurrent.DefaultEventExecutorGroup;
import io.netty.util.concurrent.DefaultThreadFactory;
import io.netty.util.concurrent.EventExecutorGroup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * RTSP 服务
 * <p>
 * <ul>
 * <li>1、服务有 {@link SessionMode#PUBLISH PUBLISH}, {@link SessionMode#PLAY PLAY} 两种运用场景，
 * 分别用于：向服务器推流，和从播放器拉流。
 * </li>
 * <li>2、利用 {@link RtspSession} 实现流内容共享。 服务器会把  {@link SessionMode#PUBLISH PUBLISH} 模式收取的流，转发给 {@link SessionMode#PLAY PLAY} 模式的客户端，</li>
 *
 * </ul>
 * </p>
 *
 * @author 陈修恒
 * @date 2016年4月15日
 */
public class RtspServer {
    private static final Logger LOGGER = LoggerFactory.getLogger(RtspServer.class);

    private final RtspServerContext serverContext;

    // 连接数统计
    private Counter channelsCounter;
    // input 方向的 IO 流量
    private Meter inboundIoMeter;
    // output 方向的 IO 流量
    private Meter outboundIoMeter;


    private final int numBossThreads = 1;
    private final int maxWorkerThreads = RTSP.SERVER_MAX_WORKER_THREADS;
    private final EventLoopGroup bossGroup;
    private final EventLoopGroup workerGroup;
    public final static EventExecutorGroup EXECUTOR_GROUP = new DefaultEventExecutorGroup(RTSP.SERVER_MAX_WORKER_THREADS, new DefaultThreadFactory("netty-executor", Thread.MAX_PRIORITY))
    private final Class<? extends ServerChannel> channelClass;

    /**
     * 监听的接口
     */
    private List<ListenWork> works = new ArrayList<>();


    public static RtspServerBuilder builder() {
        return new RtspServerBuilder();
    }

    RtspServer(RtspServerContext serverContext, MetricRegistry registry) {
        this.serverContext = serverContext;
        this.channelsCounter = registry.counter(MetricRegistry.name(getName(), "channels"));
        this.inboundIoMeter = registry.meter(MetricRegistry.name(getName(), "inbound"));
        this.outboundIoMeter = registry.meter(MetricRegistry.name(getName(), "outbound"));


        final DefaultThreadFactory bossThreadFactory = new DefaultThreadFactory("netty-boss", Thread.MAX_PRIORITY);
        final DefaultThreadFactory workerThreadFactory = new DefaultThreadFactory("netty-worker", Thread.MAX_PRIORITY);
        if (Epoll.isAvailable()) {
            bossGroup = new EpollEventLoopGroup(numBossThreads, bossThreadFactory);
            workerGroup = new EpollEventLoopGroup(maxWorkerThreads, workerThreadFactory);
            channelClass = EpollServerSocketChannel.class;
        } else {
            bossGroup = new NioEventLoopGroup(numBossThreads, bossThreadFactory);
            workerGroup = new NioEventLoopGroup(maxWorkerThreads, workerThreadFactory);
            channelClass = NioServerSocketChannel.class;
        }


        LOGGER.info("epoll {} available, use {} as socket channel", (Epoll.isAvailable() ? "is" : "is NOT"), channelClass);
    }

    public RtspServerContext getServerContext() {
        return serverContext;
    }

    public void postEvent(Object event) {
        serverContext.getEventBus().post(event);
    }

    /**
     * @param listener
     * @see EventBus
     * @see Event
     */
    public void addEventListener(Object listener) {
        serverContext.getEventBus().register(listener);
    }


    public String getName() {
        return serverContext.getName();
    }

    private static class RtspServerChannelOutboundHandler extends ChannelOutboundHandlerAdapter {
        private RtspServer server;

        public RtspServerChannelOutboundHandler(RtspServer server) {
            super();
            this.server = server;
        }

        @Override
        public void write(ChannelHandlerContext ctx,
                          Object msg,
                          ChannelPromise promise) throws Exception {
            final ByteBuf buf = (ByteBuf) msg;
            final int writableBytes = buf.readableBytes();

            ctx.write(msg, promise);

            server.outboundIoMeter.mark(8 * writableBytes);
        }
    }

    private static class RtspServerChannelInboundHandler extends ChannelInboundHandlerAdapter {
        private RtspServer server;

        public RtspServerChannelInboundHandler(RtspServer server) {
            super();
            this.server = server;
        }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelActive();

            // 新来一个连接
            server.channelsCounter.inc();
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            ctx.fireChannelInactive();

            // 断开了一个连接
            server.channelsCounter.dec();
        }

        @Override
        public void channelRead(ChannelHandlerContext ctx,
                                Object msg) throws Exception {
            final ByteBuf buf = (ByteBuf) msg;
            final int readableBytes = buf.readableBytes();

            ctx.fireChannelRead(msg);

            // 统计输入流量
            server.inboundIoMeter.mark(8 * (readableBytes - buf.readableBytes()));
        }
    }


    public void shutdown() {
        // 停止监听端口
        for (ListenWork work : works) {
            try {
                work.shutdown();
            } catch (Exception ex) {
                LOGGER.warn("Fail shutdown {} for {}", work, ex.getMessage(), ex);
            }
        }

        // 关闭链接
        LOGGER.info("boss group shutdown");
        bossGroup.shutdownGracefully();

        // 关闭工作线程
        LOGGER.info("work group shutdown");
        workerGroup.shutdownGracefully();
    }


    public void listen(RtspServerConfig config) {
        works.add(new ListenWork(getName(), config).listen(this));
    }

    private ServerBootstrap newServerBootstrap(ChannelInitializer<Channel> initializer) {
        ServerBootstrap bootstrap = new ServerBootstrap();

        // socket 参数都使用系统默认的，这样方便运维调试配置
        bootstrap.childOption(ChannelOption.SO_SNDBUF, RTSP.PLAYER_MAX_BUFFER_BYTES);
        bootstrap.childOption(ChannelOption.SO_RCVBUF, RTSP.PLAYER_MIN_BUFFER_BYTES);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_HIGH_WATER_MARK, RTSP.PLAYER_MAX_BUFFER_BYTES);
        bootstrap.childOption(ChannelOption.WRITE_BUFFER_LOW_WATER_MARK, RTSP.PLAYER_MIN_BUFFER_BYTES);

        // 减少延时
        bootstrap.childOption(ChannelOption.TCP_NODELAY, true);
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, true);
        bootstrap.childOption(ChannelOption.SO_REUSEADDR, false);

        // bootstrap.childOption(EpollChannelOption.EPOLL_MODE,
        // EpollMode.EDGE_TRIGGERED);
        // 
        // bootstrap.childOption(ChannelOption.SO_LINGER, 3); // 等待把 rtcp
        // 包发送出去

        bootstrap.group(bossGroup, workerGroup);
        bootstrap.channel(channelClass);
        bootstrap.childHandler(initializer);

        return bootstrap;
    }

    private static class ListenWork {
        private final RtspServerConfig config;
        private ServerBootstrap bootstrap = null;

        public ListenWork(String name, RtspServerConfig config) {
            this.config = config;
        }


        public ListenWork listen(RtspServer server) {
            bootstrap =
                    server.newServerBootstrap(new ChannelInitializer<Channel>() {
                        @Override
                        protected void initChannel(Channel ch) throws Exception {
                            final ChannelPipeline pipeline = ch.pipeline();
                            final IdleStateHandler idleStateHandler = new IdleStateHandler(30, 30, 60);

                            // ssl
                            if (config.isUseSSL()) {
                                pipeline.addLast(SSL.newHandler(ch.alloc()));
                            }

                            // 流量统计
                            pipeline.addLast(new RtspServerChannelInboundHandler(server));
                            pipeline.addLast(new RtspServerChannelOutboundHandler(server));

                            // idle 事件
                            pipeline.addLast("IDEL_STATE_HANDLER", idleStateHandler);
                            pipeline.addLast("DECODER", new RtspObjectDecoder());

                            // 选择协议
                            pipeline.addLast(new RtspServerPrepareHandler(pipeline, server, config));

                        }
                    });

            // 监听
            String host = config.getHost();
            int port = config.getPort();
            try {
                LOGGER.info("bind {}://{}:{}", config.isUseSSL() ? "rtsps" : "rtsp", host, port);
                LOGGER.info("allows {}", Arrays.toString(config.getMethods()));
                ChannelFuture future = bootstrap.bind(host, port);

                future.get();
            } catch (Exception e) {
                LOGGER.error("can't listen on {}:{} for {}", host, port, e.getMessage(), e);

                // 监听端口失败，3s 后把进程杀掉
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("bind(" + host + "," + port + ") failed: Address already in use");
                            e.printStackTrace();
                        } finally {
                            System.exit(-1);
                        }
                    }
                }).start();

                throw new RuntimeException("can't listen [" + host + ":" + port + "]", e);
            }

            return this;
        }

        public void shutdown() {
        }
    }

}
