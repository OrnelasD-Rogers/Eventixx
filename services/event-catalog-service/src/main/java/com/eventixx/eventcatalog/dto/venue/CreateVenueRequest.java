package com.eventixx.eventcatalog.dto.venue;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

/** Request DTO to create a venue. */
public record CreateVenueRequest(
    @NotBlank @Size(max = 200) String name,
    @NotBlank @Size(max = 500) String address,
    @NotBlank @Size(max = 100) String city,
    @NotBlank @Size(max = 100) String country,
    @NotNull @Min(1) @Max(100_000) Integer capacity) {}
