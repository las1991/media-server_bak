package com.sengled.media.ssl;

import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.sengled.media.server.rtsp.RTSP;

import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.UnpooledByteBufAllocator;
import io.netty.handler.ssl.ApplicationProtocolConfig;
import io.netty.handler.ssl.ApplicationProtocolConfig.Protocol;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectedListenerFailureBehavior;
import io.netty.handler.ssl.ApplicationProtocolConfig.SelectorFailureBehavior;
import io.netty.handler.ssl.CipherSuiteFilter;
import io.netty.handler.ssl.ClientAuth;
import io.netty.handler.ssl.OpenSsl;
import io.netty.handler.ssl.SslContext;
import io.netty.handler.ssl.SslContextBuilder;
import io.netty.handler.ssl.SslHandler;
import io.netty.handler.ssl.SslProvider;

/**
 * netty 集成 SSL 加密
 *
 * @author chenxh
 */
public class SSL {
    private static final Logger LOGGER = LoggerFactory.getLogger(SSL.class);

    private static final SslContext SSL_CONTEXT;

    static {


        SslContext sslContext = null;
        try {
            // 添加对 openssl server.pem 文件的支持
            // http://stackoverflow.com/questions/6559272/algid-parse-error-not-a-sequence
            java.security.Security.addProvider(
                    new org.bouncycastle.jce.provider.BouncyCastleProvider()
            );


            File pemFile = new File(RTSP.OPENSSL_PEM_FILE);
            SslContextBuilder builder = SslContextBuilder
                    .forServer(pemFile, pemFile)
                    .sslProvider(OpenSsl.isAvailable() ? SslProvider.OPENSSL : SslProvider.JDK)
                    .trustManager(pemFile);

            // ciphers
            // 老固件：TLS_RSA_WITH_AES_128_CBC_SHA256
            // 新固件：ECDHE_RSA_WITH_AES_128_GCM_SHA256
            builder.ciphers(Arrays.asList("TLS", "ECDHE"), new CipherSuiteFilter() {
                @Override
                public String[] filterCipherSuites(Iterable<String> ciphers, List<String> defaultCiphers,
                                                   Set<String> supportedCiphers) {
                    return supportedCiphers.toArray(new String[0]);
                }
            });

            // apn
            ApplicationProtocolConfig apn = new ApplicationProtocolConfig(
                    OpenSsl.isAlpnSupported() ? Protocol.NPN_AND_ALPN : Protocol.NPN,
                    // is only supported by openssl and jdk
                    SelectorFailureBehavior.NO_ADVERTISE,
                    SelectedListenerFailureBehavior.ACCEPT,
                    // "SSLv2Hello,SSLv2,SSLv3,TLSv1,TLSv1.1,TLSv1.2".split(",")
                    "TLSv1,TLSv1.1,TLSv1.2".split(",")
            );
            builder.applicationProtocolConfig(apn);

            // 用户验证
            builder.clientAuth(ClientAuth.NONE);

            sslContext = builder.build();

            LOGGER.info("      ssl:{}", sslContext);
            LOGGER.info("     alpn:{}", OpenSsl.isAlpnSupported());
            LOGGER.info("  ciphers:{}", sslContext.cipherSuites());
            LOGGER.info("  openssl:{}, {}", OpenSsl.versionString(), OpenSsl.version());
        } catch (Exception ex) {
            LOGGER.error("Fail load ssl for {}.", ex.getMessage(), ex);
        }

        SSL_CONTEXT = sslContext;
    }

    private SSL() {
    }

    public static SslHandler newHandler(ByteBufAllocator alloc) {
        return SSL_CONTEXT.newHandler(alloc);
    }


    public static SSLEngine newSSLEngine(String host, Integer port) {
        if (null != host && null != port) {
            return SSL_CONTEXT.newEngine(UnpooledByteBufAllocator.DEFAULT, host, port);
        }
        return SSL_CONTEXT.newEngine(UnpooledByteBufAllocator.DEFAULT);
    }

    public static SSLEngine newSSLEngine() {
        return newSSLEngine(null, null);
    }


    public static boolean isAvaliable() {
        return null != SSL_CONTEXT;
    }
}
