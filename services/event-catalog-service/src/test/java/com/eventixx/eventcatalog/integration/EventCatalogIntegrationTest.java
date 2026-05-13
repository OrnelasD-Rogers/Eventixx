package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import org.apache.kafka.clients.consumer.Consumer;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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

        ResponseEntity<EventResponse> publishResponse = restTemplate.exchange(
                "/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(publishResponse.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(publishResponse.getBody().status()).isEqualTo("PUBLISHED");
    }

    // A2: Kafka message payload verification (EventPublished JSON)
    @Test
    void shouldSendKafkaMessageOnPublish() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restTemplate.exchange("/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

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

        var draftResponse = restTemplate.exchange(
                "/api/v1/events?status=DRAFT", HttpMethod.GET, null,
                new ParameterizedTypeReference<RestPage<EventSummaryResponse>>() { });
        assertThat(draftResponse.getBody().getContent())
                .extracting(EventSummaryResponse::title)
                .contains("Test Event");

        restTemplate.exchange("/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        var publishedResponse = restTemplate.exchange(
                "/api/v1/events?status=PUBLISHED", HttpMethod.GET, null,
                new ParameterizedTypeReference<RestPage<EventSummaryResponse>>() { });
        assertThat(publishedResponse.getBody().getContent())
                .extracting(EventSummaryResponse::title)
                .contains("Test Event");
    }

    // A4: Cancel published event
    @Test
    void shouldCancelPublishedEvent() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restTemplate.exchange("/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        ResponseEntity<EventResponse> cancelResponse = restTemplate.exchange(
                "/api/v1/events/{id}/cancel", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(cancelResponse.getBody().status()).isEqualTo("CANCELLED");
    }

    // A5: Soft delete → 404
    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restTemplate.exchange("/api/v1/events/{id}", HttpMethod.DELETE,
                authEntity(), Void.class, event.id());

        ResponseEntity<EventResponse> getResponse = restTemplate.getForEntity(
                "/api/v1/events/{id}", EventResponse.class, event.id());
        assertThat(getResponse.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    // A6: Update event
    @Test
    void shouldUpdateEvent() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        var update = new UpdateEventRequest("Updated Title", null, null, null, null, null, null);
        ResponseEntity<EventResponse> updateResponse = restTemplate.exchange(
                "/api/v1/events/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authEntity().getHeaders()), EventResponse.class, event.id());

        assertThat(updateResponse.getBody().title()).isEqualTo("Updated Title");
    }

    // A7: GET without auth header
    @Test
    void shouldAllowPublicGetWithoutAuthHeader() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        ResponseEntity<EventResponse> response = restTemplate.getForEntity(
                "/api/v1/events/{id}", EventResponse.class, event.id());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
    }

    // B1: Publish already published → 409
    @Test
    void shouldReturn409WhenAlreadyPublished() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restTemplate.exchange("/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        ResponseEntity<EventResponse> response = restTemplate.exchange(
                "/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // B2: Cancel DRAFT → 400
    @Test
    void shouldReturn400WhenCancellingDraft() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        ResponseEntity<EventResponse> response = restTemplate.exchange(
                "/api/v1/events/{id}/cancel", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // B3: Publish without ticket types → 400
    @Test
    void shouldReturn400WhenPublishingWithoutTicketTypes() {
        var venue = createVenue();
        var category = createCategory(1);
        var request = buildCreateRequest(venue.id(), category.id(), List.of());
        ResponseEntity<EventResponse> createResponse = restTemplate.exchange(
                "/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()), EventResponse.class);
        var event = createResponse.getBody();

        ResponseEntity<EventResponse> response = restTemplate.exchange(
                "/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // B4: Ticket quantity exceeds capacity → 400
    @Test
    void shouldReturn400WhenTicketQuantityExceedsCapacity() {
        var venue = createVenue();
        var category = createCategory(1);
        var overCapacityTickets = List.of(
                new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 150));
        var request = buildCreateRequest(venue.id(), category.id(), overCapacityTickets);
        ResponseEntity<EventResponse> createResponse = restTemplate.exchange(
                "/api/v1/events", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()), EventResponse.class);
        var event = createResponse.getBody();

        ResponseEntity<EventResponse> response = restTemplate.exchange(
                "/api/v1/events/{id}/publish", HttpMethod.POST,
                authEntity(), EventResponse.class, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    // B5: Event not found → 404
    @Test
    void shouldReturn404WhenEventNotFound() {
        ResponseEntity<EventResponse> response = restTemplate.getForEntity(
                "/api/v1/events/{id}", EventResponse.class, UUID.randomUUID());
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
