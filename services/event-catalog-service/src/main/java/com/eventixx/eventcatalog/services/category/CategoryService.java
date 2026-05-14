package com.eventixx.eventcatalog.services.category;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;

import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

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
     */
    @Transactional
    public CategoryResponse update(UUID id, UpdateCategoryRequest request) {
        Category category = findCategoryOrThrow(id);
        categoryMapper.updateEntity(request, category);
        return categoryMapper.toResponse(category);
    }

    @Transactional
    public void delete(UUID id) {
        Category category = findCategoryOrThrow(id);
        categoryRepository.delete(category);
    }

    private Category findCategoryOrThrow(UUID id) {
        return categoryRepository.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Category with id " + id + " not found"));
    }
}
