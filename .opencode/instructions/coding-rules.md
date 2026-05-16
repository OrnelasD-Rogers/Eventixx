# Zero-Warnings Code Rules — Eventixx

Follow these rules to avoid `mvn verify` failures. Organized by tool.

---

## 1. Javadoc (Checkstyle)

EVERY public class/interface/record/enum → `/** ... */`
EXCEPT: @Service, @RestController, @Repository, @Component, @Entity,
        @Configuration, @Mapper, @SpringBootApplication

EVERY public method with >=2 lines → `/** @param ... @return ... */`
EXCEPT: @Override, @Test, @BeforeEach, @AfterEach, @BeforeAll, @AfterAll

EVERY public @Bean method → `/** ... */` ALWAYS (no exceptions)

Format:
```java
/** Short description. */
public Record method(...)
```

---

## 2. Imports (Checkstyle)

NEVER use wildcard imports:
```java
// DON'T
import org.springframework.web.bind.annotation.*;

// DO
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
```

NEVER leave unused imports. Remove them.

---

## 3. Line Length (Checkstyle)

Maximum: **120 characters** per line.
Break long lines before operators/dots:
```java
// DON'T
return eventRepository.findByCategoryIdAndDeletedAtIsNull(categoryId, Pageable.ofSize(20));

// DO
return eventRepository.findByCategoryIdAndDeletedAtIsNull(
        categoryId, Pageable.ofSize(20));
```

---

## 4. Indentation & Whitespace (Checkstyle + .editorconfig)

- **4 spaces** per indent (no tabs)
- No trailing whitespace
- File must end with a newline
- LF line endings
- UTF-8 charset

---

## 5. DTOs (Project convention)

ALWAYS use **Java records** for DTOs. NEVER use @Data/@Getter/@Setter:
```java
// DON'T
@Data @Builder
public class EventResponse {
    private UUID id;
    private String name;
}

// DO
@Builder
public record EventResponse(UUID id, String name) {}
```

Use @Builder.Default for default values:
```java
public record SearchCriteria(
    @Builder.Default @Max(100) int size = 20,
    String query
) {}
```

---

## 6. Controllers (Project convention)

Return DTO **directly**, NEVER ResponseEntity<>:
```java
// DON'T
public ResponseEntity<EventResponse> getById(@PathVariable UUID id) {
    return ResponseEntity.ok(service.findById(id));
}

// DO
public EventResponse getById(@PathVariable UUID id) {
    return service.findById(id);
}
```

Add @RequestHeader("X-User-Id") on every mutating endpoint:
```java
public EventResponse create(
        @RequestBody @Valid CreateEventRequest request,
        @RequestHeader("X-User-Id") String userId
) {
```

Add @Tag/@Operation/@ApiResponse:
```java
@Tag(name = "Events")
@RestController
@RequestMapping("/api/v1/events")
public class EventController {
    @GetMapping("/{id}")
    @Operation(summary = "Get event by ID")
    @ApiResponse(responseCode = "200", description = "Event found")
    @ApiResponse(responseCode = "404", description = "Event not found")
    public EventResponse getById(@PathVariable UUID id) {
```

---

## 7. Complexity (PMD)

- **CyclomaticComplexity** ≤ 10
- **CognitiveComplexity** ≤ 15

Extract private helper methods when a method becomes complex:
```java
public SearchResult search(SearchCriteria criteria) {
    var builder = buildBaseQuery(criteria);
    applyFilters(builder, criteria);
    applySort(builder, criteria);
    applyCursor(builder, criteria);
    return executeSearch(builder, criteria);
}
```

---

## 8. Exception Handling (PMD + SpotBugs)

NEVER catch generic Exception without logging and wrapping:
```java
// DON'T
try { ... } catch (Exception e) { throw new RuntimeException(e); }

// DO
try { ... }
catch (RuntimeException e) {
    log.warn("Search failed: {}", e.getMessage());
    throw new SearchUnavailableException("Search temporarily unavailable", e);
}
```

Use domain-specific exceptions:
- `ResourceNotFoundException` (404)
- `ConflictException` (409)
- `BusinessException` (422)

All extend RuntimeException with ProblemDetail support.

---

## 9. Lombok + MapStruct

@RequiredArgsConstructor for DI (all fields private final):
```java
@Service
@RequiredArgsConstructor
public class EventService {
    private final EventRepository eventRepository;
```

@Builder on entities needs @NoArgsConstructor(PROTECTED) + @AllArgsConstructor(PRIVATE):
```java
@Entity
@Builder
@NoArgsConstructor(access = PROTECTED)
@AllArgsConstructor(access = PRIVATE)
public class Event { ... }
```

MapStruct: use config = MapStructConfig.class, NEVER @Autowired:
```java
@Mapper(config = MapStructConfig.class)
public interface EventMapper {
    EventResponse toResponse(Event event);
}
```

---

## 10. SLF4J (Checkstyle + Project convention)

NEVER use System.out / System.err:
```java
// DON'T
System.out.println("Event created: " + id);

// DO
log.info("Event created: {}", id);
```

Use @Slf4j on every class that needs logging.

---

## 11. @Transactional (ArchUnit + Spring)

ALWAYS public. NEVER final.
```java
// Class level for reads
@Service
@Transactional(readOnly = true)
public class EventService {

    // Override for writes
    @Transactional
    public EventResponse create(CreateEventRequest request) { ... }
}
```

---

## 12. Soft Delete (Project convention)

ALL entities: @SQLRestriction + @SQLDelete + deletedAt field.
ALL queries: filter `WHERE deleted_at IS NULL`.

---

## 13. Architecture Layers (ArchUnit)

- `entities` → NEVER import from `controllers`, `services`, or `dto`
- `repositories` → NEVER import from `controllers`
- `dto` → NEVER import from `entities`
- No cyclic dependencies between packages
- `controllers` → depends on `services` and `dto`
- `services` → depends on `repositories`, `entities`, `dto`, `exceptions`

---

## 14. .editorconfig

Run `mvn editorconfig:format` to auto-fix formatting violations.
The build fails at `verify` if .editorconfig rules are violated.

---

## 15. Fast-Fail Workflow

Antes do `mvn verify` completo (~37s), execute a análise estática primeiro (~7s):

```bash
./mvnw spotless:apply -pl services/<service> && \
  ./mvnw checkstyle:check pmd:check pmd:cpd-check spotbugs:check \
    -pl services/<service> -DskipTests
```

Isso falha rápido em violações de formatação/lint sem esperar os testes.

---

## 16. Test Code (Project convention)

Use Spring Boot 4 migration patterns:
- `@MockitoBean` (NOT `@MockBean`)
- `RestTestClient` (NOT `TestRestTemplate`)
- `@DynamicPropertySource` with singleton containers (NOT `@ServiceConnection`)
- `-parameters` is already configured — no extra config needed

Test directory structure:
```
src/test/java/com/eventixx/<service>/
├── unit/           # JUnit 5 + Mockito, no Spring
├── web/            # @WebMvcTest + @MockitoBean
├── repository/     # @DataJpaTest + Testcontainers
├── integration/    # @SpringBootTest + Testcontainers
└── arch/           # ArchUnit rules
```
