package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;

public class UserException extends RuntimeException {

  private final HttpStatus httpStatus;

  public UserException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public UserException(String message, HttpStatus httpStatus, Throwable cause) {
    super(message, cause);
    this.httpStatus = httpStatus;
  }

  public UserException(String message) {
    super(message);
    this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
  }


  public HttpStatus getHttpStatus() {
    return httpStatus;
  }
}