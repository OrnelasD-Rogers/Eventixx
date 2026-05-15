# Edge Case Categories

This catalog organizes edge cases by category. Each entry includes the trigger condition, why it matters (real-world impact), and how Spring maps to each scenario.

---

## C1: Input Validation

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | Missing `@Valid` on request body | Controller parameter lacks `@Valid` or `@Validated` | Invalid data passes through, causes DB constraint violations or data corruption | `@Valid` on `@RequestBody` triggers `MethodArgumentNotValidException` |
| 2 | Missing field-level constraints | DTO record/class fields lack `@NotBlank`, `@Email`, `@Size`, `@Pattern`, etc. | Malformed input accepted, leading to injection or downstream errors | Jakarta Bean Validation annotations |
| 3 | `@PathVariable` with special characters | Path variable can contain `/`, `%`, or whitespace | Routing breaks, 404 on valid resources | `@PathVariable` splits on `/` by default; use `@MatrixVariable` or encode |
| 4 | Boolean query param with primitive `boolean` | `boolean active` instead of `Boolean active` | Missing param defaults to `false` silently — wrong filter applied | Use `Boolean` (wrapper) for optional boolean params |
| 5 | Numeric overflow in IDs | `@PathVariable long id` with value > `Long.MAX_VALUE` | 400 error, but no clear message; or silent truncation | Use `String` for large IDs, validate format |
| 6 | Missing `@RequestParam(required = false)` default value | Optional query param without explicit default | Client gets null pointer or wrong behavior | Always document and set defaults explicitly |
| 7 | Content-Type validation missing | Controller accepts `application/json` but doesn't enforce it | XXE attacks via XML content type to JSON endpoint | Use `@PostMapping(consumes = "application/json")` |
| 8 | Request body size unlimited | No `spring.servlet.multipart.max-request-size` | Memory exhaustion via large payloads | Configure `MaxUploadSizeExceededException` handler |
| 9 | Trailing slash mismatch (Spring Boot 3.x) | Client sends `/api/events/` but controller expects `/api/events` | 404 where 200 expected — silent breaking change | Spring Boot 3.x changed default `setUseTrailingSlashMatch=false` |
| 10 | Charset encoding not enforced | No `CharsetFilter` or `spring.http.encoding.force=true` | Non-ASCII data silently corrupted in DB | Configure `spring.http.encoding.force=true` |
| 11 | UUID format validation missing | `@PathVariable String id` for UUID without `@Pattern` | Invalid UUID strings accepted, cause 500 in JPA | Add `@Pattern(regexp = "...")` or use `UUID` type directly |

## C2: Soft Delete / Resource Not Found

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | GET returns soft-deleted resource | Service doesn't filter `WHERE deleted_at IS NULL` | Deleted data leaked to clients | Repository must have `@SQLRestriction("WHERE deleted_at IS NULL")` |
| 2 | JOIN leaks soft-deleted data | JPQL/ Criteria query joins without `deleted_at` check on joined table | Deleted related entities exposed via parent | Always add `AND r.deleted_at IS NULL` in JOINs |
| 3 | DELETE returns 200 on already-deleted | `DELETE /resource/{id}` called twice | Debate: idempotent (204 always) vs accurate (404 second time) | Document chosen behavior; 204 is safer |
| 4 | Restore (undelete) violates unique constraint | Soft-deleted record restored, unique column conflicts with active record | Unique constraint violation on restore | Use partial unique indexes: `CREATE UNIQUE INDEX ... WHERE deleted_at IS NULL` |
| 5 | Count includes soft-deleted | `COUNT(*)` in pagination or admin queries | Wrong totals, incorrect pagination metadata | Always filter `WHERE deleted_at IS NULL` in count queries |
| 6 | No permissions for restore/read-deleted | Any user can restore or view deleted records | Data recovery without authorization | Separate roles/scopes: `read:deleted`, `restore` |
| 7 | Cache returns deleted resource | CDN/API cache serves stale deleted data | Clients see deleted content | Vary cache key on `include=deleted` query param; invalidate on delete |
| 8 | Search index not updated on soft delete | Elasticsearch still has document after soft delete | Search returns deleted results | Publish `ResourceDeleted` event, consumer removes from index |
| 9 | Soft-delete without audit trail | `deleted_at` but no `deleted_by` or `deleted_reason` | Cannot trace who deleted what or why | Add `deleted_by VARCHAR` and `deleted_reason TEXT` columns |
| 10 | 410 Gone vs 404 for deleted resources | API always returns 404 for soft-deleted | Client can't distinguish "never existed" from "was deleted" | Return 410 Gone when resource is known to be deleted; 404 otherwise |

