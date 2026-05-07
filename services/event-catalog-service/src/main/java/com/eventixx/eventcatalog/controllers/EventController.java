package com.eventixx.eventcatalog.controllers;

import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.services.event.EventService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/events")
@RequiredArgsConstructor
@Tag(name = "Events", description = "Event catalog management")
public class EventController {

    private final EventService eventService;

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new event")
    @ApiResponse(responseCode = "201", description = "Event created successfully")
    public EventResponse create(@Valid @RequestBody CreateEventRequest request,
                                @RequestHeader("X-User-Id") String userId) {
        return eventService.create(request);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    public EventResponse getById(@PathVariable UUID id) {
        return eventService.findById(id);
    }

    /**
     * Lists events with optional status filter.
     *
     * @param pageable pagination info
     * @param status   optional status filter
     * @return page of events
     */
    @GetMapping
    @Operation(summary = "List events")
    public Page<EventSummaryResponse> list(Pageable pageable,
                                           @RequestParam(required = false) String status) {
        if (status != null && !status.isBlank()) {
            return eventService.findAllByStatus(status, pageable);
        }
        return eventService.findAll(pageable);
    }

    /**
     * Updates an existing event.
     *
     * @param id     the event ID
     * @param request the update request
     * @param userId the user ID from header
     * @return the updated event
     */
    /**
     * Updates an existing event.
     *
     * @param id      the event ID
     * @param request the update request
     * @param userId  the user ID from header
     * @return the updated event
     */
    @PutMapping("/{id}")
    @Operation(summary = "Update event")
    public EventResponse update(@PathVariable UUID id,
                                @Valid @RequestBody UpdateEventRequest request,
                                @RequestHeader("X-User-Id") String userId) {
        return eventService.update(id, request);
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "Publish event")
    @ApiResponse(responseCode = "200", description = "Event published")
    @ApiResponse(responseCode = "409", description = "Event already published")
    public EventResponse publish(@PathVariable UUID id,
                                 @RequestHeader("X-User-Id") String userId) {
        return eventService.publish(id);
    }

    @PostMapping("/{id}/cancel")
    @Operation(summary = "Cancel event")
    @ApiResponse(responseCode = "200", description = "Event cancelled")
    @ApiResponse(responseCode = "400", description = "Event is not published")
    public EventResponse cancel(@PathVariable UUID id,
                                @RequestHeader("X-User-Id") String userId) {
        return eventService.cancel(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Delete event")
    public void delete(@PathVariable UUID id,
                       @RequestHeader("X-User-Id") String userId) {
        eventService.delete(id);
    }
}
