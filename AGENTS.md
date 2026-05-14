# AGENTS.md — Eventixx

Guide for AI agents working in this repository.

---

## Project Overview

**Eventixx** is a microservices-based event-ticketing backend (inspired by Ticketmaster). Stack: Java 21, Spring Boot 4.0.6, Kafka (KRaft), PostgreSQL, Elasticsearch, Redis, Docker Compose.

---

## Build & Test Commands

```bash
# Full build (compile + unit tests)
./mvnw clean install

# Build without tests
./mvnw clean install -DskipTests

# Run tests for a specific service
./mvnw test -pl services/event-catalog-service

# Run tests by package
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.unit.*"
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.web.*"

# Run only ArchUnit tests
./mvnw test -pl services/event-catalog-service -Dtest=ArchitectureTest

# Full verification (tests + SpotBugs + PMD + Checkstyle + ArchUnit)
./mvnw verify -pl services/event-catalog-service

# Static analysis only (skip tests)
./mvnw verify -DskipTests

# Static analysis with full stacktraces
./mvnw verify -DskipTests -e

# Run a service locally (dev profile, no Eureka)
docker compose -f docker-compose.event-catalog.yml up -d
./mvnw spring-boot:run -pl services/event-catalog-service -Dspring-boot.run.profiles=dev

# Start full infrastructure
docker-compose up -d
```

---

## Commit Rules

- **Before every commit**, run full verification:
  ```bash
  ./mvnw verify -pl services/event-catalog-service
  # or for all modules:
  ./mvnw verify -pl services/event-catalog-service,services/search-service
  ```
- Only commit if **all checks pass** (compiler warnings + tests + SpotBugs + PMD + Checkstyle + ArchUnit).
  - **Zero compiler warnings** in both `src/main/java` and `src/test/java`. The parent POM enforces `-Xlint:all,-processing` with `failOnWarning=true` for main sources. Test sources use `-Xlint:all,-processing,-rawtypes,-unchecked` (warnings shown but not blocking).
- If verify fails, fix violations before committing.
- Never use `--no-verify`, `--no-gpg-sign`, or any flags that skip hooks.
- Commits must be atomic with descriptive messages in Portuguese or English.

---

## Architecture Constraints

| Rule | Description |
|------|-------------|
| Database per Service | Each service owns its database exclusively. **Never** access another service's database. |
| Async over Sync | Cross-service workflows use Kafka. REST only for simple queries (sub-100ms, max 2-3 hops). |
| Event-Driven | State changes are published as domain events to Kafka. |
| Design for Failure | Every external call: timeout + retry + circuit breaker. Every request: correlation ID. |
| API-First | OpenAPI specs before implementation. Versioning via URL (`/api/v1/...`). |
| Saga Orchestration | Distributed transactions via Saga Orchestrator Service with state persisted in PostgreSQL. |
| Selective CQRS | Search Service with Elasticsearch read model. Reservation/Payment with partial event sourcing. |

**Existing services:**

| Service | Bounded Context | DB (Docker) | Port |
|---------|----------------|-------------|------|
| API Gateway | Infrastructure | — | — |
| Discovery Service | Infrastructure | — | 8761 |
| User Service | Identity & Access | `user-db` | 5432 |
| Event Catalog Service | Event Management | `event-catalog-db` | 5433 |
| Search Service | Search & Discovery | Elasticsearch | 9200 |
| Reservation Service | Reservation | `reservation-db` | 5434 |
| Payment Service | Payment (Mock) | `payment-db` | 5435 |
| Saga Orchestrator Service | Process Coordination | `saga-db` | 5436 |
| Notification Service | Notifications | `notification-db` | 5437 |
| Check-in Service | Event Entry | `checkin-db` | 5438 |

---

## Code Conventions

### Package Structure
```
com.eventixx.<service>/
├── config/
├── controllers/
├── dto/
├── entities/
├── exceptions/
├── mappers/
├── repositories/
└── services/
```

