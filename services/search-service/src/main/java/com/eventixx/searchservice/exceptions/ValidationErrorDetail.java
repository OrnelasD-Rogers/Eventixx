package com.eventixx.searchservice.exceptions;

/** A single validation error with a human-readable message and a JSON Pointer to the invalid field. */
public record ValidationErrorDetail(String detail, String pointer) {
}
