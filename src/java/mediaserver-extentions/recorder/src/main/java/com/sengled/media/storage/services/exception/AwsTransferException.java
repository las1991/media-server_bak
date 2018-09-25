package com.sengled.media.storage.services.exception;

public class AwsTransferException extends Exception{

    /**
     * 
     */
    private static final long serialVersionUID = 1L;
    public AwsTransferException(String msg, Throwable cause) {
        super(msg, cause);
    }
    public AwsTransferException(String msg) {
        super(msg);
    }


}
