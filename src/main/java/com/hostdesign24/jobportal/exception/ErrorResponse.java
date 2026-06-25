package com.hostdesign24.jobportal.exception;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ErrorResponse {
    private String message;
    private int status;
    private long timestamp;
    private String errorCode;
    private String errorDescription;
}
