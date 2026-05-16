# Common Failures

> Failure patterns per agent, diagnosis, and resolution.

---

## Orchestrator

### O-F01: Incomplete spec dispatched to code-writer

**Symptom:** quality-runner returns violations that could have been prevented
by a complete spec (missing Edge Cases, missing APIs to Avoid).

**Diagnosis:** Spec validation checklist was skipped or passed incompletely.

**Resolution:**
- Always run the 4-item validation checklist before dispatching
- If APIs to Avoid is empty, re-query librarian (insist on deprecation info)
- If Edge Cases is generic, add PMD/Cognitive complexity thresholds

### O-F02: Wrong subagent selection

**Symptom:** Subagent returns "cannot do this" or produces wrong output.

**Diagnosis:** Intent classification was wrong (e.g., sent implementation to
librarian, or research to code-writer).

**Resolution:**
- Use the routing heuristics table explicitly before each dispatch
- If unclear, ask user (max 3 questions) rather than guessing
- When in doubt, favor explore + librarian over direct code-writer dispatch

### O-F03: Error propagation from subagent to final output

**Symptom:** User sees an error from a subagent in the final synthesis.

**Diagnosis:** Missing result validation before synthesis.

**Resolution:**
- Always validate subagent output before forwarding to next phase
- If subagent returns error/status=fail, do NOT forward to user
- Retry once with more specific instructions, or report as failure

## Code Writer

### C-F01: Compilation fails on first attempt

**Symptom:** `mvn compile` fails with compilation errors.

**Possible Causes:**
- API signature mismatch (guessed instead of using javap)
- Import missing or wrong
- Method signature different from expected

**Resolution:**
- Run javap on every Spring/Kafka/JPA API before use
- Read the compilation error carefully — it usually tells you the exact fix
- Check for Spring Boot 4 migration issues (see migration-guide.md)

### C-F02: Checkstyle/PMD violations on first pass

**Symptom:** quality-runner returns code style or complexity violations.

**Possible Causes:**
- Missing Javadoc on public method
- Cyclomatic complexity > 10
- Wildcard imports
- Line length > 120 chars

**Resolution:**
- Run `mvn spotless:apply` before submitting to quality-runner
- For Javadoc: check if the class/method has one of the exempt annotations
- For complexity: extract helper methods — aim for ≤8 to leave margin
- Use `@Builder.Default` for DTO defaults instead of complex constructors

### C-F04: Bash commands denied (./ prefix mismatch)

**Symptom:** `mvnw` commands silently fail — no error output, but commands
don't execute. Agent reports "command not found" or "permission denied".

**Diagnosis:** The OpenCode runtime strips `./` from command invocations
before matching against permission patterns. If the permission block only
has `"./mvnw compile*"`, the runtime sees `mvnw compile ...` and finds no
match, falling through to `"*": deny`.

**Resolution:**
- Always add patterns both WITH and WITHOUT `./` prefix for mvnw commands:
  ```yaml
  bash:
    "./mvnw compile*": allow
    "mvnw compile*": allow
  ```
- Audit all agent .md files for this pattern whenever bash permissions are modified

### C-F05: Skill tool not permitted in permission block

**Symptom:** Agent's prompt says "Load skill({ name: ... })" but the skill
never loads. Agent may silently skip the skill-loading step or produce output
without the skill's analysis.

**Diagnosis:** The agent's frontmatter has a `permission:` block, which means
tools NOT listed are implicitly denied. If `skill:` is missing from the block,
the agent cannot load skills regardless of what the prompt instructs.

**Resolution:**
- Add a `skill:` subsection to the permission block:
  ```yaml
  permission:
    skill:
      "edge-case-hunter": allow
      "*": deny
  ```
- Verify that every agent that references `skill()` in its prompt has `skill:` in its permission block
- Check during agent-improver audits: run a grep for `skill(` in prompts, then verify the frontmatter

### C-F06: Edge cases not covered

**Symptom:** Tests miss null inputs, soft-deleted resources, pagination limits.

**Diagnosis:** edge-case-hunter skill was not loaded, or results were ignored.

**Resolution:**
- Always load `skill({ name: "edge-case-hunter" })` for new endpoints
- At minimum, cover: 404, 409, 422, pagination boundary, soft delete
- For POST endpoints: include validation error test cases
- For paginated endpoints: test size=0, size=1, size=max, cursor=null

## Librarian

### L-F01: Outdated API returned

**Symptom:** code-writer compiles against an API that doesn't exist or is
deprecated in Spring Boot 4.

**Diagnosis:** Search query didn't specify version, or relied on pre-2025 results.

**Resolution:**
- Always include version: "Spring Boot 4.0", "Spring Security 6.x"
- Verify dates on search results — prefer 2025-2026
- Cross-reference with `.opencode/references/migration-guide.md`
- Report confidence in output (✅ / ⚠️ / ❓)

### L-F02: Search latency >30s

**Symptom:** Orchestrator waiting too long for research phase.

**Diagnosis:** Too many queries, overly broad searches, or network issues.

**Resolution:**
- Limit to 3 targeted searches per task
- Use webfetch on known URLs instead of websearch when possible
- Cache results: if same API was researched this session, reuse

## Quality Runner

### Q-F01: False positive (reports violation that isn't real)

**Symptom:** code-writer investigates and finds no actual violation.

**Diagnosis:** Parser error or misread Maven output.

**Resolution:**
- Verify the reported file:line actually contains the alleged violation
- If false positive, report in next eval session for threshold adjustment
- Common source: multi-module projects where error from another module bleeds

### Q-F02: Step 0 fails but code-writer already ran spotless

**Symptom:** Spotless reformats files in Step 0 that should already be clean.

**Diagnosis:** code-writer skipped `mvn spotless:apply` before submitting.

**Resolution:**
- Report as process violation (code-writer must fix)
- Do NOT proceed to Step 1 until code-writer acknowledges and re-submits

### Q-F03: Pipeline timeout

**Symptom:** `mvn verify` exceeds timeout (~5 min).

**Diagnosis:** Full verification includes heavyweight integration tests.

**Resolution:**
- Use `-DskipTests` for the fast-lint check
- If tests are the bottleneck, check if Testcontainers are starting unnecessarily
- Consider splitting into unit-only and full verify steps

## Docs Updater

### D-F01: Wrong doc updated

**Symptom:** Changed a controller but updated data-model instead of api-contracts.

**Diagnosis:** Categorization error — endpoint change classified as entity change.

**Resolution:**
- Use the categorization table strictly (see docs-updater.md Step 2)
- If unsure, read the git diff of the file to determine if it's entity/endpoint/dto
- Cross-reference: if you changed a controller, api-contracts is ALWAYS affected

### D-F02: Overly broad edit (rewrote entire file)

**Symptom:** Instead of surgical edit, the entire doc file was replaced.

**Diagnosis:** edit tool was used without reading the existing content first.

**Resolution:**
- Always read the doc BEFORE editing
- Use edit() tool with oldString/newString for surgical changes
- Never use write() on an existing doc unless adding a new section

### D-F03: Cross-reference verification skipped

**Symptom:** AGENTS.md version doesn't match setup.md version.

**Diagnosis:** Step 4 (Verify Consistency) was skipped.

**Resolution:**
- Always run Step 4 after edits
- Check at minimum: AGENTS.md ↔ setup.md, AGENTS.md ↔ PROJECT_CHARTER.md
- If any mismatch, fix before closing
