package com.eventixx.eventcatalog.controllers;

import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import com.eventixx.eventcatalog.exceptions.ProblemType;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.exceptions.ValidationErrorDetail;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

/**
 * Global exception handler for the event catalog service.
 * Converts exceptions into RFC 9457 ProblemDetail responses.
 */
@Slf4j
@ControllerAdvice
public class GlobalExceptionHandler {

  /**
   * Handles resource-not-found errors with HTTP 404.
   */
  @ExceptionHandler(ResourceNotFoundException.class)
  public ProblemDetail handleResourceNotFoundException(ResourceNotFoundException ex) {
    log.warn("Resource not found: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.NOT_FOUND, ex.getMessage());
    problem.setType(ProblemType.NOT_FOUND);
    problem.setTitle(ProblemType.TITLE_NOT_FOUND);
    return problem;
  }

  /**
   * Handles conflict errors with HTTP 409.
   */
  @ExceptionHandler(ConflictException.class)
  public ProblemDetail handleConflictException(ConflictException ex) {
    log.warn("Conflict: {}", ex.getMessage());
    ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.CONFLICT, ex.getMessage());
    problem.setType(ProblemType.CONFLICT);
    problem.setTitle(ProblemType.TITLE_CONFLICT);
    return problem;
  }

  /**
   * Handles business-rule violations with HTTP 400.
   */
  @ExceptionHandler(BusinessException.class)
  public ProblemDetail handleBusinessException(BusinessException ex) {
    log.warn("Business exception: {}", ex.getMessage());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, ex.getMessage());
    problem.setType(ProblemType.BUSINESS_RULE);
    problem.setTitle(ProblemType.TITLE_BUSINESS_RULE);
    return problem;
  }

  /**
   * Handles missing required request headers with HTTP 400.
   */
  @ExceptionHandler(MissingRequestHeaderException.class)
  public ProblemDetail handleMissingRequestHeader(MissingRequestHeaderException ex) {
    log.warn("Missing required header: {}", ex.getHeaderName());
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST, "Required header '" + ex.getHeaderName() + "' is missing");
    problem.setType(ProblemType.VALIDATION_ERROR);
    problem.setTitle(ProblemType.TITLE_VALIDATION_ERROR);
    return problem;
  }

  /**
   * Handles validation errors (e.g. @Valid DTOs).
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  public ProblemDetail handleValidationException(MethodArgumentNotValidException ex) {
    log.warn("Validation failed: {}", ex.getMessage());
    int errorCount = ex.getBindingResult().getErrorCount();
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.BAD_REQUEST,
            "Request body contains "
                + errorCount
                + " validation error(s). Check 'errors' for details.");
    problem.setType(ProblemType.VALIDATION_ERROR);
    problem.setTitle(ProblemType.TITLE_VALIDATION_ERROR);
    List<ValidationErrorDetail> errors = new ArrayList<>();
    for (FieldError fieldError : ex.getBindingResult().getFieldErrors()) {
      errors.add(
          new ValidationErrorDetail(fieldError.getDefaultMessage(), "/" + fieldError.getField()));
    }
    problem.setProperty("errors", errors);
    return problem;
  }

  /**
   * Handles unexpected / uncaught exceptions.
   */
  @ExceptionHandler(Exception.class)
  public ProblemDetail handleGenericException(Exception ex) {
    log.error("Unexpected error: {}", ex.getMessage(), ex);
    ProblemDetail problem =
        ProblemDetail.forStatusAndDetail(
            HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
    problem.setType(ProblemType.INTERNAL_ERROR);
    problem.setTitle(ProblemType.TITLE_INTERNAL_ERROR);
    return problem;
  }
}
