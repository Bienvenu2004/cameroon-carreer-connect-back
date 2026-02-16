package com.hostdesign24.jobportal.exception;

public class PasswordNotMatchException extends RuntimeException {
    private String message;
    public PasswordNotMatchException(String message) {
        super(message);
    }
}
