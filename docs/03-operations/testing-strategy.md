# Testing Strategy

> Approach to unit, integration, and architecture testing for the Eventixx platform.

---

## 1. Testing Pyramid

We follow the standard test pyramid with an emphasis on fast feedback:

```
        /\
       /  \     Integration Tests  (slow, full Spring context + containers)
      /----\    @SpringBootTest + Testcontainers (PostgreSQL + Kafka + ES)
     /      \
    /--------\  Repository Tests   (medium, database layer only)
   /          \ @DataJpaTest + Testcontainers PostgreSQL
  /------------\ Web Tests         (fast, controllers + MockMvc)
 /              \@WebMvcTest + @MockitoBean
/----------------\ Unit Tests      (fastest, pure logic)
                  JUnit 5 + Mockito (no Spring context)
```

| Layer | Scope | Speed | Count (target) |
|-------|-------|-------|----------------|
| **Unit** | Services, validators, publishers, mappers | < 1s / class | 100+ |
| **Web** | Controllers, DTO serialization, exception handling | ~2s / class | 50+ |
| **Repository** | JPA queries, soft delete, constraints | ~5s / class | 20+ |
| **Integration** | End-to-end flows with real database and Kafka | ~30s / class | 10+ |
| **Architecture** | Package cycles, layer dependencies, naming | ~2s total | 10+ |

---

## 2. Technology Stack

| Tool | Version | Purpose |
|------|---------|---------|
| JUnit 5 | 5.12 (via Spring Boot 4.0.6) | Test framework |
| Mockito | 5.20 (via Spring Boot 4.0.6) | Mocking |
| AssertJ | 3.27 (via Spring Boot 4.0.6) | Fluent assertions |
| ArchUnit | 1.4.0 | Architecture rules |
| Testcontainers | 2.0.5 (via Spring Boot BOM) | Docker containers for integration tests |
| Awaitility | 4.3.0 (via Spring Boot BOM) | Async assertions (Kafka) |

### Spring Boot 4 Test-Specific Dependencies

```xml
<!-- In each service pom.xml -->
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-webmvc-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa-test</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-testcontainers</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-junit-jupiter</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-postgresql</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.testcontainers</groupId>
    <artifactId>testcontainers-kafka</artifactId>
    <scope>test</scope>
</dependency>
<dependency>
    <groupId>org.awaitility</groupId>
    <artifactId>awaitility</artifactId>
    <scope>test</scope>
</dependency>
```

> **Note:** Spring Boot 4 uses Testcontainers 2.x, which renamed artifacts from `junit-jupiter` to `testcontainers-junit-jupiter`, `postgresql` to `testcontainers-postgresql`, etc. The version is managed by Spring Boot's BOM — do not specify it explicitly.

---

## 3. Spring Boot 4 Migration Notes

Critical changes from Spring Boot 3.x that affect testing:

| Spring Boot 3.x | Spring Boot 4.0.6 | Impact |
|-----------------|-------------------|--------|
| `@MockBean` | `@MockitoBean` | All `@WebMvcTest` and `@SpringBootTest` classes must update imports |
| `@WebMvcTest` in `spring-boot-test-autoconfigure` | `@WebMvcTest` in `spring-boot-webmvc-test` module | New starter `spring-boot-starter-webmvc-test` required |
| `org.testcontainers:junit-jupiter` | `org.testcontainers:testcontainers-junit-jupiter` | Artifact rename; version managed by BOM |
| `org.testcontainers:postgresql` | `org.testcontainers:testcontainers-postgresql` | Artifact rename |
| `org.testcontainers:kafka` | `org.testcontainers:testcontainers-kafka` | Artifact rename |
| `-parameters` flag | **Mandatory** | Add `<arg>-parameters</arg>` to `maven-compiler-plugin` or `@PathVariable` fails at runtime |
| `@DynamicPropertySource` | `@ServiceConnection` | `@ServiceConnection` has lifecycle issues with `@DataJpaTest` + shared static containers. We use explicit `@DynamicPropertySource` for reliability. |
| `TestEntityManager` in `spring-boot-test-autoconfigure` | `TestEntityManager` in `spring-boot-starter-data-jpa-test` | New starter `spring-boot-starter-data-jpa-test` required; package changed to `org.springframework.boot.jpa.test.autoconfigure` |

