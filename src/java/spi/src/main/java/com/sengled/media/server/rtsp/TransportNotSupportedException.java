package com.sengled.media.server.rtsp;

import java.io.IOException;

public class TransportNotSupportedException extends IOException {

    /**
     * 
     */
    private static final long serialVersionUID = -3487537848974901306L;

    public TransportNotSupportedException() {
        super();
    }

    public TransportNotSupportedException(String message, Throwable rootCause) {
        super(message, rootCause);
    }

    public TransportNotSupportedException(String message) {
        super(message);
    }

    public TransportNotSupportedException(Throwable rootCause) {
        super(rootCause);
    }

}
