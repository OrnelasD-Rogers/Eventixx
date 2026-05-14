package com.eventixx.eventcatalog.dto.venue;

import java.util.UUID;

/** Response DTO for a venue summary. */
public record VenueSummaryResponse(UUID id, String name, String city, String country) {}
