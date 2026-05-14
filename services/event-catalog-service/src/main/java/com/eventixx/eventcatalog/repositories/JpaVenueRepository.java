package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Venue;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaVenueRepository extends JpaRepository<Venue, UUID>, VenueRepository {}
