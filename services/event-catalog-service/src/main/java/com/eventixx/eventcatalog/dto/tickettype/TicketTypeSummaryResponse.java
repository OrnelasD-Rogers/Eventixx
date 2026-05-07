package com.eventixx.eventcatalog.dto.tickettype;

import java.math.BigDecimal;
import java.util.UUID;

/** Response DTO for a ticket type summary. */
public record TicketTypeSummaryResponse(
    UUID id,
    String name,
    BigDecimal price,
    Integer quantityAvailable
) { }
