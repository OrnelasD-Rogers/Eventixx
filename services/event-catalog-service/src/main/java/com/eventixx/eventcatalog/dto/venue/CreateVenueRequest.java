package com.eventixx.eventcatalog.dto.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record CreateVenueRequest(
    @NotBlank String name,
    @NotBlank String address,
    @NotBlank String city,
    @NotBlank String country,
    @NotNull @Min(1) Integer capacity
) {}
