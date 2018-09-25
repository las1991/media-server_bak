package com.sengled.media.bootstrap.spring;

import java.util.Arrays;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

import org.apache.http.client.config.RequestConfig;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.DefaultConnectionKeepAliveStrategy;
import org.apache.http.impl.client.HttpClients;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.jetty.server.Handler;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.Slf4jRequestLog;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.server.handler.RequestLogHandler;
import org.eclipse.jetty.servlet.DefaultServlet;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.jetty.JettyServerCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowBuilderCustomizer;
import org.springframework.boot.context.embedded.undertow.UndertowEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.web.servlet.ServletRegistrationBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.repository.configuration.EnableRedisRepositories;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.servlet.resource.ResourceHttpRequestHandler;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import com.codahale.metrics.MetricRegistry;
import com.sengled.media.bootstrap.AmazonAwsConfig;
import com.sengled.media.bootstrap.metrics.MetricsGraphicsController;
import com.sengled.media.bootstrap.osmonitor.OSMonitor;
import com.sengled.media.bootstrap.redis.Redis;

@SpringBootApplication(scanBasePackageClasses = {Redis.class})
@Configuration
@ConfigurationProperties
@EnableRedisRepositories
@EnableWebSocket
public class DefaultBootApplication {
    private static final Logger LOGGER = LoggerFactory.getLogger(DefaultBootApplication.class);

    @Value("${rest.read.timeout:10000}")
    private int restReadTimeout;

    @Value("${rest.connection.timeout:5000}")
    private int restConnectionTimeout;


    @Bean
    public Slf4jRequestLog getSlf4jRequestLog() {
        Slf4jRequestLog requestLogger = new Slf4jRequestLog();
        requestLogger.setLogLatency(true);
        requestLogger.setLogDateFormat("yyyy-MM-dd HH:mm:ss.SSS");


        return requestLogger;
    }

    @Bean
    public JettyEmbeddedServletContainerFactory embeddedServletContainerFactory(Slf4jRequestLog rli) {
        JettyEmbeddedServletContainerFactory factory;
        factory = new JettyEmbeddedServletContainerFactory();

        factory.addServerCustomizers(new JettyServerCustomizer() {

            @Override
            public void customize(Server server) {
                HandlerCollection handlers = new HandlerCollection();
                for (Handler handler : server.getHandlers()) {
                    handlers.addHandler(handler);
                }

                // for jetty 9.3+
                RequestLogHandler reqLogs = new RequestLogHandler();
                reqLogs.setServer(server);
                reqLogs.setRequestLog(rli);
                handlers.addHandler(reqLogs);

                try {
                    // 先把 logger 启动了
                    rli.start();

                    // 在加到 server 里面取
                    server.setHandler(handlers);
                } catch (Exception e) {
                    LOGGER.error("{}", e.getMessage(), e);
                }
            }
        });

        return factory;
    }


    @Bean
    public AmazonAwsConfig getAwsConfig(@Value("${MEDIA_AWS_REGION}") String awsRegion) {
        return new AmazonAwsConfig(awsRegion);
    }


    /**
     * OS Monitor
     * <p>
     * 用于系统监控
     *
     * @return
     */
    @Bean
    public OSMonitor getOsMonitor() {
        return OSMonitor.getInstance();
    }

    @Bean
    public MetricsGraphicsController metricsGraphicsController() {
        return new MetricsGraphicsController();
    }

    @Bean()
    public MetricRegistry metricRegistry() {
        return new MetricRegistry();
    }

}
