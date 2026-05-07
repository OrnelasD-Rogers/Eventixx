package com.eventixx.eventcatalog.dto.event;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for an event summary. */
public record EventSummaryResponse(
    UUID id,
    String title,
    String status,
    Instant startTime,
    Instant endTime,
    String venueName,
    String categoryName,
    Instant publishedAt
) { }
