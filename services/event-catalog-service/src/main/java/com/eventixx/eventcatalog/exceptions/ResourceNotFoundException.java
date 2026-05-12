package com.eventixx.eventcatalog.exceptions;

/**
 * Thrown when a requested resource is not found. Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

    public ResourceNotFoundException(String message) {
        super(message);
    }
}
