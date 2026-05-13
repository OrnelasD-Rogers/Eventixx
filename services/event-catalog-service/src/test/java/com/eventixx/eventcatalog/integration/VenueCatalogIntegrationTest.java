package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueCatalogIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void shouldCreateVenue() {
        var request = new CreateVenueRequest("Arena", "123 St", "SP", "BR", 5000);
        ResponseEntity<VenueResponse> response = restTemplate.exchange(
                "/api/v1/venues", HttpMethod.POST,
                new HttpEntity<>(request, authEntity().getHeaders()),
                VenueResponse.class);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().name()).isEqualTo("Arena");
    }

    @Test
    void shouldGetVenueById() {
        var venue = createVenue();

        ResponseEntity<VenueResponse> response = restTemplate.getForEntity(
                "/api/v1/venues/{id}", VenueResponse.class, venue.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().name()).isEqualTo(venue.name());
    }

    @Test
    void shouldListVenues() {
        createVenue();
        createVenue();

        var response = restTemplate.exchange(
                "/api/v1/venues", HttpMethod.GET, null,
                new ParameterizedTypeReference<RestPage<VenueSummaryResponse>>() { });

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody().getContent())
                .hasSize(2)
                .allSatisfy(v -> assertThat(v.name()).isEqualTo("Test Venue"));
    }

    @Test
    void shouldUpdateVenue() {
        var venue = createVenue();
        var update = new UpdateVenueRequest("Updated Arena", "456 New St", "New City", "New Country", 200);

        ResponseEntity<VenueResponse> response = restTemplate.exchange(
                "/api/v1/venues/{id}", HttpMethod.PUT,
                new HttpEntity<>(update, authEntity().getHeaders()),
                VenueResponse.class, venue.id());

        assertThat(response.getBody().name()).isEqualTo("Updated Arena");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();

        restTemplate.exchange("/api/v1/venues/{id}", HttpMethod.DELETE,
                authEntity(), Void.class, venue.id());

        ResponseEntity<VenueResponse> response = restTemplate.getForEntity(
                "/api/v1/venues/{id}", VenueResponse.class, venue.id());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void shouldReturn404WhenNotFound() {
        ResponseEntity<VenueResponse> response = restTemplate.getForEntity(
                "/api/v1/venues/{id}", VenueResponse.class, UUID.randomUUID());

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }
}
