package com.eventixx.eventcatalog.repository;

import com.eventixx.eventcatalog.entities.Venue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueRepositorySoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSoftDeleteVenue() {
        // 1. Create and persist
        Venue venue = Venue.builder()
            .name("Test Venue")
            .address("123 Main St")
            .city("Sao Paulo")
            .country("Brazil")
            .capacity(100)
            .build();

        Venue saved = entityManager.persistFlushFind(venue);
        UUID id = saved.getId();

        // 2. Soft delete ( Hibernate @SQLDelete replaces the DELETE SQL )
        entityManager.remove(saved);
        entityManager.flush();

        // 3. Assert: @SQLRestriction filters deleted records
        assertThat(entityManager.find(Venue.class, id)).isNull();

        // 4. Assert: record still exists in DB with deleted_at populated
        Instant deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM venues WHERE id = ?", Instant.class, id);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void shouldNotIncludeDeletedVenueInQuery() {
        Venue venue = Venue.builder()
            .name("Deleted Venue")
            .address("456 Side St")
            .city("Rio")
            .country("Brazil")
            .capacity(200)
            .build();

        Venue saved = entityManager.persistFlushFind(venue);
        entityManager.remove(saved);
        entityManager.flush();

        // Native query to count all records including deleted
        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM venues", Integer.class);
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM venues WHERE deleted_at IS NULL", Integer.class);

        assertThat(totalCount).isGreaterThan(activeCount);
    }
}