## C3: Concurrency / Race Conditions

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | Lost update (read-then-write) | Two threads read same entity, both modify, one overwrites other | Data loss: last writer wins, first writer's changes disappear | `@Version` field for optimistic locking; OR `PESSIMISTIC_WRITE` |
| 2 | `@Transactional` self-invocation | `this.method()` calls another `@Transactional` method in same class | Transaction annotation **silently ignored** — no rollback, no isolation | Always call through separate bean (different `@Service`) |
| 3 | `@Transactional` on private method | `@Transactional` on `private` or `final` method | Annotation silently ignored — proxy can't intercept | `@Transactional` must be `public` and `not final` |
| 4 | Checked exception doesn't rollback | `@Transactional` method throws `IOException` or custom checked exception | Transaction is COMMITTED despite exception | Set `@Transactional(rollbackFor = Exception.class)` |
| 5 | `@Transactional` spanning network I/O | Transaction held open across HTTP call, Kafka publish, or slow external API | Connection pool exhaustion, database lock contention | Commit before network call; use Outbox Pattern for events |
| 6 | `REQUIRES_NEW` via self-invocation | `this.method()` with `Propagation.REQUIRES_NEW` | Runs in outer transaction — `REQUIRES_NEW` ignored | Must be called from DIFFERENT bean |
| 7 | Shared mutable state in `@Service` | Service has instance variable set during request | Request A's data leaks into Request B's processing | Services must be stateless; use method-local variables |
| 8 | No idempotency on unsafe operations | `POST /api/payments` without idempotency key | Duplicate payment on network retry | Client sends `Idempotency-Key` header; server deduplicates |
| 9 | Atomic compound operation | Read → Check → Write is not atomic | Check passes for both threads, both write, invariant broken | Use `@Transactional` with proper isolation OR DB-level locking |
| 10 | No retry for `OptimisticLockException` | `@Version` present but no `@Retryable` | User gets 409 Conflict with no retry — poor UX | Add `@Retryable(OptimisticLockException.class, maxAttempts = 3)` |
| 11 | Lazy initialization race | `@PostConstruct` or `@Lazy` init without synchronization | Two threads both initialize → duplication or corruption | Spring manages `@Lazy` safely; manual lazy init needs `synchronized` |

## C4: Security / Authorization (OWASP API Top 10)

