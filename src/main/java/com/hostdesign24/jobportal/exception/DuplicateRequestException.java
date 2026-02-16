package com.hostdesign24.jobportal.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(code = HttpStatus.CONFLICT)
public class DuplicateRequestException extends RuntimeException {

  public DuplicateRequestException(String message) {
    super(message);
  }
}
