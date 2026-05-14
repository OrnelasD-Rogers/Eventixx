package com.eventixx.eventcatalog.services.event;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import org.springframework.stereotype.Component;

@Component
public class EventValidator {

  /**
   * Validates that the event can be published.
   */
  public void validateCanBePublished(Event event) {
    if (event.getStatus() != EventStatus.DRAFT) {
      throw new ConflictException("Event is already published");
    }
    if (event.getTicketTypes().isEmpty()) {
      throw new BusinessException("Event must have at least one ticket type");
    }
    int totalTickets =
        event.getTicketTypes().stream().mapToInt(TicketType::getQuantityAvailable).sum();
    if (totalTickets > event.getVenue().getCapacity()) {
      throw new BusinessException("Total ticket quantity exceeds venue capacity");
    }
  }

  /**
   * Validates that the event can be updated.
   */
  public void validateCanBeUpdated(Event event) {
    if (event.getStatus() != EventStatus.DRAFT) {
      throw new BusinessException("Only draft events can be updated");
    }
  }

  /**
   * Validates that the event can be cancelled.
   */
  public void validateCanBeCancelled(Event event) {
    if (event.getStatus() != EventStatus.PUBLISHED) {
      throw new BusinessException("Only published events can be cancelled");
    }
  }
}
