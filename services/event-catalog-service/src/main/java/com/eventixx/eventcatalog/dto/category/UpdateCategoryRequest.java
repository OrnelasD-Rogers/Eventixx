package com.eventixx.eventcatalog.dto.category;

import jakarta.validation.constraints.Size;

/** Request DTO to update a category. */
public record UpdateCategoryRequest(
    @Size(max = 100) String name, @Size(max = 500) String description) {}
