package com.hostdesign24.jobportal.exception;

public class InvalidVerificationException extends RuntimeException {
  public InvalidVerificationException() {
    super();
  }

  public InvalidVerificationException(String message) {
    super(message);
  }

  public InvalidVerificationException(String message, Throwable cause) {
    super(message, cause);
  }

  public InvalidVerificationException(Throwable cause) {
    super(cause);
  }
}
