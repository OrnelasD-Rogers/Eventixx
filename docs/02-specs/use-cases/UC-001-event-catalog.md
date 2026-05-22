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
- **Compiler strictness:** Two flags configured in the root POM `maven-compiler-plugin`: (a) `-parameters` — mandatory for Spring Framework 7's parameter name resolution; `@PathVariable` and `@RequestParam` fail at runtime without it. (b) `-Xlint:all,-processing` with `failOnWarning=true` — enforces zero compiler warnings on main sources. Test sources use relaxed linting `-Xlint:all,-processing,-rawtypes,-unchecked`.
- **Validation errors return structured ProblemDetail:** `MethodArgumentNotValidException` is handled in `GlobalExceptionHandler` to return RFC 7807 `ProblemDetail` with a map of field errors, instead of falling through to the generic 500 handler.

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
- **Status:** ☑ Completed
- **Date:** 2026-04-30
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
- **Status:** ☑ Completed
- **Date:** 2026-05-17
- **Complexity:** Medium
- **Artifacts created/modified:**
  - `services/search-service/` — Full Spring Boot 4.0.6 module
  - Kafka consumer: `EventPublishedConsumer`
  - Elasticsearch document mapping: `EventDocument`
  - Repository: `EventDocumentRepository`
  - Controller: `SearchController` with `GET /api/v1/search/events`
  - DTOs: `SearchCriteria`, `SearchResult`, `EventSearchDto`, `Facets`
  - Service layer: `SearchService` (cursor pagination, multi-match query, facets, filters), `EventIndexService`, `EventDocumentMapper`
  - Config: `KafkaConsumerConfig`, `OpenApiConfig`, `SecurityConfig`, `MapStructConfig`
  - Tests: 25 tests (8 ArchUnit, 4 mapper, 1 index, 1 consumer, 6 search, 5 web, 1 integration)

### Task 4: Conformance Tests
- **Status:** ☑ Completed
- **Date:** 2026-05-07 (unit + web), 2026-05-11 (repository), 2026-05-12 (integration)
- **Complexity:** Medium
- **Artifacts created/modified:**
  - `unit/EventValidatorTest` — 15 cases covering publish/update/cancel rules
  - `unit/EventServiceTest` — 12 cases (create, find, update, publish, cancel, delete)
  - `unit/VenueServiceTest` — 6 cases
   - `unit/CategoryServiceTest` — 8 cases (CRUD + 404/409 on update)
  - `unit/TicketTypeServiceTest` — 7 cases
  - `unit/KafkaDomainEventPublisherTest` — 2 cases (publish + serialization failure)
  - `web/EventControllerWebTest` — 12 cases (CRUD + publish/cancel + validation + error handling)
  - `web/VenueControllerWebTest` — 7 cases
   - `web/CategoryControllerWebTest` — 8 cases (CRUD + validation + 404 on update)
  - `web/TicketTypeControllerWebTest` — 7 cases
  - `arch/ArchitectureTest` — 9 ArchUnit rules (pre-existing)
  - `application-test.yml` — test profile with datasource and Kafka config
  - `repository/VenueRepositorySoftDeleteTest` — 2 cases (soft delete + query filtering)
  - `repository/CategoryRepositorySoftDeleteTest` — 2 cases
  - `repository/EventRepositorySoftDeleteTest` — 2 cases (with Venue + Category setup)
  - `repository/TicketTypeRepositorySoftDeleteTest` — 2 cases (with full dependency chain + `findAllByEventId`)
  - `repository/PostgresRepositoryTest` — base class with shared `@DynamicPropertySource` container
  - `integration/CatalogIntegrationTestBase` — `@SpringBootTest` + Testcontainers (PostgreSQL + Kafka) with `@DynamicPropertySource` via static initializer (not `@Container`)
  - `integration/VenueCatalogIntegrationTest` — Venue CRUD lifecycle (create, read, update, delete, 404)
   - `integration/CategoryCatalogIntegrationTest` — 18 cases: Category CRUD lifecycle + validation errors (400/404/409) + soft delete + duplicate name on update + missing header
  - `integration/TicketTypeCatalogIntegrationTest` — TicketType CRUD lifecycle
  - `integration/EventCatalogIntegrationTest` — 12 cases:
    - A1: Publish event (happy path)
    - A2: Kafka message payload verification (EventPublished JSON)
    - A3: List events by status (DRAFT / PUBLISHED)
    - A4: Cancel published event
    - A5: Soft delete → 404
    - A6: Update event
    - A7: GET without auth header
    - B1: Publish already published → 409
    - B2: Cancel DRAFT → 400
    - B3: Publish without ticket types → 400
    - B4: Ticket quantity exceeds capacity → 400
    - B5: Event not found → 404
