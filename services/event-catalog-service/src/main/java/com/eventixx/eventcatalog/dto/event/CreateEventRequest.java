package com.eventixx.eventcatalog.dto.event;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/** Request DTO to create an event. */
public record CreateEventRequest(
    @NotBlank @Size(max = 300) String title,
    String description,
    @NotNull UUID venueId,
    @NotNull UUID categoryId,
    @NotNull @Future Instant startTime,
    @NotNull @Future Instant endTime,
    @NotEmpty List<CreateTicketTypeRequest> ticketTypes) {

  @AssertTrue(message = "endTime must be after startTime") boolean isEndTimeAfterStartTime() {
    return endTime != null && startTime != null && endTime.isAfter(startTime);
  }
}
