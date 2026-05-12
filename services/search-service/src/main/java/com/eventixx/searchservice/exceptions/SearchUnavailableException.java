package com.eventixx.searchservice.exceptions;

/** Thrown when the search backend is unavailable. */
public class SearchUnavailableException extends RuntimeException {
    public SearchUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
