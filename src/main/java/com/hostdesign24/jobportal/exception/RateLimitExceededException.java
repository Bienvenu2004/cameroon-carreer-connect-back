package com.hostdesign24.jobportal.exception;

public class RateLimitExceededException extends RuntimeException {
  public RateLimitExceededException() {
    super();
  }

  public RateLimitExceededException(String message) {
    super(message);
  }

  public RateLimitExceededException(String message, Throwable cause) {
    super(message, cause);
  }

  public RateLimitExceededException(Throwable cause) {
    super(cause);
  }
}
