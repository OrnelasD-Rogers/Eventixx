package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCatalogIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void shouldCreateCategory() {
        var request = new CreateCategoryRequest("Music", "Live music events");
        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                "/api/v1/categories", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                CategoryResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Music");
    }

    @Test
    void shouldGetCategoryById() {
        var category = createCategory(1);

        ResponseEntity<CategoryResponse> response = restTemplate.getForEntity(
                "/api/v1/categories/{id}", CategoryResponse.class, category.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assert response.getBody() != null;
        assertThat(response.getBody().name()).isEqualTo(category.name());
    }

    @Test
    void shouldListCategories() {
        createCategory(1);
        createCategory(2);

        var response = restTemplate.exchange(
                "/api/v1/categories", HttpMethod.GET, null,
                new ParameterizedTypeReference<RestPage<CategorySummaryResponse>>() { });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assert response.getBody() != null;
        assertThat(response.getBody().getContent())
                .hasSize(2)
                .extracting(CategorySummaryResponse::name)
                .contains("Test Category_1", "Test Category_2");
    }

    @Test
    void shouldUpdateCategory() {
        var category = createCategory(1);
        var update = new UpdateCategoryRequest("Updated Music", "Updated description");

        ResponseEntity<CategoryResponse> response = restTemplate.exchange(
                "/api/v1/categories/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authEntity().getHeaders()),
                CategoryResponse.class, category.id());

        assert response.getBody() != null;
        assertThat((response.getBody()).name()).isEqualTo("Updated Music");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var category = createCategory(1);

        restTemplate.exchange("/api/v1/categories/{id}", HttpMethod.DELETE,
                authEntity(), Void.class, category.id());

        ResponseEntity<CategoryResponse> response = restTemplate.getForEntity(
                "/api/v1/categories/{id}", CategoryResponse.class, category.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WhenNotFound() {
        ResponseEntity<CategoryResponse> response = restTemplate.getForEntity(
                "/api/v1/categories/{id}", CategoryResponse.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
