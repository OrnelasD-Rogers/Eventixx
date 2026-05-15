---
name: edge-case-hunter
description: >-
  Analyzes REST API endpoints (controllers + services + entities + DTOs + repositories) to find MISSING edge cases, security vulnerabilities, concurrency bugs, state transition gaps, pagination issues, input validation holes, AND integration test gaps (Testcontainers, transactional rollback, lazy loading, OSIV, Kafka error handling, DB constraints) — then generates JUnit 5 test code for each uncovered scenario. Use this skill whenever the user asks about "edge cases", "missing tests", "coverage gaps", "what could break", "testes que faltam", "cenários de borda", "bug hunting", "encontre bugs", "preciso de mais testes", "me ajuda a testar", or during code review before merge. Also triggers when adding new endpoints, modifying existing endpoints, preparing test plans, discussing "concorrência" or "race condition", "validação de dados", "security test", "teste de segurança", or when the user says "what could go wrong?", "find bugs", "analisar endpoint" or asks for a specific test with "preciso de um teste para". NEVER let the user manually hunt for edge cases — run this skill to systematically analyze every endpoint against 7 categories: Input Validation, Soft Delete/Resource Not Found, Concurrency/Race Conditions, Security/Authorization, State Transitions, Pagination/Limits, Integration Tests. Outputs a full report with missing scenarios + JUnit 5 test code.
license: MIT
metadata:
  version: "1.0.0"
  domain: testing
  triggers: edge cases, missing tests, coverage gaps, what could break, bug hunting, teste que falta, cenario de borda, find bugs, what could go wrong, test plan, code review, encontre bugs, preciso de mais testes, me ajuda a testar, analisar endpoint, concorrencia, race condition, validacao de dados, security test, teste de seguranca, preciso de um teste para
  role: qa-engineer
  related-skills: docs-sync
---

# Edge Case Hunter

> Finds missing edge cases in REST endpoints and generates JUnit 5 test code for each uncovered scenario.

## Skill Location

This skill's internal files (references/) are at `<skill-path>`. Reference them with `<skill-path>/` prefix in all commands and file reads.

## When to Use

- User asks "find edge cases" or "what could go wrong?" for an endpoint
- User asks "missing tests" or "coverage gaps" or "testes que faltam"
- During code review before merge — systematically check for edge cases
- After adding/modifying endpoints — ensure all error paths are covered
- User asks "find bugs" or "bug hunting" in the API
- Preparing test plans for a sprint or release
- User asks "o que pode quebrar?" or "cenários de borda"

## Core Workflow

### Step 1: Parse the Target

The user provides a REST endpoint description. Extract:

| Piece | Example |
|-------|---------|
| HTTP Method | `GET`, `POST`, `PUT`, `PATCH`, `DELETE` |
| Path | `/api/v1/events/{id}` |
| What it does | "Finds event by ID", "Creates new event" |
| (optional) class name | `EventController` |

If only the endpoint string is given, find the controller class by searching for the path pattern in `@RequestMapping` / `@GetMapping` etc. annotations.

### Step 2: Read the Source Files

Read ALL of these files for the endpoint:

1. **Controller** — the `@RestController` handling the endpoint
2. **Service** — the `@Service` class(es) injected in the controller method
3. **Entity** — the `@Entity` class(es) involved
4. **DTO** — request/response DTOs (records)
5. **Repository** — the `@Repository` interface(es)
6. **Existing tests** — any test files for the controller/service/entity (glob for `*ControllerTest*`, `*ServiceTest*`, `*IntegrationTest*`, `*RepositoryTest*`)
7. **Test base classes** — look for `PostgresRepositoryTest`, `AbstractIntegrationTest`, or similar base classes that set up Testcontainers

For each file, read the FULL content — not just snippets. The analysis depends on understanding every annotation, relationship, and validation constraint.

If a controller has multiple endpoints, focus on the specific one the user asked about, but note other endpoints in the controller for context.

### Step 3: Analyze Against Edge Case Catalog

Open `<skill-path>/references/CATEGORIES.md` and systematically evaluate each category. There are 7 categories — do NOT skip any:

- **C1**: Input Validation
- **C2**: Soft Delete / Resource Not Found
- **C3**: Concurrency / Race Conditions
- **C4**: Security / Authorization
- **C5**: State Transitions
- **C6**: Pagination / Limits
- **C7**: Integration Tests

