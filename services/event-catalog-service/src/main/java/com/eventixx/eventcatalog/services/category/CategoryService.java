package com.eventixx.eventcatalog.services.category;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;

import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final CategoryMapper categoryMapper;

    @Transactional
    public CategoryResponse create(CreateCategoryRequest request) {
        Category category = categoryMapper.toEntity(request);
        return categoryMapper.toResponse(categoryRepository.save(category));
    }

    public CategoryResponse findById(UUID id) {
        return categoryMapper.toResponse(findCategoryOrThrow(id));
    }

    public Page<CategorySummaryResponse> findAll(Pageable pageable) {
        return categoryRepository.findAll(pageable).map(categoryMapper::toSummary);
    }

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
            .orElseThrow(() -> new BusinessException("Category with id " + id + " not found", HttpStatus.NOT_FOUND));
    }
}
