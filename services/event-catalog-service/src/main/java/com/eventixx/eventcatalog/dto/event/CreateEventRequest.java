package com.eventixx.eventcatalog.dto.event;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request DTO to create an event. */
public record CreateEventRequest(
    @NotBlank String title,
    String description,
    @NotNull UUID venueId,
    @NotNull UUID categoryId,
    @NotNull @Future Instant startTime,
    @NotNull @Future Instant endTime,
    @NotNull List<CreateTicketTypeRequest> ticketTypes) {}
