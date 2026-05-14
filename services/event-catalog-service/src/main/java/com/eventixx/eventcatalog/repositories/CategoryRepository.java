package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Category;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

/**
 * Repository for category persistence operations.
 */
public interface CategoryRepository {

  Category save(Category category);

  Optional<Category> findById(UUID id);

  Page<Category> findAll(Pageable pageable);

  void delete(Category category);

  boolean existsByName(String name);

  Category saveAndFlush(Category category);
}
