package com.eventixx.eventcatalog.web;

import com.eventixx.eventcatalog.controllers.EventController;
import com.eventixx.eventcatalog.dto.event.CreateEventRequest;
import com.eventixx.eventcatalog.dto.event.EventResponse;
import com.eventixx.eventcatalog.dto.event.EventSummaryResponse;
import com.eventixx.eventcatalog.dto.event.UpdateEventRequest;
import com.eventixx.eventcatalog.exceptions.BusinessException;
import com.eventixx.eventcatalog.exceptions.ConflictException;
import com.eventixx.eventcatalog.exceptions.ResourceNotFoundException;
import com.eventixx.eventcatalog.services.event.EventService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static com.eventixx.eventcatalog.exceptions.ProblemType.TITLE_NOT_FOUND;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(EventController.class)
class EventControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private EventService eventService;

    private final UUID eventId = UUID.randomUUID();

    @Test
    void shouldCreateEvent() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
            "Concert", "A music concert", UUID.randomUUID(), UUID.randomUUID(),
            Instant.now().plusSeconds(3600), Instant.now().plusSeconds(7200),
            List.of(new com.eventixx.eventcatalog.dto.tickettype.CreateTicketTypeRequest("GA", BigDecimal.valueOf(50), 100))
        );
        EventResponse response = new EventResponse(
            eventId, "Concert", "A music concert", "DRAFT",
            Instant.now(), Instant.now(), UUID.randomUUID(), "Venue",
            UUID.randomUUID(), "Music", null, Instant.now(), Instant.now(), List.of()
        );
        when(eventService.create(any())).thenReturn(response);

        mockMvc.perform(post("/api/v1/events")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(eventId.toString()))
            .andExpect(jsonPath("$.title").value("Concert"));
    }

    @Test
    void shouldReturn400_whenCreateRequestInvalid() throws Exception {
        CreateEventRequest request = new CreateEventRequest(
            "", "Desc", null, null,
            null, null, null
        );

        mockMvc.perform(post("/api/v1/events")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldGetEventById() throws Exception {
        EventResponse response = new EventResponse(
            eventId, "Concert", "Desc", "DRAFT",
            Instant.now(), Instant.now(), UUID.randomUUID(), "Venue",
            UUID.randomUUID(), "Music", null, Instant.now(), Instant.now(), List.of()
        );
        when(eventService.findById(eventId)).thenReturn(response);

        mockMvc.perform(get("/api/v1/events/{id}", eventId))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.id").value(eventId.toString()));
    }

    @Test
    void shouldReturn404_whenEventNotFound() throws Exception {
        when(eventService.findById(eventId))
            .thenThrow(new ResourceNotFoundException("Event not found"));

        mockMvc.perform(get("/api/v1/events/{id}", eventId))
            .andExpect(status().isNotFound())
            .andExpect(jsonPath("$.title").value(TITLE_NOT_FOUND));
    }

    @Test
    void shouldListEvents() throws Exception {
        Page<EventSummaryResponse> page = new PageImpl<>(List.of(
            new EventSummaryResponse(eventId, "Concert", "DRAFT", Instant.now(), Instant.now(), "Venue", "Music", null)
        ));
        when(eventService.findAll(any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/events"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].id").value(eventId.toString()));
    }

    @Test
    void shouldListEventsByStatus() throws Exception {
        Page<EventSummaryResponse> page = new PageImpl<>(List.of(
            new EventSummaryResponse(eventId, "Concert", "DRAFT", Instant.now(), Instant.now(), "Venue", "Music", null)
        ));
        when(eventService.findAllByStatus(eq("DRAFT"), any())).thenReturn(page);

        mockMvc.perform(get("/api/v1/events").param("status", "DRAFT"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.content[0].status").value("DRAFT"));
    }

    @Test
    void shouldUpdateEvent() throws Exception {
        UpdateEventRequest request = new UpdateEventRequest("Updated", null, null, null, null, null, null);
        EventResponse response = new EventResponse(
            eventId, "Updated", "Desc", "DRAFT",
            Instant.now(), Instant.now(), UUID.randomUUID(), "Venue",
            UUID.randomUUID(), "Music", null, Instant.now(), Instant.now(), List.of()
        );
        when(eventService.update(eq(eventId), any())).thenReturn(response);

        mockMvc.perform(put("/api/v1/events/{id}", eventId)
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.title").value("Updated"));
    }

    @Test
    void shouldPublishEvent() throws Exception {
        EventResponse response = new EventResponse(
            eventId, "Concert", "Desc", "PUBLISHED",
            Instant.now(), Instant.now(), UUID.randomUUID(), "Venue",
            UUID.randomUUID(), "Music", Instant.now(), Instant.now(), Instant.now(), List.of()
        );
        when(eventService.publish(eventId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("PUBLISHED"));
    }

    @Test
    void shouldReturn409_whenAlreadyPublished() throws Exception {
        when(eventService.publish(eventId))
            .thenThrow(new ConflictException("Event is already published"));

        mockMvc.perform(post("/api/v1/events/{id}/publish", eventId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isConflict());
    }

    @Test
    void shouldCancelEvent() throws Exception {
        EventResponse response = new EventResponse(
            eventId, "Concert", "Desc", "CANCELLED",
            Instant.now(), Instant.now(), UUID.randomUUID(), "Venue",
            UUID.randomUUID(), "Music", Instant.now(), Instant.now(), Instant.now(), List.of()
        );
        when(eventService.cancel(eventId)).thenReturn(response);

        mockMvc.perform(post("/api/v1/events/{id}/cancel", eventId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.status").value("CANCELLED"));
    }

    @Test
    void shouldReturn400_whenCancelNotPublished() throws Exception {
        when(eventService.cancel(eventId))
            .thenThrow(new BusinessException("Only published events can be cancelled"));

        mockMvc.perform(post("/api/v1/events/{id}/cancel", eventId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isBadRequest());
    }

    @Test
    void shouldDeleteEvent() throws Exception {
        mockMvc.perform(delete("/api/v1/events/{id}", eventId)
                .header("X-User-Id", "user-123"))
            .andExpect(status().isNoContent());
    }
}
