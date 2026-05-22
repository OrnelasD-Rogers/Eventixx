package com.eventixx.eventcatalog.services.event;

import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.repositories.EventRepository;
import com.eventixx.eventcatalog.repositories.TicketTypeRepository;
import com.eventixx.eventcatalog.repositories.VenueRepository;
import com.eventixx.eventcatalog.services.DomainEventPublisher;
import com.eventixx.eventcatalog.services.EventMetricsService;
import com.eventixx.eventcatalog.services.messaging.EventPublished;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

  private final EventRepository eventRepository;
  private final VenueRepository venueRepository;
  private final CategoryRepository categoryRepository;
  private final TicketTypeRepository ticketTypeRepository;
  private final EventMapper eventMapper;
  private final DomainEventPublisher domainEventPublisher;
  private final EventValidator eventValidator;
  private final EventMetricsService eventMetricsService;

  /**
   * Creates a new event.
   */
  @Transactional
  public EventResponse create(CreateEventRequest request) {
    Venue venue = findVenueOrThrow(request.venueId());
    Category category = findCategoryOrThrow(request.categoryId());

    Event event = buildEvent(request, venue, category);
    attachTicketTypes(event, request.ticketTypes());

    EventResponse response = eventMapper.toResponse(eventRepository.save(event));
    eventMetricsService.recordEventCreated();
    return response;
  }

  public EventResponse findById(UUID id) {
    return eventMapper.toResponse(findEventOrThrow(id));
  }

  public Page<EventSummaryResponse> findAll(Pageable pageable) {
    return eventRepository.findAll(pageable).map(eventMapper::toSummary);
  }

  public Page<EventSummaryResponse> findAllByStatus(String status, Pageable pageable) {
    EventStatus eventStatus = EventStatus.valueOf(status);
    return eventRepository.findAllByStatus(eventStatus, pageable).map(eventMapper::toSummary);
  }

  /**
   * Updates an existing event.
   */
  @Transactional
  public EventResponse update(UUID id, UpdateEventRequest request) {
    Event event = findEventOrThrow(id);
    eventValidator.validateCanBeUpdated(event);
    applyUpdates(event, request);
    return eventMapper.toResponse(eventRepository.save(event));
  }

  /**
   * Publishes an event.
   *
   * @param id the event ID
   * @return the updated event
   */
  @Transactional
  public EventResponse publish(UUID id) {
    Event event = findEventOrThrow(id);
    eventValidator.validateCanBePublished(event);
    markAsPublished(event);
    publishEventToKafka(event);
    EventResponse response = eventMapper.toResponse(eventRepository.save(event));
    eventMetricsService.recordEventPublished();
    return response;
  }

  /**
   * Cancels a published event.
   *
   * @param id the event ID
   * @return the updated event
   */
  @Transactional
  public EventResponse cancel(UUID id) {
    Event event = findEventOrThrow(id);
    eventValidator.validateCanBeCancelled(event);
    event.setStatus(EventStatus.CANCELLED);
    return eventMapper.toResponse(eventRepository.save(event));
  }

  /**
   * Soft-deletes an event and its ticket types.
   *
   * @param id the event ID
   * @param userId the user performing the deletion
   */
  @Transactional
  public void delete(UUID id, String userId) {
    Event event = findEventOrThrow(id);
    String reason = "User deleted event";
    for (var tt : event.getTicketTypes()) {
      ticketTypeRepository.softDelete(tt.getId(), userId, "Cascade from event deletion");
    }
    eventRepository.softDelete(id, userId, reason);
  }

  // === Private helpers ===

  private Event findEventOrThrow(UUID id) {
    return eventRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Event with id " + id + " not found"));
  }

  private Venue findVenueOrThrow(UUID id) {
    return venueRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Venue with id " + id + " not found"));
  }

  private Category findCategoryOrThrow(UUID id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
  }

  private Event buildEvent(CreateEventRequest request, Venue venue, Category category) {
    return Event.builder()
        .title(request.title())
        .description(request.description())
        .startTime(request.startTime())
        .endTime(request.endTime())
        .venue(venue)
        .category(category)
        .build();
  }

  private void attachTicketTypes(Event event, List<CreateTicketTypeRequest> ticketTypeRequests) {
    for (var ttReq : ticketTypeRequests) {
      TicketType ticketType =
          TicketType.builder()
              .name(ttReq.name())
              .price(ttReq.price())
              .quantityAvailable(ttReq.quantityAvailable())
              .event(event)
              .build();
      event.getTicketTypes().add(ticketType);
    }
  }

  private void applyUpdates(Event event, UpdateEventRequest request) {
    Optional.ofNullable(request.title()).ifPresent(event::setTitle);
    Optional.ofNullable(request.description()).ifPresent(event::setDescription);
    Optional.ofNullable(request.startTime()).ifPresent(event::setStartTime);
    Optional.ofNullable(request.endTime()).ifPresent(event::setEndTime);
    Optional.ofNullable(request.venueId())
        .ifPresent(venueId -> event.setVenue(findVenueOrThrow(venueId)));
    Optional.ofNullable(request.categoryId())
        .ifPresent(categoryId -> event.setCategory(findCategoryOrThrow(categoryId)));
  }

  private void markAsPublished(Event event) {
    event.setStatus(EventStatus.PUBLISHED);
    event.setPublishedAt(Instant.now());
  }

  private void publishEventToKafka(Event event) {
    EventPublished eventPublished =
        new EventPublished(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getCategory().getId(),
            event.getCategory().getName(),
            event.getVenue().getId(),
            event.getVenue().getName(),
            event.getVenue().getCity(),
            event.getVenue().getCountry(),
            event.getStartTime(),
            event.getEndTime(),
            event.getTicketTypes().stream()
                .map(
                    tt ->
                        new EventPublished.TicketTypeInfo(
                            tt.getName(), tt.getPrice(), tt.getQuantityAvailable()))
                .toList(),
            Instant.now(),
            event.getId(),
            UUID.randomUUID());
    domainEventPublisher.publish(eventPublished);
  }
}
