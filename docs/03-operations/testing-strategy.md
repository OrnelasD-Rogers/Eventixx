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
| `-Xlint` compiler flags | Main: `-Xlint:all,-processing` with `failOnWarning=true`; Test: `-Xlint:all,-processing,-rawtypes,-unchecked` | Zero-compiler-warning policy for `src/main/java`. Test warnings visible but non-blocking (`rawtypes`/`unchecked` excluded due to Mockito noise). |
| `TestRestTemplate` | `RestTestClient` (`org.springframework.test.web.servlet.client`) | Use `@AutoConfigureRestTestClient` + inject `RestTestClient`. Fluent API replaces `exchange()`/`getForEntity()`. Dependencies: `spring-boot-resttestclient` (test) + `spring-boot-restclient` (compile). |
| `@DynamicPropertySource` | `@ServiceConnection` | `@ServiceConnection` has lifecycle issues with `@DataJpaTest` + shared static containers. We use explicit `@DynamicPropertySource` for reliability. |
| `TestEntityManager` in `spring-boot-test-autoconfigure` | `TestEntityManager` in `spring-boot-starter-data-jpa-test` | New starter `spring-boot-starter-data-jpa-test` required; package changed to `org.springframework.boot.jpa.test.autoconfigure` |

### Singleton Container Pattern

When sharing a PostgreSQL container across multiple `@DataJpaTest` classes, do **not** use `@Testcontainers` + `@Container` on the base class or subclasses. The JUnit 5 extension restarts the container between classes, causing `Connection refused` errors when Spring tries to reuse the cached context with stale port mappings.

Instead, use a **static initializer block** in the abstract base class:

```java
public abstract class PostgresRepositoryTest {
    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
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
│   ├── CatalogIntegrationTestBase.java   # shared base: PostgreSQL + Kafka containers, auth helpers
│   ├── RestPage.java                      # helper to deserialize Page<T> via Jackson @JsonCreator
│   ├── EventCatalogIntegrationTest.java  # Event lifecycle (create, publish, cancel, delete)
│   ├── VenueCatalogIntegrationTest.java  # Venue CRUD
│   ├── CategoryCatalogIntegrationTest.java # Category CRUD
│   └── TicketTypeCatalogIntegrationTest.java # TicketType CRUD
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

# Only integration tests (Testcontainers)
./mvnw test -pl services/event-catalog-service -Dtest="com.eventixx.eventcatalog.integration.*"

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

    static final PostgreSQLContainer POSTGRES = new PostgreSQLContainer("postgres:16-alpine");

    static {
        POSTGRES.start(); // singleton: started once, shared across all subclasses
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
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
@AutoConfigureRestTestClient
@ActiveProfiles("test")
public abstract class CatalogIntegrationTestBase {

    static final PostgreSQLContainer POSTGRES;
    static final KafkaContainer KAFKA;

    static {
        POSTGRES = new PostgreSQLContainer("postgres:16-alpine");
        KAFKA = new KafkaContainer("apache/kafka:4.0.0");
        POSTGRES.start();
        KAFKA.start();
    }

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("spring.kafka.bootstrap-servers", KAFKA::getBootstrapServers);
    }

    @Autowired
    protected RestTestClient restClient;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @BeforeEach
    void cleanDatabase() {
        jdbcTemplate.execute("TRUNCATE TABLE ticket_types, events, venues, categories RESTART IDENTITY CASCADE");
    }

    protected static java.util.function.Consumer<HttpHeaders> requestHeaders() {
        return headers -> headers.set("X-User-Id", "user-1");
    }

    protected VenueResponse createVenue() {
        var request = new CreateVenueRequest("Test Venue", "123 Street", "City", "Country", 100);
        return restClient.post()
                .uri("/api/v1/venues")
                .headers(requestHeaders())
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .expectBody(VenueResponse.class)
                .returnResult().getResponseBody();
    }

    protected Consumer<String, String> createKafkaConsumer() {
        Map<String, Object> props = KafkaTestUtils.consumerProps(
                KAFKA.getBootstrapServers(), "test-group-" + UUID.randomUUID(), false);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class);
        var consumer = new DefaultKafkaConsumerFactory<String, String>(
                props, new StringDeserializer(), new StringDeserializer()
        ).createConsumer();
        consumer.subscribe(List.of("event.published"));
        return consumer;
    }
}
```

### Page Deserialization with RestTestClient

Spring Data's `Page` is an interface — Jackson cannot construct it directly. Use `RestPage<T>`, a concrete `PageImpl<T>` subclass annotated for Jackson:

```java
@JsonIgnoreProperties(ignoreUnknown = true, value = {"pageable"})
public class RestPage<T> extends PageImpl<T> {

    @JsonCreator(mode = JsonCreator.Mode.PROPERTIES)
    public RestPage(
            @JsonProperty("content") List<T> content,
            @JsonProperty("number") int page,
            @JsonProperty("size") int size,
            @JsonProperty("totalElements") long total) {
        super(content, PageRequest.of(page, size), total);
    }
}
```

Usage in integration tests:

```java
var page = restClient.get()
        .uri("/api/v1/venues")
        .exchange()
        .expectBody(new ParameterizedTypeReference<RestPage<VenueSummaryResponse>>() {})
        .returnResult().getResponseBody();
assertThat(page.getContent()).hasSize(2);
```

For endpoints returning a plain `List<T>` (not `Page<T>`):

```java
var ticketTypes = restClient.get()
        .uri("/api/v1/events/{eventId}/ticket-types", eventId)
        .exchange()
        .expectBody(new ParameterizedTypeReference<List<TicketTypeResponse>>() {})
        .returnResult().getResponseBody();
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
| `RestTestClient` not injected (`NoSuchBeanDefinitionException`) | Annotation `@AutoConfigureRestTestClient` missing | Add `@AutoConfigureRestTestClient` on test class + `spring-boot-resttestclient` (test) + `spring-boot-restclient` (compile) dependencies |
| Compiler warning blocking the build | `-Xlint:all,-processing` + `failOnWarning=true` on main sources | Fix the warning (e.g., deprecation, removal, unused variable). Test code uses relaxed linting (`-rawtypes,-unchecked`) where Mockito noise is expected. |
| Checkstyle violation blocking the build | `severity=error` + `failOnViolation=true` on both main and test sources | Fix the violation (e.g., unused imports, line length, naming). Test method naming underscores are suppressed via `SuppressionSingleFilter`. |

---

> *"Fast, reliable tests are a feature. Slow, flaky tests are technical debt."*
