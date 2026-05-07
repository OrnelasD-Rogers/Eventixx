package com.eventixx.eventcatalog.services.messaging;

import com.eventixx.eventcatalog.services.DomainEvent;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Domain event emitted when an event is published.
 */
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
        List<TicketTypeInfo> ticketTypes,
        Instant timestamp,
        UUID aggregateId,
        UUID correlationId
) implements DomainEvent {

    @Override
    public UUID getEventId() {
        return eventId;
    }

    @Override
    public String getEventType() {
        return "event.published";
    }

    @Override
    public String getEventVersion() {
        return "1.0";
    }

    @Override
    public Instant getTimestamp() {
        return timestamp;
    }

    @Override
    public UUID getAggregateId() {
        return aggregateId;
    }

    @Override
    public UUID getCorrelationId() {
        return correlationId;
    }

    /**
     * Information about a ticket type within an event.
     */
    public record TicketTypeInfo(String name, BigDecimal price, Integer quantityAvailable) {
    }
}
