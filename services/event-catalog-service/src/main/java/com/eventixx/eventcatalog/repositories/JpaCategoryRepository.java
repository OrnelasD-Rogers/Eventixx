package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Category;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.UUID;

@Repository
public interface JpaCategoryRepository extends JpaRepository<Category, UUID>, CategoryRepository {
}
