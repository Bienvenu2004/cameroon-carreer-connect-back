package com.hostdesign24.jobportal.exception;


public class DeviceIdGenerationException extends RuntimeException {
    public DeviceIdGenerationException(String message) {
        super(message);
    }

    public DeviceIdGenerationException(String message, Throwable cause) {
        super(message, cause);
    }
}
