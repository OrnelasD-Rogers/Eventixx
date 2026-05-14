package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpStatus;
import org.springframework.kafka.test.utils.KafkaTestUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

class EventCatalogIntegrationTest extends CatalogIntegrationTestBase {

    // A1: Publish event (happy path)
    @Test
    void shouldCreateAndPublishEvent() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        var published = restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();
        assertThat(published.status()).isEqualTo("PUBLISHED");
    }

    // A2: Kafka message payload verification (EventPublished JSON)
    @Test
    void shouldSendKafkaMessageOnPublish() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange();

        Consumer<String, String> consumer = createKafkaConsumer();
        await().atMost(Duration.ofSeconds(10)).untilAsserted(() -> {
            ConsumerRecords<String, String> records = KafkaTestUtils.getRecords(consumer);
            assertThat(records.count()).isGreaterThan(0);
            boolean found = false;
            for (var record : records) {
                if (event.id().toString().equals(record.key())) {
                    assertThat(record.value()).contains("\"title\":\"Test Event\"");
                    found = true;
                    break;
                }
            }
            assertThat(found).isTrue();
        });
    }

    // A3: List events by status (DRAFT / PUBLISHED)
    @Test
    void shouldListEventsByStatus() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        var draftPage = restClient.get()
                .uri("/api/v1/events?status=DRAFT")
                .exchange()
                .expectBody(new ParameterizedTypeReference<RestPage<EventSummaryResponse>>() {})
                .returnResult().getResponseBody();
        assertThat(draftPage.getContent())
                .extracting(EventSummaryResponse::title)
                .contains("Test Event");

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange();

        var publishedPage = restClient.get()
                .uri("/api/v1/events?status=PUBLISHED")
                .exchange()
                .expectBody(new ParameterizedTypeReference<RestPage<EventSummaryResponse>>() {})
                .returnResult().getResponseBody();
        assertThat(publishedPage.getContent())
                .extracting(EventSummaryResponse::title)
                .contains("Test Event");
    }

    // A4: Cancel published event
    @Test
    void shouldCancelPublishedEvent() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange();

        var cancelled = restClient.post()
                .uri("/api/v1/events/{id}/cancel", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();
        assertThat(cancelled.status()).isEqualTo("CANCELLED");
    }

    // A5: Soft delete → 404
    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.delete()
                .uri("/api/v1/events/{id}", event.id())
                .headers(requestHeaders())
                .exchange();

        restClient.get()
                .uri("/api/v1/events/{id}", event.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    // A6: Update event
    @Test
    void shouldUpdateEvent() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        var update = new UpdateEventRequest("Updated Title", null, null, null, null, null, null);
        var updated = restClient.put()
                .uri("/api/v1/events/{id}", event.id())
                .headers(requestHeaders())
                .body(update)
                .exchange()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();
        assertThat(updated.title()).isEqualTo("Updated Title");
    }

    // A7: GET without auth header
    @Test
    void shouldAllowPublicGetWithoutAuthHeader() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        var response = restClient.get()
                .uri("/api/v1/events/{id}", event.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();
        assertThat(response.status()).isEqualTo("DRAFT");
    }

    // B1: Publish already published → 409
    @Test
    void shouldReturn409WhenAlreadyPublished() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange();

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectStatus().isEqualTo(HttpStatus.CONFLICT);
    }

    // B2: Cancel DRAFT → 400
    @Test
    void shouldReturn400WhenCancellingDraft() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.post()
                .uri("/api/v1/events/{id}/cancel", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectStatus().isBadRequest();
    }

    // B3: Publish without ticket types → 400
    @Test
    void shouldReturn400WhenPublishingWithoutTicketTypes() {
        var venue = createVenue();
        var category = createCategory(1);
        var request = buildCreateRequest(venue.id(), category.id(), List.of());
        var event = restClient.post()
                .uri("/api/v1/events")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectStatus().isBadRequest();
    }

    // B4: Ticket quantity exceeds capacity → 400
    @Test
    void shouldReturn400WhenTicketQuantityExceedsCapacity() {
        var venue = createVenue();
        var category = createCategory(1);
        var overCapacityTickets = List.of(
                new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 150));
        var request = buildCreateRequest(venue.id(), category.id(), overCapacityTickets);
        var event = restClient.post()
                .uri("/api/v1/events")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectBody(EventResponse.class)
                .returnResult().getResponseBody();

        restClient.post()
                .uri("/api/v1/events/{id}/publish", event.id())
                .headers(requestHeaders())
                .exchange()
                .expectStatus().isBadRequest();
    }

    // B5: Event not found → 404
    @Test
    void shouldReturn404WhenEventNotFound() {
        restClient.get()
                .uri("/api/v1/events/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }
}
