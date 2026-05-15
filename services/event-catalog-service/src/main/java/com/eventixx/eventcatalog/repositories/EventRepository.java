package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for event persistence operations.
 */
public interface EventRepository {

  Event save(Event event);

  Optional<Event> findById(UUID id);

  Page<Event> findAll(Pageable pageable);

  Page<Event> findAllByStatus(EventStatus status, Pageable pageable);

  boolean existsByCategoryId(UUID categoryId);

  void delete(Event event);
}