| # | Edge Case | OWASP ID | Why It Matters | Spring Boot Context |
|---|-----------|----------|----------------|---------------------|
| 1 | BOLA — Missing object-level authorization | API1 | User A can access User B's data by changing ID in URL | Verify ownership: `event.getUserId().equals(currentUserId)` |
| 2 | BFLA — Missing function-level authorization | API5 | Regular user can call admin endpoint | `@PreAuthorize("hasRole('ADMIN')")` on admin endpoints |
| 3 | BOPLA — Mass assignment / excessive data exposure | API3 | Return entity directly, exposing internal fields (passwords, internal IDs) | Use DTOs; never return `@Entity` from controller |
| 4 | No `X-User-Id` header on mutating endpoints | — | No way to identify who performed the action | Add `@RequestHeader("X-User-Id")` on POST/PUT/PATCH/DELETE |
| 5 | SQL injection via string concatenation | A03 | Attacker can read/delete database | Use JPA, parameterized queries, never `"WHERE id = " + id` |
| 6 | JWT algorithm confusion | — | Attacker uses "none" algorithm or weak HMAC secret | Validate algorithm; use strong secrets; reject "none" |
| 7 | Exposed Actuator endpoints | A05 | `/actuator/env` leaks DB URLs, API keys | Separate management port; restrict to `health`, `prometheus` |
| 8 | CORS misconfiguration | A05 | Any origin can call the API from browser | `@CrossOrigin(origins = "https://trusted.com")` not `"*"` |
| 9 | SSRF via user-controlled URL | API7 | Attacker redirects server to internal metadata endpoint (169.254.169.254) | Validate/block private IP ranges in `RestTemplate`/`WebClient` calls |
| 10 | Rate limiting missing | API4 | Attacker brute-forces endpoints, exhausts resources | Add rate limiting (Bucket4j, Resilience4j, or gateway-level) |
| 11 | Stack traces leaked to client | — | Exception exposes implementation details (class names, SQL) | `@ControllerAdvice` catches all; never return `e.getMessage()` to client |
| 12 | `ddl-auto=create-drop` in production | — | Hibernate drops all tables on restart | Set `spring.jpa.hibernate.ddl-auto=validate` in production; use Flyway |
| 13 | `Allow` header missing on 405 | — | Client can't know which HTTP methods are valid | Spring Boot adds this automatically for mapped endpoints; check if present |
| 14 | Auth failures not logged | A09 | Can't detect brute-force or credential stuffing attacks | Log all 401/403 with user identifier, IP, timestamp |

## C5: State Transitions

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | Invalid status transition | Endpoint accepts arbitrary status values | Order goes from PENDING to DELIVERED without CONFIRMED → SHIPPED | State machine: `ALLOWED_TRANSITIONS[currentStatus]` |
| 2 | Missing transition guard | No validation between current and new status | Business logic bypass: free shipping, skip approval | Guard clause at service layer: `if (!allowed.contains(newStatus)) throw ...` |
| 3 | Workflow without audit trail | Status changes not recorded | No accountability for state changes, can't debug | Create `status_history` table; log every transition with actor + timestamp |
| 4 | Async workflow without polling URL | `202 Accepted` returned but no way to check progress | Client doesn't know when workflow completes | Return `Location` header with polling URL |
| 5 | Race on concurrent status change | Two threads change status simultaneously | Both succeed, last one wins — inconsistent state | Optimistic locking on version; or `PESSIMISTIC_WRITE` on status check |
| 6 | Transactional boundary spans status changes | Status update + side effect (email, event) in same transaction | If side effect fails, status is rolled back — but user already saw 200 | Outbox pattern: commit status change, publish event separately |
| 7 | Cancelled resource can be reactivated | API allows PUT on cancelled/closed resource | Business continuity broken: reactivate without re-approval | Guard: check resource can be reactivated from current state |

