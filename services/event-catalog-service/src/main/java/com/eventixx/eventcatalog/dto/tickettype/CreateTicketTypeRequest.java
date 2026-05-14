package com.eventixx.eventcatalog.dto.tickettype;

import jakarta.validation.constraints.Digits;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;

/** Request DTO to create a ticket type. */
public record CreateTicketTypeRequest(
    @NotBlank @Size(max = 100) String name,
    @NotNull @Min(0) @Digits(integer = 8, fraction = 2) BigDecimal price,
    @NotNull @Min(1) Integer quantityAvailable) {}
