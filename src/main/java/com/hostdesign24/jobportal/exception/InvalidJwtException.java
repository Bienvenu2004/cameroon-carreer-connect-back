package com.hostdesign24.jobportal.exception;

public class InvalidJwtException extends RuntimeException {

  public InvalidJwtException(String message) {
    super(message);
  }
}
