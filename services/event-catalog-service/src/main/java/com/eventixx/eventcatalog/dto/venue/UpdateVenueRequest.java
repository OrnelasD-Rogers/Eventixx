package com.eventixx.eventcatalog.dto.venue;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** Request DTO to update a venue. */
public record UpdateVenueRequest(
    @Size(max = 200) String name,
    @Size(max = 500) String address,
    @Size(max = 100) String city,
    @Size(max = 100) String country,
    @Min(1) Integer capacity) {}
