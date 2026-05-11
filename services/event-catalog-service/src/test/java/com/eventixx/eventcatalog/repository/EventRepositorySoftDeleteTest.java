package com.eventixx.eventcatalog.repository;

import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.entities.Event;
import com.eventixx.eventcatalog.entities.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EventRepositorySoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSoftDeleteEvent() {
        // 1. Setup dependencies
        Venue venue = Venue.builder()
            .name("Test Venue")
            .address("123 Main St")
            .city("Sao Paulo")
            .country("Brazil")
            .capacity(100)
            .build();

        Category category = Category.builder()
            .name("Music" + UUID.randomUUID())
            .description("Music events")
            .build();

        venue = entityManager.persistFlushFind(venue);
        category = entityManager.persistFlushFind(category);

        Event event = Event.builder()
            .title("Test Event")
            .description("A test event")
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category)
            .build();

        Event saved = entityManager.persistFlushFind(event);
        UUID id = saved.getId();

        // 2. Soft delete
        entityManager.remove(saved);
        entityManager.flush();

        // 3. Assert: @SQLRestriction filters deleted records
        assertThat(entityManager.find(Event.class, id)).isNull();

        // 4. Assert: record still exists in DB with deleted_at populated
        Instant deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM events WHERE id = ?", Instant.class, id);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void shouldNotIncludeDeletedEventInQuery() {
        Venue venue = Venue.builder()
            .name("Venue 2")
            .address("456")
            .city("Rio")
            .country("Brazil")
            .capacity(200)
            .build();

        Category category = Category.builder()
            .name("Sports" + UUID.randomUUID())
            .description("Sports events")
            .build();

        venue = entityManager.persistFlushFind(venue);
        category = entityManager.persistFlushFind(category);

        Event event = Event.builder()
            .title("Deleted Event")
            .startTime(Instant.now().plusSeconds(3600))
            .endTime(Instant.now().plusSeconds(7200))
            .venue(venue)
            .category(category)
            .build();

        Event saved = entityManager.persistFlushFind(event);
        entityManager.remove(saved);
        entityManager.flush();

        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM events", Integer.class);
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM events WHERE deleted_at IS NULL", Integer.class);

        assertThat(totalCount).isGreaterThan(activeCount);
    }
}