## C6: Pagination / Limits

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | Offset pagination at depth | `OFFSET 100000 LIMIT 20` | Scans 100K rows — O(n) degradation, timeout | Switch to cursor-based pagination for production APIs |
| 2 | Missing max page size | Controller accepts `size` param without upper bound | `size=10000000` OOMs the server | `@Max(100) int size` or Spring's `Pageable` with max page size config |
| 3 | Non-unique sort field | Sort by `created_at` only, multiple records share timestamp | Duplicates across pages, or infinite loop in cursor pagination | Always append `id` as tie-breaker: `ORDER BY created_at, id` |
| 4 | Missing `has_more` flag | Response returns empty page instead of `has_more: false` | Client doesn't know when to stop paginating | Return `has_more` boolean in every paginated response |
| 5 | `COUNT(*)` on large table | Pagination metadata includes `totalElements` | Full table scan on every request | Approximate count or omit; or use a pre-computed counter |
| 6 | Sort by `updated_at` causes jumps | Records updated while user is scrolling | Items jump between pages — confusing UX | Use immutable sort key (`created_at`) for scroll; `updated_at` for refresh |
| 7 | Cursor invalid after delete | Pointed-to record is hard-deleted between pages | 404 or cursor error on next page | Cursor should be robust: if record gone, return empty page (no error) |
| 8 | Missing `Retry-After` header on 429 | Rate limit hit without retry guidance | Client retries immediately, making problem worse | Add `Retry-After` header with seconds until reset |
| 9 | Parallel fetch with cursor pagination | Client sends multiple cursor-based requests in parallel | Cursors are sequential by design — parallel fetches return same page | Document: cursor pagination is strictly sequential |
| 10 | Exposing `Pageable` internals | Returning `Page<T>` from Spring Data directly | Client depends on `pageable.sort.sorted`, `pageable.offset` — coupling | Wrap in DTO that exposes only intended fields |
| 11 | Pagination with soft delete | `WHERE deleted_at IS NULL` not applied in count + query | Wrong total count, deleted records visible | Always ensure both data query AND count query filter deleted |

---

## C7: Integration Tests

| # | Edge Case | Trigger | Why It Matters | Spring Boot Context |
|---|-----------|---------|----------------|---------------------|
| 1 | Transaction rollback across layers | Service saves entity + publishes Kafka event in same `@Transactional` | Event sent even if transaction rolls back — inconsistency | Use Outbox Pattern: event table in same DB, separate publisher |
| 2 | `@SpringBootTest` without rollback | Test class lacks `@Transactional` or cleanup strategy | Data leaks between tests, order-dependent failures | Each test must clean up: `@Transactional`, TRUNCATE, or cleanup script |
| 3 | LazyInitializationException | Service returns entity with lazy `@OneToMany` or `@ManyToMany` | Jackson serialization triggers lazy load outside transaction → 500 | Use DTOs, `JOIN FETCH`, or `@EntityGraph` |
| 4 | OSIV (open-in-view) active in production | `spring.jpa.open-in-view=true` (default) | DB connection held during serialization → pool exhaustion | Set `spring.jpa.open-in-view=false` in production config |
| 5 | Unique constraint violation under concurrency | Two parallel requests insert same unique value | One succeeds, other gets `DataIntegrityViolationException` (500) | Handle gracefully: 409 Conflict with clear message |
| 6 | Foreign key violation on soft delete | DELETE parent entity while children reference it | Constraint violation, error 500 | Soft-delete: set deleted_at on parent + children, OR validate children first |
| 7 | Testcontainer starts per class (no singleton) | `@Container` field without `static` modifier | Tests slow: container starts/teardown per test class | Use singleton pattern with `static` container + `@DynamicPropertySource` |
| 8 | Kafka consumer without retry/DLQ | `@KafkaListener` throws exception, no error handler | Message lost permanently | Configure `SeekToCurrentErrorHandler` + dead-letter topic |
| 9 | Flyway/Liquibase + entities diverge | Entity code changed without migration | `spring.jpa.hibernate.ddl-auto=validate` fails on startup | Generate migration before changing entity; run `validate` in CI |
| 10 | Migration version conflict in parallel tests | Two test classes run migrations concurrently | Flyway migration collision, test failure | Use separate schema per test class, or @TestExecutionListeners |
| 11 | H2 in-memory DB differs from production | Tests use H2, production uses PostgreSQL | Tests pass but SQL breaks in production (function, type differences) | Use Testcontainers with same DB image as production |
| 12 | Database cleanup not idempotent | `TRUNCATE` without `CASCADE` on tables with FK | Foreign key violation on truncate | `TRUNCATE ... CASCADE` or use `@Transactional` rollback |
| 13 | Missing `@DynamicPropertySource` | Test uses random port / hardcoded DB URL | Port conflicts, connection refused | `@DynamicPropertySource` with singleton container properties |
