package org.jcodec.containers.mp4.boxes;

import java.util.List;

/**
 * Track fragment box
 * 
 * Contains routines dedicated to simplify working with track fragments
 * 
 * @author Jay Codec
 * 
 */
public class TrackFragmentBox extends NodeBox {

    public TrackFragmentBox() {
        super(new Header(fourcc()));
    }

    public static String fourcc() {
        return "traf";
    }

    protected void getModelFields(List<String> model) {

    }

    public int getTrackId() {
        TrackFragmentHeaderBox tfhd = Box
                .findFirst(this, TrackFragmentHeaderBox.class, TrackFragmentHeaderBox.fourcc());
        if (tfhd == null)
            throw new RuntimeException("Corrupt track fragment, no header atom found");
        return tfhd.getTrackId();
    }
}
