package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ProblemDetail;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class CategoryCatalogIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void shouldCreateCategory() {
        var request = new CreateCategoryRequest("Music", "Live music events");
        var created = restClient.post()
                .uri("/api/v1/categories")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(CategoryResponse.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("Music");
    }

    @Test
    void shouldGetCategoryById() {
        var category = createCategory(1);

        var response = restClient.get()
                .uri("/api/v1/categories/{id}", category.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(CategoryResponse.class)
                .returnResult().getResponseBody();
        assertThat(response).isNotNull();
        assertThat(response.name()).isEqualTo(category.name());
    }

    @Test
    void shouldListCategories() {
        createCategory(1);
        createCategory(2);

        var page = restClient.get()
                .uri("/api/v1/categories")
                .exchange()
                .expectStatus().isOk()
                .expectBody(new ParameterizedTypeReference<RestPage<CategorySummaryResponse>>() { })
                .returnResult().getResponseBody();

        assertThat(page).isNotNull();
        assertThat(page.getContent())
                .hasSize(2)
                .extracting(CategorySummaryResponse::name)
                .contains("Test Category_1", "Test Category_2");
    }

    @Test
    void shouldUpdateCategory() {
        var category = createCategory(1);
        var update = new UpdateCategoryRequest("Updated Music", "Updated description");

        var updated = restClient.put()
                .uri("/api/v1/categories/{id}", category.id())
                .headers(requestHeaders())
                .body(update)
                .exchange()
                .expectBody(CategoryResponse.class)
                .returnResult().getResponseBody();
        assertThat(updated).isNotNull();
        assertThat(updated.name()).isEqualTo("Updated Music");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var category = createCategory(1);

        restClient.delete()
                .uri("/api/v1/categories/{id}", category.id())
                .headers(requestHeaders())
                .exchange();

        restClient.get()
                .uri("/api/v1/categories/{id}", category.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn404WhenNotFound() {
        restClient.get()
                .uri("/api/v1/categories/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn400WhenNameIsBlank() {
        var request = new CreateCategoryRequest("", "Description");

        ProblemDetail problem = restClient.post()
                .uri("/api/v1/categories")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult().getResponseBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturn400WhenNameIsNull() {
        var request = new CreateCategoryRequest(null, "Description");

        ProblemDetail problem = restClient.post()
                .uri("/api/v1/categories")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isBadRequest()
                .expectBody(ProblemDetail.class)
                .returnResult().getResponseBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Validation Error");
        assertThat(problem.getStatus()).isEqualTo(400);
    }

    @Test
    void shouldReturn400WhenMissingUserIdHeader() {
        restClient.post()
                .uri("/api/v1/categories")
                .body(new CreateCategoryRequest("Music", "Description"))
                .exchange()
                .expectStatus().isBadRequest();
    }

    @Test
    void shouldReturn409WhenDuplicateName() {
        createCategory(1);

        var request = new CreateCategoryRequest("Test Category_1", "Description");
        ProblemDetail problem = restClient.post()
                .uri("/api/v1/categories")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isEqualTo(409)
                .expectBody(ProblemDetail.class)
                .returnResult().getResponseBody();

        assertThat(problem).isNotNull();
        assertThat(problem.getTitle()).isEqualTo("Conflict");
        assertThat(problem.getDetail()).contains("Test Category_1");
    }
}
