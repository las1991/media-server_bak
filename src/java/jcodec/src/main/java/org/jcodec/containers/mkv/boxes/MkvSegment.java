package org.jcodec.containers.mkv.boxes;

import static org.jcodec.containers.mkv.MKVType.Cluster;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlEncode;
import static org.jcodec.containers.mkv.util.EbmlUtil.ebmlLength;

import java.nio.ByteBuffer;

import org.jcodec.containers.mkv.util.EbmlUtil;


/**
 * This class is part of JCodec ( www.jcodec.org ) This software is distributed
 * under FreeBSD License
 * 
 * EBML IO implementation
 * 
 * @author Jay Codec
 * 
 */
public class MkvSegment extends EbmlMaster {
    
    int headerSize = 0;

    public MkvSegment(byte[] id) {
        super(id);
    }
    
    public MkvSegment() {
        super(Segment.id);
    }

    public ByteBuffer getHeader() {
        long headerSize = getHeaderSize();
        
        if (headerSize > Integer.MAX_VALUE)
            System.out.println("MkvSegment.getHeader: id.length "+id.length+"  Element.getEbmlSize("+dataLen+"): "+EbmlUtil.ebmlLength(dataLen)+" size: "+dataLen);
        ByteBuffer bb = ByteBuffer.allocate((int)headerSize);

        bb.put(id);
        bb.put(ebmlEncode(getDataLen()));

        if (children != null && !children.isEmpty()){
            // all non-cluster elements go to header
            for(EbmlBase e : children){
                if (Cluster.equals(e.type))
                    continue;
                
                bb.put(e.getData()); 
            }
        }
        
        bb.flip();
        
        return bb;
    }
    
    public long getHeaderSize(){
        long returnValue = id.length;
        returnValue += ebmlLength(getDataLen());
        if (children != null && !children.isEmpty()){
            for(EbmlBase e : children){
                if (Cluster.equals(e.type))
                    continue;
                
                returnValue += e.size(); 
            }
        }
        
        return returnValue;
    }

}
