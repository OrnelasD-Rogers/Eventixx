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

<!--
Template for each entry:
## YYYY-MM-DD
- **Endpoint**: {HTTP} {path}
- **What worked**: ...
- **What didn't**: ...
- **New patterns discovered**: ...
- **False positives**: ...
-->
