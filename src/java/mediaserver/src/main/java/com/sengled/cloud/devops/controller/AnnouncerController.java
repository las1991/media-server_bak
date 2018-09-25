package com.sengled.cloud.devops.controller;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import org.apache.commons.io.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import com.sengled.media.MediaDispatcher;
import com.sengled.media.MediaSource;
import com.sengled.media.configuration.MediaAnnouncerProperties;
import com.sengled.media.server.http.FlvStreamingMediaSink;
import com.sengled.media.server.rtsp.RtspServerContext;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.GenericFutureListener;

@Controller
public class AnnouncerController {

    @Autowired
    private RtspServerContext context;

    @Autowired
    private MediaAnnouncerProperties announcer;

    /**
     * @param token
     * @return
     * @throws IOException
     */
    @GetMapping("/announcer/stopHttpClients")
    @ResponseBody
    public Map<String, Object> stopTcpConnections(@RequestParam("token") String token) throws IOException {
        Map<String, Object> result = new HashMap<>();
        result.put("result", "StreamNotFound");

        MediaSource resource = context.getMediaSource(token);
        if (null == resource) {
            return result;
        }


        for (FlvStreamingMediaSink sink : resource.getMediaSinks(FlvStreamingMediaSink.class)) {
            final boolean isHttpClient = isHttpClient(sink);
            if (isHttpClient) {
                resource.submit(new Function<MediaDispatcher, Boolean>() {
                    @Override
                    public Boolean apply(MediaDispatcher dispatcher) {
                        return null != dispatcher && dispatcher.removeMediaSink(sink);
                    }
                }).addListener(new GenericFutureListener() {
                    @Override
                    public void operationComplete(Future future) throws Exception {
                        sink.close();
                    }
                });
            }
        }

        result.put("result", "ok");
        return result;
    }

    private boolean isHttpClient(FlvStreamingMediaSink sink) {
        InetSocketAddress inet = null;

        SocketAddress socketAddress = sink.localAddress();
        if (socketAddress instanceof InetSocketAddress) {
            inet = (InetSocketAddress) socketAddress;
        }

        return null != inet && inet.getPort() == announcer.getTcp().getPort();
    }
}
