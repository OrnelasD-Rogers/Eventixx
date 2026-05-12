package com.eventixx.searchservice.unit;

import com.eventixx.searchservice.dto.SearchCriteria;
import com.eventixx.searchservice.dto.SearchResult;
import com.eventixx.searchservice.exceptions.SearchUnavailableException;
import com.eventixx.searchservice.repositories.EventDocument;
import com.eventixx.searchservice.services.SearchService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock
    private ElasticsearchOperations operations;

    @InjectMocks
    private SearchService searchService;

    @Test
    void shouldReturnEmptyResultWhenNoEvents() {
        SearchHits<EventDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenReturn(searchHits);

        var criteria = new SearchCriteria("test", null, null, null, null, null, null, null, null, 20);
        SearchResult result = searchService.search(criteria);

        assertThat(result.events()).isEmpty();
        assertThat(result.total()).isZero();
        assertThat(result.nextCursor()).isNull();
    }

    @Test
    void shouldThrowSearchUnavailableOnElasticsearchError() {
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenThrow(new RuntimeException("Connection refused"));

        var criteria = new SearchCriteria("test", null, null, null, null, null, null, null, null, 20);

        assertThatThrownBy(() -> searchService.search(criteria))
                .isInstanceOf(SearchUnavailableException.class);
    }

    @Test
    void shouldReturnEventsWhenFound() {
        var doc = EventDocument.builder()
                .eventId(UUID.randomUUID().toString())
                .title("Test Event")
                .description("Description")
                .categoryName("Music")
                .venueName("Venue")
                .city("Sao Paulo")
                .country("Brazil")
                .startTime(Instant.now())
                .endTime(Instant.now().plusSeconds(3600))
                .minPrice(BigDecimal.valueOf(50))
                .maxPrice(BigDecimal.valueOf(100))
                .ticketTypes(List.of())
                .build();

        SearchHit<EventDocument> hit = mock(SearchHit.class);
        when(hit.getContent()).thenReturn(doc);
        when(hit.getSortValues()).thenReturn(List.of("value1"));

        SearchHits<EventDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of(hit));
        when(searchHits.getTotalHits()).thenReturn(1L);
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenReturn(searchHits);

        var criteria = new SearchCriteria("test", null, null, null, null, null, null, null, null, 20);
        SearchResult result = searchService.search(criteria);

        assertThat(result.events()).hasSize(1);
        assertThat(result.events().get(0).title()).isEqualTo("Test Event");
        assertThat(result.events().get(0).categoryName()).isEqualTo("Music");
        assertThat(result.total()).isEqualTo(1L);
    }

    @Test
    void shouldFilterByCity() {
        SearchHits<EventDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(searchHits.getTotalHits()).thenReturn(0L);
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenReturn(searchHits);

        var criteria = new SearchCriteria(null, "Sao Paulo", null, null, null, null, null, null, null, 20);
        SearchResult result = searchService.search(criteria);

        assertThat(result.events()).isEmpty();
    }

    @Test
    void shouldFilterByDateRange() {
        SearchHits<EventDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenReturn(searchHits);

        var criteria = new SearchCriteria(null, null, null, LocalDate.now(), LocalDate.now().plusDays(30), null, null, null, null, 20);
        SearchResult result = searchService.search(criteria);

        assertThat(result.events()).isEmpty();
    }

    @Test
    void shouldFilterByPriceRange() {
        SearchHits<EventDocument> searchHits = mock(SearchHits.class);
        when(searchHits.getSearchHits()).thenReturn(List.of());
        when(operations.search(any(NativeQuery.class), eq(EventDocument.class)))
                .thenReturn(searchHits);

        var criteria = new SearchCriteria(null, null, null, null, null, BigDecimal.TEN, BigDecimal.valueOf(100), null, null, 20);
        SearchResult result = searchService.search(criteria);

        assertThat(result.events()).isEmpty();
    }
}
