package com.eventixx.eventcatalog.repositories;

import com.eventixx.eventcatalog.entities.Category;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface JpaCategoryRepository extends JpaRepository<Category, UUID>, CategoryRepository {}