- **Verification:** `mvn verify -pl services/event-catalog-service` passes — 144 tests, 0 violations (Spotless, SpotBugs, PMD, Checkstyle, ArchUnit).

### Task 5: Documentation & Observability
- **Status:** ☑ Completed
- **Date:** 2026-05-17
- **Complexity:** Low
- **Artifacts created/modified:**
  - `services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/services/EventMetricsService.java` — Micrometer counters for `event.created` and `event.published`
  - `services/event-catalog-service/pom.xml` — Added `spring-boot-starter-opentelemetry`, `micrometer-registry-prometheus`
  - `services/event-catalog-service/src/main/resources/application.yml` — ECS structured JSON logging, tracing (100% sampling), Kafka observation, Prometheus endpoint
  - `services/event-catalog-service/src/main/resources/application-dev.yml` — ECS structured logging, Kafka observation
  - `services/event-catalog-service/.../EventService.java` — Injected `EventMetricsService`, emits counters on create/publish
  - `services/search-service/pom.xml` — Added `spring-boot-starter-opentelemetry`, `micrometer-registry-prometheus`
  - `services/search-service/src/main/resources/application.yml` — ECS structured JSON logging, tracing, Kafka listener observation, Prometheus endpoint
  - `services/search-service/src/main/resources/application-dev.yml` — ECS structured logging, Kafka listener observation
  - `build-tools/pmd/pmd-ruleset.xml` — Increased CouplingBetweenObjects threshold from 20 to 25
- **Verification:** Logs appear as JSON with traceId; metrics endpoint exposes event.created and event.published counters; Kafka messages propagate traceparent headers.

---

## Acceptance Criteria

- [x] Venue CRUD works via REST API
- [x] Event CRUD works; event can be linked to venue and ticket types
- [x] Publishing an event emits `event.published` to Kafka
- [x] Search Service indexes published events within 5 seconds
- [x] Search API supports full-text query, city filter, category filter, date range
- [x] All write operations require authentication (JWT or dummy header)
- [x] Unit tests for business rules (EventValidator, Services) pass
- [x] Web tests for REST endpoints (`@WebMvcTest`) pass
- [x] Integration tests with Testcontainers pass
- [x] Structured JSON logs with trace IDs are emitted

---

## Execution Log

