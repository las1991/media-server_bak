package org.jcodec.containers.mkv.muxer;

import static org.jcodec.containers.mkv.MKVType.CodecID;
import static org.jcodec.containers.mkv.MKVType.Cues;
import static org.jcodec.containers.mkv.MKVType.DateUTC;
import static org.jcodec.containers.mkv.MKVType.DocType;
import static org.jcodec.containers.mkv.MKVType.DocTypeReadVersion;
import static org.jcodec.containers.mkv.MKVType.DocTypeVersion;
import static org.jcodec.containers.mkv.MKVType.EBML;
import static org.jcodec.containers.mkv.MKVType.EBMLMaxIDLength;
import static org.jcodec.containers.mkv.MKVType.EBMLMaxSizeLength;
import static org.jcodec.containers.mkv.MKVType.EBMLReadVersion;
import static org.jcodec.containers.mkv.MKVType.EBMLVersion;
import static org.jcodec.containers.mkv.MKVType.Info;
import static org.jcodec.containers.mkv.MKVType.MuxingApp;
import static org.jcodec.containers.mkv.MKVType.Name;
import static org.jcodec.containers.mkv.MKVType.PixelHeight;
import static org.jcodec.containers.mkv.MKVType.PixelWidth;
import static org.jcodec.containers.mkv.MKVType.Segment;
import static org.jcodec.containers.mkv.MKVType.TimecodeScale;
import static org.jcodec.containers.mkv.MKVType.TrackEntry;
import static org.jcodec.containers.mkv.MKVType.TrackNumber;
import static org.jcodec.containers.mkv.MKVType.TrackType;
import static org.jcodec.containers.mkv.MKVType.TrackUID;
import static org.jcodec.containers.mkv.MKVType.Tracks;
import static org.jcodec.containers.mkv.MKVType.Video;
import static org.jcodec.containers.mkv.MKVType.WritingApp;
import static org.jcodec.containers.mkv.MKVType.createByType;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedList;
import java.util.List;

import org.jcodec.common.SeekableByteChannel;
import org.jcodec.common.model.Size;
import org.jcodec.containers.mkv.CuesFactory;
import org.jcodec.containers.mkv.MKVType;
import org.jcodec.containers.mkv.SeekHeadFactory;
import org.jcodec.containers.mkv.boxes.EbmlBase;
import org.jcodec.containers.mkv.boxes.EbmlBin;
import org.jcodec.containers.mkv.boxes.EbmlDate;
import org.jcodec.containers.mkv.boxes.EbmlFloat;
import org.jcodec.containers.mkv.boxes.EbmlMaster;
import org.jcodec.containers.mkv.boxes.EbmlString;
import org.jcodec.containers.mkv.boxes.EbmlUint;
import org.jcodec.containers.mkv.boxes.MkvBlock;
import org.jcodec.containers.mkv.muxer.MKVMuxerTrack.MKVMuxerTrackType;

public class MKVMuxer {

    private List<MKVMuxerTrack> tracks = new ArrayList<MKVMuxerTrack>();
    private MKVMuxerTrack videoTrack = null;
    private EbmlMaster mkvInfo;
    private EbmlMaster mkvTracks;
    private EbmlMaster mkvCues;
    private EbmlMaster mkvSeekHead;
    private List<EbmlMaster> clusterList = new LinkedList<EbmlMaster>();

    public MKVMuxerTrack createVideoTrack(Size dimentions, String codecId) {
        if (videoTrack == null) {
            videoTrack = new MKVMuxerTrack();
            tracks.add(videoTrack);
            videoTrack.codecId = codecId;
            videoTrack.frameDimentions = dimentions;
            videoTrack.trackNo = tracks.size();
        }
        return videoTrack;
    }

    public void mux(SeekableByteChannel s) throws IOException {
        List<EbmlMaster> mkvFile = new ArrayList<EbmlMaster>();
        EbmlMaster ebmlHeader = defaultEbmlHeader();
        mkvFile.add(ebmlHeader);

        EbmlMaster segmentElem = (EbmlMaster) createByType(Segment);
        mkvInfo = muxInfo();
        mkvTracks = muxTracks();
        mkvCues = (EbmlMaster) createByType(Cues);
        mkvSeekHead = muxSeekHead();
        muxCues();
        
        segmentElem.add(mkvSeekHead);
        segmentElem.add(mkvInfo);
        segmentElem.add(mkvTracks);
        segmentElem.add(mkvCues);
        for (EbmlMaster aCluster : clusterList)
            segmentElem.add(aCluster);
        mkvFile.add(segmentElem);
        
        for(EbmlMaster el: mkvFile)
            el.mux(s);
    }
    
    private EbmlMaster defaultEbmlHeader() {
        EbmlMaster master = (EbmlMaster) createByType(EBML);

        createChild(master, EBMLVersion, 1);
        createChild(master, EBMLReadVersion, 1);
        createChild(master, EBMLMaxIDLength, 4);
        createChild(master, EBMLMaxSizeLength, 8);

        createChild(master, DocType, "webm");
        createChild(master, DocTypeVersion, 2);
        createChild(master, DocTypeReadVersion, 2);

        return master;
    }

