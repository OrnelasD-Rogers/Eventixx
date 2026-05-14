package com.eventixx.searchservice.exceptions;

import java.net.URI;

/** RFC 9457 problem type definitions. */
@SuppressWarnings("PMD.DataClass")
public final class ProblemType {

  public static final URI VALIDATION_ERROR =
      URI.create("tag:eventixx.com,2026:problem:validation-error");
  public static final URI SERVICE_UNAVAILABLE =
      URI.create("tag:eventixx.com,2026:problem:service-unavailable");
  public static final URI BUSINESS_RULE = URI.create("tag:eventixx.com,2026:problem:business-rule");
  public static final URI INTERNAL_ERROR = URI.create("about:blank");

  public static final String TITLE_VALIDATION_ERROR = "Validation Error";
  public static final String TITLE_SERVICE_UNAVAILABLE = "Service Unavailable";
  public static final String TITLE_BUSINESS_RULE = "Business Rule Violation";
  public static final String TITLE_INTERNAL_ERROR = "Internal Server Error";

  private ProblemType() {}
}