For C7 (Integration Tests), also check the existing test infrastructure:
- Is there a Testcontainers singleton base class? If not → **MISSING** (tests slow or flaky)
- Do tests clean up the database between runs? If not → **MISSING** (data leaks)
- Does the project use `@SpringBootTest` without database isolation? → **MISSING**
- Are there Kafka/event tests with proper retry/DLQ handling? → **MISSING**

**For each category:**
1. Read the category's checklist items
2. For each item, determine: **is this edge case already handled?**
   - Check: does the code have validation, guards, or tests for it?
   - Check: does the code MISS this edge case entirely?
3. Mark each as:
   - ✅ **COVERED** — handled in code or tests
   - ❌ **MISSING** — not handled, should be added
   - ⚪️ **N/A** — not applicable to this endpoint

Focus on what's MISSING — the skill is called "edge case hunter" because it hunts for what's not there.

### Step 4: Generate the Report

Output a report to the user in this exact format:

```markdown
## 🔍 Edge Case Report: {METHOD} {PATH}

### Summary
- **Total checks**: {N}
- **✅ Covered**: {N}
- **❌ Missing**: {N}
- **⚪️ N/A**: {N}

### Missing Edge Cases

#### 1. ❌ {Category}: {Short description}
**Why it matters**: {1-2 sentences about real-world impact}
**Location**: `File.java:{line}`
**To trigger**: {curl or Java code snippet}
**Suggested fix**: {brief fix description}

...

### Generated Tests
```

For each ❌ MISSING item, generate a complete JUnit 5 test method using templates from `<skill-path>/references/TEMPLATES.md`.

### Step 5: Write Test Code

**ALWAYS check for existing test files first.** Before creating a new test file:

1. Glob for `*{ControllerName}*Test*`, `*{ServiceName}*Test*`, `*Integration*Test*` in the project's test directory
2. If a matching file exists, **append** new tests to it (adding imports as needed)
3. Only create a new file if no existing test file covers the endpoint
4. Add section comments (e.g., `// --- Concurrency edge cases ---`) to separate groups of new tests

For each missing edge case, generate a complete JUnit 5 test method. Requirements:

- Annotate with `@DisplayName` describing the scenario
- Mock the service layer with `@MockitoBean` (Spring Boot 4) or `@MockBean`
- Use `RestTestClient` (NOT `TestRestTemplate`) for Spring Boot 4 projects
- Follow the test patterns from the project's existing tests
- Each test class at the top: include the category label as a comment
- Group related tests in the same file (e.g., all pagination edge cases in one file)

**Test type selection guide:**

| If the edge case involves... | Use this test type | Template from TEMPLATES.md |
|------------------------------|-------------------|---------------------------|
| Request validation, HTTP status codes, headers | `@WebMvcTest` (mocked service) | Web MVC templates |
| Service logic, state transitions, calculations | `@ExtendWith(MockitoExtension.class)` | Unit templates |
| Repository queries, JPQL, soft delete filtering | `@DataJpaTest` + Testcontainers | Repository templates |
| **Database constraints, transaction rollback, lazy loading, OSIV, Kafka, full-stack** | **`@SpringBootTest` + Testcontainers** | **Integration templates** |
| Concurrent requests, optimistic locking | `@SpringBootTest` + `ExecutorService` | Concurrency templates |

**For integration tests:** always use the project's existing Testcontainers base class (e.g., `PostgresRepositoryTest` or `AbstractIntegrationTest`). If none exists, generate one following the singleton container pattern and flag it as a finding.

### Step 6: Prioritize Findings

At the end of the report, order missing edge cases by risk:

| Priority | Criteria |
|----------|----------|
| 🔴 **Critical** | Security bypass, data loss, financial impact |
| 🟠 **High** | Race condition, state corruption, broken authorization |
| 🟡 **Medium** | Validation gap, inconsistent pagination, wrong status code |
| 🟢 **Low** | Missing edge case unlikely in production, cosmetic issues |

## Analysis Heuristics

These heuristics help you decide when an edge case is MISSING vs COVERED:

