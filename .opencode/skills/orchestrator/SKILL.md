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

### Step 0: Load Lessons Learned

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

Delegation rules:
- Phase 1 (research): `explore` + `librarian` in parallel
- Phase 2 (implement): `code-writer` → `quality-runner` sequential loop
- Phase 3 (close): `docs-updater` once, after all pass

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
