package com.hostdesign24.jobportal.exception;

public class UserAlreadyExistsException  extends RuntimeException{
    public UserAlreadyExistsException(String message) {
        super(message);
    }
}
