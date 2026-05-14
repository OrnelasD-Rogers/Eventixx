package com.eventixx.eventcatalog.controllers;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;
import com.eventixx.eventcatalog.services.venue.VenueService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
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
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Venue management")
public class VenueController {

  private final VenueService venueService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new venue")
  @ApiResponse(responseCode = "201", description = "Venue created successfully")
  public VenueResponse create(
      @Valid @RequestBody CreateVenueRequest request, @RequestHeader("X-User-Id") String userId) {
    return venueService.create(request);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get venue by ID")
  public VenueResponse getById(@PathVariable UUID id) {
    return venueService.findById(id);
  }

  @GetMapping
  @Operation(summary = "List all venues")
  public Page<VenueSummaryResponse> list(Pageable pageable) {
    return venueService.findAll(pageable);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update venue")
  public VenueResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateVenueRequest request,
      @RequestHeader("X-User-Id") String userId) {
    return venueService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete venue")
  public void delete(@PathVariable UUID id, @RequestHeader("X-User-Id") String userId) {
    venueService.delete(id);
  }
}
