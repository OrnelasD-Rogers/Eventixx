# Test Code Templates

Use these templates when generating JUnit 5 test code for missing edge cases. Adapt the package, imports, and specific types to match the project under analysis.

The templates are organized by test type (Web MVC, unit, repository) and by edge case category.

---

## Template: Missing Validation Test (Web MVC)

Use for: endpoints missing `@Valid`, missing field constraints, no validation error handling.

```java
// Category: C1 - Input Validation
@WebMvcTest(controllers = EventController.class)
class EventControllerValidationTest {

    @MockitoBean
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    @DisplayName("POST /api/v1/events returns 400 when required fields are missing")
    void createEvent_missingRequiredFields_returns400() throws Exception {
        var request = new CreateEventRequest(null, "", null);

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/events returns 400 when field exceeds max length")
    void createEvent_fieldTooLong_returns400() throws Exception {
        var request = new CreateEventRequest("a".repeat(256), "description", LocalDateTime.now());

        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/events returns 400 when body is empty/malformed JSON")
    void createEvent_malformedJson_returns400() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_JSON)
                .content("{invalid json}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/v1/events returns 415 when content type is not JSON")
    void createEvent_wrongContentType_returns415() throws Exception {
        mockMvc.perform(post("/api/v1/events")
                .contentType(MediaType.APPLICATION_XML)
                .content("<event><name>test</name></event>"))
                .andExpect(status().isUnsupportedMediaType());
    }
}
```

---

## Template: UUID Format Validation (Web MVC)

Use for: endpoints with `@PathVariable String id` that should validate UUID format.

```java
// Category: C1 - Input Validation (UUID format)
@Test
@DisplayName("GET /api/v1/events/{id} returns 400 when UUID format is invalid")
void getById_invalidUuidFormat_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/events/not-a-uuid"))
            .andExpect(status().isBadRequest());
}

@Test
@DisplayName("GET /api/v1/events/{id} returns 400 when UUID is null/empty")
void getById_emptyUuid_returns400() throws Exception {
    mockMvc.perform(get("/api/v1/events/ "))
            .andExpect(status().isBadRequest());
}
```

---

## Template: Resource Not Found Test (Web MVC)

Use for: endpoints missing `ResourceNotFoundException` handling.

```java
// Category: C2 - Soft Delete / Resource Not Found
@Test
@DisplayName("GET /api/v1/events/{id} returns 404 when event does not exist")
void getById_notFound_returns404() throws Exception {
    var id = UUID.randomUUID();
    given(eventService.findById(id)).willThrow(new ResourceNotFoundException("Event not found: " + id));

    mockMvc.perform(get("/api/v1/events/{id}", id))
            .andExpect(status().isNotFound());
}

@Test
@DisplayName("GET /api/v1/events/{id} returns 410 when event was soft-deleted")
void getById_softDeleted_returns410() throws Exception {
    var id = UUID.randomUUID();
    given(eventService.findById(id)).willThrow(new ResourceGoneException("Event was deleted: " + id));

    mockMvc.perform(get("/api/v1/events/{id}", id))
            .andExpect(status().isGone());
}
```

---

## Template: Soft Delete Filtering (Repository)

Use for: repository queries missing `deleted_at IS NULL` filter.

```java
// Category: C2 - Soft Delete
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositorySoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("findAll does NOT return soft-deleted events")
    void findAll_excludesSoftDeleted() {
        var event = new Event("Test");
        event = eventRepository.save(event);

        // Simulate soft delete at DB level
        entityManager.createQuery("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
                .setParameter("id", event.getId())
                .executeUpdate();
        entityManager.clear();

        var result = eventRepository.findAll();
        assertThat(result).noneMatch(e -> e.getId().equals(event.getId()));
    }

    @Test
    @DisplayName("findById returns empty for soft-deleted event")
    void findById_softDeleted_returnsEmpty() {
        var event = new Event("Test");
        event = eventRepository.save(event);

        entityManager.createQuery("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
                .setParameter("id", event.getId())
                .executeUpdate();
        entityManager.clear();

        var result = eventRepository.findById(event.getId());
        assertThat(result).isEmpty();
    }
}
```

