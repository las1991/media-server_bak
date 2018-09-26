package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;

import java.util.Arrays;
import java.util.List;

/**
 * The RTP header has the following format:
 * <p>
 * 0               1               2               3
 * 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1 2 3 4 5 6 7 8 9 0 1
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |V=2|P|X|  CC   |M|     PT      |       sequence number         |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |                           timestamp                           |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * |           synchronization source (SSRC) identifier            |
 * +=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+=+
 * |            contributing source (CSRC) identifiers             |
 * |                             ....                              |
 * +-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+-+
 * <p>
 * The first twelve octets are present in every RTP packet, while the
 * list of CSRC identifiers is present only when inserted by a mixer.
 * <p>
 * The version defined by RFC3550 specification is two.
 *
 * @author las
 * @date 18-9-20
 */
public abstract class RtpPacket implements SourceRtpPacket {

    private ByteBuf content;

    protected int version;
    protected boolean padding;
    protected boolean extension;
    protected int cc;
    protected boolean marker;

    protected int payloadType;
    protected int seqNumber;

    protected long time;
    protected long syncSource;
    protected List<Long> cSources;

    protected int profile;
    protected int headerExtensionLength;
    protected byte[] headerExtension;

    public RtpPacket(ByteBuf content, int version, boolean padding, boolean extension, int cc, boolean marker, int payloadType, int seqNumber, long time, long syncSource, List<Long> cSources, int profile, int headerExtensionLength, byte[] headerExtension) {
        this.content = content;
        this.version = version;
        this.padding = padding;
        this.extension = extension;
        this.cc = cc;
        this.marker = marker;
        this.payloadType = payloadType;
        this.seqNumber = seqNumber;
        this.time = time;
        this.syncSource = syncSource;
        this.cSources = cSources;
        this.profile = profile;
        this.headerExtensionLength = headerExtensionLength;
        this.headerExtension = headerExtension;
    }

    public RtpPacket(ByteBuf content, int version, boolean padding, int cc, boolean marker, int payloadType, int seqNumber, long time, long syncSource, List<Long> cSources) {
        this(content, version, padding, false, cc, marker, payloadType, seqNumber, time, syncSource, cSources, 0, 0, null);
    }

    /**
     * copy 时使用
     *
     * @param content
     * @param that
     */
    protected RtpPacket(ByteBuf content, RtpPacket that) {
        this.content = content;
        this.version = that.version;
        this.padding = that.padding;
        this.extension = that.extension;
        this.cc = that.cc;
        this.marker = that.marker;
        this.payloadType = that.payloadType;
        this.seqNumber = that.seqNumber;
        this.time = that.time;
        this.syncSource = that.syncSource;
        this.cSources = that.cSources;
        this.profile = that.profile;
        this.headerExtensionLength = that.headerExtensionLength;
        this.headerExtension = that.headerExtension;
    }

    public int getVersion() {
        return version;
    }

    public boolean isPadding() {
        return padding;
    }

    public boolean isExtension() {
        return extension;
    }

    public int getCc() {
        return cc;
    }

    public boolean isMarker() {
        return marker;
    }

    public int getPayloadType() {
        return payloadType;
    }

    public int getSeqNumber() {
        return seqNumber;
    }

    public long getTime() {
        return time;
    }

    public long getSyncSource() {
        return syncSource;
    }

    public List<Long> getcSources() {
        return cSources;
    }

    public int getProfile() {
        return profile;
    }

    public int getHeaderExtensionLength() {
        return headerExtensionLength;
    }

    public byte[] getHeaderExtension() {
        return headerExtension;
    }

    @Override
    public ByteBuf content() {
        return content;
    }

    @Override
    public int refCnt() {
        return content.refCnt();
    }

    @Override
    public RtpPacket retain() {
        content.retain();
        return this;
    }

    @Override
    public RtpPacket retain(int increment) {
        content.retain(increment);
        return this;
    }

    @Override
    public boolean release() {
        return content.release();
    }

    @Override
    public boolean release(int decrement) {
        return content.release(decrement);
    }

    @Override
    public String toString() {
        StringBuilder buf = new StringBuilder()
                .append("RtpPacket{")
                .append(" version=").append(version)
                .append(", padding=").append(padding)
                .append(", extension=").append(extension)
                .append(", cc=").append(cc)
                .append(", marker=").append(marker)
                .append(", payloadType=").append(payloadType)
                .append(", seqNumber=").append(seqNumber)
                .append(", time=").append(time)
                .append(", syncSource=").append(syncSource)
                .append(", cSources=").append(cSources)
                .append(", profile=").append(profile)
                .append(", headerExtensionLength=").append(headerExtensionLength)
                .append(", headerExtension=").append(Arrays.toString(headerExtension))
                .append(", content=").append(content)
                .append('}');
        return buf.toString();

    }
}
