package org.jcodec.movtool.streaming;

import static org.jcodec.movtool.streaming.MovieHelper.produceHeader;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.List;

import org.jcodec.codecs.h264.H264Utils;
import org.jcodec.containers.mp4.Brand;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * ISO BMF ( MP4 ) specific muxing
 * 
 * @author The JCodec project
 * 
 */
public class VirtualMP4Movie extends VirtualMovie {
    private Brand brand;

    public VirtualMP4Movie(VirtualTrack... tracks) throws IOException {
        this(Brand.MP4, tracks);
    }

    public VirtualMP4Movie(Brand brand, VirtualTrack... tracks) throws IOException {
        super(tracks);
        this.brand = brand;
        muxTracks();
    }

    protected MovieSegment headerChunk(List<MovieSegment> ch, VirtualTrack[] tracks, long dataSize) throws IOException {
        PacketChunk[] chunks = ch.toArray(new PacketChunk[0]);
        int headerSize = produceHeader(chunks, tracks, dataSize, brand).remaining();
        for (PacketChunk chunk : chunks) {
            chunk.offset(headerSize);
        }

        final ByteBuffer header = produceHeader(chunks, tracks, dataSize, brand);
        return new MovieSegment() {
            public ByteBuffer getData() {
                return header.duplicate();
            }

            public int getNo() {
                return 0;
            }

            public long getPos() {
                return 0;
            }

            public int getDataLen() {
                return header.remaining();
            }
        };
    }

    protected MovieSegment packetChunk(VirtualTrack track, VirtualPacket pkt, int chunkNo, int trackNo, long pos) {
        return new PacketChunk(pkt, trackNo, chunkNo, pos, track.getCodecMeta().getFourcc());
    }

    public class PacketChunk implements MovieSegment {
        private VirtualPacket packet;
        private int track;
        private int no;
        private long pos;
        private String fourcc;

        public PacketChunk(VirtualPacket packet, int track, int no, long pos, String fourcc) {
            this.packet = packet;
            this.track = track;
            this.no = no;
            this.pos = pos;
            this.fourcc = fourcc;
        }

        public ByteBuffer getData() throws IOException {
            ByteBuffer buf = packet.getData().duplicate();
//            if ("avc1".equals(fourcc)) {
//                H264Utils.wipePS(buf, null, null);
//                H264Utils.encodeMOVPacket(buf);
//            }
            return buf;
        }

        public int getNo() {
            return no;
        }

        public long getPos() {
            return pos;
        }

        public void offset(int off) {
            pos += off;
        }

        public int getDataLen() throws IOException {
            return packet.getDataLen();
        }

        public VirtualPacket getPacket() {
            return packet;
        }

        public int getTrackNo() {
            return track;
        }
    }
}