package com.accreditations_service.accreditations_service.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;

public class ExceptionHandlers {
    @ExceptionHandler(SalePointException.class)
    public ResponseEntity<String> handleSalePointException(SalePointException e) {
        return new ResponseEntity<>(e.getMessage(), HttpStatus.BAD_REQUEST);
    }
}
