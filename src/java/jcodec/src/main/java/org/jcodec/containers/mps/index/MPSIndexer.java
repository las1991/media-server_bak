package org.jcodec.containers.mps.index;

import static org.jcodec.containers.mps.MPSUtils.mediaStream;
import static org.jcodec.containers.mps.MPSUtils.readPESHeader;

import java.io.File;
import java.io.IOException;
import java.nio.ByteBuffer;

import org.jcodec.common.NIOUtils;
import org.jcodec.common.NIOUtils.FileReader;
import org.jcodec.common.SeekableByteChannel;
import org.jcodec.containers.mps.MPSDemuxer.PESPacket;

/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * Indexes MPEG PS file for the purpose of quick random access in the future
 * 
 * @author The JCodec project
 * 
 */
public class MPSIndexer extends BaseIndexer {
    private long predFileStart;

    public void index(File source, NIOUtils.FileReaderListener listener) throws IOException {
        newReader().readFile(source, 0x10000, listener);
    }

    public void index(SeekableByteChannel source, NIOUtils.FileReaderListener listener) throws IOException {
        newReader().readFile(source, 0x10000, listener);
    }

    private FileReader newReader() {
        return new NIOUtils.FileReader() {
            @Override
            protected void data(ByteBuffer data, long filePos) {
                analyseBuffer(data, filePos);
            }
            @Override
            protected void done() {
                finishAnalyse();
            }
        };
    }

    protected void pes(ByteBuffer pesBuffer, long start, int pesLen, int stream) {
        if (!mediaStream(stream))
            return;
        PESPacket pesHeader = readPESHeader(pesBuffer, start);
        int leading = 0;
        if (predFileStart != start) {
            leading += (int) (start - predFileStart);
        }
        predFileStart = start + pesLen;
        savePESMeta(stream, MPSIndex.makePESToken(leading, pesLen, pesBuffer.remaining()));
        getAnalyser(stream).pkt(pesBuffer, pesHeader);
    }

    public static void main(String[] args) throws IOException {
        MPSIndexer indexer = new MPSIndexer();
        indexer.index(new File(args[0]), new NIOUtils.FileReaderListener() {
            public void progress(int percentDone) {
                System.out.println(percentDone);
            }
        });
        ByteBuffer index = ByteBuffer.allocate(0x10000);
        indexer.serialize().serializeTo(index);
        NIOUtils.writeTo(index, new File(args[1]));
    }
}