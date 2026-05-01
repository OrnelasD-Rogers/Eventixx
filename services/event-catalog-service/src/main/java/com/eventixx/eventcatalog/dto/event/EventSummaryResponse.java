package com.eventixx.eventcatalog.dto.event;

import java.time.Instant;
import java.util.UUID;

public record EventSummaryResponse(
    UUID id,
    String title,
    String status,
    Instant startTime,
    Instant endTime,
    String venueName,
    String categoryName,
    Instant publishedAt
) {}
