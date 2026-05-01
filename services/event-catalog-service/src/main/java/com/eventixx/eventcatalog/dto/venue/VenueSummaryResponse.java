package com.eventixx.eventcatalog.dto.venue;

import java.util.UUID;

public record VenueSummaryResponse(
    UUID id,
    String name,
    String city,
    String country
) {}
