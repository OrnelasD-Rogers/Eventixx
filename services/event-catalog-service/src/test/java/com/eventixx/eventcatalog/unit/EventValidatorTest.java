package com.eventixx.eventcatalog.unit;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.EventStatus;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import com.eventixx.eventcatalog.services.event.EventValidator;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

class EventValidatorTest {

  private EventValidator validator;

  @BeforeEach
  void setUp() {
    validator = new EventValidator();
  }

  // === validateCanBePublished ===

  @Test
  void shouldAllowPublish_whenDraftWithTicketsAndCapacityOk() {
    Event event = draftEvent(venueWithCapacity(100), List.of(ticketType("GA", 50)));

    validator.validateCanBePublished(event);
    // no exception expected
  }

  @Test
  void shouldAllowPublish_whenTotalTicketsExactlyEqualsCapacity() {
    Event event =
        draftEvent(venueWithCapacity(100), List.of(ticketType("GA", 60), ticketType("VIP", 40)));

    validator.validateCanBePublished(event);
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      names = {"PUBLISHED", "CANCELLED", "ENDED"})
  void shouldRejectPublish_whenStatusIsNotDraft(EventStatus status) {
    Event event = eventWithStatus(status, venueWithCapacity(100), List.of(ticketType("GA", 10)));

    assertThatThrownBy(() -> validator.validateCanBePublished(event))
        .isInstanceOf(ConflictException.class)
        .hasMessageContaining("already published");
  }

  @Test
  void shouldRejectPublish_whenNoTicketTypes() {
    Event event = draftEvent(venueWithCapacity(100), List.of());

    assertThatThrownBy(() -> validator.validateCanBePublished(event))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("at least one ticket type");
  }

  @Test
  void shouldRejectPublish_whenTotalTicketsExceedCapacity() {
    Event event =
        draftEvent(venueWithCapacity(100), List.of(ticketType("GA", 60), ticketType("VIP", 50)));

    assertThatThrownBy(() -> validator.validateCanBePublished(event))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("exceeds venue capacity");
  }

  // === validateCanBeUpdated ===

  @Test
  void shouldAllowUpdate_whenDraft() {
    Event event = draftEvent(venueWithCapacity(100), List.of(ticketType("GA", 10)));

    validator.validateCanBeUpdated(event);
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      names = {"PUBLISHED", "CANCELLED", "ENDED"})
  void shouldRejectUpdate_whenNotDraft(EventStatus status) {
    Event event = eventWithStatus(status, venueWithCapacity(100), List.of(ticketType("GA", 10)));

    assertThatThrownBy(() -> validator.validateCanBeUpdated(event))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Only draft events can be updated");
  }

  // === validateCanBeCancelled ===

  @Test
  void shouldAllowCancel_whenPublished() {
    Event event =
        eventWithStatus(
            EventStatus.PUBLISHED, venueWithCapacity(100), List.of(ticketType("GA", 10)));

    validator.validateCanBeCancelled(event);
  }

  @ParameterizedTest
  @EnumSource(
      value = EventStatus.class,
      names = {"DRAFT", "CANCELLED", "ENDED"})
  void shouldRejectCancel_whenNotPublished(EventStatus status) {
    Event event = eventWithStatus(status, venueWithCapacity(100), List.of(ticketType("GA", 10)));

    assertThatThrownBy(() -> validator.validateCanBeCancelled(event))
        .isInstanceOf(BusinessException.class)
        .hasMessageContaining("Only published events can be cancelled");
  }

  // === Helpers ===

  private Event draftEvent(Venue venue, List<TicketType> ticketTypes) {
    return eventWithStatus(EventStatus.DRAFT, venue, ticketTypes);
  }

  private Event eventWithStatus(EventStatus status, Venue venue, List<TicketType> ticketTypes) {
    Event event =
        Event.builder()
            .id(UUID.randomUUID())
            .title("Test Event")
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category())
            .status(status)
            .ticketTypes(new ArrayList<>(ticketTypes))
            .build();
    // Bidirectional relationship
    ticketTypes.forEach(tt -> tt.setEvent(event));
    return event;
  }

  private Venue venueWithCapacity(int capacity) {
    return Venue.builder()
        .id(UUID.randomUUID())
        .name("Test Venue")
        .address("123 Main St")
        .city("Sao Paulo")
        .country("Brazil")
        .capacity(capacity)
        .build();
  }

  private com.eventixx.eventcatalog.entities.Category category() {
    return com.eventixx.eventcatalog.entities.Category.builder()
        .id(UUID.randomUUID())
        .name("Music")
        .build();
  }

  private TicketType ticketType(String name, int quantity) {
    return TicketType.builder()
        .id(UUID.randomUUID())
        .name(name)
        .price(BigDecimal.valueOf(100))
        .quantityAvailable(quantity)
        .build();
  }
}
