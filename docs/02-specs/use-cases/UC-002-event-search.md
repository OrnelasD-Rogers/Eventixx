# UC-002 — Event Search & Discovery

**Actor:** Public User (no auth required)  
**Preconditions:** Event Catalog Service has published events; Search Service has indexed them in Elasticsearch.

## Main Flow

1. User sends a search query with optional filters (keywords, city, category, date range, price range).
2. API Gateway routes the request to Search Service.
3. Search Service queries the Elasticsearch read model.
4. Results are returned with pagination (cursor-based), facets (category counts, city counts), and sorting options (relevance, date, price).
5. User clicks an event to view details; API Gateway routes to Event Catalog Service for full event data.

## Exceptions

| Condition | HTTP Status | Message |
|-----------|-------------|---------|
| No results found | 200 | Empty list (not an error) |
| Invalid date format | 400 | Invalid date format. Use ISO-8601 (yyyy-MM-dd) |
| Invalid cursor | 400 | Invalid pagination cursor |
| Elasticsearch unavailable | 503 | Search temporarily unavailable. Try again later. |

## Services Involved

| Service | Responsibility |
|---------|---------------|
| `api-gateway` | Routes `/api/v1/search/**` to Search Service; routes `/api/v1/events/**` to Event Catalog Service |
| `search-service` | Maintains Elasticsearch read model; executes queries with filters, facets, pagination |
| `event-catalog-service` | Source of truth for full event details (fallback if needed) |

## Architecture Decisions

### Referenced ADRs
- `ADR-005` — CQRS: Search Service is a pure read model. It does NOT write to PostgreSQL. All data comes from Kafka events emitted by Event Catalog Service.
- `ADR-002` — Communication: Search queries are synchronous REST. Index updates are asynchronous Kafka events.
- `ADR-003` — Data Strategy: Elasticsearch is the dedicated store for the search read model.

### Inline Decisions (no ADR needed)
- **Cursor-based pagination:** Prevents deep-pagination performance issues and result shifting on concurrent writes. Cursor is a Base64-encoded `search_after` value.
- **Facets are pre-computed at index time:** Elasticsearch aggregations provide fast facet counts without additional queries.
- **Search is public:** No JWT required for search endpoints.

---

## Implementation Plan

### Task 1: Scaffold / Base Structure
- **Status:** ☐ Pending
- **Complexity:** Low
- **Artifacts to create/modify:**
  - `services/search-service/` — Already scaffolded in UC-001 Task 3. If not done yet, create it now.
  - Elasticsearch index mapping: `events` index with analyzers for title/description.
- **Prompt for agent:**
  > Ensure the `search-service` module exists with Spring Boot **4.0.6**, Java 21, Spring Data Elasticsearch, Spring Kafka, Eureka Client, Lombok. Define the `EventDocument` entity with `@Document(indexName = "events")`. Configure a custom Elasticsearch index mapping with: `title` (text, keyword subfield), `description` (text), `category` (keyword), `venueName` (text, keyword), `city` (keyword), `country` (keyword), `startDate` (date), `endDate` (date), `minPrice` (scaled_float), `maxPrice` (scaled_float), `ticketTypes` (nested). Use a standard analyzer for text fields.
- **Verification:** Service starts; index mapping is created in Elasticsearch on startup.

### Task 2: Core Implementation — Search API
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - `SearchController` with `GET /api/v1/search/events`
  - `SearchService` with query builder
  - `SearchCriteria` DTO
  - `SearchResult` DTO with facets and pagination
- **Prompt for agent:**
  > Implement the search endpoint `GET /api/v1/search/events` in Search Service. Query parameters: `q` (full-text on title/description), `city`, `category`, `dateFrom`, `dateTo`, `minPrice`, `maxPrice`, `sort` (relevance | date_asc | date_desc | price_asc | price_desc), `cursor`, `size` (default 20, max 100). Build a `BoolQuery` with `must` for text query (multi-match), `filter` for exact matches (city, category, date range, price range). Use `search_after` for cursor-based pagination. Return a `SearchResult` containing: `events` (list), `facets` (category counts, city counts), `nextCursor` (nullable), `total`. If Elasticsearch is down, return 503 with a clear message. Add OpenAPI annotations.