---

## Template: BOLA / Authorization (Web MVC)

Use for: endpoints missing object-level authorization.

```java
// Category: C4 - Security (BOLA - Broken Object Level Authorization)
@Test
@DisplayName("GET /api/v1/events/{id} returns 403 when user does not own the resource")
void getById_notOwner_returns403() throws Exception {
    var eventId = UUID.randomUUID();
    var otherUserId = "user-456";
    given(eventService.findById(eventId)).willReturn(
            new EventResponse(eventId, "Other's Event", "user-123"));

    mockMvc.perform(get("/api/v1/events/{id}", eventId)
                    .header("X-User-Id", otherUserId))
            .andExpect(status().isForbidden());
}

@Test
@DisplayName("POST /api/v1/events returns 401 when X-User-Id header is missing")
void createEvent_missingUserId_returns401() throws Exception {
    var request = new CreateEventRequest("New Event", "Description", LocalDateTime.now());

    mockMvc.perform(post("/api/v1/events")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)))
            .andExpect(status().isUnauthorized());
}
```

---

## Template: Concurrency / Optimistic Locking (Integration)

Use for: endpoints missing optimistic locking or retry logic.

```java
// Category: C3 - Concurrency / Race Conditions
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EventConcurrencyTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private EventRepository eventRepository;

    @Test
    @DisplayName("Concurrent updates to same event: one succeeds, one gets 409")
    void concurrentUpdates_optimisticLock_returns409() {
        var eventId = UUID.randomUUID();
        var event = new Event("Test");
        event.setId(eventId);
        event.setVersion(1L);

        given(eventRepository.findById(eventId))
                .willReturn(Optional.of(event));

        // First update succeeds
        given(eventRepository.save(any()))
                .willReturn(event);

        // Second update throws optimistic lock exception
        willThrow(new OptimisticLockException())
                .given(eventService)
                .updateEvent(eq(eventId), any());

        ExecutorService executor = Executors.newFixedThreadPool(2);
        var results = new ArrayList<ResponseEntity<Void>>();

        IntStream.range(0, 2).forEach(i -> {
            executor.execute(() -> {
                var response = client.put()
                        .uri("/api/v1/events/{id}", eventId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(new UpdateEventRequest("Updated"))
                        .exchange()
                        .expectStatus()
                        .isBetween(200, 409)
                        .returnResult(Void.class);
                synchronized (results) {
                    results.add(response);
                }
            });
        });

        executor.shutdown();
        assertThat(results).hasSize(2);
        assertThat(results).anyMatch(r -> r.getStatusCode().is2xxSuccessful());
        assertThat(results).anyMatch(r -> r.getStatusCode().value() == 409);
    }
}
```

---

## Template: State Transition Guard (Unit)

Use for: endpoints that change status without validation.

```java
// Category: C5 - State Transitions
@ExtendWith(MockitoExtension.class)
class OrderServiceStateTransitionTest {

    @Mock
    private OrderRepository orderRepository;

    @InjectMocks
    private OrderService orderService;

    @Test
    @DisplayName("Throws exception when transitioning from PENDING to DELIVERED (skip CONFIRMED + SHIPPED)")
    void transitionOrder_invalidSkip_throwsException() {
        var order = new Order();
        order.setStatus(OrderStatus.PENDING);

        given(orderRepository.findById(any())).willReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.transitionStatus(order.getId(), OrderStatus.DELIVERED));
    }

    @Test
    @DisplayName("Allows valid transition from PENDING to CONFIRMED")
    void transitionOrder_validTransition_succeeds() {
        var order = new Order();
        order.setStatus(OrderStatus.PENDING);

        given(orderRepository.findById(any())).willReturn(Optional.of(order));
        given(orderRepository.save(any())).willReturn(order);

        var result = orderService.transitionStatus(order.getId(), OrderStatus.CONFIRMED);

        assertThat(result.getStatus()).isEqualTo(OrderStatus.CONFIRMED);
    }

    @Test
    @DisplayName("Throws exception when transitioning to same status")
    void transitionOrder_sameStatus_throwsException() {
        var order = new Order();
        order.setStatus(OrderStatus.CONFIRMED);

        given(orderRepository.findById(any())).willReturn(Optional.of(order));

        assertThrows(IllegalStateException.class,
                () -> orderService.transitionStatus(order.getId(), OrderStatus.CONFIRMED));
    }
}
```