### Singleton Container Pattern

When sharing a PostgreSQL container across multiple `@DataJpaTest` classes, do **not** use `@Testcontainers` + `@Container` on the base class or subclasses. The JUnit 5 extension restarts the container between classes, causing `Connection refused` errors when Spring tries to reuse the cached context with stale port mappings.

Instead, use a **static initializer block** in the abstract base class:

```java
public abstract class PostgresRepositoryTest {
    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }
}
```

This ensures the container starts once when the classloader loads the base class, and stays alive for the entire test suite.

---

## 4. Test Package Structure

```
src/test/java/com/eventixx/<service>/
├── unit/                           # Pure unit tests (no Spring context)
│   ├── EventValidatorTest.java
│   ├── EventServiceTest.java
│   ├── VenueServiceTest.java
│   ├── CategoryServiceTest.java
│   ├── TicketTypeServiceTest.java
│   └── KafkaDomainEventPublisherTest.java
├── web/                            # Controller tests (@WebMvcTest)
│   ├── EventControllerWebTest.java
│   ├── VenueControllerWebTest.java
│   ├── CategoryControllerWebTest.java
│   └── TicketTypeControllerWebTest.java
├── repository/                     # JPA tests (@DataJpaTest + Testcontainers)
│   ├── PostgresRepositoryTest.java   # shared base class: singleton PostgreSQL container (static initializer) + @DynamicPropertySource
│   ├── VenueRepositorySoftDeleteTest.java
│   ├── CategoryRepositorySoftDeleteTest.java
│   ├── EventRepositorySoftDeleteTest.java
│   └── TicketTypeRepositorySoftDeleteTest.java
├── integration/                    # End-to-end (@SpringBootTest + Testcontainers)
│   └── EventCatalogIntegrationTest.java
└── arch/                           # Architecture rules
    └── ArchitectureTest.java
```

---

## 5. Test Profiles

### `application-test.yml`

Each service has a dedicated test profile at `src/test/resources/application-test.yml`:

```yaml
spring:
  application:
    name: <service-name>
  datasource:
    url: jdbc:postgresql://localhost:5432/<db>_test
    username: eventixx
    password: eventixx
  jpa:
    hibernate:
      ddl-auto: validate
  flyway:
    enabled: true
  kafka:
    bootstrap-servers: localhost:9092
server:
  port: 0
eureka:
  client:
    enabled: false
```

For Testcontainers-based tests, containers override these properties via `@DynamicPropertySource`.

---

## 6. Running Tests

```bash
# All tests in a service
./mvnw test -pl services/event-catalog-service

# Only unit tests
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.unit.*"

# Only web tests
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.web.*"

# Only repository tests (Testcontainers)
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.repository.*"

# Only ArchUnit
./mvnw test -pl services/event-catalog-service -Dtest=ArchitectureTest

# Full verification (tests + quality checks)
./mvnw verify -pl services/event-catalog-service

# Quality checks only (SpotBugs + PMD + Checkstyle, no tests)
./mvnw verify -DskipTests

# Same as above with full error stacktraces
./mvnw verify -DskipTests -e

# Skip tests during build
./mvnw clean install -DskipTests
```

### Useful Maven Flags

| Flag | Purpose |
|------|---------|
| `-DskipTests` | Compiles tests but does not execute them |
| `-Dmaven.test.skip=true` | Skips both compilation and execution of tests |
| `-e` | Prints the full stacktrace on error (essential for debugging plugin failures) |
| `-X` | Enables debug output (verbose Maven lifecycle logging) |
| `-pl <module>` | Restricts execution to a specific module (e.g., `-pl services/event-catalog-service`) |

---

## 7. Key Testing Patterns

### Unit Test Pattern (Mockito)

