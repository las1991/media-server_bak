package com.sengled.media.bootstrap.spring;

import org.eclipse.jetty.http.HttpScheme;
import org.eclipse.jetty.http.HttpVersion;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.embedded.ConfigurableEmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerCustomizer;
import org.springframework.boot.context.embedded.jetty.JettyEmbeddedServletContainerFactory;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.util.ResourceUtils;
import com.sengled.media.ssl.SSL;
import java.io.FileNotFoundException;
import java.net.InetSocketAddress;
import javax.net.ssl.SSLEngine;

@Configuration
@EnableConfigurationProperties(HttpsProperties.class)
public class HttpsConfiguration {
    private static final Logger LOGGER = LoggerFactory.getLogger(HttpsConfiguration.class);

    @Autowired
    private HttpsProperties properties;
    @Value("${server.port}")
    private int httpPort;

    @Bean
    public EmbeddedServletContainerCustomizer servletContainerCustomizer() {
        return new EmbeddedServletContainerCustomizer() {

            @Override
            public void customize(ConfigurableEmbeddedServletContainer container) {
                if (container instanceof JettyEmbeddedServletContainerFactory) {
                    customizeJetty((JettyEmbeddedServletContainerFactory) container);
                }
            }

            private void customizeJetty(JettyEmbeddedServletContainerFactory container) {

                container.addServerCustomizers((Server server) -> {
                    // HTTP
                    ServerConnector connector = new ServerConnector(server);
                    connector.setPort(httpPort);

                    // HTTPS
                    SslContextFactory sslContextFactory = new SslContextFactory() {
                        public SSLEngine newSSLEngine(InetSocketAddress address) {
                            if (address == null)
                                return SSL.newSSLEngine();

                            boolean useHostName = getNeedClientAuth();
                            String hostName = useHostName ? address.getHostName() : address.getAddress().getHostAddress();
                            return SSL.newSSLEngine(hostName, address.getPort());
                        }
                    };

                    HttpConfiguration config = new HttpConfiguration();
                    config.setSecureScheme(HttpScheme.HTTPS.asString());
                    config.addCustomizer(new SecureRequestCustomizer());

                    ServerConnector sslConnector = new ServerConnector(
                            server,
                            new SslConnectionFactory(sslContextFactory, HttpVersion.HTTP_1_1.asString()),
                            new HttpConnectionFactory(config));
                    sslConnector.setPort(properties.getPort());

                    server.setConnectors(new Connector[]{connector, sslConnector});
                    LOGGER.info(" Jetty SSL setting successful." + properties.getPort());
                });
            }
        };
    }

}