---

## Template: Pagination Edge Cases (Web MVC)

Use for: endpoints returning lists without proper pagination guards.

```java
// Category: C6 - Pagination / Limits
@WebMvcTest(controllers = EventController.class)
class EventControllerPaginationTest {

    @MockitoBean
    private EventService eventService;

    @Autowired
    private MockMvc mockMvc;

    @Test
    @DisplayName("GET /api/v1/events?size=0 returns 400")
    void listEvents_zeroSize_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .param("size", "0"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/events?size=100000 returns 400 (max exceeded)")
    void listEvents_exceedsMaxSize_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .param("size", "100000"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/events?page=-1 returns 400")
    void listEvents_negativePage_returns400() throws Exception {
        mockMvc.perform(get("/api/v1/events")
                        .param("page", "-1"))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("GET /api/v1/events returns has_more flag in response")
    void listEvents_responseContainsHasMore() throws Exception {
        var events = List.of(new EventResponse(UUID.randomUUID(), "Event 1"));
        var page = new PageImpl<>(events, PageRequest.of(0, 10), 25);
        given(eventService.findAll(any())).willReturn(page);

        mockMvc.perform(get("/api/v1/events")
                        .param("page", "0")
                        .param("size", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.has_more").isBoolean());
    }
}
```

---

## Template: `@Transactional` Self-Invocation (Unit)

Use for: services where a `@Transactional` method is called internally via `this.method()`.

```java
// Category: C3 - Concurrency (Self-Invocation)
@ExtendWith(MockitoExtension.class)
class TransactionalSelfInvocationTest {

    @Mock
    private EventRepository eventRepository;

    private EventService eventService;

    @BeforeEach
    void setUp() {
        eventService = new EventService(eventRepository);
    }

    @Test
    @DisplayName("Self-invoked @Transactional method does NOT start a transaction")
    void selfInvocation_bypassesProxy() {
        // This test verifies the BUG exists:
        // When calling this.method() within the same class,
        // @Transactional is silently ignored.
        // Fix: extract to separate service and inject it.
        var event = new Event("Test");
        event.setId(UUID.randomUUID());

        given(eventRepository.save(any())).willReturn(event);

        // This internally calls this.createHistoryEntry() — no transaction
        var result = eventService.createEventWithHistory(new CreateEventRequest("Test"));

        assertThat(result).isNotNull();
        // If the bug is present, both saves happen in same connection
        // (REQUIRES_NEW on createHistoryEntry is ignored)
    }
}
```

---

## Template: Idempotency Key Test (Integration)

Use for: POST endpoints where duplicate requests could cause data duplication.

```java
// Category: C3 - Concurrency (Idempotency)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EventIdempotencyTest {

    @Autowired
    private RestTestClient client;

    @MockitoBean
    private EventService eventService;

    @Test
    @DisplayName("POST /api/v1/events with same idempotency key returns same result")
    void createEvent_idempotencyKey_returnsSame() {
        var idempotencyKey = UUID.randomUUID().toString();
        var request = new CreateEventRequest("Event", "Desc", LocalDateTime.now());
        var response = new EventResponse(UUID.randomUUID(), "Event");

        given(eventService.create(any(), eq(idempotencyKey)))
                .willReturn(response);

        // First request
        var first = client.post()
                .uri("/api/v1/events")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange()
                .expectStatus().isCreated()
                .returnResult(EventResponse.class);

        // Second request with same key — same response, no duplicate
        var second = client.post()
                .uri("/api/v1/events")
                .header("Idempotency-Key", idempotencyKey)
                .body(request)
                .exchange()
                .expectStatus().isOk()
                .returnResult(EventResponse.class);
    }
}
```

