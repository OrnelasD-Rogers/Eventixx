package com.eventixx.eventcatalog.services;

/**
 * Publishes domain events to the messaging infrastructure.
 */
public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
