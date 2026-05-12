package com.eventixx.searchservice.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

/** DTO representing an event in search results. */
public record EventSearchDto(
        String eventId,
        String title,
        String description,
        String categoryName,
        String venueName,
        String city,
        String country,
        Instant startTime,
        Instant endTime,
        BigDecimal minPrice,
        BigDecimal maxPrice,
        List<TicketTypeDto> ticketTypes
) {
    /** DTO for ticket type within a search result event. */
    public record TicketTypeDto(String name, BigDecimal price, Integer quantityAvailable) {
    }
}
