package com.eventixx.eventcatalog.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.eventixx.eventcatalog.dto.category.CategoryResponse;
import com.eventixx.eventcatalog.dto.category.CategorySummaryResponse;
import com.eventixx.eventcatalog.dto.category.CreateCategoryRequest;
import com.eventixx.eventcatalog.dto.category.UpdateCategoryRequest;
import java.util.UUID;
import org.assertj.core.api.InstanceOfAssertFactories;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.ProblemDetail;

class CategoryCatalogIntegrationTest extends CatalogIntegrationTestBase {

  @Test
  void shouldCreateCategory() {
    var request = new CreateCategoryRequest("Music", "Live music events");
    var created =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(CategoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertThat(created).isNotNull();
    assertThat(created.name()).isEqualTo("Music");
    assertThat(created.description()).isEqualTo("Live music events");
    assertThat(created.id()).isNotNull();
    assertThat(created.createdAt()).isNotNull();
    assertThat(created.updatedAt()).isNotNull();
  }

  @Test
  void shouldCreateCategoryWithoutDescription() {
    var request = new CreateCategoryRequest("Music", null);
    var created =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isCreated()
            .expectBody(CategoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertThat(created).isNotNull();
    assertThat(created.name()).isEqualTo("Music");
    assertThat(created.description()).isNull();
  }

  @Test
  void shouldReturn400WhenNameExceedsMaxLength() {
    var request = new CreateCategoryRequest("a".repeat(101), "Description");

    ProblemDetail problem =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

    assertThat(problem).isNotNull();
    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getProperties()).containsKey("errors");
  }

  @Test
  void shouldReturn400WhenDescriptionExceedsMaxLength() {
    var request = new CreateCategoryRequest("Music", "a".repeat(501));

    ProblemDetail problem =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

    assertThat(problem).isNotNull();
    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getProperties()).containsKey("errors");
  }