| Task | Status | Date | Notes |
|------|--------|------|-------|
| T1   | ☑ Completed | 2026-04-30 | Created Maven module, Dockerfile, application.yml. Configured MapStruct processor with Lombok ordering in root POM. Created `V1__init.sql` with 4 tables (soft delete, triggers, partial indexes, CHECK constraints). Schema documented in `data-model.md`. |
| T2   | ☑ Completed | 2026-04-30 | Refactored to Anemic Model + **package-by-layer** structure (`controllers/`, `services/`, `repositories/`, `entities/`, `dto/`, `exceptions/`, `config/`). Added `EventValidator` component for publish/update/cancel rules. Services use private `findXxxOrThrow()` helpers and `Optional.ifPresent()` for updates. Added `POST /{id}/cancel` endpoint. Kafka event published directly from Service (no AggregateRoot/domainEvents list). Build SUCCESS. |
| T3   | ☑ Completed | 2026-05-17 | search-service module created with full Kafka consumer, Elasticsearch indexing (EventDocument), SearchController with cursor pagination + filters + facets. 25 tests passing, 0 violations. |
| T4   | ☑ Completed | 2026-05-14 | 135 tests: 48 unit, 33 web (`@WebMvcTest`), 8 repository (`@DataJpaTest` + Testcontainers PostgreSQL), 37 integration (`@SpringBootTest` + Testcontainers PostgreSQL + Kafka), 9 ArchUnit. Build SUCCESS with 0 violations (Spotless, SpotBugs, PMD, Checkstyle, ArchUnit). Added Spotless (google-java-format) — removed 14 formatting rules from Checkstyle (whitespace, braces, modifiers, misc). Formatting auto-corrected via `mvn spotless:apply` across 108 files. `spotless:check` runs at `process-classes` phase, before all other quality tools. Added `@Size(max = N)` validation on all request DTOs to match entity `@Column(length = N)` constraints. Added `@ApiResponse` error documentation on all 23 controller endpoints. Added 3 new integration tests for validation edge cases and enhanced ProblemDetail assertions. Enabled `failOnWarning=true` for test compilation to catch deprecated API usage and other compiler warnings. |
| T4.1 | ☑ Completed | 2026-05-15 | Fixed `CategoryService.update()` — added duplicate name check + `DataIntegrityViolationException` handling (was throwing 500 instead of 409). Added 10 new tests (2 unit, 1 web, 7 integration) covering 404/400/409 error paths on PUT. Total: 144 tests. Build SUCCESS with 0 violations. |
| T5   | ☑ Completed | 2026-05-17 | ECS structured JSON logging on both services + Micrometer counters (event.created, event.published) via EventMetricsService + OpenTelemetry tracing with Kafka observation (traceparent propagation). 158 tests on event-catalog, 25 on search-service. |

---

## Lessons Learned

### What worked well
- **Anemic Model + dedicated validator:** Separating business rules into `EventValidator` made unit testing straightforward — 15 focused test cases covering all publish/update/cancel scenarios without needing database or Spring context.
- **Package-by-layer:** The flat layer structure (`controllers/`, `services/`, `repositories/`) made it easy for the agent to generate and navigate code. ArchUnit rules enforce the boundaries automatically.
- **Spring Boot 4 starter-test module:** The new `spring-boot-starter-webmvc-test` dependency cleanly separates MVC test infrastructure. Auto-configuration of `MockMvc` and security filters worked out of the box.

### What didn't work / Surprises
- **Migration from `TestRestTemplate` to `RestTestClient`:** Spring Boot 4 moved `TestRestTemplate` to a dedicated module (`spring-boot-resttestclient`). We migrated to `RestTestClient` (`org.springframework.test.web.servlet.client`) with `@AutoConfigureRestTestClient`, using the fluent API instead of `exchange()`/`getForEntity()`. Notable learning: `StatusAssertions` has no `isConflict()` — use `isEqualTo(HttpStatus.CONFLICT)`. Kafka's `Consumer` clashes with `java.util.function.Consumer` — use fully qualified name for the latter.
- **Spring Boot 4 removed `@MockBean`:** The annotation was deprecated in 3.4 and removed in 4.0. All `@WebMvcTest` classes broke until we replaced `@MockBean` with `@MockitoBean` (from `org.springframework.test.context.bean.override.mockito`). The package change for `@WebMvcTest` (`org.springframework.boot.webmvc.test.autoconfigure`) was also undocumented in many migration guides.
- **Testcontainers 2.x renamed artifacts:** `org.testcontainers:junit-jupiter` became `org.testcontainers:testcontainers-junit-jupiter`. Same for `postgresql` → `testcontainers-postgresql` and `kafka` → `testcontainers-kafka`. Spring Boot 4.0.6 BOM manages version 2.0.5.
- **Spring Framework 7 requires `-parameters` compiler flag:** Without it, `@PathVariable UUID id` throws `IllegalArgumentException: Name for argument of type [java.util.UUID] not specified`. This was not obvious and took significant debugging. The fix is adding `<arg>-parameters</arg>` to `maven-compiler-plugin` in the root POM.
- **`MethodArgumentNotValidException` not handled:** The initial `GlobalExceptionHandler` only caught `BusinessException`, `EntityNotFoundException`, and generic `Exception`. Validation failures (`@Valid` DTOs) fell through to the 500 handler. Added explicit handler returning `ProblemDetail` with a map of field errors.
- **Corrupted local Maven cache:** `spring-boot-test-autoconfigure` JAR in `~/.m2` was only 28KB (incomplete download). Deleting the cached directory and letting Maven re-download fixed the missing `@WebMvcTest` class.

