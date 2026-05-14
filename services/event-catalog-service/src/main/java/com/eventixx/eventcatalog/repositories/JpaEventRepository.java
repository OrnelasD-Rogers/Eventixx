package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Event;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaEventRepository extends JpaRepository<Event, UUID>, EventRepository {}
