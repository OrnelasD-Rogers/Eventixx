# Edge Case Hunter — Lessons Learned

> Auto-populated after each invocation. Helps the skill improve over time.

## 2026-05-15
- **Endpoint**: POST /api/v1/categories
- **What worked**: CATEGORIES.md catalog was comprehensive — found 13 gaps across all 7 categories. Analysis heuristics correctly flagged userId descartado, FK violation on soft delete, and missing @Version.
- **What didn't**: Step 5 told the agent to create new test files when existing files already existed. The 4 new files were unnecessary noise.
- **New patterns discovered**: Race condition test must use `ExecutorService` + `AtomicInteger` counters; `returnResult(Object.class).getStatus().value()` is the way to get status codes from concurrent RestTestClient calls.
- **Fix applied**: Step 5 now instructs "ALWAYS check for existing test files first, append to them, only create new file if none exists."
- **False positives**: None in this run. The Paginação/Pagination heuristics flagged the `Page<T>` exposure correctly — it's a genuine issue even if the project considers it acceptable.

<!--
Template for each entry:
## YYYY-MM-DD
- **Endpoint**: {HTTP} {path}
- **What worked**: ...
- **What didn't**: ...
- **New patterns discovered**: ...
- **False positives**: ...
-->
