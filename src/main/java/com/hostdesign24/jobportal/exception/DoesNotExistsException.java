package com.hostdesign24.jobportal.exception;

public class DoesNotExistsException extends RuntimeException {
    public DoesNotExistsException(String message) {
        super(message);
    }
}
