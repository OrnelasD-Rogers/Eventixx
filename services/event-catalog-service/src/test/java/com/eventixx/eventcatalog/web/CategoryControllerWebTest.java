package com.eventixx.eventcatalog.web;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.eventixx.eventcatalog.controllers.CategoryController;
import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.services.category.CategoryService;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(CategoryController.class)
class CategoryControllerWebTest {

  @Autowired private MockMvc mockMvc;

  @Autowired private ObjectMapper objectMapper;

  @MockitoBean private CategoryService categoryService;

  private final UUID categoryId = UUID.randomUUID();

  @Test
  void shouldCreateCategory() throws Exception {
    CreateCategoryRequest request = new CreateCategoryRequest("Music", "Live music events");
    CategoryResponse response =
        new CategoryResponse(
            categoryId, "Music", "Live music events", Instant.now(), Instant.now());
    when(categoryService.create(any())).thenReturn(response);

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.id").value(categoryId.toString()))
        .andExpect(jsonPath("$.name").value("Music"));
  }

  @Test
  void shouldReturn400_whenCreateRequestInvalid() throws Exception {
    CreateCategoryRequest request = new CreateCategoryRequest("", null);

    mockMvc
        .perform(
            post("/api/v1/categories")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isBadRequest());
  }

  @Test
  void shouldGetCategoryById() throws Exception {
    CategoryResponse response =
        new CategoryResponse(categoryId, "Music", "Desc", Instant.now(), Instant.now());
    when(categoryService.findById(categoryId)).thenReturn(response);

    mockMvc
        .perform(get("/api/v1/categories/{id}", categoryId))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.id").value(categoryId.toString()));
  }

  @Test
  void shouldReturn404_whenCategoryNotFound() throws Exception {
    when(categoryService.findById(categoryId))
        .thenThrow(new ResourceNotFoundException("Category not found"));

    mockMvc.perform(get("/api/v1/categories/{id}", categoryId)).andExpect(status().isNotFound());
  }

  @Test
  void shouldListCategories() throws Exception {
    Page<CategorySummaryResponse> page =
        new PageImpl<>(List.of(new CategorySummaryResponse(categoryId, "Music")));
    when(categoryService.findAll(any())).thenReturn(page);

    mockMvc
        .perform(get("/api/v1/categories"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].id").value(categoryId.toString()));
  }

  @Test
  void shouldUpdateCategory() throws Exception {
    UpdateCategoryRequest request = new UpdateCategoryRequest("Updated Music", null);
    CategoryResponse response =
        new CategoryResponse(categoryId, "Updated Music", "Desc", Instant.now(), Instant.now());
    when(categoryService.update(eq(categoryId), any())).thenReturn(response);

    mockMvc
        .perform(
            put("/api/v1/categories/{id}", categoryId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.name").value("Updated Music"));
  }

  @Test
  void shouldDeleteCategory() throws Exception {
    mockMvc
        .perform(delete("/api/v1/categories/{id}", categoryId).header("X-User-Id", "user-123"))
        .andExpect(status().isNoContent());
  }
}
