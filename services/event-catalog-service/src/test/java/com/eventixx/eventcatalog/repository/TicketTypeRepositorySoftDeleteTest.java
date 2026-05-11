package com.eventixx.eventcatalog.repository;

import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.TicketType;
import com.eventixx.eventcatalog.entities.Venue;
import com.eventixx.eventcatalog.repositories.TicketTypeRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTypeRepositorySoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Autowired
    private TicketTypeRepository ticketTypeRepository;

    @Test
    void shouldSoftDeleteTicketType() {
        // 1. Setup dependencies
        Venue venue = Venue.builder()
            .name("Test Venue")
            .address("123")
            .city("Sao Paulo")
            .country("Brazil")
            .capacity(100)
            .build();

        Category category = Category.builder()
            .name("Music" + UUID.randomUUID())
            .build();

        venue = entityManager.persistFlushFind(venue);
        category = entityManager.persistFlushFind(category);

        Event event = Event.builder()
            .title("Test Event")
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category)
            .build();

        event = entityManager.persistFlushFind(event);

        TicketType ticketType = TicketType.builder()
            .name("GA")
            .price(BigDecimal.valueOf(50))
            .quantityAvailable(100)
            .event(event)
            .build();

        TicketType saved = entityManager.persistFlushFind(ticketType);
        UUID id = saved.getId();

        // 2. Soft delete
        entityManager.remove(saved);
        entityManager.flush();

        // 3. Assert: @SQLRestriction filters deleted records
        assertThat(entityManager.find(TicketType.class, id)).isNull();

        // 4. Assert: custom query findAllByEventId also filters deleted records
        assertThat(ticketTypeRepository.findAllByEventId(event.getId())).isEmpty();

        // 5. Assert: record still exists in DB with deleted_at populated
        Instant deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM ticket_types WHERE id = ?", Instant.class, id);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void shouldNotIncludeDeletedTicketTypeInFindAllByEventId() {
        Venue venue = Venue.builder()
            .name("Venue 2")
            .address("456")
            .city("Rio")
            .country("Brazil")
            .capacity(200)
            .build();

        Category category = Category.builder()
            .name("Sports" + UUID.randomUUID())
            .build();

        venue = entityManager.persistFlushFind(venue);
        category = entityManager.persistFlushFind(category);

        Event event = Event.builder()
            .title("Event with tickets")
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category)
            .build();

        event = entityManager.persistFlushFind(event);

        TicketType ticketType = TicketType.builder()
            .name("VIP")
            .price(BigDecimal.valueOf(100))
            .quantityAvailable(50)
            .event(event)
            .build();

        TicketType saved = entityManager.persistFlushFind(ticketType);
        entityManager.remove(saved);
        entityManager.flush();

        assertThat(ticketTypeRepository.findAllByEventId(event.getId())).isEmpty();
    }
}