```java
@ExtendWith(MockitoExtension.class)
class EventValidatorTest {
    @InjectMocks
    private EventValidator validator;

    @Test
    void shouldAllowPublish_whenDraftWithTicketsAndCapacityOk() {
        Event event = draftEvent(venueWithCapacity(100), List.of(ticketType("GA", 50)));
        assertThatNoException().isThrownBy(() -> validator.validateCanBePublished(event));
    }
}
```

### Web Test Pattern (@WebMvcTest)

```java
@WebMvcTest(EventController.class)
class EventControllerWebTest {
    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;
    @MockitoBean private EventService eventService;

    @Test
    void shouldCreateEvent() throws Exception {
        when(eventService.create(any())).thenReturn(response);
        mockMvc.perform(post("/api/v1/events")
                .header("X-User-Id", "user-123")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isCreated());
    }
}
```

### Repository Test Pattern (@DataJpaTest + Singleton Container)

```java
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
@ImportAutoConfiguration({ FlywayAutoConfiguration.class })
@ActiveProfiles("test")
public abstract class PostgresRepositoryTest {

    static final PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine");

    static {
        postgres.start(); // singleton: started once, shared across all subclasses
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
    }

    @Autowired
    protected JdbcTemplate jdbcTemplate;
}

class VenueRepositorySoftDeleteTest extends PostgresRepositoryTest {
    @Autowired
    private TestEntityManager entityManager;

    @Test
    void shouldSoftDeleteVenue() {
        Venue saved = entityManager.persistFlushFind(venue);
        entityManager.remove(saved);
        entityManager.flush();

        // @SQLRestriction filters deleted records
        assertThat(entityManager.find(Venue.class, saved.getId())).isNull();

        // But record still exists with deleted_at populated
        Instant deletedAt = jdbcTemplate.queryForObject(
            "SELECT deleted_at FROM venues WHERE id = ?", Instant.class, saved.getId());
        assertThat(deletedAt).isNotNull();
    }
}
```

### Integration Test Pattern (Testcontainers)

```java
@SpringBootTest(webEnvironment = RANDOM_PORT)
@Testcontainers
class EventCatalogIntegrationTest {
    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    static KafkaContainer kafka = new KafkaContainer("confluentinc/cp-kafka:latest");

    // No @DynamicPropertySource needed — @ServiceConnection auto-wires everything
}
```

---

## 8. Quality Gates

| Gate | Criteria |
|------|----------|
| **Unit** | ≥ 80% line coverage on Service and Validator classes |
| **Web** | All controller endpoints exercised; 4xx/5xx error paths covered |
| **Repository** | All custom queries tested; soft delete verified |
| **Integration** | Happy path + at least one error path per major flow |
| **Architecture** | All ArchUnit rules pass; no package cycles introduced |

---

## 9. Known Issues & Workarounds

| Issue | Cause | Fix |
|-------|-------|-----|
| `@PathVariable` throws `IllegalArgumentException` | Missing `-parameters` compiler flag | Add `<arg>-parameters</arg>` to root POM `maven-compiler-plugin` |
| `MockBean` not found | Removed in Spring Boot 4 | Use `@MockitoBean` from `org.springframework.test.context.bean.override.mockito` |
| `WebMvcTest` not found | Moved to `spring-boot-webmvc-test` module | Add `spring-boot-starter-webmvc-test` dependency |
| Testcontainers dependency version missing | Artifact renamed in 2.x | Use `testcontainers-*` prefix (e.g., `testcontainers-postgresql`) |
| Corrupted `spring-boot-test-autoconfigure` JAR | Incomplete Maven download | Delete `~/.m2/repository/org/springframework/boot/spring-boot-test-autoconfigure/` and re-run |
| Docker fails with "TTRPC connection: unsupported protocol" | Incompatibility between Docker daemon and containerd/runc versions | Restart Docker daemon or downgrade containerd to a compatible version |

---

> *"Fast, reliable tests are a feature. Slow, flaky tests are technical debt."*
