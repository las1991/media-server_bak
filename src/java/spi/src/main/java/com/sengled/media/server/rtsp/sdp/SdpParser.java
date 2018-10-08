package com.sengled.media.server.rtsp.sdp;

import gov.nist.core.StringTokenizer;
import gov.nist.javax.sdp.SessionDescriptionImpl;
import gov.nist.javax.sdp.fields.SDPField;
import gov.nist.javax.sdp.parser.ParserFactory;
import gov.nist.javax.sdp.parser.SDPParser;
import org.apache.commons.lang.StringUtils;
import org.mobicents.media.server.io.sdp.attributes.RtpMapAttribute;
import org.mobicents.media.server.io.sdp.attributes.parser.RtpMapAttributeParser;
import org.slf4j.Logger;

import javax.sdp.MediaDescription;
import javax.sdp.SdpException;
import javax.sdp.SdpParseException;
import javax.sdp.SessionDescription;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Vector;

public class SdpParser {
    private static final Logger LOGGER = org.slf4j.LoggerFactory.getLogger(SdpParser.class);


    @SuppressWarnings("unchecked")
    public static java.util.List<MediaDescription> getMediaDescriptions(SessionDescription sd) {
        try {
            if (null != sd) {
                @SuppressWarnings("rawtypes")
                Vector vector = sd.getMediaDescriptions(false);
                if (null != vector) {
                    return new ArrayList<MediaDescription>(vector);
                }
            }
        } catch (SdpException e) {
            LOGGER.warn("fail get getMediaDescriptions from {}, {}", sd, e.getMessage(), e);
        }

        return Collections.emptyList();
    }

    public static RtpMapAttribute getRtpMap(SessionDescription sd, int streamIndex) throws SdpParseException {
        java.util.List<MediaDescription> mdList = getMediaDescriptions(sd);

        return getRtpMap(mdList.get(streamIndex));
    }

    public static RtpMapAttribute getRtpMap(MediaDescription md) throws SdpParseException {
        final String rtpmapValue = md.getAttribute("rtpmap");
        RtpMapAttribute rtpmap = null;
        try {
            if (null != rtpmapValue) {
                RtpMapAttributeParser parser = new RtpMapAttributeParser();
                rtpmap = parser.parse("a=rtpmap:" + rtpmapValue);
            } else {
                rtpmap = new RtpMapAttribute();
            }
        } catch (org.mobicents.media.server.io.sdp.SdpException e) {
            LOGGER.error("Fail parse rtpmap for '{}'", e.getMessage(), e);
        }

        return rtpmap;
    }

    public static SessionDescription parse(String SDP) throws ParseException {
        if (StringUtils.isEmpty(SDP)) {
            return null;
        }

        SessionDescriptionImpl sd = new SessionDescriptionImpl();
        StringTokenizer tokenizer = new StringTokenizer(SDP);
        while (tokenizer.hasMoreChars()) {
            String line = tokenizer.nextToken();
            SDPParser paser = ParserFactory.createParser(line);
            if (null != paser) {
                SDPField obj = paser.parse();
                sd.addField(obj);
            }
        }

        return sd;
    }

}
