package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Event;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, UUID>, EventRepository {
}
