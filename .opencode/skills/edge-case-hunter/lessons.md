# Edge Case Hunter — Lessons Learned

> Auto-populated after each invocation. Helps the skill improve over time.

## 2026-05-15
- **Endpoint**: POST /api/v1/categories
- **What worked**: CATEGORIES.md catalog was comprehensive — found 13 gaps across all 7 categories. Analysis heuristics correctly flagged userId descartado, FK violation on soft delete, and missing @Version.
- **What didn't**: Step 5 told the agent to create new test files when existing files already existed. The 4 new files were unnecessary noise.
- **New patterns discovered**: Race condition test must use `ExecutorService` + `AtomicInteger` counters; `returnResult(Object.class).getStatus().value()` is the way to get status codes from concurrent RestTestClient calls.
- **Fix applied**: Step 5 now instructs "ALWAYS check for existing test files first, append to them, only create new file if none exists."
- **False positives**: None in this run. The Paginação/Pagination heuristics flagged the `Page<T>` exposure correctly — it's a genuine issue even if the project considers it acceptable.

## 2026-05-16
- **Context**: Observability & Documentation (Task 5) — MetricsConfig, EventMetricsService, KafkaTracingConfig, logback-spring.xml
- **What worked**: CATEGORIES.md catalog C7(1) correctly identified missing Prometheus registry dependency — endpoint exposed but no meter registry on classpath. Heuristics for `@Counted` vs explicit Counter injection correctly classified the project's choice to avoid Spring AOP complexity.
- **What didn't**: C4(7) "Exposed Actuator endpoints" flagged Prometheus exposure as a risk — but this is intentional for monitoring. The heuristic should distinguish between "leaking sensitive data" (env, beans) vs "intentional monitoring endpoints" (health, prometheus, metrics). Suggest a note in CATEGORIES.md distinguishing sensitive vs safe actuator endpoints.
- **New patterns discovered**: When analyzing observability infrastructure (metrics, tracing, logging) rather than REST endpoints, the 7-category model still works but needs adaptation — C3 (Concurrency) maps to Micrometer Counter thread-safety, C7 (Integration) maps to dependency availability at startup. Infrastructure changes don't always have a single HTTP method/path to analyze.
- **False positives**: None. The `MeterRegistryCustomizer` import LSP error was an environment issue (LSP classpath resolution), not a compilation error — this was confirmed by the search-service having the same import pattern.

## 2026-05-17
- **Context**: Search service observability — structured JSON logging (ECS), OpenTelemetry tracing, Kafka listener observation
- **What worked**: CATEGORIES.md C7 integration check correctly identified that existing tests serve as validation for structured logging configuration — the `SearchControllerWebTest` test output showed `{"@timestamp":"...","ecs":{"version":"8.11"}}` lines, confirming ECS JSON logging was active without any test changes.
- **What didn't**: The 7-category catalog focuses on REST endpoints with controllers/services/entities. This task was purely config changes (pom.xml + yml) with no new Java files. The edge-case analysis was minimal — no DTOs, no entities, no repositories to analyze. The catalog could benefit from a "Configuration change" category.
- **New patterns discovered**: Structured logging (ECS) can be verified during existing test runs — JSON output in the `@WebMvcTest` logs confirms the format is active. No need for a dedicated test if the observation is a side effect. Also: `logging.structured.format.console=ecs` is the correct property (NOT `logging.structured.format=json`).
- **False positives**: None.
- **Endpoint**: N/A — configuration-only change

<!--
Template for each entry:
## YYYY-MM-DD
- **Endpoint**: {HTTP} {path}
- **What worked**: ...
- **What didn't**: ...
- **New patterns discovered**: ...
- **False positives**: ...
-->
