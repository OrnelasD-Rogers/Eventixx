package com.eventixx.searchservice.exceptions;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.List;

/** Global exception handler that maps exceptions to RFC-9457 Problem Details. */
@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /** Handles search backend unavailability (503). */
    @ExceptionHandler(SearchUnavailableException.class)
    public ProblemDetail handleSearchUnavailable(SearchUnavailableException e) {
        log.error("Search service unavailable", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.SERVICE_UNAVAILABLE, e.getMessage());
        problem.setType(ProblemType.SERVICE_UNAVAILABLE);
        problem.setTitle(ProblemType.TITLE_SERVICE_UNAVAILABLE);
        return problem;
    }

    /** Handles validation errors (400) exposing individual field errors. */
    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ProblemDetail handleValidationError(MethodArgumentNotValidException e) {
        List<ValidationErrorDetail> errors = e.getBindingResult().getFieldErrors().stream()
                .map(fe -> new ValidationErrorDetail(
                        fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        "/" + fe.getField()))
                .toList();
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.BAD_REQUEST, "Validation failed");
        problem.setType(ProblemType.VALIDATION_ERROR);
        problem.setTitle(ProblemType.TITLE_VALIDATION_ERROR);
        problem.setProperty("errors", errors);
        return problem;
    }

    /** Handles invalid arguments (400). */
    @ExceptionHandler(BusinessException.class)
    public ProblemDetail handleBusinessException(BusinessException e) {
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(HttpStatus.BAD_REQUEST, e.getMessage());
        problem.setType(ProblemType.BUSINESS_RULE);
        problem.setTitle(ProblemType.TITLE_BUSINESS_RULE);
        return problem;
    }

    /** Handles unexpected errors (500). */
    @ExceptionHandler(Exception.class)
    public ProblemDetail handleGenericException(Exception e) {
        log.error("Unexpected error", e);
        ProblemDetail problem = ProblemDetail.forStatusAndDetail(
                HttpStatus.INTERNAL_SERVER_ERROR, "An unexpected error occurred");
        problem.setType(ProblemType.INTERNAL_ERROR);
        problem.setTitle(ProblemType.TITLE_INTERNAL_ERROR);
        return problem;
    }
}