  @Test
  void shouldGetCategoryById() {
    var category = createCategory(1);

    var response =
        restClient
            .get()
            .uri("/api/v1/categories/{id}", category.id())
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(CategoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertThat(response).isNotNull();
    assertThat(response.name()).isEqualTo(category.name());
  }

  @Test
  void shouldListCategories() {
    createCategory(1);
    createCategory(2);

    var page =
        restClient
            .get()
            .uri("/api/v1/categories")
            .exchange()
            .expectStatus()
            .isOk()
            .expectBody(new ParameterizedTypeReference<RestPage<CategorySummaryResponse>>() {})
            .returnResult()
            .getResponseBody();

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

    var updated =
        restClient
            .put()
            .uri("/api/v1/categories/{id}", category.id())
            .headers(requestHeaders())
            .body(update)
            .exchange()
            .expectBody(CategoryResponse.class)
            .returnResult()
            .getResponseBody();
    assertThat(updated).isNotNull();
    assertThat(updated.name()).isEqualTo("Updated Music");
  }

  @Test
  void shouldReturn404AfterSoftDelete() {
    var category = createCategory(1);

    restClient
        .delete()
        .uri("/api/v1/categories/{id}", category.id())
        .headers(requestHeaders())
        .exchange();

    restClient
        .get()
        .uri("/api/v1/categories/{id}", category.id())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldReturn404WhenNotFound() {
    restClient
        .get()
        .uri("/api/v1/categories/{id}", UUID.randomUUID())
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldReturn400WhenNameIsBlank() {
    var request = new CreateCategoryRequest("", "Description");

    ProblemDetail problem =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

    assertThat(problem).isNotNull();
    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getType()).hasToString("tag:eventixx.com,2026:problem:validation-error");
    assertThat(problem.getProperties())
        .extractingByKey("errors")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .anyMatch(e -> e.toString().contains("/name"));
  }

  @Test
  void shouldReturn400WhenNameIsNull() {
    var request = new CreateCategoryRequest(null, "Description");

    ProblemDetail problem =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

    assertThat(problem).isNotNull();
    assertThat(problem.getStatus()).isEqualTo(400);
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getType()).hasToString("tag:eventixx.com,2026:problem:validation-error");
    assertThat(problem.getProperties())
        .extractingByKey("errors")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .anyMatch(e -> e.toString().contains("/name"));
  }

  @Test
  void shouldReturn400WhenMissingUserIdHeader() {
    restClient
        .post()
        .uri("/api/v1/categories")
        .body(new CreateCategoryRequest("Music", "Description"))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void shouldReturn404WhenUpdatingNonExistentCategory() {
    var update = new UpdateCategoryRequest("Updated", "Desc");
    ProblemDetail problem =
        restClient
            .put()
            .uri("/api/v1/categories/{id}", UUID.randomUUID())
            .headers(requestHeaders())
            .body(update)
            .exchange()
            .expectStatus()
            .isNotFound()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Resource Not Found");
  }

  @Test
  void shouldReturn400WhenUpdateNameExceedsMaxLength() {
    var category = createCategory(1);
    var update = new UpdateCategoryRequest("a".repeat(101), null);
    ProblemDetail problem =
        restClient
            .put()
            .uri("/api/v1/categories/{id}", category.id())
            .headers(requestHeaders())
            .body(update)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getProperties())
        .extractingByKey("errors")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .anyMatch(e -> e.toString().contains("/name"));
  }

  @Test
  void shouldReturn400WhenUpdateDescriptionExceedsMaxLength() {
    var category = createCategory(1);
    var update = new UpdateCategoryRequest("Music", "a".repeat(501));
    ProblemDetail problem =
        restClient
            .put()
            .uri("/api/v1/categories/{id}", category.id())
            .headers(requestHeaders())
            .body(update)
            .exchange()
            .expectStatus()
            .isBadRequest()
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Validation Error");
    assertThat(problem.getProperties())
        .extractingByKey("errors")
        .asInstanceOf(InstanceOfAssertFactories.LIST)
        .anyMatch(e -> e.toString().contains("/description"));
  }

  @Test
  void shouldReturn400WhenMissingUserIdHeaderOnUpdate() {
    var category = createCategory(1);
    restClient
        .put()
        .uri("/api/v1/categories/{id}", category.id())
        .body(new UpdateCategoryRequest("Updated", null))
        .exchange()
        .expectStatus()
        .isBadRequest();
  }

  @Test
  void shouldReturn404WhenUpdatingSoftDeletedCategory() {
    var category = createCategory(1);
    restClient
        .delete()
        .uri("/api/v1/categories/{id}", category.id())
        .headers(requestHeaders())
        .exchange();
    restClient
        .put()
        .uri("/api/v1/categories/{id}", category.id())
        .headers(requestHeaders())
        .body(new UpdateCategoryRequest("Updated", null))
        .exchange()
        .expectStatus()
        .isNotFound();
  }

  @Test
  void shouldReturn409WhenUpdatingToDuplicateName() {
    createCategory(1);
    var category2 = createCategory(2);
    var update = new UpdateCategoryRequest("Test Category_1", null);
    ProblemDetail problem =
        restClient
            .put()
            .uri("/api/v1/categories/{id}", category2.id())
            .headers(requestHeaders())
            .body(update)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();
    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Conflict");
    assertThat(problem.getDetail()).contains("Test Category_1");
  }

  @Test
  void shouldReturn409WhenDuplicateName() {
    createCategory(1);

    var request = new CreateCategoryRequest("Test Category_1", "Description");
    ProblemDetail problem =
        restClient
            .post()
            .uri("/api/v1/categories")
            .headers(requestHeaders())
            .body(request)
            .exchange()
            .expectStatus()
            .isEqualTo(409)
            .expectBody(ProblemDetail.class)
            .returnResult()
            .getResponseBody();

    assertThat(problem).isNotNull();
    assertThat(problem.getTitle()).isEqualTo("Conflict");
    assertThat(problem.getDetail()).contains("Test Category_1");
  }
}
