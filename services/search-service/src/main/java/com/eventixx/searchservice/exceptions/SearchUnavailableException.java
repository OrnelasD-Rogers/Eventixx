package com.eventixx.searchservice.exceptions;

/** Thrown when the search backend is unavailable. */
public class SearchUnavailableException extends RuntimeException {

  private static final long serialVersionUID = 1L;

  public SearchUnavailableException(String message, Throwable cause) {
    super(message, cause);
  }
}
