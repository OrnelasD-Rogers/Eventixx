package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class TicketTypeCatalogIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void shouldCreateTicketType() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);

        var created = restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("VIP");
    }

    @Test
    void shouldGetTicketTypeById() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();

        var response = restClient.get()
                .uri("/api/v1/events/{eventId}/ticket-types/{id}", event.id(), ticketType.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();
        assertThat(response.name()).isEqualTo("VIP");
    }

    @Test
    void shouldListTicketTypes() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request1 = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var request2 = new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 100);
        restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request1)
                .exchange()
                .expectStatus().isCreated();
        restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request2)
                .exchange()
                .expectStatus().isCreated();

        var ticketTypes = restClient.get()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .exchange()
                .expectBody(new ParameterizedTypeReference<List<TicketTypeResponse>>() {})
                .returnResult().getResponseBody();
        assertThat(ticketTypes)
                .extracting(TicketTypeResponse::name)
                .contains("VIP", "Standard");
    }

    @Test
    void shouldUpdateTicketType() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();
        var update = new UpdateTicketTypeRequest("Updated VIP", BigDecimal.valueOf(250), 40);

        var updated = restClient.put()
                .uri("/api/v1/events/{eventId}/ticket-types/{id}", event.id(), ticketType.id())
                .headers(requestHeaders())
                .body(update)
                .exchange()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();
        assertThat(updated.name()).isEqualTo("Updated VIP");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restClient.post()
                .uri("/api/v1/events/{eventId}/ticket-types", event.id())
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(TicketTypeResponse.class)
                .returnResult().getResponseBody();

        restClient.delete()
                .uri("/api/v1/events/{eventId}/ticket-types/{id}", event.id(), ticketType.id())
                .headers(requestHeaders())
                .exchange();

        restClient.get()
                .uri("/api/v1/events/{eventId}/ticket-types/{id}", event.id(), ticketType.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn404WhenNotFound() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        restClient.get()
                .uri("/api/v1/events/{eventId}/ticket-types/{id}", event.id(), UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }
}
