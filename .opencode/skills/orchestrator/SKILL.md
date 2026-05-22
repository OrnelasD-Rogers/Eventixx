---
name: orchestrator
description: >-
  Decomposition and delegation workflow for the orchestrator agent. Use when
  the task requires splitting into subtasks, routing to multiple subagents,
  or parallel execution. Provides task spec templates and routing heuristics.
license: MIT
metadata:
  version: "1.0.0"
  domain: orchestration
  triggers: complex task, multi-step, parallel delegation, decomposition, routing
  role: orchestrator
  related-skills: edge-case-hunter, docs-sync, javap-inspector
---

# Orchestrator

Decomposition and delegation workflow. Load this when the task requires more
than a single subagent call.

## When to Use

- Task touches multiple services or domains
- Can be parallelized (independent subtasks)
- Requires phased execution (research → implement → close)
- User request is ambiguous and needs decomposition

## Core Workflow

### Step 0: Recall Past Context

At the START of every session, BEFORE parsing the request, run:

```
memory_search(query="<topic>")
```

This recalls past decisions, lessons learned, and known error patterns.
Apply relevant context before planning any work.

Skip only if the user explicitly says "ignore memory".

### Step 0b: Load Lessons Learned

Check if `<skill-path>/lessons.md` exists. If yes, read and apply relevant
guidance.

### Step 1: Parse & Classify

Read the user request. Classify the intent:

| Intent | Definition | Example |
|--------|-----------|---------|
| Implementation | Write or modify Java code | "Adiciona endpoint X no event-catalog" |
| Research | Understand codebase or APIs | "Como funciona o sistema de busca?" |
| Debug | Fix a bug or error | "Erro 500 no POST /api/v1/reservations" |
| Review | Code review or audit | "Revisa o PR do payment-service" |
| Documentation | Doc-only changes | "Atualiza o data-model com a nova tabela" |

### Step 2: Decompose

Break the request into atomic, independent subtasks. Build a DAG:

```
Request: "Adicionar endpoint de busca por categoria no event-catalog-service"

Decomposition:
  A: explore event-catalog-service structure (independent)
  B: librarian search Spring Data JPA Specification best practice 2026 (independent)
  └─→ A + B → C: code-writer implement endpoint (depends on A, B)
       └─→ D: quality-runner verify (depends on C)
             └─→ E: docs-updater sync docs (depends on D)
```

Explore can be called **multiple times** at different thoroughness levels:
- `thoroughness=quick` — directory structure, signatures, key config (default for Phase 1)
- `thoroughness=deep` — full file content (opt-in, when implementation needs it)
- `thoroughness=verify` — targeted lookup (opt-in, during debugging)

This avoids dumping hundreds of irrelevant lines in the initial research pass.

Rules:
- Max 5 subtasks per request
- If 1 subagent solves it → 1 subtask (no over-decomposition)
- Ask user before decomposing if the request is too vague

### Step 3: Route

Select subagents using this matrix:

| Subtask Type | Subagent | Strategy |
|-------------|----------|----------|
| Codebase discovery | `explore` | Single or parallel |
| Web research | `librarian` | Single or parallel |
| Write/modify code | `code-writer` | Sequential (after research) |
| Run quality checks | `quality-runner` | Sequential (after code) |
| Update docs | `docs-updater` | Sequential (after quality passes) |

### Step 4: Delegate

Use the Task tool. For non-trivial code-writer tasks, include all 8 sections:

```
Task(
  description="brief (3-5 words)",
  prompt="""
Goal: [1-line what to achieve]
Context: [files/services affected]
APIs to Use: [from librarian research]
APIs to Avoid: [from librarian research]
Conventions: [relevant coding-rules.md sections]
Files to Modify/Create: [specific paths]
Verification: [what quality-runner will check]
Edge Cases: [known risks from edge-case-hunter]
""",
  subagent_type="code-writer"
)
```

Include TWO new sections at the top of every code-writer task prompt:

1. **Trace Context** — for traceability across agents:
```
## Trace Context
- parent_trace_id: <session-trace-id>
- YOUR_trace_id: <generate a unique ID>
```

2. **SKILLS** — skills the code-writer must load:
```
## SKILLS
SKILLS: file:.opencode/skills/edge-case-hunter/SKILL.md, file:.opencode/skills/javap-inspector/SKILL.md
```

