package com.sengled.media.server.rtsp.rtp;

import java.util.Vector;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;

import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.server.MediaCodecExtra;
import com.sengled.media.server.rtsp.rtp.packetizer.RtpDePacketizer;
import com.sengled.media.server.rtsp.sdp.SdpParser;

public class MediaDescriptionParserFactory {
    //private static final Logger LOGGER = LoggerFactory.getLogger(MediaDescriptionParserFactory.class);
    private static final MediaDescriptionParser[] MEDIA_DESCRIPTION_PARSERS;

    static {
        MEDIA_DESCRIPTION_PARSERS = new MediaDescriptionParser[]{
                new com.sengled.media.server.rtsp.rtp.packetizer.h264.MediaDescriptionParser(),
                new com.sengled.media.server.rtsp.rtp.packetizer.mpeg4.MediaDescriptionParser(),
                new com.sengled.media.server.rtsp.rtp.packetizer.g711.MediaDescriptionParser(),
                new com.sengled.media.server.rtsp.rtp.packetizer.speex.MediaDescriptionParser(),
        };
    }

    private MediaDescriptionParserFactory() {
    }

    public static RtpDePacketizer<? extends MediaCodecExtra>[] parse(SessionDescription sd) {

        try {
            if (null == sd) {
                throw new IllegalArgumentException("Arguments has NULL");
            }

            @SuppressWarnings("unchecked")
            Vector<MediaDescription> mds = sd.getMediaDescriptions(true);

            RtpDePacketizer<? extends MediaCodecExtra>[] rtpStreams = new RtpDePacketizer[mds.size()];

            int numStreams = 0;
            for (MediaDescription mediaDescription : mds) {
                String control = mediaDescription.getAttribute("control");

                int streamIndex = numStreams++;

                RtpDePacketizer<? extends MediaCodecExtra> rtpStream;
                rtpStream = MediaDescriptionParserFactory.parse(mediaDescription, streamIndex);
                rtpStream.setControl(control);

                rtpStreams[streamIndex] = rtpStream;
            }

            return rtpStreams;
        } catch (javax.sdp.SdpException e) {
            throw new IllegalArgumentException("Illegal sdp: " + sd, e);
        }
    }

    public static RtpDePacketizer<? extends MediaCodecExtra> parse(MediaDescription md, int streamIndex) throws SdpParseException {
        final javax.sdp.Media media = md.getMedia();
        RtpMapAttribute rtpmap = SdpParser.getRtpMap(md);

        // rtpmap 解析不了
        if (null == rtpmap) {
            return null;
        }

        for (MediaDescriptionParser mediaDescriptionParser : MEDIA_DESCRIPTION_PARSERS) {
            if (mediaDescriptionParser.accept(media, rtpmap)) {
                return mediaDescriptionParser.parse(rtpmap, md, streamIndex);
            }
        }


        return null;
    }


}
