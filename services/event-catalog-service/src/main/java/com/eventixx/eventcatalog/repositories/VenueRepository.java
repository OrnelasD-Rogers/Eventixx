package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Venue;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

/**
 * Repository for venue persistence operations.
 */
public interface VenueRepository {

    Venue save(Venue venue);

    Optional<Venue> findById(UUID id);

    Page<Venue> findAll(Pageable pageable);

    void delete(Venue venue);
}