Delegation rules:
- Phase 1 (research): `explore` + `librarian` in parallel
- Phase 2 (implement): `code-writer` → `quality-runner` sequential loop
  - **Reuse task_id**: save the task_id from the first code-writer call.
    On retry, pass the same task_id to preserve the code-writer's context.
  - **Trust code-writer**: do NOT validate output before quality-runner.
    The quality-runner catches all issues.
- Phase 3 (close): `docs-updater` once, after all pass

### Quality-Runner 2-Pass Workflow

The quality-runner now uses a **3-step pipeline** instead of a single `mvn verify`:

1. **Fast-Lint** (~3s): spotless:apply + checkstyle + pmd + spotbugs (all -DskipTests)
2. **Static Analysis** (~7s): `verify -DskipTests` — runs full quality pipeline without tests
3. **Tests** (~15-37s): `mvn test` — runs tests separately

**Why:** If tests fail in `mvn verify`, static analysis never runs (Maven aborts at test
phase). This caused 3+ loop iterations. With 2-pass, the code-writer receives ALL
violations (static + test) in the FIRST failure report, reducing loops to 1-2.

**How the orchestrator should handle quality-runner results:**
- If any step fails: forward ALL failure details (static violations + test errors) to code-writer
- Do NOT filter or prioritize — let code-writer fix everything in one iteration

### Step 5: Collect & Validate

After each subagent returns:
- Verify output format matches expectations
- If result is incomplete → retry with more specific instructions (max 1 retry)
- If result is wrong → report back to the subagent with specific feedback

### Step 6: Synthesize

After all phases complete:
1. Combine results from all subagents
2. Resolve any conflicts (e.g., librarian and explore disagree)
3. Produce a single cohesive response to the user

## Constraints

### MUST DO
- Decompose before delegating
- Run research phase (explore + librarian) in parallel
- Include task spec with all 8 sections for code-writer
- Reference explore evidence types when building task specs (DIRECT = trust, INFERRED = verify if high-risk)
- Run docs-updater once at the end, never inside the code loop
- Max 1 retry per failed subagent result

### MUST NOT DO
- Execute work directly (edit, bash, etc.)
- Delegate without task spec
- Skip research phase for implementation tasks
- Run docs-updater inside the code-writer loop
- Over-decompose simple requests

## Routing Heuristics

If the request... | Then...
---|---
Touches 1 service, <5 files | 1 code-writer session
Touches 2+ services, independent | Fan-out parallel code-writer sessions
Is pure investigation | explore only (skip code/docs phases)
Is adding a new endpoint | Full pipeline: explore + librarian → code-writer → quality-runner → docs-updater
Is a bug fix | explore (trace) → code-writer → quality-runner
Is a dependency update | librarian (research impact) → code-writer → quality-runner → docs-updater
The librarian now returns a **✅ Recommended** section in its output.
Trust this recommendation — it considers project context. Still verify
"APIs to Avoid" before building the task spec.

Is about agent improvement/audit | agent-improver (standalone, no pipeline needed)
Is about improving reliability | agent-improver (standalone, no pipeline needed)

**Free-Text Relay Flow for agent-improver:**
```
1. task(agent-improver, task_id=NEW)
   → agent-improver returns plain text report with ## Plano de Ação Proposto
2. Present report to user in free text (do NOT use question() tool)
3. Ask user: "O que você deseja fazer?" — free text response
4. task(agent-improver, task_id=SAME, "Decisão do usuário: <free text>")
   → agent-improver resumes with full context, implements or adjusts
5. Present final result to user
```

**Key difference from old flow:**
- Old: `==QUESTION==` block → `question()` tool (multiple choice)
- New: Plain text report → Free text user response → `task_id` relay

## Output

After execution, provide a flow summary:

```
## Orchestration Summary

| Phase | Subagent | Status | Details |
|-------|----------|--------|---------|
| Research | explore | ✅ | Found 3 files |
| Research | librarian | ✅ | 2 APIs confirmed |
| Implement | code-writer | ✅ | 2 files modified |
| Verify | quality-runner | ✅ | All checks pass |
| Close | docs-updater | ✅ | 1 doc updated |

Result: [final response to user]
```
