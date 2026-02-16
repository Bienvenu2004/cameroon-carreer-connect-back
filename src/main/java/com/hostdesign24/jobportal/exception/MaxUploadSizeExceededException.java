package com.hostdesign24.jobportal.exception;

public class MaxUploadSizeExceededException extends RuntimeException{
    public MaxUploadSizeExceededException(String message) {
        super(message);
    }
}
