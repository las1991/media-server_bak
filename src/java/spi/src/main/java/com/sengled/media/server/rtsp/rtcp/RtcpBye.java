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
    protected void encode(ByteBuf byteBuf) {

        int startPosition = byteBuf.writerIndex();

        super.encode(byteBuf);

        int lengthIndex = byteBuf.writerIndex() - 2;

        for (int i = 0; i < this.count; i++) {
            long ssrc = ssrcs[i];
            byteBuf.writeInt((int) ssrc);
        }

        /* Reduce 4 octest of header and length is in terms 32bits word */
        this.length = (byteBuf.writerIndex() - startPosition - 4) / 4;

        byteBuf.setShort(lengthIndex, this.length);
        
    }

    public void addSsrc(long ssrc) {
        this.ssrcs[this.count++] = ssrc;
    }

    public long[] getSsrcs() {
        return ssrcs;
    }

}
