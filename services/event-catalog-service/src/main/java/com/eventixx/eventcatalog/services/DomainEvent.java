package com.eventixx.eventcatalog.services;

import java.time.Instant;
import java.util.UUID;

public interface DomainEvent {

    UUID getEventId();

    String getEventType();

    String getEventVersion();

    Instant getTimestamp();

    UUID getAggregateId();

    UUID getCorrelationId();
}