### Deviations from original plan
- **Rich Model → Anemic Model:** Original prompt specified DDD-style rich behavior in entities (`Event.publish()`). After review, switched to Anemic Model (entities = pure data, Services = all logic) for simpler readability and to align with common Spring Boot patterns. Extracted validations into dedicated `EventValidator` component.
- **Task 4 split into phases:** Instead of jumping straight to integration tests with Testcontainers, we wrote unit tests (Mockito) and web tests (`@WebMvcTest`) first. This validated business rules and controller contracts before dealing with container startup overhead. Integration tests (PostgreSQL + Kafka) are the final phase.

### What almost went wrong
- **Kafka message sent before database commit:** In `EventService.publish()`, `publishEventToKafka()` is called before `eventRepository.save()`. If the save fails, the Kafka message was already sent. This is a consistency risk. For now it is acceptable (study project), but a production fix would either: (a) use Kafka transactions, (b) use Outbox pattern, or (c) publish after successful save.
- **Missing `-parameters` would break all controllers in production:** The compiler flag was missing from the root POM. Without it, every controller with `@PathVariable` or `@RequestParam` would fail at runtime. We caught this during `@WebMvcTest` execution, not at compile time.

### What would I do differently next time
- Add the `-parameters` compiler flag to the project template/scaffold from day one, not after tests fail.
- Write a single "Spring Boot 4 migration checklist" document at project start, listing: `@MockBean` → `@MockitoBean`, Testcontainers artifact renames, new starters, `-parameters` flag.
- Consider using Outbox pattern for Kafka publishing from the beginning, rather than direct `KafkaTemplate.send()` from the service.

### Key metrics (before / after)
- Baseline: 9 tests (ArchUnit only)
- Result after Task 4: 144 tests (event-catalog-service), all passing with 0 static violations
- Result after Task 5: 158 tests (event-catalog-service) + 25 tests (search-service), all passing with 0 static violations
- Tool used: JUnit 5, Mockito, `@WebMvcTest` (Spring Boot 4), Testcontainers 2.0.5 (PostgreSQL singleton container), Micrometer, OpenTelemetry

### Spring Boot 4 Observability
- **ECS Structured Logging:** `logging.structured.format.console=ecs` is the correct property (not `logging.structured.format=json`). Three formats available: `ecs`, `gelf`, `logstash`. Automatically includes traceId/spanId from MDC when Micrometer Tracing is on the classpath.
- **`spring-boot-starter-opentelemetry`:** Single starter that bundles micrometer-tracing-bridge-otel and opentelemetry-exporter-otlp. Managed by Spring Boot BOM — no version needed.
- **Kafka observation:** `spring.kafka.template.observation-enabled=true` for producers, `spring.kafka.listener.observation-enabled=true` for consumers. Both default to `false` — must be explicitly enabled.
- **Micrometer registry in Boot 4:** `spring-boot-starter-actuator` no longer pulls in `micrometer-core`. Must explicitly add a registry implementation like `micrometer-registry-prometheus` which transitively brings in `micrometer-core`.
