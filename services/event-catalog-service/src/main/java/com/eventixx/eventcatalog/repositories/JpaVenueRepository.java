package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Venue;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaVenueRepository extends JpaRepository<Venue, UUID>, VenueRepository {
}
