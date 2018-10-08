package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpBye extends RtcpHeader {

    private long[] ssrcs = new long[31];

    protected RtcpBye() {

    }

    public RtcpBye(boolean padding) {
        super(padding, RtcpHeader.RTCP_BYE);
    }

    @Override
    protected void decode(ByteBuf byteBuf) {

        super.decode(byteBuf);

        for (int i = 0; i < this.count; i++) {
            this.ssrcs[i] = byteBuf.readUnsignedInt();
        }

        // Do we acre for optional part?

    }

    @Override
    protected int encode(byte[] rawData, int offSet) {

        int startPosition = offSet;

        offSet = super.encode(rawData, offSet);

        for (int i = 0; i < this.count; i++) {
            long ssrc = ssrcs[i];

            rawData[offSet++] = ((byte) ((ssrc & 0xFF000000) >> 24));
            rawData[offSet++] = ((byte) ((ssrc & 0x00FF0000) >> 16));
            rawData[offSet++] = ((byte) ((ssrc & 0x0000FF00) >> 8));
            rawData[offSet++] = ((byte) ((ssrc & 0x000000FF)));
        }

        /* Reduce 4 octest of header and length is in terms 32bits word */
        this.length = (offSet - startPosition - 4) / 4;

        rawData[startPosition + 2] = ((byte) ((this.length & 0xFF00) >> 8));
        rawData[startPosition + 3] = ((byte) (this.length & 0x00FF));

        return offSet;
    }

    public void addSsrc(long ssrc) {
        this.ssrcs[this.count++] = ssrc;
    }

    public long[] getSsrcs() {
        return ssrcs;
    }

}
