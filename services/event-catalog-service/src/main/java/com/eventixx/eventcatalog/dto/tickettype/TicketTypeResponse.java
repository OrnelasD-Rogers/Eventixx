package com.eventixx.eventcatalog.dto.tickettype;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/** Response DTO for a ticket type. */
public record TicketTypeResponse(
    UUID id,
    String name,
    BigDecimal price,
    Integer quantityAvailable,
    Instant createdAt,
    Instant updatedAt
) { }
