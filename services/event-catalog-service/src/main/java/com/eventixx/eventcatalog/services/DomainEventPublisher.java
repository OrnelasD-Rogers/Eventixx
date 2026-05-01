package com.eventixx.eventcatalog.services;

public interface DomainEventPublisher {

    void publish(DomainEvent event);
}
