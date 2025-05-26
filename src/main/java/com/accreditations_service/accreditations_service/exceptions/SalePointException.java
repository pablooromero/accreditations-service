package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;

public class SalePointException extends RuntimeException {
    private final HttpStatus httpStatus;

    public SalePointException(String message) {
        super(message);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }

    public SalePointException(String message, HttpStatus httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public SalePointException(String message, HttpStatus httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public SalePointException(String message, Throwable cause) {
        super(message, cause);
        this.httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;
    }


    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}