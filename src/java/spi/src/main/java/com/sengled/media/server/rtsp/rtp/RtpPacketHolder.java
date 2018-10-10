package com.sengled.media.server.rtsp.rtp;

import io.netty.buffer.ByteBuf;

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
public class RtpPacketHolder implements RtpPacket {

    private final RtpHeader header;
    private ByteBuf payload;

    public RtpPacketHolder(RtpHeader header, ByteBuf content) {
        this.header = header;
        this.payload = content;
    }

    public RtpHeader rtpHeader() {
        return header;
    }

    @Override
    public ByteBuf payload() {
        return payload;
    }

    @Override
    public int version() {
        return header.version();
    }

    @Override
    public boolean padding() {
        return header.padding();
    }

    @Override
    public boolean extension() {
        return header.extension();
    }

    @Override
    public int cc() {
        return header.cc();
    }

    @Override
    public boolean marker() {
        return header.marker();
    }

    @Override
    public int payloadType() {
        return header.payloadType();
    }

    @Override
    public int seqNumber() {
        return header.seqNumber();
    }

    @Override
    public long time() {
        return header.time();
    }

    @Override
    public long SSRC() {
        return header.SSRC();
    }

    @Override
    public List<Long> CSRC() {
        return header.CSRC();
    }

    @Override
    public int profile() {
        return header.profile();
    }

    @Override
    public int extensionLength() {
        return header.extensionLength();
    }

    @Override
    public byte[] extensionHeader() {
        return header.extensionHeader();
    }

    @Override
    public ByteBuf content() {
        return payload;
    }

    @Override
    public RtpPacketHolder copy() {
        return new RtpPacketHolder(header, payload.copy());
    }

    @Override
    public RtpPacketHolder duplicate() {
        payload.duplicate();
        return this;
    }

    @Override
    public int refCnt() {
        return payload.refCnt();
    }

    @Override
    public RtpPacketHolder retain() {
        payload.retain();
        return this;
    }

    @Override
    public RtpPacketHolder retain(int increment) {
        payload.retain(increment);
        return this;
    }

    @Override
    public boolean release() {
        return payload.release();
    }

    @Override
    public boolean release(int decrement) {
        return payload.release(decrement);
    }

    @Override
    public String toString() {
        return "RtpPacketHolder{" +
                "header=" + header +
                ", payload=" + payload +
                '}';
    }
}
