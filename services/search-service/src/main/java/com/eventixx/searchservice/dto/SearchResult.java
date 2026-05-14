package com.eventixx.searchservice.dto;

import java.util.List;

/** Search result containing events, facets, cursor pagination and total count. */
public record SearchResult(
    List<EventSearchDto> events, Facets facets, String nextCursor, long total) {}
