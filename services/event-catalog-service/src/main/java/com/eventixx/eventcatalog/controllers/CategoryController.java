package com.eventixx.eventcatalog.controllers;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import com.eventixx.eventcatalog.services.category.CategoryService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/categories")
@RequiredArgsConstructor
@Tag(name = "Categories", description = "Event category management")
public class CategoryController {

  private final CategoryService categoryService;

  @PostMapping
  @ResponseStatus(HttpStatus.CREATED)
  @Operation(summary = "Create a new category")
  @ApiResponse(responseCode = "201", description = "Category created successfully")
  @ApiResponse(responseCode = "400", description = "Invalid input or missing header")
  @ApiResponse(responseCode = "409", description = "Category with the same name already exists")
  public CategoryResponse create(
      @Valid @RequestBody CreateCategoryRequest request,
      @RequestHeader("X-User-Id") String userId) {
    return categoryService.create(request);
  }

  @GetMapping("/{id}")
  @Operation(summary = "Get category by ID")
  @ApiResponse(responseCode = "200", description = "Category found")
  @ApiResponse(responseCode = "404", description = "Category not found")
  public CategoryResponse getById(@PathVariable UUID id) {
    return categoryService.findById(id);
  }

  @GetMapping
  @Operation(summary = "List all categories")
  @ApiResponse(responseCode = "200", description = "List of categories")
  public Page<CategorySummaryResponse> list(Pageable pageable) {
    return categoryService.findAll(pageable);
  }

  @PutMapping("/{id}")
  @Operation(summary = "Update category")
  @ApiResponse(responseCode = "200", description = "Category updated")
  @ApiResponse(responseCode = "400", description = "Invalid input")
  @ApiResponse(responseCode = "404", description = "Category not found")
  public CategoryResponse update(
      @PathVariable UUID id,
      @Valid @RequestBody UpdateCategoryRequest request,
      @RequestHeader("X-User-Id") String userId) {
    return categoryService.update(id, request);
  }

  @DeleteMapping("/{id}")
  @ResponseStatus(HttpStatus.NO_CONTENT)
  @Operation(summary = "Delete category")
  @ApiResponse(responseCode = "204", description = "Category deleted")
  @ApiResponse(responseCode = "404", description = "Category not found")
  public void delete(@PathVariable UUID id, @RequestHeader("X-User-Id") String userId) {
    categoryService.delete(id);
  }
}
