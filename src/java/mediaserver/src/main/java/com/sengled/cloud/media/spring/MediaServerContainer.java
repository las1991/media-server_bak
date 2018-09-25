package com.sengled.cloud.media.spring;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.filefilter.IOFileFilter;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.SmartLifecycle;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import com.amazonaws.services.kinesis.producer.KinesisProducer;
import com.codahale.metrics.Gauge;
import com.codahale.metrics.MetricRegistry;
import com.sengled.media.bootstrap.sqs.SQSTemplate;
import com.sengled.media.configuration.MediaAnnouncerProperties;
import com.sengled.media.configuration.MediaDescriberProperties;
import com.sengled.media.configuration.ProtocolConfig;
import com.sengled.media.server.rtsp.RtspServer;
import com.sengled.media.server.rtsp.RtspServerConfig;
import com.sengled.media.server.rtsp.RtspServerContext;

/**
 * 初始化一个 Media-Server 容器
 * 
 * @author chenxh
 */
public class MediaServerContainer implements SmartLifecycle {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaServerContainer.class);

    /** 一个 server context 可以关联好多个 server */
    private final RtspServerContext serverContext;

    @Autowired
    private ClientHttpRequestFactory clientHttpRequestFactory;
    
    @Autowired(required=false)
    private SQSTemplate sqsTemplate;
    
    @Autowired(required=false)
    private KinesisProducer kinesisProducer;
    
    @Value("${media.app.rootpath}")
    private String scriptRootPath;
    
    @Autowired
    private MetricRegistry metricRegistry;

    @Autowired
    private MediaDescriberProperties mediaDescriberProperties;

    @Autowired
    private MediaAnnouncerProperties mediaAnnouncerProperties;

    private RtspServer server;
    
    public MediaServerContainer(RtspServerContext serverContext) {
        this.serverContext = serverContext;
    }

    @Override
    public void start() {
        server = RtspServer.builder().withMetricRegistry(metricRegistry).build(serverContext);
        
        // 加载本地脚本
        loadJavaScripts(server);

        // 上行
        listPort(mediaAnnouncerProperties.getTcp(), server);
        // listPort(mediaAnnouncerProperties.getTalkback(), server);
        listPort(mediaAnnouncerProperties.getTls(), server);
        
        // 下行
        listPort(mediaDescriberProperties.getApp(), server);
        listPort(mediaDescriberProperties.getAwsEcho(), server);
        
        
        // metrics-grsphics 记录任务数量
        metricRegistry.register(MetricRegistry.name(server.getName(), "streams"), new Gauge<Long>() {
            @Override
            public Long getValue() {
                return (long) server.getServerContext().getMediaSourceNames().size();
            }
        });
        
        LOGGER.info("{} server started", serverContext.getName());
    }

    private void listPort(ProtocolConfig config, RtspServer server) {
        if (null != config && config.getPort() > 0) {
            final int port = config.getPort();
            server.listen(RtspServerConfig.newInstance(config.isSsl())
                                          .withMethods(config.getMethods().split(","))
                                          .withPort(port)
                                          .withHttpProtocol(config.isSupportedHttp())
                                          .withRtspProtocol());
        }
    }
    
    
    /***
     * 开始加载脚本
     */
    private void loadJavaScripts(RtspServer server) {
        IOFileFilter fileFilter = new SuffixFileFilter("js");
        IOFileFilter dirFilter = null;

        // 脚本运行时变量
        Map<String, Object> runtimeVariables = new HashMap<>();
        runtimeVariables.put("RestTemplate", new RestTemplate(clientHttpRequestFactory));
        runtimeVariables.put("ServerContext", serverContext);
        runtimeVariables.put("KinesisProducer", kinesisProducer);
        runtimeVariables.put("SQSTemplate", sqsTemplate);

        // 脚在脚本文件
        for (File file : FileUtils.listFiles(new File(scriptRootPath), fileFilter, dirFilter)) {
            try {
                serverContext.loadApplication(FilenameUtils.getBaseName(file.getName()), file, runtimeVariables);
            } catch (Exception e) {
                LOGGER.error("{}", e.getMessage(), e);
            }
        }
    }
    
    @Override
    public int getPhase() {
        return 100;
    }
    
    @Override
    public boolean isRunning() {
        return null != server;
    }
    
    @Override
    public boolean isAutoStartup() {
        return true;
    }

    @Override
    public void stop() {
        serverContext.clear();
        
        server.shutdown();
    }
    
    @Override
    public void stop(Runnable callback) {
        try {
            stop();
        } finally {
            callback.run();
        }
    }
    
}
