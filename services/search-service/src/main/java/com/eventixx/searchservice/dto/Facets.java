package com.eventixx.searchservice.dto;

import java.util.Map;

/** Faceted aggregation results (categories, cities, etc.). */
public record Facets(
        Map<String, Long> categories,
        Map<String, Long> cities
) {
}
