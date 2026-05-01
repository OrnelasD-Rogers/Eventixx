package com.eventixx.eventcatalog.dto.venue;

import jakarta.validation.constraints.Min;

public record UpdateVenueRequest(
    String name,
    String address,
    String city,
    String country,
    @Min(1) Integer capacity
) {}
