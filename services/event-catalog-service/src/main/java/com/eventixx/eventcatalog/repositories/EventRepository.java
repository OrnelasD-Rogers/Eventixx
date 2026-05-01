package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface EventRepository {

    Event save(Event event);

    Optional<Event> findById(UUID id);

    Page<Event> findAll(Pageable pageable);

    Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

    void delete(Event event);
}
