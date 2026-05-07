package com.eventixx.eventcatalog.services.tickettype;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.repositories.EventRepository;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.repositories.TicketTypeRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TicketTypeService {

    private final TicketTypeRepository ticketTypeRepository;
    private final EventRepository eventRepository;
    private final TicketTypeMapper ticketTypeMapper;

    /**
     * Creates a new ticket type for an event.
     *
     * @param eventId the event ID
     * @param request the ticket type request
     * @return the created ticket type
     */
    @Transactional
    public TicketTypeResponse create(UUID eventId, CreateTicketTypeRequest request) {
        Event event = eventRepository.findById(eventId)
            .orElseThrow(() -> new BusinessException(
                "Event with id " + eventId + " not found", HttpStatus.NOT_FOUND));
        TicketType ticketType = ticketTypeMapper.toEntity(request);
        ticketType.setEvent(event);
        return ticketTypeMapper.toResponse(ticketTypeRepository.save(ticketType));
    }

    /**
     * Finds a ticket type by ID.
     *
     * @param id the ticket type ID
     * @return the ticket type
     */
    public TicketTypeResponse findById(UUID id) {
        return ticketTypeMapper.toResponse(ticketTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException(
                "Ticket type with id " + id + " not found", HttpStatus.NOT_FOUND)));
    }

    /**
     * Lists all ticket types for an event.
     *
     * @param eventId the event ID
     * @return list of ticket types
     */
    public List<TicketTypeResponse> findAllByEventId(UUID eventId) {
        return ticketTypeMapper.toResponseList(ticketTypeRepository.findAllByEventId(eventId));
    }

    /**
     * Updates a ticket type.
     *
     * @param id      the ticket type ID
     * @param request the update request
     * @return the updated ticket type
     */
    @Transactional
    public TicketTypeResponse update(UUID id, UpdateTicketTypeRequest request) {
        TicketType ticketType = ticketTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Ticket type with id " + id + " not found", HttpStatus.NOT_FOUND));
        ticketTypeMapper.updateEntity(request, ticketType);
        return ticketTypeMapper.toResponse(ticketType);
    }

    /**
     * Soft-deletes a ticket type.
     *
     * @param id the ticket type ID
     */
    @Transactional
    public void delete(UUID id) {
        TicketType ticketType = ticketTypeRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Ticket type with id " + id + " not found", HttpStatus.NOT_FOUND));
        ticketTypeRepository.delete(ticketType);
    }
}
