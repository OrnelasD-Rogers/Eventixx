package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.TicketType;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

/**
 * Repository for ticket type persistence operations.
 */
public interface TicketTypeRepository {

  TicketType save(TicketType ticketType);

  Optional<TicketType> findById(UUID id);

  List<TicketType> findAllByEventId(UUID eventId);

  void softDelete(UUID id, String deletedBy, String deletedReason);

  void delete(TicketType ticketType);
}
