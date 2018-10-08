package com.sengled.media.server.rtsp.rtcp;

import io.netty.buffer.ByteBuf;

/**
 * @author las
 * @date 18-10-8
 */
public class RtcpAppDefined extends RtcpHeader {
    private byte[] data;
    private String name;
    private long ssrc;

    public RtcpAppDefined() {
        // TODO Auto-generated constructor stub
    }

    protected RtcpAppDefined(boolean padding, int subType, long ssrc, String name, byte[] data) {
        super(padding, RtcpHeader.RTCP_APP);
        this.count = subType;
        this.ssrc = ssrc;

        // Should we check if name is 4 octets?
        this.name = name;
        this.data = data;
    }

    @Override
    protected void decode(ByteBuf byteBuf) {

        super.decode(byteBuf);

        this.ssrc = byteBuf.readUnsignedInt();

        byte[] nameBytes = new byte[4];
        byteBuf.readBytes(nameBytes);

        this.name = new String(nameBytes);

        this.data = new byte[(this.length - 12)];
        byteBuf.readBytes(data);

    }


    @Override
    protected void encode(ByteBuf byteBuf) {

        int startPosition = byteBuf.writerIndex();

        super.encode(byteBuf);

        int lengthIndex = byteBuf.writerIndex() - 2;

        byteBuf.writeInt((int) this.ssrc);

        byte[] nameBytes = this.name.getBytes();

        byteBuf.writeBytes(nameBytes, 0, 4);

        byteBuf.writeBytes(data, 0, data.length);

        /* Reduce 4 octets of header and length is in terms 32bits word */
        this.length = (byteBuf.writerIndex() - startPosition - 4) / 4;

        byteBuf.setShort(lengthIndex, this.length);

    }

    public byte[] getData() {
        return data;
    }

    public String getName() {
        return name;
    }

    public long getSsrc() {
        return ssrc;
    }

    public int getSubType() {
        return this.count;
    }
}
