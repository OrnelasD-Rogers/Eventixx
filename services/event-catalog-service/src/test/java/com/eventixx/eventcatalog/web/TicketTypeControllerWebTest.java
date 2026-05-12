package com.eventixx.eventcatalog.web;

import com.eventixx.eventcatalog.controllers.TicketTypeController;
import com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest;
import com.eventixx.eventcatalog.dto.tickettype.TicketTypeResponse;
import com.eventixx.eventcatalog.dto.tickettype.UpdateTicketTypeRequest;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.services.tickettype.TicketTypeService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
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

@WebMvcTest(TicketTypeController.class)
class TicketTypeControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TicketTypeService ticketTypeService;

    private final UUID eventId = UUID.randomUUID();
    private final UUID ticketTypeId = UUID.randomUUID();

    @Test
    void shouldCreateTicketType() throws Exception {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest("VIP", BigDecimal.valueOf(200), 50);
        TicketTypeResponse response = new TicketTypeResponse(ticketTypeId, "VIP", BigDecimal.valueOf(200), 50, Instant.now(), Instant.now());
        when(ticketTypeService.create(eq(eventId), any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/{eventId}/ticket-types", eventId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(ticketTypeId.toString()))
            .andExpect(jsonPath("$.name").value("VIP"));
    }

    @Test
    void shouldReturn400_whenCreateRequestInvalid() throws Exception {
        CreateTicketTypeRequest request = new CreateTicketTypeRequest("", BigDecimal.valueOf(-1), 0);

        mockMvc.perform(post("/api/v1/events/{eventId}/ticket-types", eventId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetTicketTypeById() throws Exception {
        TicketTypeResponse response = new TicketTypeResponse(ticketTypeId, "GA", BigDecimal.valueOf(50), 100, Instant.now(), Instant.now());
        when(ticketTypeService.findById(ticketTypeId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/{eventId}/ticket-types/{id}", eventId, ticketTypeId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(ticketTypeId.toString()));
    }

    @Test
    void shouldReturn404_whenTicketTypeNotFound() throws Exception {
        when(ticketTypeService.findById(ticketTypeId))
            .thenThrow(new ResourceNotFoundException("Ticket type not found"));

        mockMvc.perform(get("/api/v1/events/{eventId}/ticket-types/{id}", eventId, ticketTypeId))
            .andExpect(status().isNotFound());
    }

    @Test
    void shouldListTicketTypesForEvent() throws Exception {
        List<TicketTypeResponse> list = List.of(
            new TicketTypeResponse(ticketTypeId, "GA", BigDecimal.valueOf(50), 100, Instant.now(), Instant.now())
        );
        when(ticketTypeService.findAllByEventId(eventId)).thenReturn(list);

        mockMvc.perform(get("/api/v1/events/{eventId}/ticket-types", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].id").value(ticketTypeId.toString()));
    }

    @Test
    void shouldUpdateTicketType() throws Exception {
        UpdateTicketTypeRequest request = new UpdateTicketTypeRequest("Updated VIP", null, null);
        TicketTypeResponse response = new TicketTypeResponse(ticketTypeId, "Updated VIP", BigDecimal.valueOf(200), 50, Instant.now(), Instant.now());
        when(ticketTypeService.update(eq(ticketTypeId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/events/{eventId}/ticket-types/{id}", eventId, ticketTypeId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.name").value("Updated VIP"));
    }

    @Test
    void shouldDeleteTicketType() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{eventId}/ticket-types/{id}", eventId, ticketTypeId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isNoContent());
    }
}
