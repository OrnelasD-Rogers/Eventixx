package com.eventixx.eventcatalog.dto.venue;

import java.time.Instant;
import java.util.UUID;

/** Response DTO for a venue. */
public record VenueResponse(
    UUID id,
    String name,
    String address,
    String city,
    String country,
    Integer capacity,
    Instant createdAt,
    Instant updatedAt) {}
