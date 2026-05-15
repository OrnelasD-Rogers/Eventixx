package com.eventixx.eventcatalog.exceptions;

/** Thrown when a domain event fails to serialize to JSON for Kafka publishing. Maps to HTTP 500. */
public class EventSerializationException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public EventSerializationException(String message, Throwable cause) {
    super(message, cause);
  }
}
