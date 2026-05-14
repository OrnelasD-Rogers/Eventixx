package com.eventixx.eventcatalog.integration;

import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VenueCatalogIntegrationTest extends CatalogIntegrationTestBase {

    @Test
    void shouldCreateVenue() {
        var request = new CreateVenueRequest("Arena", "123 St", "SP", "BR", 5000);
        var created = restClient.post()
                .uri("/api/v1/venues")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(VenueResponse.class)
                .returnResult().getResponseBody();
        assertThat(created).isNotNull();
        assertThat(created.name()).isEqualTo("Arena");
    }

    @Test
    void shouldGetVenueById() {
        var venue = createVenue();

        var response = restClient.get()
                .uri("/api/v1/venues/{id}", venue.id())
                .exchange()
                .expectStatus().isOk()
                .expectBody(VenueResponse.class)
                .returnResult().getResponseBody();
        assertThat(response.name()).isEqualTo(venue.name());
    }

    @Test
    void shouldListVenues() {
        createVenue();
        createVenue();

        var page = restClient.get()
                .uri("/api/v1/venues")
                .exchange()
                .expectBody(new ParameterizedTypeReference<RestPage<VenueSummaryResponse>>() { })
                .returnResult().getResponseBody();
        assertThat(page.getContent())
                .hasSize(2)
                .allSatisfy(v -> assertThat(v.name()).isEqualTo("Test Venue"));
    }

    @Test
    void shouldUpdateVenue() {
        var venue = createVenue();
        var update = new UpdateVenueRequest("Updated Arena", "456 New St", "New City", "New Country", 200);

        var updated = restClient.put()
                .uri("/api/v1/venues/{id}", venue.id())
                .headers(requestHeaders())
                .body(update)
                .exchange()
                .expectBody(VenueResponse.class)
                .returnResult().getResponseBody();
        assertThat(updated.name()).isEqualTo("Updated Arena");
    }

    @Test
    void shouldReturn404AfterSoftDelete() {
        var venue = createVenue();

        restClient.delete()
                .uri("/api/v1/venues/{id}", venue.id())
                .headers(requestHeaders())
                .exchange();

        restClient.get()
                .uri("/api/v1/venues/{id}", venue.id())
                .exchange()
                .expectStatus().isNotFound();
    }

    @Test
    void shouldReturn404WhenNotFound() {
        restClient.get()
                .uri("/api/v1/venues/{id}", UUID.randomUUID())
                .exchange()
                .expectStatus().isNotFound();
    }
}