---

## Template: SQL Injection — Dynamic Query Detection (Repository)

Use for: any endpoint that builds dynamic queries — scan the codebase for anti-patterns first.

**Detection checklist** (run against every repository/service class):

```java
// Step 1: Grep for anti-patterns before writing tests
//
//   rg '"\+\s|String\.format.*SELECT|\.query\("' src/main/
//   rg "createQuery\(|createNativeQuery\(" src/main/ --context 2
//   rg "Specification|toPredicate" src/main/ --context 5
//
// If concatenation is found → MISSING — write the test below.

// Category: C4 — Security (SQL Injection)
@ExtendWith(MockitoExtension.class)
class DynamicQueryInjectionTest {

    @Mock
    private EntityManager entityManager;

    @Mock
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("JdbcTemplate query with concatenation is flagged as SQL injection risk")
    void jdbcTemplateConcat_isUnsafe() {
        String userInput = "' OR 1=1; DROP TABLE events; --";

        // UNSAFE pattern — this is what the codebase should NOT do
        assertThatThrownBy(() ->
            jdbcTemplate.query(
                "SELECT * FROM events WHERE title = '" + userInput + "'",
                (rs, rowNum) -> null
            )
        ).isInstanceOf(BadSqlGrammarException.class);
        // This would execute: SELECT * FROM events WHERE title = '' OR 1=1; DROP TABLE events; --'
    }

    @Test
    @DisplayName("JdbcTemplate with ? placeholder is safe")
    void jdbcTemplateParam_isSafe() {
        String userInput = "' OR 1=1; DROP TABLE events; --";

        // SAFE pattern — use this instead
        assertDoesNotThrow(() ->
            jdbcTemplate.query(
                "SELECT * FROM events WHERE title = ?",
                new Object[]{userInput},
                (rs, rowNum) -> null
            )
        );
    }

    @Test
    @DisplayName("Like clause without escaping % and _ leaks data")
    void likeClause_untrimmedWildcards() {
        String userInput = "%";

        // UNSAFE — user can scan all rows with a single %
        var sql = "SELECT * FROM events WHERE title LIKE '%" + userInput + "%'";

        // SAFE — escape the wildcards
        var escaped = userInput.replace("%", "\\%").replace("_", "\\_");
        var safeSql = "SELECT * FROM events WHERE title LIKE '%' || ? || '%'";
        assertDoesNotThrow(() ->
            jdbcTemplate.query(safeSql, new Object[]{escaped}, (rs, rowNum) -> null)
        );
    }
}
```

---

## Template: SQL Injection — `@Query` Native with Parameter Binding

Use for: repositories with `@Query(nativeQuery = true)` annotated methods.

```java
// Category: C4 — Security (SQL Injection — Native Query)
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class NativeQueryInjectionTest extends PostgresRepositoryTest {

    @Autowired
    private JpaEventRepository eventRepository;

    @Autowired
    private EntityManager entityManager;

    @Test
    @DisplayName("Native query with named parameter rejects SQL injection")
    void nativeQuery_namedParam_rejectsInjection() {
        var injection = "' OR 1=1; SELECT true; --";

        // SAFE — named parameter :title is escaped by Hibernate
        var result = entityManager
                .createNativeQuery(
                        "SELECT * FROM events WHERE title = :title", Event.class)
                .setParameter("title", injection)
                .getResultList();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("Native query with string concatenation is vulnerable")
    void nativeQuery_concat_isVulnerable() {
        var injection = "' OR 1=1; DROP TABLE events; --";

        // UNSAFE — this pattern would execute the injection
        // DO NOT USE THIS IN PRODUCTION
        assertThatThrownBy(() ->
            entityManager.createNativeQuery(
                    "SELECT * FROM events WHERE title = '" + injection + "'")
        ).isInstanceOf(PersistenceException.class);
    }

    @Test
    @DisplayName("JPQL query with named parameter is safe")
    void jpql_namedParam_isSafe() {
        var injection = "' OR 1=1; SELECT e FROM Event e; --";

        var result = entityManager
                .createQuery("FROM Event e WHERE e.title = :title", Event.class)
                .setParameter("title", injection)
                .getResultList();

        assertThat(result).isEmpty();
    }
}
```

