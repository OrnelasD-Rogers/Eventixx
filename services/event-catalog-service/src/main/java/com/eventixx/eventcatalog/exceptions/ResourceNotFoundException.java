package com.eventixx.eventcatalog.exceptions;

/**
 * Thrown when a requested resource is not found. Maps to HTTP 404 NOT_FOUND.
 */
public class ResourceNotFoundException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public ResourceNotFoundException(String message) {
    super(message);
  }
}
