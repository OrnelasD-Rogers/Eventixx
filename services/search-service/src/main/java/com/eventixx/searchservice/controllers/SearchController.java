package com.eventixx.searchservice.controllers;

import com.eventixx.searchservice.dto.SearchCriteria;
import com.eventixx.searchservice.dto.SearchResult;
import com.eventixx.searchservice.services.SearchService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.time.LocalDate;

/** REST controller for event search endpoints. */
@RestController
@RequestMapping("/api/v1/search")
@RequiredArgsConstructor
@Tag(name = "Search", description = "Event search and discovery")
public class SearchController {

    private final SearchService searchService;

    /** Searches events with optional filters, sorting, and cursor-based pagination. */
    @GetMapping("/events")
    @Operation(summary = "Search events")
    @ApiResponse(responseCode = "200", description = "Search results returned successfully")
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public SearchResult searchEvents(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String city,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) LocalDate dateFrom,
            @RequestParam(required = false) LocalDate dateTo,
            @RequestParam(required = false) @Valid BigDecimal minPrice,
            @RequestParam(required = false) @Valid BigDecimal maxPrice,
            @RequestParam(required = false) String sort,
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") @Valid int size,
            @RequestHeader("X-User-Id") String userId) {

        var criteria = new SearchCriteria(q, city, category, dateFrom, dateTo, minPrice, maxPrice, sort, cursor, size);
        return searchService.search(criteria);
    }
}