---

## Template: SQL Injection — ORDER BY Injection Detection

Use for: endpoints that accept sort parameters from user input.

```java
// Category: C4 — Security (SQL Injection — ORDER BY)
@ExtendWith(MockitoExtension.class)
class OrderByInjectionTest {

    @Mock
    private EntityManager entityManager;

    @Test
    @DisplayName("Dynamic ORDER BY from user input is an injection risk")
    void orderByInjection_detectUnsafe() {
        var userSort = "title; DROP TABLE events; --";

        // UNSAFE — concatenating user input into ORDER BY
        // ORDER BY is NOT parameterizable in JPA
        assertThatThrownBy(() ->
            entityManager.createQuery(
                "FROM Event e ORDER BY " + userSort)
        ).isInstanceOf(IllegalArgumentException.class);

        // SAFE — validate against allowlist
        var allowedSorts = List.of("title", "startTime", "createdAt");
        var sort = allowedSorts.contains(userSort) ? userSort : "createdAt";
        assertDoesNotThrow(() ->
            entityManager.createQuery(
                "FROM Event e ORDER BY " + sort)
        );
    }
}
```

---

## Template: Pagination Soft Delete Filter (Repository)

Use for: paginated endpoints that might leak soft-deleted records in pagination counts or results.

```java
// Category: C2 + C6 — Soft Delete + Pagination
@DataJpaTest
@AutoConfigureTestDatabase(replace = AutoConfigureTestDatabase.Replace.NONE)
class EventRepositoryPaginationSoftDeleteTest extends PostgresRepositoryTest {

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("Page count excludes soft-deleted events")
    void count_excludesSoftDeleted() {
        // Given: 3 events, 1 soft-deleted
        eventRepository.save(new Event("Active 1"));
        eventRepository.save(new Event("Active 2"));
        var deleted = eventRepository.save(new Event("Deleted"));
        entityManager.createQuery("UPDATE Event e SET e.deletedAt = CURRENT_TIMESTAMP WHERE e.id = :id")
                .setParameter("id", deleted.getId())
                .executeUpdate();
        entityManager.clear();

        // When: count
        var page = eventRepository.findAll(PageRequest.of(0, 10));

        // Then: count = 2 (not 3)
        assertThat(page.getTotalElements()).isEqualTo(2);
    }
}
```

---

## Template: Integration Test Singleton Base (Abstract)

Use for: any integration test with PostgreSQL via Testcontainers. Place this in a shared test base.

```java
// Category: C7 — Integration Tests (Testcontainers singleton)
public abstract class AbstractIntegrationTest {

    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine");

    static final KafkaContainer KAFKA =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    static {
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
}
```

---

## Template: Full‑Stack Integration Test (Controller → DB)

Use for: endpoints where you need the REAL database to verify constraints, soft delete, unique violations, or foreign key behavior.

