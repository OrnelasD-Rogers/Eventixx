package com.eventixx.eventcatalog.dto.tickettype;

import jakarta.validation.constraints.Min;
import java.math.BigDecimal;

/** Request DTO to update a ticket type. */
public record UpdateTicketTypeRequest(
    String name,
    @Min(0) BigDecimal price,
    @Min(1) Integer quantityAvailable
) { }
