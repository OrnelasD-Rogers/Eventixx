package com.eventixx.eventcatalog.unit;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import com.eventixx.eventcatalog.entities.Category;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.repositories.CategoryRepository;
import com.eventixx.eventcatalog.services.category.CategoryMapper;
import com.eventixx.eventcatalog.services.category.CategoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;
    @Mock
    private CategoryMapper categoryMapper;

    @InjectMocks
    private CategoryService categoryService;

    private Category category;
    private CategoryResponse categoryResponse;

    @BeforeEach
    void setUp() {
        category = Category.builder()
            .id(UUID.randomUUID())
            .name("Music")
            .description("Live music events")
            .build();

        categoryResponse = new CategoryResponse(
            category.getId(), category.getName(), category.getDescription(),
            Instant.now(), Instant.now()
        );
    }

    @Test
    void shouldCreateCategory() {
        CreateCategoryRequest request = new CreateCategoryRequest("Sports", "Sports events");
        Category newCategory = Category.builder().name("Sports").description("Sports events").build();

        when(categoryMapper.toEntity(request)).thenReturn(newCategory);
        when(categoryRepository.saveAndFlush(newCategory)).thenReturn(newCategory);
        when(categoryMapper.toResponse(newCategory)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.create(request);

        assertThat(result).isEqualTo(categoryResponse);
    }

    @Test
    void shouldFindCategoryById() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.findById(category.getId());

        assertThat(result).isEqualTo(categoryResponse);
    }

    @Test
    void shouldThrow_whenCategoryNotFound() {
        UUID id = UUID.randomUUID();
        when(categoryRepository.findById(id)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> categoryService.findById(id))
            .isInstanceOf(ResourceNotFoundException.class)
            .hasMessageContaining("Category with id");
    }

    @Test
    void shouldFindAllCategories() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Category> page = new PageImpl<>(List.of(category));
        when(categoryRepository.findAll(pageable)).thenReturn(page);
        when(categoryMapper.toSummary(any(Category.class))).thenReturn(
            new CategorySummaryResponse(category.getId(), category.getName())
        );

        Page<CategorySummaryResponse> result = categoryService.findAll(pageable);

        assertThat(result.getContent()).hasSize(1);
    }

    @Test
    void shouldUpdateCategory() {
        UpdateCategoryRequest request = new UpdateCategoryRequest("Updated Music", null);

        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));
        when(categoryMapper.toResponse(category)).thenReturn(categoryResponse);

        CategoryResponse result = categoryService.update(category.getId(), request);

        assertThat(result).isEqualTo(categoryResponse);
        verify(categoryMapper).updateEntity(request, category);
    }

    @Test
    void shouldDeleteCategory() {
        when(categoryRepository.findById(category.getId())).thenReturn(Optional.of(category));

        categoryService.delete(category.getId());

        verify(categoryRepository).delete(category);
    }
}
