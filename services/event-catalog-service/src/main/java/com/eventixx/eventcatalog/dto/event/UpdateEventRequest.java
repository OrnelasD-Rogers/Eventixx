package com.eventixx.eventcatalog.dto.event;

import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request DTO to update an event. */
public record UpdateEventRequest(
    @Size(max = 300) String title,
    String description,
    UUID venueId,
    UUID categoryId,
    @Future Instant startTime,
    @Future Instant endTime,
    List<UpdateTicketTypeRequest> ticketTypes) {}
