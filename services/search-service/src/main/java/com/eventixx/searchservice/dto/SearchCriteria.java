package com.eventixx.searchservice.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import java.math.BigDecimal;
import java.time.LocalDate;

/** Search/filter criteria for event queries. */
public record SearchCriteria(
    String q,
    String city,
    String category,
    LocalDate dateFrom,
    LocalDate dateTo,
    @Min(0) BigDecimal minPrice,
    @Min(0) BigDecimal maxPrice,
    String sort,
    String cursor,
    @Min(1) @Max(100) int size) {}
