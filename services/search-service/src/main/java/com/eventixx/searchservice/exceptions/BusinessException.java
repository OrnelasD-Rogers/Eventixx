package com.eventixx.searchservice.exceptions;

/** Runtime exception for business rule violations (HTTP 400). */
public class BusinessException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public BusinessException(String message) {
        super(message);
    }
}
