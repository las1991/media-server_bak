package org.mobicents.media.server.impl.rtp.statistics;

public interface InterleavedRtpPacket {
    /**
     * Sequence number field.
     *
     * The sequence number increments by one for each RTP data packet
     * sent, and may be used by the receiver to detect packet loss and
     * to restore packet sequence. The initial value of the sequence
     * number is random (unpredictable) to make known-plaintext attacks
     * on encryption more difficult, even if the source itself does not
     * encrypt, because the packets may flow through a translator that
     * does.
     *
     * @return the sequence number value.
     */
    int getSeqNumber();
    /**
     * Get RTP payload length from a RTP packet
     *
     * @return RTP payload length from source RTP packet
     */
    int getPayloadLength();
    /**
     * Timestamp field.
     *
     * The timestamp reflects the sampling instant of the first octet
     * in the RTP data packet. The sampling instant must be derived
     * from a clock that increments monotonically and linearly in time
     * to allow synchronization and jitter calculations.
     * The resolution of the clock must be sufficient for the
     * desired synchronization accuracy and for measuring packet
     * arrival jitter (one tick per video frame is typically not
     * sufficient).  The clock frequency is dependent on the format of
     * data carried as payload and is specified statically in the
     * profile or payload format specification that defines the format,
     * or may be specified dynamically for payload formats defined
     * through non-RTP means. If RTP packets are generated
     * periodically, the nominal sampling instant as determined from
     * the sampling clock is to be used, not a reading of the system
     * clock. As an example, for fixed-rate audio the timestamp clock
     * would likely increment by one for each sampling period.  If an
     * audio application reads blocks covering 160 sampling periods
     * from the input device, the timestamp would be increased by 160
     * for each such block, regardless of whether the block is
     * transmitted in a packet or dropped as silent.
     *
     * The initial value of the timestamp is random, as for the sequence
     * number. Several consecutive RTP packets may have equal timestamps if
     * they are (logically) generated at once, e.g., belong to the same
     * video frame. Consecutive RTP packets may contain timestamps that are
     * not monotonic if the data is not transmitted in the order it was
     * sampled, as in the case of MPEG interpolated video frames. (The
     * sequence numbers of the packets as transmitted will still be
     * monotonic.)
     *
     * @return timestamp value
     */
    long getTime();

    /**
     * Synchronization source field.
     *
     * The SSRC field identifies the synchronization source. This
     * identifier is chosen randomly, with the intent that no two
     * synchronization sources within the same RTP session will have
     * the same SSRC identifier. Although the
     * probability of multiple sources choosing the same identifier is
     * low, all RTP implementations must be prepared to detect and
     * resolve collisions.  Section 8 describes the probability of
     * collision along with a mechanism for resolving collisions and
     * detecting RTP-level forwarding loops based on the uniqueness of
     * the SSRC identifier. If a source changes its source transport
     * address, it must also choose a new SSRC identifier to avoid
     * being interpreted as a looped source.
     * 
     * @return the sysncronization source 
     */
    long getSyncSource();
}
