package com.hostdesign24.jobportal.exception;

public class TooManyAttemptsException extends RuntimeException {
    public TooManyAttemptsException() {
        super();
    }

    public TooManyAttemptsException(String message) {
        super(message);
    }

    public TooManyAttemptsException(String message, Throwable cause) {
        super(message, cause);
    }

    public TooManyAttemptsException(Throwable cause) {
        super(cause);
    }
}
