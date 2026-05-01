package com.eventixx.eventcatalog.services.event;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.entities.TicketType;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

@Component
public class EventValidator {

    public void validateCanBePublished(Event event) {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("Event is already published", HttpStatus.CONFLICT);
        }
        if (event.getTicketTypes().isEmpty()) {
            throw new BusinessException("Event must have at least one ticket type", HttpStatus.BAD_REQUEST);
        }
        int totalTickets = event.getTicketTypes().stream()
            .mapToInt(TicketType::getQuantityAvailable)
            .sum();
        if (totalTickets > event.getVenue().getCapacity()) {
            throw new BusinessException("Total ticket quantity exceeds venue capacity", HttpStatus.BAD_REQUEST);
        }
    }

    public void validateCanBeUpdated(Event event) {
        if (event.getStatus() != EventStatus.DRAFT) {
            throw new BusinessException("Only draft events can be updated", HttpStatus.BAD_REQUEST);
        }
    }

    public void validateCanBeCancelled(Event event) {
        if (event.getStatus() != EventStatus.PUBLISHED) {
            throw new BusinessException("Only published events can be cancelled", HttpStatus.BAD_REQUEST);
        }
    }
}
