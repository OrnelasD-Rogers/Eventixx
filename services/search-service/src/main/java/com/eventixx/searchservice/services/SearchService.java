package com.eventixx.searchservice.services;

import co.elastic.clients.elasticsearch._types.aggregations.Aggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsAggregate;
import co.elastic.clients.elasticsearch._types.aggregations.StringTermsBucket;
import co.elastic.clients.elasticsearch._types.aggregations.TermsAggregation;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import com.eventixx.searchservice.dto.EventSearchDto;
import com.eventixx.searchservice.dto.Facets;
import com.eventixx.searchservice.dto.SearchCriteria;
import com.eventixx.searchservice.dto.SearchResult;
import com.eventixx.searchservice.exceptions.SearchUnavailableException;
import com.eventixx.searchservice.repositories.EventDocument;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregation;
import org.springframework.data.elasticsearch.client.elc.ElasticsearchAggregations;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.client.elc.NativeQueryBuilder;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHit;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private final ElasticsearchOperations operations;

    /** Executes a search with filters, facets, and cursor pagination. */
    @SuppressWarnings("PMD.AvoidCatchingGenericException")
    public SearchResult search(SearchCriteria criteria) {
        try {
            NativeQueryBuilder queryBuilder = NativeQuery.builder();

            List<Query> mustQueries = new ArrayList<>();
            List<Query> filterQueries = new ArrayList<>();

            addTextQuery(criteria, mustQueries);
            addCityFilter(criteria, filterQueries);
            addCategoryFilter(criteria, filterQueries);
            addDateRangeFilter(criteria, filterQueries);
            addPriceRangeFilter(criteria, filterQueries);

            if (!mustQueries.isEmpty() || !filterQueries.isEmpty()) {
                queryBuilder.withQuery(q -> q.bool(b -> {
                    mustQueries.forEach(b::must);
                    filterQueries.forEach(b::filter);
                    return b;
                }));
            }

            addSorting(queryBuilder, criteria.sort());
            addCursor(queryBuilder, criteria);
            queryBuilder.withMaxResults(criteria.size());
            addAggregations(queryBuilder);

            NativeQuery query = queryBuilder.build();
            SearchHits<EventDocument> searchHits = operations.search(query, EventDocument.class);

            List<EventSearchDto> events = searchHits.getSearchHits().stream()
                    .map(this::toEventDto)
                    .toList();

            Facets facets = extractFacets(searchHits);
            String nextCursor = resolveCursor(searchHits);

            return new SearchResult(events, facets, nextCursor, searchHits.getTotalHits());
        } catch (RuntimeException e) {
            log.error("Search failed: {}", e.getMessage(), e);
            throw new SearchUnavailableException("Search temporarily unavailable", e);
        }
    }

    private void addTextQuery(SearchCriteria criteria, List<Query> mustQueries) {
        String q = criteria.q();
        if (q != null && !q.isBlank()) {
            mustQueries.add(Query.of(query -> query
                    .multiMatch(m -> m
                            .fields("title", "description")
                            .query(q))));
        }
    }

    private void addCityFilter(SearchCriteria criteria, List<Query> filterQueries) {
        String city = criteria.city();
        if (city != null && !city.isBlank()) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t.field("city").value(city))));
        }
    }

    private void addCategoryFilter(SearchCriteria criteria, List<Query> filterQueries) {
        String category = criteria.category();
        if (category != null && !category.isBlank()) {
            filterQueries.add(Query.of(q -> q
                    .term(t -> t.field("categoryName").value(category))));
        }
    }

    private void addDateRangeFilter(SearchCriteria criteria, List<Query> filterQueries) {
        if (criteria.dateFrom() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r.date(d -> d
                            .field("startTime")
                            .gte(criteria.dateFrom().toString())))));
        }

        if (criteria.dateTo() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r.date(d -> d
                            .field("endTime")
                            .lte(criteria.dateTo().toString())))));
        }
    }

    private void addPriceRangeFilter(SearchCriteria criteria, List<Query> filterQueries) {
        if (criteria.minPrice() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r.number(d -> d
                            .field("minPrice")
                            .gte(criteria.minPrice().doubleValue())))));
        }

        if (criteria.maxPrice() != null) {
            filterQueries.add(Query.of(q -> q
                    .range(r -> r.number(d -> d
                            .field("maxPrice")
                            .lte(criteria.maxPrice().doubleValue())))));
        }
    }

    private void addCursor(NativeQueryBuilder queryBuilder, SearchCriteria criteria) {
        String cursor = criteria.cursor();
        if (cursor != null && !cursor.isBlank()) {
            queryBuilder.withSearchAfter(parseCursor(cursor));
        }
    }

    private String resolveCursor(SearchHits<EventDocument> searchHits) {
        if (!searchHits.getSearchHits().isEmpty()) {
            SearchHit<EventDocument> lastHit = searchHits.getSearchHits().getLast();
            List<Object> sortValues = lastHit.getSortValues();
            if (!sortValues.isEmpty()) {
                return buildCursor(sortValues);
            }
        }
        return null;
    }

    private void addAggregations(NativeQueryBuilder queryBuilder) {
        var categoriesAgg = new TermsAggregation.Builder()
                .field("categoryName")
                .size(50)
                .build();
        queryBuilder.withAggregation("categories",
                new co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder()
                        .terms(categoriesAgg)
                        .build());

        var citiesAgg = new TermsAggregation.Builder()
                .field("city")
                .size(50)
                .build();
        queryBuilder.withAggregation("cities",
                new co.elastic.clients.elasticsearch._types.aggregations.Aggregation.Builder()
                        .terms(citiesAgg)
                        .build());
    }

    private void addSorting(NativeQueryBuilder queryBuilder, String sort) {
        if (sort == null || sort.isBlank() || "relevance".equals(sort)) {
            return;
        }
        switch (sort) {
            case "date_asc" -> queryBuilder.withSort(Sort.by(Sort.Direction.ASC, "startTime"));
            case "date_desc" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "startTime"));
            case "price_asc" -> queryBuilder.withSort(Sort.by(Sort.Direction.ASC, "minPrice"));
            case "price_desc" -> queryBuilder.withSort(Sort.by(Sort.Direction.DESC, "minPrice"));
            default -> {
            }
        }
    }

    private EventSearchDto toEventDto(SearchHit<EventDocument> hit) {
        EventDocument doc = hit.getContent();
        return new EventSearchDto(
                doc.getEventId(),
                doc.getTitle(),
                doc.getDescription(),
                doc.getCategoryName(),
                doc.getVenueName(),
                doc.getCity(),
                doc.getCountry(),
                doc.getStartTime(),
                doc.getEndTime(),
                doc.getMinPrice(),
                doc.getMaxPrice(),
                doc.getTicketTypes().stream()
                        .map(tt -> new EventSearchDto.TicketTypeDto(
                                tt.getName(),
                                tt.getPrice(),
                                tt.getQuantityAvailable()))
                        .toList()
        );
    }

    private Facets extractFacets(SearchHits<EventDocument> searchHits) {
        Map<String, Long> categories = new ConcurrentHashMap<>();
        Map<String, Long> cities = new ConcurrentHashMap<>();

        if (searchHits.getAggregations() instanceof ElasticsearchAggregations esAggregations) {
            extractStringTerms(esAggregations.get("categories"), categories);
            extractStringTerms(esAggregations.get("cities"), cities);
        }

        return new Facets(categories, cities);
    }

    private void extractStringTerms(ElasticsearchAggregation esAgg, Map<String, Long> target) {
        if (esAgg != null) {
            Aggregate aggregate = esAgg.aggregation().getAggregate();
            if (aggregate.isSterms()) {
                StringTermsAggregate sterms = aggregate.sterms();
                for (StringTermsBucket bucket : sterms.buckets().array()) {
                    target.put(bucket.key().stringValue(), bucket.docCount());
                }
            }
        }
    }

    private List<Object> parseCursor(String cursor) {
        return List.of(cursor);
    }

    private String buildCursor(List<Object> sortValues) {
        return sortValues.stream()
                .map(Object::toString)
                .collect(Collectors.joining("|"));
    }
}
