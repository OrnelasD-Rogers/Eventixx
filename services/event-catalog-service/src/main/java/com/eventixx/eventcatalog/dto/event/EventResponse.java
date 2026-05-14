package com.eventixx.eventcatalog.dto.event;

import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Response DTO for an event. */
public record EventResponse(
    UUID id,
    String title,
    String description,
    String status,
    Instant startTime,
    Instant endTime,
    UUID venueId,
    String venueName,
    UUID categoryId,
    String categoryName,
    Instant publishedAt,
    Instant createdAt,
    Instant updatedAt,
    List<TicketTypeResponse> ticketTypes) {}
