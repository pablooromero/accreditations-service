package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;

public class SalePointException extends Exception {
    private HttpStatus httpStatus;
    public SalePointException(String message) {
        super(message);
    }

    public SalePointException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public HttpStatus getHttpStatus() {return httpStatus;}
}

