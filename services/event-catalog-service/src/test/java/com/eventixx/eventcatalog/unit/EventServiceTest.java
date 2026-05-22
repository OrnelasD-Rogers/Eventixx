package com.eventixx.eventcatalog.unit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.repositories.EventRepository;
import com.eventixx.eventcatalog.repositories.VenueRepository;
import com.eventixx.eventcatalog.services.DomainEventPublisher;
import com.eventixx.eventcatalog.services.EventMetricsService;
import com.eventixx.eventcatalog.services.event.EventMapper;
import com.eventixx.eventcatalog.services.event.EventService;
import com.eventixx.eventcatalog.services.event.EventValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

@ExtendWith(MockitoExtension.class)
class EventServiceTest {

  @Mock private EventRepository eventRepository;
  @Mock private VenueRepository venueRepository;
  @Mock private CategoryRepository categoryRepository;
  @Mock private EventMapper eventMapper;
  @Mock private DomainEventPublisher domainEventPublisher;
  @Mock private EventValidator eventValidator;
  @Mock private EventMetricsService eventMetricsService;

  @InjectMocks private EventService eventService;

  private Venue venue;
  private Category category;
  private Event event;
  private EventResponse eventResponse;

  @BeforeEach
  void setUp() {
    venue =
        Venue.builder()
            .id(UUID.randomUUID())
            .name("Test Venue")
            .address("123 Main St")
            .city("Sao Paulo")
            .country("Brazil")
            .capacity(100)
            .build();

    category = Category.builder().id(UUID.randomUUID()).name("Music").build();

    event =
        Event.builder()
            .id(UUID.randomUUID())
            .title("Test Event")
            .description("A test event")
            .status(EventStatus.DRAFT)
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category)
            .ticketTypes(new java.util.ArrayList<>())
            .build();

