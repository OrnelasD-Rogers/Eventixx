package com.eventixx.eventcatalog.dto.event;

import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import jakarta.validation.constraints.Future;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record UpdateEventRequest(
    String title,
    String description,
    UUID venueId,
    UUID categoryId,
    @Future Instant startTime,
    @Future Instant endTime,
    List<UpdateTicketTypeRequest> ticketTypes
) {}
