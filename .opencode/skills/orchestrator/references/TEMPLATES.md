# Orchestrator — Task Delegation Templates

Templates for the `task()` tool. Use these when delegating to subagents.

## Template: explore

```
Task(
  description="Explore [area]",
  prompt="""
Map the codebase for [topic]. Find:
- All files related to [entity/feature]
- Controller endpoints handling [path/verb]
- Service methods, repositories, DTOs involved
- Existing tests and their coverage

Return:
- File paths with line numbers for key methods
- Dependencies between files
- Any patterns or conventions used
""",
  subagent_type="explore"
)
```

## Template: librarian

```
Task(
  description="Research [library/api]",
  prompt="""
Research current (2026) best practices for [topic]:
- Library: [name] version [number]
- Context: [what we're building, e.g. "Spring Boot 4 REST endpoint with pagination"]
- Key APIs we'll need: [list specific classes if known]

Search for:
1. Current API patterns (official docs, spring.io blog)
2. Deprecated APIs to avoid (what replaces them)
3. Migration guides from older versions
4. Code examples from 2025-2026 sources

Return findings in librarian report format.
""",
  subagent_type="librarian"
)
```

## Template: code-writer (full task spec)

```
Task(
  description="Implement [feature]",
  prompt="""
Goal: [1-line what to achieve]

Context:
- Service: [service-name]
- Files affected: [from explore, with paths]
- Pattern to follow: [reference existing similar code]

APIs to Use:
| API | Source | Confidence |
|-----|--------|------------|
| [from librarian research] | | |

APIs to Avoid:
| DON'T Use | Replacement | Reason |
|-----------|-------------|--------|
| [from librarian research] | | |

Conventions:
- [specific coding-rules.md sections relevant to this task]

Files to Modify/Create:
- [specific paths with expected changes]

Verification:
- Compile: mvn compile -pl services/[name]
- Quality checks: spotless, checkstyle, PMD, SpotBugs, tests

Edge Cases:
- [known risks from orchestrator analysis]
""",
  subagent_type="code-writer"
)
```

## Template: quality-runner

```
Task(
  description="Verify [service] quality",
  prompt="""
Run quality verification on services/[service-name].

Pipeline:
1. ./mvnw spotless:apply -pl services/[name]
2. ./mvnw checkstyle:check pmd:check pmd:cpd-check spotbugs:check -pl services/[name] -DskipTests
3. ./mvnw test -pl services/[name]

Report all failures with file:line, message, and suggested fix.
""",
  subagent_type="quality-runner"
)
```

## Template: docs-updater

```
Task(
  description="Sync docs after changes",
  prompt="""
Synchronize project documentation after code changes in [service-name].

Load skill({ name: "docs-sync" }) and execute full workflow:
1. git diff to identify changed files
2. Categorize changes
3. Map to affected docs
4. Apply surgical edits
5. Cross-reference versions
6. Save lessons learned

Report summary table of docs updated.
""",
  subagent_type="docs-updater"
)
```

## Template: Parallel Fan-Out (Phase 1 — Research)

```
// Launch simultaneously (single message with 2 task calls)
Task(description="Explore [X]", prompt="...", subagent_type="explore")
Task(description="Research [Y]", prompt="...", subagent_type="librarian")

// Wait for both → cross-reference → build task spec for code-writer
```

## Template: Sequential Pipeline (Phase 2-3 — Implement + Close)

```
1. Task(code-writer) → wait
2. Task(quality-runner) → wait
   If FAIL → Task(code-writer fix) → Task(quality-runner) → wait
3. Task(docs-updater) → wait
4. Synthesize final response
```