- **Verification:** Search returns correct results for various filter combinations; pagination works via cursor; facets return accurate counts.

### Task 3: Integration — Event Index Consumer
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - `EventPublishedConsumer` (Kafka listener)
  - `EventDocumentMapper`
  - `EventDocumentRepository`
- **Prompt for agent:**
  > Implement the Kafka consumer in Search Service that listens to `event.published` topic. Deserialize the event payload and save/update the `EventDocument` in Elasticsearch. If the event already exists (by `eventId`), update it. Handle schema versioning gracefully (ignore unknown fields). Ensure the consumer is idempotent (upsert by `eventId`). Add a `@RetryableTopic` or dead-letter topic configuration for failed messages.
- **Verification:** Publish 3 events from Event Catalog; all appear in search results within 5 seconds; re-publishing the same event updates the index without duplicates.

### Task 4: Conformance Tests
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - `SearchServiceIntegrationTest` with Testcontainers (Elasticsearch + Kafka)
  - `SearchControllerWebTest` with `@WebMvcTest`
- **Prompt for agent:**
  > Write integration tests for Search Service. Use Testcontainers for Elasticsearch and Kafka. If any test requires PostgreSQL (e.g., for saga state or local projections), use a dedicated isolated PostgreSQL container. Test scenarios: (1) index an event document directly and verify search by keyword; (2) index documents with different categories/cities and verify facet counts; (3) test cursor pagination (first page + next page); (4) test filter by date range and price range; (5) consume a Kafka `event.published` message and verify it becomes searchable. Use `@DynamicPropertySource` for container ports.
- **Verification:** All tests pass with `mvn verify`.

### Task 5: Documentation & Observability
- **Status:** ☐ Pending
- **Complexity:** Low
- **Artifacts to create/modify:**
  - Micrometer timer: `search.query.duration`
  - Counter: `search.query.total`
  - Structured logging for every search query (logged query params + result count)
- **Prompt for agent:**
  > Add Micrometer `@Timed` annotation on the search service method. Add a counter `search.query.total` tagged by `has_text_query`, `has_filter`. Log every search query as structured JSON with fields: `query`, `filters`, `resultCount`, `durationMs`, `traceId`.
- **Verification:** Metrics endpoint shows `search_query_duration_seconds` histogram and `search_query_total` counter.

---

## Acceptance Criteria

- [ ] Search by keyword returns relevant events ordered by relevance
- [ ] Filters (city, category, date range, price range) work independently and combined
- [ ] Cursor-based pagination returns correct pages without duplicates or skips
- [ ] Facets show accurate counts for categories and cities in the current result set
- [ ] Sorting works for relevance, date, and price
- [ ] Elasticsearch downtime returns 503 gracefully
- [ ] Kafka consumer indexes events within 5 seconds of publication
- [ ] Integration tests with Testcontainers pass
- [ ] Search metrics (duration, count) are exposed via Actuator

---

## Execution Log

| Task | Status | Date | Notes |
|------|--------|------|-------|
| T1   |        |      |       |
| T2   |        |      |       |
| T3   |        |      |       |
| T4   |        |      |       |
| T5   |        |      |       |

---

## Lessons Learned

### What worked well
{...}

### What didn't work / Surprises
{...}

### Deviations from original plan
{What changed and why?}

### What almost went wrong
{Problems avoided by little — prime material for LinkedIn storytelling}

### What would I do differently next time
{Retrospective insight}

### Key metrics (before / after)
- Baseline: {...}
- Result: {...}
- Tool used: {...}