### Input Validation
- DTO uses `@NotBlank`, `@Email`, `@Size`, etc. → covered
- DTO is a record with no annotations → **MISSING**
- Controller has `@Valid` on parameter → covered
- No `@Valid` → **MISSING** (validation annotations won't fire)
- `@PathVariable` with `String` for UUID → need to check format

### Resource Not Found
- Service method calls `findById(id).orElseThrow(...)` → covered
- Service returns `Optional` without throwing → **MISSING** (returns null/empty to client)
- Controller handles 404 exception → covered
- No `ResourceNotFoundException` or similar → **MISSING**

### Soft Delete
- Entity has `deleted_at` field → soft delete is used
- Repository has `@SQLRestriction("WHERE deleted_at IS NULL")` → covered
- Service queries lack `deletedAtIsNull()` → **MISSING** (leaks deleted records)
- GET on soft-deleted resource returns 200 → **MISSING** (should return 404/410)

### Concurrency
- Entity has `@Version` → optimistic locking exists
- Service method with `@Transactional` is called from another bean → covered
- **PRIVATE** `@Transactional` method → **MISSING** (proxy skip)
- Self-invocation: `this.method()` calling `@Transactional` → **MISSING** (bypasses proxy)
- No `@Version` on entity with concurrent writes → **MISSING**

### Security
- Controller has `@PreAuthorize` or Spring Security filter → covered
- Endpoint mutates data without auth check → **MISSING**
- `@PathVariable` for resource owner ID without verifying ownership → **MISSING BOLA**
- No `@RequestHeader("X-User-Id")` on mutating endpoint → **MISSING**

### SQL Injection
Search ALL source files (repository, service, controller) for these anti-patterns:

- Any `jdbcTemplate.query(...)` or `jdbcTemplate.update(...)` with **string concatenation** (`+`, `String.format`, `MessageFormat`) → **MISSING** (SQL injection)
- Any `entityManager.createQuery(...)` or `entityManager.createNativeQuery(...)` with string concatenation → **MISSING**
- Any `@Query` annotation with `nativeQuery = true` AND string concatenation → **MISSING**
- Any `Statement` or `PreparedStatement` built manually → **MISSING** (should use JPA/Spring Data)
- Any `Specification` / `CriteriaBuilder` predicate that concatenates user input → **MISSING**
- Any dynamic `ORDER BY` or `SORT` clause built from user input without validation → **MISSING** (allows ORDER BY injection)
- Any `LIKE '%' + input + '%'` without escaping the `%` and `_` characters → **MISSING** (wildcard leak)
- `@Query` with named parameters (`:name`) or `?1` positional → covered
- `JdbcTemplate` with `?` placeholders → covered
- `EntityManager` with `setParameter(...)` → covered
- Spring Data JPA derived query methods (`findBy...`) → covered safely by default

### Pagination
- Controller accepts `Pageable` → pagination exists
- No `@Max` on `size` parameter → **MISSING** (resource exhaustion)
- Returns `Page<T>` directly → **MISSING** (leaks Pageable internals)
- No `hasMore` or next cursor in response → **MISSING** (client can't navigate)
- Cursor pagination without tie-breaker (`id`) → **MISSING** (duplicates with same timestamp)

### State Transitions
- Endpoint updates status without validating current status → **MISSING**
- State machine / transition map exists → covered
- No validation on status changes → **MISSING** (arbitrary transitions)

### Integration Tests (C7)
- Project has Testcontainers singleton base class (`static` container + `@DynamicPropertySource`) → covered
- No base class, each test starts its own container → **MISSING** (slow, flaky)
- Existing integration tests clean database between runs (`@Transactional`, TRUNCATE, or cleanup script) → covered
- No cleanup strategy → **MISSING** (data leaks between tests)
- Service uses `@Transactional` + Kafka/HTTP call in same transaction → **MISSING** (partial state on failure — needs Outbox Pattern)
- `spring.jpa.open-in-view=true` in production config → **MISSING** (risk of connection pool exhaustion)
- Kafka consumer tests exist without DLQ/retry verification → **MISSING**
- Entity has lazy relationships but no DTO (returns entity directly) → **MISSING** (LazyInitializationException risk)
- H2 in-memory DB used for tests but production is PostgreSQL → **MISSING** (SQL divergence)
- Flyway migrations exist, entity changes don't produce new migration → **MISSING** (schema drift)
- Edge case tests exist at ALL levels (unit + web + repository + integration) → covered
- Edge case tests exist but only at one level (e.g., only unit tests) → **MISSING** (missing integration verification)

## Self-Improvement

After each invocation, append to `<skill-path>/lessons.md` with:
- What worked well (which heuristics caught real gaps)
- What didn't (false positives or missed edge cases)
- New edge case patterns discovered during this run

This makes the skill smarter over time.
