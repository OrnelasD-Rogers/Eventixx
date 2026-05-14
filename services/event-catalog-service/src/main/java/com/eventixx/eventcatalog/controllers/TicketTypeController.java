package com.eventixx.eventcatalog.controllers;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import com.eventixx.eventcatalog.services.tickettype.TicketTypeService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/events/{eventId}/ticket-types")
@RequiredArgsConstructor
@Tag(name = "Ticket Types", description = "Ticket type management for events")
public class TicketTypeController {

  private final TicketTypeService ticketTypeService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create ticket type for event")
  public TicketTypeResponse create(
      @PathVariable UUID eventId,
      @Valid @RequestBody CreateTicketTypeRequest request,
      @RequestHeader("X-User-Id") String userId) {
    return ticketTypeService.create(eventId, request);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get ticket type by ID")
  public TicketTypeResponse getById(@PathVariable UUID eventId, @PathVariable UUID id) {
    return ticketTypeService.findById(id);
  }

  @GetMapping
  @Operation(summary = "List ticket types for event")
  public List<TicketTypeResponse> list(@PathVariable UUID eventId) {
    return ticketTypeService.findAllByEventId(eventId);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update ticket type")
  public TicketTypeResponse update(
      @PathVariable UUID eventId,
      @PathVariable UUID id,
      @Valid @RequestBody UpdateTicketTypeRequest request,
      @RequestHeader("X-User-Id") String userId) {
    return ticketTypeService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete ticket type")
  public void delete(
      @PathVariable UUID eventId,
      @PathVariable UUID id,
      @RequestHeader("X-User-Id") String userId) {
    ticketTypeService.delete(id);
  }
}
