package com.eventixx.eventcatalog.dto.category;

import java.util.UUID;

/** Response DTO for a category summary. */
public record CategorySummaryResponse(
    UUID id,
    String name
) { }
