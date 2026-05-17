# Docs Sync — Lessons Learned

## 2026-05-14

- This project uses OpenAPI annotations on controllers as the source of truth for API docs rather than standalone `api-contracts/*.md` files. When adding `@ApiResponse` annotations, no separate contract doc update is needed.
- DTO `@Size(max = N)` validation is a code-level enforcement of existing `@Column(length = N)` constraints already documented in `data-model.md`. No doc update needed unless the actual DB schema changes.
- Integration test count changes should be tracked in `UC-001-event-catalog.md` Task 4 and the Execution Log.
- The ProblemDetail assertion pattern (verifying `type`, `title`, `status`, and `errors` array) is novel enough to warrant its own subsection in `testing-strategy.md`.
- Build config changes (`failOnWarning`, compiler flags) must be reflected in `setup.md` "Spring Boot 4 Note". The sentence describing test compilation was out of date after enabling `failOnWarning=true`.
- Code examples in `testing-strategy.md` must not use deprecated APIs (`.asList()`) since `failOnWarning=true` now enforces zero-warnings in test code too.

## 2026-05-15

- Private helper methods in service classes never require doc updates. They don't change the public API, add use case scenarios, introduce domain concepts, or alter architectural decisions. Only public/protected method changes that alter observable behavior warrant UC Execution Log updates.
- The `service` category in the mapping table should be interpreted with scrutiny: a change to a service file does not automatically trigger a UC update — only when the change affects observable behavior (new public method, new use case flow, completed task). Pure refactoring (extract method, rename, inline) is invisible to docs.

## 2026-05-15

- An existing `data-model.md` entry does not automatically mean no doc update is needed — verify that the described fields match exactly. In this case `ticket_types` table was already complete, but the pattern of checking field-by-field should be followed every time.
- When a new entity's fields are a subset of an existing documented table, the entity may have been implemented as a simplified version: check the actual entity class for extra columns (e.g., `quantityAvailable` + audit fields) before declaring a mismatch.
- Top-level vs. nested endpoints (`/api/v1/ticket-types` vs. `/api/v1/events/{eventId}/ticket-types`) affect different controller files but both map to the `endpoint` category. The doc impact is identical — no need to differentiate in the mapping table.
- Glossary updates are easy to forget. Every new entity that represents a core domain concept should trigger a glossary check, even when the entity name seems "obvious" from the existing UC text.
- PUT semantics vs PATCH: `@BeanMapping(nullValuePropertyMappingStrategy = IGNORE)` is wrong for PUT because PUT replaces the entire resource. Only use `IGNORE` for PATCH endpoints. The user caught this — don't assume partial update without confirming the HTTP verb.
- Service bug fixes that change observable behavior (e.g., 500 → 409) warrant UC Execution Log entries, even though the endpoint contracts remain the same. The fix fixes a bug in the implementation, not the API spec.
- Build tool config changes (PMD ruleset, SpotBugs exclusions) rarely need doc updates. The tool config files themselves are the source of truth. Only update `setup.md` if a new tool is added or a significant behavioral change occurs.
- New exception classes following existing project patterns (extending RuntimeException, in exceptions package) do not warrant doc updates. They are implementation details, not API contracts or data model changes.

## 2026-05-16

- Observability instrumentation (OpenTelemetry, Micrometer counters, structured JSON logging) that doesn't change external API contracts, setup procedures, or test patterns only needs UC documentation — no updates to `setup.md`, `testing-strategy.md`, or `AGENTS.md` needed.
- The `dependency` category should distinguish BOM-managed Spring Boot starters (no version to track, no doc update) from standalone libraries with explicit version pins (update `setup.md` + `AGENTS.md` `spring-boot-starter-opentelemetry` is managed by the Spring Boot BOM).
- When a planned tech-stack item from `PROJECT_CHARTER.md` is implemented (e.g., OpenTelemetry was already listed under Observability), the charter does not need updating — it was already defined as in-scope.
- UC-001 serves as the single source of truth for cross-service observability changes. Applying structured JSON logging, tracing, and metrics to both `event-catalog-service` and `search-service` is appropriately documented in a single UC task section rather than duplicated across docs.

## 2026-05-17

- Kafka message records (like `EventPublished`) are message contracts, not JPA entities. The `data-model.md` only documents database tables, not Kafka schemas. A field addition to a message record does not trigger a data-model.md update.
- When adding a completed entry to a UC Execution Log for a task that already has a "☐ Pending" placeholder row, the old placeholder must be explicitly removed — otherwise the task appears twice (once as Pending, once as Completed).
- `docker-compose.{service}.yml` files for individual service isolation are already covered by the generic pattern in setup.md ("Each service has a docker-compose.{service}.yml"). No deployment.md or setup.md update is needed for new docker-compose files that follow this pattern.
- The `aspectjweaver` dependency is BOM-managed by Spring Boot 4.0.6 (no explicit version pin). Per previous lesson, no setup.md/AGENTS.md version table update is needed.
