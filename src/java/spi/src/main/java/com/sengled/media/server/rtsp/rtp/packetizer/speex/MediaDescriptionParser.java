package com.sengled.media.server.rtsp.rtp.packetizer.speex;

import javax.sdp.Media;
import javax.sdp.MediaDescription;
import javax.sdp.SdpParseException;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.sengled.media.clock.Rational;

/**
 * m=audio 0 RTP/AVP 97
 * b=AS:15
 * a=rtpmap:97 speex/8000
 * a=fmtp:97 vbr=off
 * a=control:streamid=1
 */
public class MediaDescriptionParser implements com.sengled.media.server.rtsp.rtp.MediaDescriptionParser {
    private static final Logger LOGGER = LoggerFactory.getLogger(MediaDescriptionParser.class);

    @Override
    public boolean accept(Media media, RtpMapAttribute rtpmap) {
        try {
            if ("SPEEX".equalsIgnoreCase(rtpmap.getCodec())) {
                return true;
            }
            
        } catch (Exception ex) {
        }

        return false;
    }

    @Override
    public SpeexRtpDePacketizer parse(RtpMapAttribute rtpmap, MediaDescription md, int streamIndex)
            throws SdpParseException {
        int numChannels = Math.max(1, rtpmap.getCodecParams());
        int sampleRate = rtpmap.getClockRate();
        int sampleSize = 16;
        Rational timeUnit = Rational.valueOf(rtpmap.getClockRate());
        
        // FLV  默认只支持 16KHz 的时钟
        if (16000 != rtpmap.getClockRate()) {
            LOGGER.warn("illegal clock rate {}", rtpmap.getClockRate());
        }

        SpeexRtpDePacketizer ctx = new SpeexRtpDePacketizer(streamIndex, timeUnit);
        ctx.setNumChannels(numChannels);
        ctx.setSampleRate(sampleRate);
        ctx.setSampleSize(sampleSize);
        return ctx;
    }
}
