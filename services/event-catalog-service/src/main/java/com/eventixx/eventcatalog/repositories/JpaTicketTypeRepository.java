package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.TicketType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface JpaTicketTypeRepository extends JpaRepository<TicketType, UUID>, TicketTypeRepository {

    @Override
    List<TicketType> findAllByEventId(UUID eventId);
}
