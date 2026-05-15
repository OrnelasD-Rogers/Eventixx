package com.eventixx.eventcatalog.dto.category;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Request DTO to create a category. */
public record CreateCategoryRequest(
    @NotBlank @Size(max = 100) @Pattern(regexp = "[^<>&\"']*") String name,
    @Size(max = 500) @Pattern(regexp = "[^<>&\"']*") String description) {}