    private EbmlMaster muxInfo() {
        EbmlMaster master = (EbmlMaster) createByType(Info);
        int frameDurationInNanoseconds = MKVMuxerTrack.NANOSECONDS_IN_A_MILISECOND*40;
        createChild(master, TimecodeScale, frameDurationInNanoseconds);
        createChild(master, WritingApp, "JCodec v0.1.7");
        createChild(master, MuxingApp, "JCodec MKVStreamingMuxer v0.1.7");

        MkvBlock lastBlock = videoTrack.trackBlocks.get(videoTrack.trackBlocks.size()-1);
        createChild(master, MKVType.Duration, (lastBlock.absoluteTimecode+1) * frameDurationInNanoseconds*1.0);
        createChild(master, DateUTC, new Date());
        return master;
    }
    
    private EbmlMaster muxTracks() {
        EbmlMaster master = (EbmlMaster) createByType(Tracks);
        for (int i = 0; i < tracks.size(); i++) {
            MKVMuxerTrack track = tracks.get(i);
            EbmlMaster trackEntryElem = (EbmlMaster) createByType(TrackEntry);

            createChild(trackEntryElem, TrackNumber, track.trackNo);

            createChild(trackEntryElem, TrackUID, track.trackNo);
            if (MKVMuxerTrackType.VIDEO.equals(track.type)) {
                createChild(trackEntryElem, TrackType, (byte) 0x01);
                createChild(trackEntryElem, Name, "Track " + (i + 1) + " Video");
                createChild(trackEntryElem, CodecID, track.codecId);
//                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
//                VideoCodecMeta vcm = (VideoCodecMeta) codecMeta;
                
                EbmlMaster trackVideoElem = (EbmlMaster) createByType(Video);
                createChild(trackVideoElem, PixelWidth, track.frameDimentions.getWidth());
                createChild(trackVideoElem, PixelHeight, track.frameDimentions.getHeight());

                trackEntryElem.add(trackVideoElem);
                
            } else {
                createChild(trackEntryElem, TrackType, (byte) 0x02);
                createChild(trackEntryElem, Name, "Track " + (i + 1) + " Audio");
                createChild(trackEntryElem, CodecID, track.codecId);
//                createChild(trackEntryElem, CodecPrivate, codecMeta.getCodecPrivate());
            }

            master.add(trackEntryElem);
        }
        return master;
    }
    
    private void muxCues() {
        CuesFactory cf = new CuesFactory(mkvSeekHead.size() + mkvInfo.size() + mkvTracks.size(), videoTrack.trackNo-1);
        for (MkvBlock aBlock : videoTrack.trackBlocks) {
            EbmlMaster mkvCluster = singleBlockedCluster(aBlock);
            clusterList.add(mkvCluster);
            cf.add(CuesFactory.CuePointMock.make(mkvCluster));
        }

        EbmlMaster indexedCues = cf.createCues();
        
        for (EbmlBase aCuePoint : indexedCues.children)
            mkvCues.add(aCuePoint);
    }

    private EbmlMaster singleBlockedCluster(MkvBlock aBlock) {
        EbmlMaster mkvCluster = createByType(MKVType.Cluster);
        createChild(mkvCluster, MKVType.Timecode, aBlock.absoluteTimecode-aBlock.timecode);
        mkvCluster.add(aBlock);
        return mkvCluster;
    }
    
    private EbmlMaster muxSeekHead() {
        SeekHeadFactory shi = new SeekHeadFactory();
        shi.add(mkvInfo);
        shi.add(mkvTracks);
        shi.add(mkvCues);
        return shi.indexSeekHead();
    }

    public static void createChild(EbmlMaster parent, MKVType type, long value) {
        EbmlUint se = (EbmlUint) createByType(type);
        se.set(value);
        parent.add(se);
    }

    public static void createChild(EbmlMaster parent, MKVType type, String value) {
        EbmlString se = (EbmlString) createByType(type);
        se.set(value);
        parent.add(se);
    }

    public static void createChild(EbmlMaster parent, MKVType type, Date value) {
        EbmlDate se = (EbmlDate) createByType(type);
        se.setDate(value);
        parent.add(se);
    }

    public static void createChild(EbmlMaster parent, MKVType type, ByteBuffer value) {
        EbmlBin se = (EbmlBin) createByType(type);
        se.set(value);
        parent.add(se);
    }

    public static void createChild(EbmlMaster parent, MKVType type, double value) {
        try {
            EbmlFloat se = (EbmlFloat) createByType(type);
            se.set(value);
            parent.add(se);
        } catch (ClassCastException cce) {
            throw new RuntimeException("Element of type " + type + " can't be cast to EbmlFloat", cce);
        }
    }

}
