package com.eventixx.eventcatalog.repository;

import com.eventixx.eventcatalog.entities.Category;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jpa.test.autoconfigure.TestEntityManager;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryRepositorySoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSoftDeleteCategory() {
        // 1. Create and persist
        Category category = Category.builder()
            .name("Music" + UUID.randomUUID())
            .description("Music events")
            .build();

        Category saved = entityManager.persistFlushFind(category);
        UUID id = saved.getId();

        // 2. Soft delete
        entityManager.remove(saved);
        entityManager.flush();

        // 3. Assert: @SQLRestriction filters deleted records
        assertThat(entityManager.find(Category.class, id)).isNull();

        // 4. Assert: record still exists in DB with deleted_at populated
        Instant deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM categories WHERE id = ?", Instant.class, id);
        assertThat(deletedAt).isNotNull();
    }

    @Test
    void shouldNotIncludeDeletedCategoryInQuery() {
        Category category = Category.builder()
            .name("Deleted" + UUID.randomUUID())
            .description("To be deleted")
            .build();

        Category saved = entityManager.persistFlushFind(category);
        entityManager.remove(saved);
        entityManager.flush();

        Integer totalCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM categories", Integer.class);
        Integer activeCount = jdbcTemplate.queryForObject(
            "SELECT COUNT(*) FROM categories WHERE deleted_at IS NULL", Integer.class);

        assertThat(totalCount).isGreaterThan(activeCount);
    }
}