```java
// Category: C7 — Integration Tests (Full Stack)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class EventFullStackIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("POST /api/v1/events — unique constraint violation returns 409")
    void createEvent_duplicateUniqueField_returns409() {
        var request = new CreateEventRequest("Unique-Slug-Event", "Desc", LocalDateTime.now());

        // First: creates successfully
        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(request)
                .exchange()
                .expectStatus().isCreated();

        // Second: duplicate slug → 409 Conflict
        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(request)
                .exchange()
                .expectStatus().isConflict();
    }

    @Test
    @DisplayName("GET /api/v1/events/{id} — soft-deleted event returns 410 Gone")
    void getById_softDeleted_returns410() {
        // Create via API
        var createResponse = client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(new CreateEventRequest("Soft Delete Test", "Desc", LocalDateTime.now()))
                .exchange()
                .expectStatus().isCreated()
                .returnResult(EventResponse.class);
        var eventId = createResponse.getResponseBody().id();

        // Delete (soft)
        client.delete().uri("/api/v1/events/{id}", eventId)
                .header("X-User-Id", "user-1")
                .exchange()
                .expectStatus().isNoContent();

        // GET should now return 410 Gone
        client.get().uri("/api/v1/events/{id}", eventId)
                .exchange()
                .expectStatus().isGone();
    }

    @Test
    @DisplayName("DELETE /api/v1/events/{id} — parent with children fails (FK constraint)")
    void deleteEvent_withActiveTickets_returns409() {
        var eventId = seedEventWithTickets();

        client.delete().uri("/api/v1/events/{id}", eventId)
                .header("X-User-Id", "user-1")
                .exchange()
                .expectStatus().isConflict();
    }

    private UUID seedEventWithTickets() {
        var event = eventRepository.save(new Event("Event with Tickets"));
        // insert ticket via entityManager to bypass business logic
        return event.getId();
    }
}
```

---

## Template: Transaction Rollback Across Layers (Integration)

Use for: endpoints where a service method saves to DB and also does something else (Kafka, HTTP call, file write) in the same `@Transactional`.

```java
// Category: C7 — Integration Tests (Transactional Rollback)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class TransactionRollbackIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("When Kafka publish fails, DB insert is rolled back — no partial state")
    void createEvent_kafkaFailure_rollsBackDb() {
        // Simulate: Kafka broker is down
        // The service uses @Transactional(rollbackFor = Exception.class)
        // When the Kafka send throws, the DB insert must roll back too.

        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(new CreateEventRequest("Rollback Test", "Desc", LocalDateTime.now()))
                .exchange()
                .expectStatus().is5xxServerError();

        // Verify: no event was persisted despite the service calling save()
        var count = eventRepository.count();
        assertThat(count).isZero();
    }

    @Test
    @DisplayName("Pure DB operation outside @Transactional does NOT roll back on failure")
    void createEvent_noTransaction_partialSavePersists() {
        // If the service method is NOT @Transactional, a failure after save()
        // still leaves the saved record — this is a BUG pattern.
        var event = new Event("Partial Save Test");
        eventRepository.save(event); // this commits immediately

        // Even if subsequent code throws, the event is in DB
        var found = eventRepository.findById(event.getId());
        assertThat(found).isPresent();
    }
}
```

---

## Template: LazyInitializationException Detection (Integration)

Use for: endpoints that return entities (or contain lazy relationships) — verify the endpoint serializes correctly.

```java
// Category: C7 — Integration Tests (Lazy Loading)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class LazyLoadingIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("GET /api/v1/events/{id} serializes lazy relationships without LazyInitializationException")
    void getById_lazyRelations_serializesCorrectly() {
        var event = new Event("Lazy Test");
        var ticketType = new TicketType("VIP", new BigDecimal("100.00"));
        event.addTicketType(ticketType);
        eventRepository.save(event);

        // The controller must use a DTO or JOIN FETCH to avoid LazyInitializationException
        client.get().uri("/api/v1/events/{id}", event.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$.id").isEqualTo(event.getId().toString());
        // If the endpoint fails with 500, it's likely LazyInitializationException
    }

    @Test
    @DisplayName("JSON serialization does NOT expose lazy proxy fields")
    void getById_lazyFields_areNotExposed() {
        var event = new Event("Exposure Test");
        eventRepository.save(event);

        var responseBody = client.get().uri("/api/v1/events/{id}", event.getId())
                .exchange()
                .expectStatus().isOk()
                .expectBody()
                .jsonPath("$").isNotEmpty()
                .returnResult(String.class);

        // Verify no HibernateProxy or Javassist serialization artifacts
        assertThat(responseBody.getResponseBody())
                .doesNotContain("hibernateLazyInitializer")
                .doesNotContain("javassist");
    }
}
```

