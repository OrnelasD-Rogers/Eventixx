package com.eventixx.searchservice.exceptions;

/** Thrown when a request conflicts with the current state. Maps to HTTP 409 CONFLICT. */
public class ConflictException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public ConflictException(String message) {
        super(message);
    }
}
