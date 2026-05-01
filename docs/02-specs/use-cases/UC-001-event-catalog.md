# UC-001 — Event Catalog Management

**Actor:** Event Organizer (admin) / System  
**Preconditions:** User Service exists (for auth context, though catalog read is public)

## Main Flow

1. Organizer creates a venue (name, address, city, country, capacity).
2. Organizer creates an event linked to a venue (title, description, category, start/end datetime).
3. Organizer defines ticket types for the event (name, price, quantity available).
4. Organizer publishes the event (status changes from DRAFT to PUBLISHED).
5. Published event triggers `event.published` domain event to Kafka.
6. Search Service consumes event and indexes it in Elasticsearch.
7. Public users can retrieve event details via REST API.

## Exceptions

| Condition | HTTP Status | Message |
|-----------|-------------|---------|
| Venue not found | 404 | Venue with id {id} not found |
| Event not found | 404 | Event with id {id} not found |
| Invalid date range (end before start) | 400 | Event end time must be after start time |
| Ticket quantity exceeds venue capacity | 400 | Total ticket quantity exceeds venue capacity |
| Event already published | 409 | Event is already published |
| Unauthorized | 401 | Authentication required |

## Services Involved

| Service | Responsibility |
|---------|---------------|
| `event-catalog-service` | CRUD for venues, events, ticket types; publishes domain events |
| `search-service` | Consumes `event.published` and indexes in Elasticsearch |
| `api-gateway` | Routes requests, JWT validation |

## Architecture Decisions

### Referenced ADRs
- `ADR-001` — Service Boundaries: Event Catalog is an independent bounded context owning all event-related data.
- `ADR-002` — Communication: Read operations use REST; state changes (`publish`) emit Kafka events.
- `ADR-003` — Data Strategy: Event Catalog owns dedicated PostgreSQL instance (`event-catalog-db:5433`); Search Service owns Elasticsearch index.
- `ADR-005` — CQRS: Search Service maintains a separate read model updated asynchronously via events.

### Inline Decisions (no ADR needed)
- **Public read access:** `GET /api/v1/events/**` requires no authentication to simulate a real ticketing platform.
- **Write operations require JWT:** Only authenticated organizers can create/modify events.
- **Soft delete on all catalog tables:** All entities (`categories`, `venues`, `events`, `ticket_types`) use a `deleted_at` timestamp instead of hard deletion. This preserves referential integrity (e.g., events referencing venues) and enables audit trails. Queries must explicitly filter `WHERE deleted_at IS NULL`.
- **Event status enum:** `DRAFT` → `PUBLISHED` → (`CANCELLED` | `ENDED`). `ENDED` is set by a future scheduled job when `end_time` passes; `CANCELLED` is manual.

---

## Implementation Plan

### Task 1: Scaffold / Base Structure
- **Status:** ☑ Completed
- **Complexity:** Low
- **Artifacts to create/modify:**
  - `services/event-catalog-service/` — Spring Boot module
  - `services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/...`
  - `services/event-catalog-service/src/main/resources/application.yml`
  - `services/event-catalog-service/pom.xml` (or Gradle)
  - Flyway migration: `db/migration/V1__init.sql`
- **Prompt for agent:**
  > Create the `event-catalog-service` Spring Boot **4.0.6** module with Java 21. Include dependencies: Web, Data JPA, PostgreSQL, Flyway, Kafka (Spring for Apache Kafka), Validation, Lombok, **MapStruct**. Configure `application.yml` to connect to its dedicated PostgreSQL instance at `event-catalog-db:5433` (database: `eventixx`), Kafka bootstrap (`localhost:9092`), and Eureka client. Create Flyway migration `V1__init.sql` with tables: `categories`, `venues`, `events`, `ticket_types`. Schema requirements:
  > - All tables use **soft delete** (`deleted_at TIMESTAMP`) and **auto-updated `updated_at`** via PostgreSQL trigger.
  > - `events.status` is `VARCHAR` with CHECK constraint: `('DRAFT', 'PUBLISHED', 'CANCELLED', 'ENDED')`, DEFAULT 'DRAFT'.
  > - `events` has `published_at` (nullable) set when status transitions to PUBLISHED.
  > - `events` has CHECK `end_time > start_time`.
  > - `ticket_types.price` is `DECIMAL(10,2)`.
  > - All FKs use `ON DELETE RESTRICT` (soft delete prevents orphaning).
  > - Create partial indexes filtering `deleted_at IS NULL` for performance.
  > Configure `maven-compiler-plugin` with `mapstruct-processor` annotation processor (after Lombok). Ensure the service registers with Eureka.
- **Verification:** Service starts without errors; Flyway migration runs successfully; Eureka dashboard shows `EVENT-CATALOG-SERVICE` registered.

### Task 2: Core Implementation
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - **Entities** (`entities/`): `Venue`, `Event`, `TicketType`, `Category`
  - **Repositories** (`repositories/`): Spring Data JPA interfaces + JPA implementations
  - **Services** (`services/`): `VenueService`, `EventService`, `TicketTypeService`, `EventValidator`, Mappers
  - **Controllers** (`controllers/`): `VenueController`, `EventController`, `TicketTypeController`, `GlobalExceptionHandler`
  - **DTOs** (`dto/`): Requests/Responses per domain (MapStruct or manual)
  - **Exceptions** (`exceptions/`): `BusinessException`
  - **Config** (`config/`): `SecurityConfig`, `OpenApiConfig`
  - **Messaging** (`services/messaging/`): `KafkaDomainEventPublisher`, `EventPublished`
