package com.eventixx.eventcatalog.dto.category;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for a category. */
public record CategoryResponse(
    UUID id,
    String name,
    String description,
    Instant createdAt,
    Instant updatedAt
) { }
