package com.eventixx.eventcatalog.dto.tickettype;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request DTO to create a ticket type. */
public record CreateTicketTypeRequest(
    @NotBlank String name,
    @NotNull @Min(0) BigDecimal price,
    @NotNull @Min(1) Integer quantityAvailable) {}
