package com.eventixx.eventcatalog.dto.category;

import jakarta.validation.constraints.NotBlank;

/** Request DTO to create a category. */
public record CreateCategoryRequest(@NotBlank String name, String description) {}
