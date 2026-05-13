package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

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

        ResponseEntity<TicketTypeResponse> response = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                TicketTypeResponse.class, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("VIP");
    }

    @Test
    void shouldGetTicketTypeById() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                TicketTypeResponse.class, event.id()).getBody();

        ResponseEntity<TicketTypeResponse> response = restTemplate.getForEntity(
                "/api/v1/events/{eventId}/ticket-types/{id}",
                TicketTypeResponse.class, event.id(), ticketType.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo("VIP");
    }

    @Test
    void shouldListTicketTypes() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request1 = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var request2 = new CreateTicketTypeRequest("Standard", BigDecimal.valueOf(50), 100);
        restTemplate.exchange("/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request1, authEntity().getHeaders()), TicketTypeResponse.class, event.id());
        restTemplate.exchange("/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request2, authEntity().getHeaders()), TicketTypeResponse.class, event.id());

        var response = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types", HttpMethod.GET, null,
                new ParameterizedTypeReference<List<TicketTypeResponse>>() { }, event.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody())
                .extracting(TicketTypeResponse::name)
                .contains("VIP", "Standard");
    }

    @Test
    void shouldUpdateTicketType() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                TicketTypeResponse.class, event.id()).getBody();
        var update = new UpdateTicketTypeRequest("Updated VIP", BigDecimal.valueOf(250), 40);

        ResponseEntity<TicketTypeResponse> response = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authEntity().getHeaders()),
                TicketTypeResponse.class, event.id(), ticketType.id());

        assertThat(response.getBody().name()).isEqualTo("Updated VIP");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());
        var request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        var ticketType = restTemplate.exchange(
                "/api/v1/events/{eventId}/ticket-types", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                TicketTypeResponse.class, event.id()).getBody();

        restTemplate.exchange("/api/v1/events/{eventId}/ticket-types/{id}", HttpMethod.DELETE,
                authEntity(), Void.class, event.id(), ticketType.id());

        ResponseEntity<TicketTypeResponse> response = restTemplate.getForEntity(
                "/api/v1/events/{eventId}/ticket-types/{id}",
                TicketTypeResponse.class, event.id(), ticketType.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WhenNotFound() {
        var venue = createVenue();
        var category = createCategory(1);
        var event = createEvent(venue.id(), category.id());

        ResponseEntity<TicketTypeResponse> response = restTemplate.getForEntity(
                "/api/v1/events/{eventId}/ticket-types/{id}",
                TicketTypeResponse.class, event.id(), UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
