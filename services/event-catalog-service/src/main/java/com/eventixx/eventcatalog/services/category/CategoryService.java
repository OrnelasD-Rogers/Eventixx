package com.eventixx.eventcatalog.services.category;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.repositories.EventRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

  private final CategoryRepository categoryRepository;
  private final CategoryMapper categoryMapper;
  private final EventRepository eventRepository;

  /**
   * Creates a new category, throwing {@link ConflictException} if the name already exists.
   * <p>Uses check-then-act with an explicit flush to close the race window and catch
   * {@link DataIntegrityViolationException} as a safety net for concurrent duplicate inserts.</p>
   */
  @Transactional
  public CategoryResponse create(CreateCategoryRequest request) {
    if (categoryRepository.existsByName(request.name())) {
      throw new ConflictException("Category with name '" + request.name() + "' already exists");
    }
    Category category = categoryMapper.toEntity(request);
    try {
      Category saved = categoryRepository.saveAndFlush(category);
      return categoryMapper.toResponse(saved);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Category with name '" + request.name() + "' already exists", e);
    }
  }

  public CategoryResponse findById(UUID id) {
    return categoryMapper.toResponse(findCategoryOrThrow(id));
  }

  public Page<CategorySummaryResponse> findAll(Pageable pageable) {
    return categoryRepository.findAll(pageable).map(categoryMapper::toSummary);
  }

  /**
   * Updates an existing category.
   * <p>Checks for duplicate name (excluding current entity) and catches
   * {@link DataIntegrityViolationException} as a safety net for concurrent updates.
   * Handles {@link ObjectOptimisticLockingFailureException} when {@code @Version} detects a conflict.</p>
   */
  @Transactional
  public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
    Category category = findCategoryOrThrow(id);
    if (request.name() != null
        && !request.name().equals(category.getName())
        && categoryRepository.existsByName(request.name())) {
      throw new ConflictException("Category with name '" + request.name() + "' already exists");
    }
    categoryMapper.updateEntity(request, category);
    try {
      Category saved = categoryRepository.saveAndFlush(category);
      return categoryMapper.toResponse(saved);
    } catch (DataIntegrityViolationException e) {
      throw new ConflictException("Category with name '" + request.name() + "' already exists", e);
    } catch (ObjectOptimisticLockingFailureException e) {
      log.warn("Optimistic lock conflict on category {}: {}", id, e.getMessage());
      throw new ConflictException(
          "Category was updated by another user. Please reload and try again.", e);
    }
  }

  /**
   * Soft-deletes a category. Throws {@link ConflictException} if there are events linked to it.
   * <p>The DB enforces {@code ON DELETE RESTRICT}, but this check gives a clear 409 response
   * instead of a generic 500 constraint violation.</p>
   */
  @Transactional
  public void delete(UUID id) {
    Category category = findCategoryOrThrow(id);
    if (eventRepository.existsByCategoryId(id)) {
      throw new ConflictException(
          "Category '" + category.getName() + "' has events linked to it and cannot be deleted");
    }
    categoryRepository.delete(category);
  }

  private Category findCategoryOrThrow(UUID id) {
    return categoryRepository
        .findById(id)
        .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
  }
}
