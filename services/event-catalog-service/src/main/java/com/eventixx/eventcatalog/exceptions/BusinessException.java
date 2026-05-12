package com.eventixx.eventcatalog.exceptions;

/**
 * Runtime exception for business rule violations (HTTP 400).
 */
public class BusinessException extends RuntimeException {

    public BusinessException(String message) {
        super(message);
    }
}
