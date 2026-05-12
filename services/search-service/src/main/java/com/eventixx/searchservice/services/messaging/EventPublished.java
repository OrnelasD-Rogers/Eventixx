package com.eventixx.searchservice.services.messaging;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Kafka event payload for an event.published message. */
public record EventPublished(
        UUID eventId,
        String title,
        String description,
        UUID categoryId,
        String categoryName,
        UUID venueId,
        String venueName,
        String city,
        String country,
        Instant startTime,
        Instant endTime,
        Instant publishedAt,
        List<TicketTypeInfo> ticketTypes,
        Instant timestamp,
        UUID aggregateId,
        UUID correlationId
) {
    /** Ticket type information within an EventPublished record. */
    public record TicketTypeInfo(String name, BigDecimal price, Integer quantityAvailable) {
    }
}