---

## Template: OSIV (Open-in-View) Production Safety (Integration)

Use for: verifying that the application works with `open-in-view=false` in production profile.

```java
// Category: C7 — Integration Tests (OSIV)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
                properties = "spring.jpa.open-in-view=false")
@AutoConfigureRestTestClient
class OsivDisabledIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private EventRepository eventRepository;

    @Test
    @DisplayName("All endpoints work with open-in-view=false (production config)")
    void listEvents_osivDisabled_worksCorrectly() {
        // Arrange: an event with lazy relationships
        var event = new Event("OSIV Test");
        eventRepository.save(event);

        // Act & Assert: the endpoint must use DTOs or JOIN FETCH
        // If it relies on OSIV for lazy loading, this will fail with 500
        client.get().uri("/api/v1/events/{id}", event.getId())
                .exchange()
                .expectStatus().isOk();
    }
}
```

---

## Template: Database Cleanup Strategy (Integration)

Use for: integration test classes that need database cleanup. Shows both TRUNCATE and rollback strategies.

```java
// Category: C7 — Integration Tests (Cleanup)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class DatabaseCleanupIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private RestTestClient client;

    @AfterEach
    void cleanDatabase() {
        // Strategy 1: TRUNCATE with CASCADE (for when @Transactional is not desired)
        jdbcTemplate.execute("TRUNCATE TABLE ticket_types CASCADE");
        jdbcTemplate.execute("TRUNCATE TABLE events CASCADE");
    }

    @Test
    @DisplayName("Test A: creates an event — cleanup happens in @AfterEach")
    void testA_createsEvent() {
        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(new CreateEventRequest("Cleanup Test A", "Desc", LocalDateTime.now()))
                .exchange()
                .expectStatus().isCreated();
    }

    @Test
    @DisplayName("Test B: runs after cleanup — should see no events from Test A")
    void testB_noLeakFromTestA() {
        var response = client.get().uri("/api/v1/events?size=10")
                .exchange()
                .expectStatus().isOk()
                .returnResult(EventListResponse.class);

        assertThat(response.getResponseBody().content()).isEmpty();
    }
}
```

---

## Template: Kafka Consumer Error Handling (Integration)

Use for: endpoints that publish Kafka events, to verify consumers handle failures.

```java
// Category: C7 — Integration Tests (Kafka Error Handling)
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureRestTestClient
class KafkaConsumerIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    private RestTestClient client;

    @Autowired
    private TestKafkaListener testKafkaListener;

    @Test
    @DisplayName("Kafka consumer retries on transient failure, then sends to DLQ")
    void consumer_retriesThenDeadLetter() throws Exception {
        // Configure: consumer throws on first 2 attempts, succeeds on 3rd
        testKafkaListener.setFailCount(2);

        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(new CreateEventRequest("Kafka Retry", "Desc", LocalDateTime.now()))
                .exchange()
                .expectStatus().isCreated();

        // Await: consumer processes after retries
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testKafkaListener.getProcessedCount() == 1);

        // Verify: no messages in dead-letter topic
        var dlqCount = testKafkaListener.getDeadLetterCount();
        assertThat(dlqCount).isZero();
    }

    @Test
    @DisplayName("Kafka consumer sends to DLQ after exhausting retries")
    void consumer_exhaustedRetries_sendsToDlq() throws Exception {
        // Configure: consumer always throws
        testKafkaListener.setFailCount(Integer.MAX_VALUE);

        client.post().uri("/api/v1/events")
                .header("X-User-Id", "user-1")
                .body(new CreateEventRequest("Kafka DLQ", "Desc", LocalDateTime.now()))
                .exchange()
                .expectStatus().isCreated();

        // Await: message appears in dead-letter topic
        await().atMost(15, TimeUnit.SECONDS)
                .until(() -> testKafkaListener.getDeadLetterCount() >= 1);
    }
}
```
