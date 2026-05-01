package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Category;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;
import java.util.UUID;

public interface CategoryRepository {

    Category save(Category category);

    Optional<Category> findById(UUID id);

    Page<Category> findAll(Pageable pageable);

    void delete(Category category);
}
