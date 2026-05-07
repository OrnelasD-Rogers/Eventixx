package com.eventixx.eventcatalog.web;

import com.eventixx.eventcatalog.controllers.VenueController;
import com.eventixx.eventcatalog.dto.venue.CreateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.UpdateVenueRequest;
import com.eventixx.eventcatalog.dto.venue.VenueResponse;
import com.eventixx.eventcatalog.dto.venue.VenueSummaryResponse;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.services.venue.VenueService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(VenueController.class)
class VenueControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private VenueService venueService;

    private final UUID venueId = UUID.randomUUID();

    @Test
    void shouldCreateVenue() throws Exception {
        CreateVenueRequest request = new CreateVenueRequest("Arena", "123 St", "SP", "BR", 5000);
        VenueResponse response = new VenueResponse(venueId, "Arena", "123 St", "SP", "BR", 5000, Instant.now(), Instant.now());
        when(venueService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/venues")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(venueId.toString()))
            .andExpect(jsonPath("$.name").value("Arena"));
    }

    @Test
    void shouldReturn400_whenCreateRequestInvalid() throws Exception {
        CreateVenueRequest request = new CreateVenueRequest("", "", "", "", 0);

        mockMvc.perform(post("/api/v1/venues")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetVenueById() throws Exception {
        VenueResponse response = new VenueResponse(venueId, "Arena", "123 St", "SP", "BR", 5000, Instant.now(), Instant.now());
        when(venueService.findById(venueId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/venues/{id}", venueId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(venueId.toString()));
    }

    @Test
    void shouldReturn404_whenVenueNotFound() throws Exception {
        when(venueService.findById(venueId))
            .thenThrow(new BusinessException("Venue not found", HttpStatus.NOT_FOUND));

        mockMvc.perform(get("/api/v1/venues/{id}", venueId))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldListVenues() throws Exception {
        Page<VenueSummaryResponse> page = new PageImpl<>(List.of(
            new VenueSummaryResponse(venueId, "Arena", "SP", "BR")
        ));
        when(venueService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/venues"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(venueId.toString()));
    }

    @Test
    void shouldUpdateVenue() throws Exception {
        UpdateVenueRequest request = new UpdateVenueRequest("Updated Arena", null, null, null, null);
        VenueResponse response = new VenueResponse(venueId, "Updated Arena", "123 St", "SP", "BR", 5000, Instant.now(), Instant.now());
        when(venueService.update(eq(venueId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/venues/{id}", venueId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated Arena"));
    }

    @Test
    void shouldDeleteVenue() throws Exception {
        mockMvc.perform(delete("/api/v1/venues/{id}", venueId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isNoContent());
    }
}