- **Prompt for agent:**
  > Implement the full CRUD for Venue, Event, TicketType, and Category in the event-catalog-service. Organize code in **package-by-layer** structure: `controllers/`, `services/`, `repositories/`, `entities/`, `dto/`, `exceptions/`, `config/`. Use **Anemic Model**: entities are pure data (JPA + Lombok getters/setters), all business logic lives in Services. Create a dedicated `EventValidator` component for event-specific validations (publish, update, cancel rules). Services use private `findXxxOrThrow()` helpers for readability. On `Event.publish()`, publish an `EventPublished` domain event to Kafka topic `event.published`. Implement proper exception handling with `@ControllerAdvice`. Use OpenAPI annotations for automatic API docs. Ensure all write endpoints require a valid JWT (mock the auth layer if User Service is not ready yet — accept a dummy `X-User-Id` header for now).
- **Verification:** All CRUD endpoints return correct HTTP statuses; `POST /api/v1/events/{id}/publish` emits a Kafka message; integration tests pass.

### Task 3: Integration (Kafka → Search Service)
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - `services/search-service/` — Spring Boot module (scaffolded)
  - Kafka consumer: `EventPublishedConsumer`
  - Elasticsearch document mapping: `EventDocument`
  - Repository: `EventSearchRepository`
- **Prompt for agent:**
  > Create the `search-service` Spring Boot **4.0.6** module with Java 21. Dependencies: Web, Kafka, Elasticsearch (Spring Data Elasticsearch), Eureka Client, Lombok. Configure connection to Elasticsearch (`localhost:9200`) and Kafka. Implement a Kafka listener on topic `event.published` that deserializes the event and indexes it into Elasticsearch as `EventDocument`. The document should include: eventId, title, description, category, venueName, city, startDate, endDate, minPrice, maxPrice, ticketTypes. Create a REST controller `SearchController` with `GET /api/v1/search/events?q={query}&city={city}&category={category}&dateFrom={date}&dateTo={date}&page={page}&size={size}`. Implement multi-match query on title and description, plus filters.
- **Verification:** Publish an event via Event Catalog API; within 5 seconds, it is searchable via Search Service API.

### Task 4: Conformance Tests
- **Status:** ☐ Pending
- **Complexity:** Medium
- **Artifacts to create/modify:**
  - `EventCatalogServiceApplicationTests`
  - `EventControllerIntegrationTest` (with Testcontainers for PostgreSQL + Kafka)
  - `SearchServiceIntegrationTest` (with Testcontainers for Elasticsearch + Kafka)
- **Prompt for agent:**
  > Write integration tests for event-catalog-service Busing Testcontainers. Spin up a dedicated PostgreSQL container for this service (port 5433 or dynamic) and a Kafka container. Test: create event, publish event, and verify Kafka message is sent. Use `@DynamicPropertySource` to wire Testcontainers ports. Ensure tests are self-contained and can run with `mvn verify`. The test must use its own isolated PostgreSQL instance (do not share with other services).
- **Verification:** `mvn verify` passes; Testcontainers spin up and tear down automatically.

### Task 5: Documentation & Observability
- **Status:** ☐ Pending
- **Complexity:** Low
- **Artifacts to create/modify:**
  - Structured JSON logging (`logback-spring.xml`)
  - Micrometer metrics (`event.created.counter`, `event.published.counter`)
  - OpenTelemetry tracing span on Kafka producer/consumer
- **Prompt for agent:**
  > Configure structured JSON logging for event-catalog-service and search-service. Add Micrometer counters for `event.created` and `event.published`. Ensure trace IDs are propagated in Kafka message headers and logged. Add `/actuator/health` and `/actuator/metrics` endpoints.
- **Verification:** Logs appear as JSON with `traceId`; metrics endpoint exposes custom counters.

---

## Acceptance Criteria

- [ ] Venue CRUD works via REST API
- [ ] Event CRUD works; event can be linked to venue and ticket types
- [ ] Publishing an event emits `event.published` to Kafka
- [ ] Search Service indexes published events within 5 seconds
- [ ] Search API supports full-text query, city filter, category filter, date range
- [ ] All write operations require authentication (JWT or dummy header)
- [ ] Integration tests with Testcontainers pass
- [ ] Structured JSON logs with trace IDs are emitted

---

## Execution Log

| Task | Status | Date | Notes |
|------|--------|------|-------|
| T1   | ☑ Completed | 2026-04-30 | Created Maven module, Dockerfile, application.yml. Configured MapStruct processor with Lombok ordering in root POM. Created `V1__init.sql` with 4 tables (soft delete, triggers, partial indexes, CHECK constraints). Schema documented in `data-model.md`. |
| T2   | ☑ Completed | 2026-04-30 | Refactored to Anemic Model + **package-by-layer** structure (`controllers/`, `services/`, `repositories/`, `entities/`, `dto/`, `exceptions/`, `config/`). Added `EventValidator` component for publish/update/cancel rules. Services use private `findXxxOrThrow()` helpers and `Optional.ifPresent()` for updates. Added `POST /{id}/cancel` endpoint. Kafka event published directly from Service (no AggregateRoot/domainEvents list). Build SUCCESS. |
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
- **Rich Model → Anemic Model:** Original prompt specified DDD-style rich behavior in entities (`Event.publish()`). After review, switched to Anemic Model (entities = pure data, Services = all logic) for simpler readability and to align with common Spring Boot patterns. Extracted validations into dedicated `EventValidator` component.

### What almost went wrong
{Problems avoided by little — prime material for LinkedIn storytelling}

### What would I do differently next time
{Retrospective insight}

### Key metrics (before / after)
- Baseline: {...}
- Result: {...}
- Tool used: {...}
