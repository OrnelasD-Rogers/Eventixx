package com.eventixx.eventcatalog.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/** Request DTO to create a category. */
public record CreateCategoryRequest(
    @NotBlank @Size(max = 100) String name, @Size(max = 500) String description) {}