### Naming
- `@RestController` → class name ends with `Controller`
- `@Service` → class name ends with `Service`
- `@Repository` → class name ends with `Repository`

### Package Rules (ArchUnit enforced)
- `entities` **must not** depend on `controllers` or `services`
- `repositories` **must not** depend on `controllers`
- `dto` **must not** depend on `entities`
- No cycles between packages
- `@Transactional` and `@Async` must be `public` and `not final` (Spring proxy requirement)
- No `System.out` — use SLF4J (`@Slf4j`)

### Soft Delete
All entities use soft delete via `deleted_at TIMESTAMP`. Queries MUST always filter `WHERE deleted_at IS NULL`. Never hard delete.

### Spring Boot 4 — Essential Migrations
- `@MockBean` → use `@MockitoBean` (`org.springframework.test.context.bean.override.mockito`)
- `@WebMvcTest` requires `spring-boot-starter-webmvc-test`
- `@ServiceConnection` not used — always prefer `@DynamicPropertySource` with static singleton containers (avoids lifecycle issues with shared contexts)
- `TestRestTemplate` → use `RestTestClient` (`org.springframework.test.web.servlet.client`) + `@AutoConfigureRestTestClient`. Dependencies: `spring-boot-resttestclient` (test) + `spring-boot-restclient` (compile).
- `-parameters` flag already configured in parent POM (required for `@PathVariable`)

### Mapping
- MapStruct (with `defaultComponentModel=spring`, `unmappedTargetPolicy=IGNORE`)
- Lombok + MapStruct: annotation processors configured in parent POM (Lombok first)

---

## Testing Patterns

### Test Structure
```
src/test/java/com/eventixx/<service>/
├── unit/           # JUnit 5 + Mockito, no Spring context
├── web/            # @WebMvcTest + @MockitoBean
├── repository/     # @DataJpaTest + Testcontainers (singleton container)
├── integration/    # @SpringBootTest + Testcontainers
└── arch/           # ArchUnit rules
```

### Quality Gates
- Unit: ≥80% line coverage on Service and Validator classes
- Web: all endpoints + error paths (4xx/5xx)
- Repository: all custom queries + soft delete verified
- Integration: happy path + at least 1 error per major flow
- Architecture: **zero** ArchUnit violations

### Singleton Container Pattern (@DataJpaTest)
```java
public abstract class PostgresRepositoryTest {
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
    static { POSTGRES.start(); }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
```

---

## Documentation Framework

The project follows the framework documented in `docs/GUIDELINES.md`:

```
docs/
├── 00-charter/       → PROJECT_CHARTER.md (vision, scope, principles)
├── 01-decisions/     → ADRs (architectural decisions with trade-offs)
├── 02-specs/         → Use Cases (UCs), data-model, glossary, diagrams
├── 03-operations/    → Deployment, testing-strategy, security-guide
└── 04-implementation/→ Setup, tooling
```

- **Use Case** = executable specification unit (flow + tasks + acceptance criteria)
- **ADR** = only when there is a relevant trade-off (reversibility + impact). Trivial decisions stay inline in the UC.
- UCs and ADRs are sequential (UC-001, ADR-001, etc.)

---

## Key Versions

| Dependency | Version |
|------------|---------|
| Java | 21 |
| Spring Boot | 4.0.6 |
| Spring Cloud | 2025.1.0 |
| Kafka | KRaft (no Zookeeper) |
| PostgreSQL | 16-alpine |
| SpotBugs | 4.9.8 |
| PMD | 7.24.0 |
| Checkstyle | 10.23.0 |
| ArchUnit | 1.4.0 |
| MapStruct | 1.6.0 |
| Lombok | 1.18.36 |

---

## Commit Message Conventions

Short messages in Portuguese or English, describing **what** and **why**:
- `add X` — new feature
- `update X` — enhancement to existing feature
- `fix X` — bug fix
- `refactor X` — refactoring with no behavior change
- `test X` — test addition/modification
- `docs X` — documentation
