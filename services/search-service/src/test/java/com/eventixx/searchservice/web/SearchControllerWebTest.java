package com.eventixx.searchservice.web;

import com.eventixx.searchservice.controllers.SearchController;
import com.eventixx.searchservice.dto.EventSearchDto;
import com.eventixx.searchservice.dto.Facets;
import com.eventixx.searchservice.dto.SearchResult;
import com.eventixx.searchservice.exceptions.SearchUnavailableException;
import com.eventixx.searchservice.services.SearchService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(SearchController.class)
class SearchControllerWebTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SearchService searchService;

    @Test
    void shouldReturnEventsWhenQueryProvided() throws Exception {
        var event = new EventSearchDto("event-id", "Rock Concert", "Amazing concert",
                "Music", "Arena", "Sao Paulo", "Brazil",
                Instant.now(), Instant.now().plusSeconds(7200),
                BigDecimal.valueOf(50), BigDecimal.valueOf(150), List.of());

        var result = new SearchResult(List.of(event), new Facets(Map.of("Music", 1L), Map.of("Sao Paulo", 1L)),
                "cursor123", 1L);

        when(searchService.search(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/search/events")
                        .param("q", "rock")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].title").value("Rock Concert"))
                .andExpect(jsonPath("$.events[0].city").value("Sao Paulo"))
                .andExpect(jsonPath("$.total").value(1));
    }

    @Test
    void shouldReturnEmptyResultWhenNoMatches() throws Exception {
        var result = new SearchResult(List.of(), new Facets(Map.of(), Map.of()), null, 0L);

        when(searchService.search(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/search/events")
                        .param("q", "nonexistent")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events").isEmpty())
                .andExpect(jsonPath("$.total").value(0));
    }

    @Test
    void shouldReturn503WhenSearchServiceUnavailable() throws Exception {
        when(searchService.search(any()))
                .thenThrow(new SearchUnavailableException("Search unavailable", new RuntimeException()));

        mockMvc.perform(get("/api/v1/search/events")
                        .param("q", "test")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isServiceUnavailable());
    }

    @Test
    void shouldApplyFilters() throws Exception {
        var result = new SearchResult(List.of(), new Facets(Map.of(), Map.of()), null, 0L);

        when(searchService.search(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/search/events")
                        .param("city", "Sao Paulo")
                        .param("category", "Music")
                        .param("minPrice", "10")
                        .param("maxPrice", "200")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isOk());
    }

    @Test
    void shouldReturnEventsWithFiltersAndReturnResult() throws Exception {
        var event = new EventSearchDto("event-2", "Samba Night", null,
                "Music", "Club", "Rio de Janeiro", "Brazil",
                Instant.now(), Instant.now().plusSeconds(3600),
                BigDecimal.valueOf(20), BigDecimal.valueOf(80), List.of());

        var result = new SearchResult(List.of(event), new Facets(Map.of("Music", 1L), Map.of("Rio de Janeiro", 1L)),
                null, 1L);

        when(searchService.search(any())).thenReturn(result);

        mockMvc.perform(get("/api/v1/search/events")
                        .param("city", "Rio de Janeiro")
                        .param("category", "Music")
                        .header("X-User-Id", "user-123"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.events[0].title").value("Samba Night"))
                .andExpect(jsonPath("$.events[0].city").value("Rio de Janeiro"))
                .andExpect(jsonPath("$.total").value(1));
    }

}
