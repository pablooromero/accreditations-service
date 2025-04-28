package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;

public class AccreditationException extends Exception {
    private HttpStatus httpStatus;
    public AccreditationException(String message) {
        super(message);
    }

    public AccreditationException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {return httpStatus;}
}