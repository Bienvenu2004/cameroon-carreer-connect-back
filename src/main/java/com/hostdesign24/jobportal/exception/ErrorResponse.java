package com.hostdesign24.jobportal.exception;

import lombok.Data;

@Data
public class ErrorResponse {
    private String message;
    private int status;
    private long timestamp;
    private String errorCode;
    private String errorDescription;
}
