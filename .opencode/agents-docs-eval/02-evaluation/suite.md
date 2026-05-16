# Eval Suite

> How to run evaluations, task validity, outcome validity, and pass criteria.

---

## Overview

The Eval Suite is a collection of standardized tests that measure each agent's
performance across the metrics defined in `framework.md`. It follows the
**Agentic Benchmark Checklist (ABC)** guidelines: task validity, outcome
validity, and statistical significance.

## Suite Components

### 1. Task Validity

Ensure tests are realistic and achievable:

| Check | Description |
|-------|-------------|
| T.1 Real-world tasks | Each test corresponds to actual development scenarios |
| T.2 Clear pass/fail | Each task has unambiguous success criteria |
| T.3 Environment isolation | No cross-test state pollution (clean state per run) |
| T.4 Oracle available | Known correct answer exists for each task |
| T.5 No cheating | Agent cannot access ground truth during execution |

### 2. Outcome Validity

Ensure scoring is fair and meaningful:

| Check | Description |
|-------|-------------|
| O.1 Partial credit | Partial solutions get partial scores (not binary) |
| O.2 Trajectory evidence | Score based on execution trace, not just final output |
| O.3 Human calibration | LLM-as-judge validated against human labels |
| O.4 No shortcut detection | Trace analysis catches agents gaming the metric |

### 3. Benchmark Reporting

| Check | Description |
|-------|-------------|
| R.1 Statistical significance | Wilson CI ≤ ±5% for all metrics |
| R.2 Baseline comparison | Always compare against a known baseline |
| R.3 Failure mode analysis | Report top failure categories alongside aggregate scores |
| R.4 Version pinning | Pin agent config, model version, eval suite version |

## Eval Catalog

### Orchestrator Evals (10 tasks × 5 runs = 50 traces)

| ID | Task | Complexity | Success Criteria |
|----|------|------------|-----------------|
| ORC-01 | "Add GET endpoint to event-catalog" | Low | 8-field spec, correct routing, quality passes |
| ORC-02 | "Create a new service" | High | Full pipeline: explore → librarian → code-writer → quality-runner → docs-updater |
| ORC-03 | "Fix soft delete bug in reservation" | Medium | Bug correctly diagnosed, fix passes quality |
| ORC-04 | "Update MapStruct version" | Medium | librarian researches impact, code-writer updates pom, docs-updater syncs |
| ORC-05 | "Research Kafka consumer pattern" | Low | explore + librarian parallel, correct synthesis |
| ORC-06 | "Adicionar endpoint POST /api/v1/ticket-types" | Medium | Full pipeline, edge-case-hunter invoked, docs synced |
| ORC-07 | "Review event-catalog PR" | Low | explore only, correct analysis |
| ORC-08 | "Implementar busca por categoria" | High | Multi-file, DAG with dependencies |
| ORC-09 | "Corrigir violação Checkstyle no PaymentService" | Low | Single file fix, quality-runner passes |
| ORC-10 | "Sincronizar docs apos migration de entidade" | Medium | docs-updater with git diff analysis |

### Code Writer Evals (5 specs × 5 runs = 25 traces)

| ID | Spec Type | Key Validation |
|----|-----------|----------------|
| CW-01 | New controller + DTO + service | Compilation, conventions, edge-case-hunter |
| CW-02 | New entity + repository | Soft delete, @SQLRestriction, @SQLDelete |
| CW-03 | Bug fix (complexity violation) | PMD CyclomaticComplexity ≤ 10 |
| CW-04 | Service with Kafka producer | API verification via javap |
| CW-05 | Test class for existing endpoint | @MockitoBean, RestTestClient, edge cases |

### Librarian Evals (5 queries × 3 runs = 15 traces)

| ID | Query | Expected |
|-----|-------|----------|
| LIB-01 | "Spring Boot 4 @MockitoBean signature" | Correct annotations, non-deprecated |
| LIB-02 | "Kafka Transactional producer Spring Boot 4" | Current API, correct config |
| LIB-03 | "JPA Specification pagination best practice 2026" | Modern pattern, no deprecated APIs |
| LIB-04 | "Spring Security SecurityFilterChain vs old adapter" | Migration guide, deprecation detected |
| LIB-05 | "MapStruct 1.6.0 vs 1.7.0 breaking changes" | Version diff, migration impact |

### Quality Runner Evals (5 scenarios × 3 runs = 15 traces)

| ID | Scenario | Expected |
|-----|----------|----------|
| QR-01 | Clean build (no violations) | All PASS |
| QR-02 | Checkstyle violation | FAIL with correct file:line |
| QR-03 | PMD complexity violation | FAIL with correct file:line |
| QR-04 | Test failure | FAIL with correct test:line |
| QR-05 | Mixed violations | All failures reported, Step 0 detected early |

### Docs Updater Evals (5 scenarios × 3 runs = 15 traces)

| ID | Scenario | Expected |
|-----|----------|----------|
| DU-01 | New entity added | data-model.md updated |
| DU-02 | New endpoint added | api-contracts/ updated |
| DU-03 | Dependency version bump | setup.md + AGENTS.md updated |
| DU-04 | Private method change (no doc needed) | No docs updated |
| DU-05 | Security config change | security-guide.md updated |

## Running an Eval

```bash
# Collect traces to .opencode/evals/traces/
# Then analyze with:
#   R_geral = composite formula from framework.md
```

### Per-Eval Flow

1. Reset environment to clean state
2. Inject task (simulate user request)
3. Run agent workflow (capture trace)
4. Grade outcome against success criteria
5. Enrich trace with eval results
6. Append to eval dataset
7. Run statistical analysis after batch completes
