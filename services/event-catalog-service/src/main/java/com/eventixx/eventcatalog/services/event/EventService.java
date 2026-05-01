package com.eventixx.eventcatalog.services.event;

import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.services.messaging.EventPublished;
import com.eventixx.eventcatalog.repositories.EventRepository;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.services.DomainEventPublisher;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.repositories.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EventService {

    private final EventRepository eventRepository;
    private final VenueRepository venueRepository;
    private final CategoryRepository categoryRepository;
    private final EventMapper eventMapper;
    private final DomainEventPublisher domainEventPublisher;
    private final EventValidator eventValidator;

    @Transactional
    public EventResponse create(CreateEventRequest request) {
        Venue venue = findVenueOrThrow(request.venueId());
        Category category = findCategoryOrThrow(request.categoryId());

        Event event = buildEvent(request, venue, category);
        attachTicketTypes(event, request.ticketTypes());

        return eventMapper.toResponse(eventRepository.save(event));
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

    @Transactional
    public EventResponse update(UUID id, UpdateEventRequest request) {
        Event event = findEventOrThrow(id);
        eventValidator.validateCanBeUpdated(event);
        applyUpdates(event, request);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse publish(UUID id) {
        Event event = findEventOrThrow(id);
        eventValidator.validateCanBePublished(event);
        markAsPublished(event);
        publishEventToKafka(event);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public EventResponse cancel(UUID id) {
        Event event = findEventOrThrow(id);
        eventValidator.validateCanBeCancelled(event);
        event.setStatus(EventStatus.CANCELLED);
        return eventMapper.toResponse(eventRepository.save(event));
    }

    @Transactional
    public void delete(UUID id) {
        Event event = findEventOrThrow(id);
        eventRepository.delete(event);
    }

    // === Private helpers ===

    private Event findEventOrThrow(UUID id) {
        return eventRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Event with id " + id + " not found", HttpStatus.NOT_FOUND));
    }

    private Venue findVenueOrThrow(UUID id) {
        return venueRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Venue with id " + id + " not found", HttpStatus.NOT_FOUND));
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new BusinessException("Category with id " + id + " not found", HttpStatus.NOT_FOUND));
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
            TicketType ticketType = TicketType.builder()
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
        Optional.ofNullable(request.venueId()).ifPresent(venueId ->
            event.setVenue(findVenueOrThrow(venueId)));
        Optional.ofNullable(request.categoryId()).ifPresent(categoryId ->
            event.setCategory(findCategoryOrThrow(categoryId)));
    }

    private void markAsPublished(Event event) {
        event.setStatus(EventStatus.PUBLISHED);
        event.setPublishedAt(Instant.now());
    }

    private void publishEventToKafka(Event event) {
        EventPublished eventPublished = new EventPublished(
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
                .map(tt -> new EventPublished.TicketTypeInfo(tt.getName(), tt.getPrice(), tt.getQuantityAvailable()))
                .toList(),
            Instant.now(),
            event.getId(),
            UUID.randomUUID()
        );
        domainEventPublisher.publish(eventPublished);
    }
}
