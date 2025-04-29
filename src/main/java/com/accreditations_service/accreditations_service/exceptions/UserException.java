package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;

public class UserException extends Exception {
  private HttpStatus httpStatus;
  public UserException(String message) {
    super(message);
  }

  public UserException(String message, HttpStatus httpStatus) {
    super(message);
    this.httpStatus = httpStatus;
  }

  public HttpStatus getHttpStatus() {return httpStatus;}
}