    eventResponse =
        new EventResponse(
            event.getId(),
            event.getTitle(),
            event.getDescription(),
            event.getStatus().name(),
            event.getStartTime(),
            event.getEndTime(),
            venue.getId(),
            venue.getName(),
            category.getId(),
            category.getName(),
            null,
            Instant.now(),
            Instant.now(),
            List.of());
  }

  // === create ===

  @Test
  void shouldCreateEvent_withTicketTypes() {
    CreateTicketTypeRequest ttReq = new CreateTicketTypeRequest("GA", BigDecimal.valueOf(50), 10);
    CreateEventRequest request =
        new CreateEventRequest(
            "New Event",
            "Desc",
            venue.getId(),
            category.getId(),
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            List.of(ttReq));

    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
    when(eventRepository.save(any(Event.class))).thenAnswer(inv -> inv.getArgument(0));
    when(eventMapper.toResponse(any(Event.class))).thenReturn(eventResponse);

    EventResponse result = eventService.create(request);

    assertThat(result).isEqualTo(eventResponse);
    verify(eventRepository).save(any(Event.class));
  }

  @Test
  void shouldThrow_whenVenueNotFound_onCreate() {
    CreateEventRequest request =
        new CreateEventRequest(
            "New Event",
            "Desc",
            venue.getId(),
            category.getId(),
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            List.of());
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.create(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Venue with id");
  }

  @Test
  void shouldThrow_whenCategoryNotFound_onCreate() {
    CreateEventRequest request =
        new CreateEventRequest(
            "New Event",
            "Desc",
            venue.getId(),
            category.getId(),
            Instant.now().plusSeconds(3600),
            Instant.now().plusSeconds(7200),
            List.of());
    when(venueRepository.findById(venue.getId())).thenReturn(Optional.of(venue));
    when(categoryRepository.findById(category.getId())).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.create(request))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Category with id");
  }

  @Test
  void shouldThrow_whenEventNotFound() {
    UUID id = UUID.randomUUID();
    when(eventRepository.findById(id)).thenReturn(Optional.empty());

    assertThatThrownBy(() -> eventService.findById(id))
        .isInstanceOf(ResourceNotFoundException.class)
        .hasMessageContaining("Event with id");
  }

  // === findById ===

  @Test
  void shouldFindEventById() {
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(eventMapper.toResponse(event)).thenReturn(eventResponse);

    EventResponse result = eventService.findById(event.getId());

    assertThat(result).isEqualTo(eventResponse);
  }

  // === findAll ===

  @Test
  void shouldFindAllEvents() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> page = new PageImpl<>(List.of(event));
    when(eventRepository.findAll(pageable)).thenReturn(page);
    when(eventMapper.toSummary(any(Event.class)))
        .thenReturn(
            new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus().name(),
                event.getStartTime(),
                event.getEndTime(),
                venue.getName(),
                category.getName(),
                null));

    Page<EventSummaryResponse> result = eventService.findAll(pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  // === findAllByStatus ===

  @Test
  void shouldFindEventsByStatus() {
    Pageable pageable = PageRequest.of(0, 10);
    Page<Event> page = new PageImpl<>(List.of(event));
    when(eventRepository.findAllByStatus(EventStatus.DRAFT, pageable)).thenReturn(page);
    when(eventMapper.toSummary(any(Event.class)))
        .thenReturn(
            new EventSummaryResponse(
                event.getId(),
                event.getTitle(),
                event.getStatus().name(),
                event.getStartTime(),
                event.getEndTime(),
                venue.getName(),
                category.getName(),
                null));

    Page<EventSummaryResponse> result = eventService.findAllByStatus("DRAFT", pageable);

    assertThat(result.getContent()).hasSize(1);
  }

  // === update ===

  @Test
  void shouldUpdateEvent_partially() {
    UpdateEventRequest request =
        new UpdateEventRequest("Updated Title", null, null, null, null, null, null);

    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(eventRepository.save(event)).thenReturn(event);
    when(eventMapper.toResponse(event)).thenReturn(eventResponse);

    EventResponse result = eventService.update(event.getId(), request);

    assertThat(result).isEqualTo(eventResponse);
    assertThat(event.getTitle()).isEqualTo("Updated Title");
    verify(eventValidator).validateCanBeUpdated(event);
  }

  @Test
  void shouldUpdateEvent_withNewVenueAndCategory() {
    Venue newVenue =
        Venue.builder()
            .id(UUID.randomUUID())
            .name("New Venue")
            .address("A")
            .city("C")
            .country("B")
            .capacity(200)
            .build();
    Category newCategory = Category.builder().id(UUID.randomUUID()).name("Sports").build();
    UpdateEventRequest request =
        new UpdateEventRequest(null, null, newVenue.getId(), newCategory.getId(), null, null, null);

    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(venueRepository.findById(newVenue.getId())).thenReturn(Optional.of(newVenue));
    when(categoryRepository.findById(newCategory.getId())).thenReturn(Optional.of(newCategory));
    when(eventRepository.save(event)).thenReturn(event);
    when(eventMapper.toResponse(event)).thenReturn(eventResponse);

    eventService.update(event.getId(), request);

    assertThat(event.getVenue()).isEqualTo(newVenue);
    assertThat(event.getCategory()).isEqualTo(newCategory);
  }

  // === publish ===

  @Test
  void shouldPublishEvent_andSendKafkaEvent() {
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(eventRepository.save(event)).thenReturn(event);
    when(eventMapper.toResponse(event)).thenReturn(eventResponse);

    EventResponse result = eventService.publish(event.getId());

    assertThat(result).isEqualTo(eventResponse);
    assertThat(event.getStatus()).isEqualTo(EventStatus.PUBLISHED);
    assertThat(event.getPublishedAt()).isNotNull();
    verify(eventValidator).validateCanBePublished(event);
    verify(domainEventPublisher).publish(any());
  }

  // === cancel ===

  @Test
  void shouldCancelPublishedEvent() {
    event.setStatus(EventStatus.PUBLISHED);
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));
    when(eventRepository.save(event)).thenReturn(event);
    when(eventMapper.toResponse(event)).thenReturn(eventResponse);

    EventResponse result = eventService.cancel(event.getId());

    assertThat(result).isEqualTo(eventResponse);
    assertThat(event.getStatus()).isEqualTo(EventStatus.CANCELLED);
    verify(eventValidator).validateCanBeCancelled(event);
  }

  // === delete ===

  @Test
  void shouldDeleteEvent() {
    when(eventRepository.findById(event.getId())).thenReturn(Optional.of(event));

    eventService.delete(event.getId(), "user-123");

    verify(eventRepository).softDelete(event.getId(), "user-123", "User deleted event");
  }
}
