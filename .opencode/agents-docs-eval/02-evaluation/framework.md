# Evaluation Framework

> Metrics, dimensions, and formulas for measuring agent reliability.

---

## Overview

This framework adapts the **CLASSic** model (Cost, Latency, Accuracy, Stability,
Security) for the Eventixx multi-agent system. Each agent is evaluated across
multiple dimensions, then aggregated into a composite reliability score `R_geral`.

## Metrics per Agent

### Orchestrator

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| O1 | Task Decomposition Rate | ≥85% | % of tasks decomposed with 8-field spec complete |
| O2 | Subagent Selection Rate | ≥90% | % of routing decisions correct for intent |
| O3 | Spec Completeness | ≥80% | % of specs passing validation checklist |
| O4 | Error Recovery Rate | ≥75% | % of subagent failures successfully recovered |
| O5 | Result Validation Rate | ≥80% | % of results validated before synthesis |

**R_orchestrator = avg(O1, O2, O3, O4, O5)**

### Code Writer

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| C1 | First-Pass Compilation | ≥85% | % of files compiling on first attempt |
| C2 | Convention Compliance | ≥90% | % of files with zero checkstyle/PMD/SpotBugs violations |
| C3 | Edge Case Coverage | ≥80% | % of endpoints with edge-case-hunter analysis applied |
| C4 | API Correctness | ≥90% | % of APIs verified via javap before use |
| C5 | Avg Rework Cycles | ≤2 | Mean cycles through quality-runner until pass |

**R_code_writer = avg(C1, C2, C3, C4, normalize(C5, invert, max=5))**

### Librarian

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| L1 | Search Relevance | ≥80% | % of results relevant to query |
| L2 | API Accuracy | ≥85% | % of APIs reported with correct signature |
| L3 | Deprecation Detection | ≥80% | % of deprecated APIs correctly identified |
| L4 | Search Latency | ≤30s | Mean time per research query |

**R_librarian = avg(L1, L2, L3, normalize(L4, invert, max=60))**

### Quality Runner

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| Q1 | Pipeline Completion | ≥95% | % of executions completing all stages |
| Q2 | Failure Reporting Accuracy | ≥90% | % of failures reported with correct file:line |
| Q3 | False Positive Rate | ≤5% | % of flagged issues that are not real violations |
| Q4 | False Negative Rate | ≤5% | % of real violations missed |

**R_quality_runner = avg(Q1, Q2, 1-Q3, 1-Q4)**

### Docs Updater

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| D1 | Categorization Accuracy | ≥90% | % of changes correctly categorized (entity vs endpoint vs ...) |
| D2 | Edit Precision | ≥85% | % of edits that are surgical (only changed content) |
| D3 | Verification Completeness | ≥80% | % of runs that perform cross-reference verification |
| D4 | Lessons Saved | ≥90% | % of sessions where lessons are appended |

**R_docs_updater = avg(D1, D2, D3, D4)**

## Integrated Metrics (Orchestrator + Chain)

| ID | Metric | Target | Measurement |
|----|--------|--------|-------------|
| I1 | End-to-End Success@k | ≥80% | % of tasks completed in ≥1 of k runs |
| I2 | End-to-End Pass^k | ≥60% | % of tasks completed in ALL k runs |
| I3 | Error Containment | ≥75% | % of subagent errors that do NOT propagate to final output |
| I4 | Cycle Time | ≤5min | Mean time from request to completion |
| I5 | Cost Efficiency | ≤3:1 | Tokens per completed task / tokens per failed task |
| I6 | Handoff Accuracy | ≥85% | % of handoffs with complete and unambiguous spec |

**R_integrado = avg(I1, I2, I3, normalize(I4, invert, max=10), I5, I6)**

## Composite Reliability Score

```
R_geral = 0.30 × R_orchestrator
        + 0.25 × R_code_writer
        + 0.15 × R_librarian
        + 0.10 × R_quality_runner
        + 0.10 × R_docs_updater
        + 0.10 × R_integrado
```

**Target: R_geral ≥ 0.80 (80%)**

## Stability Metrics (multi-run)

| Metric | Definition | Target |
|--------|------------|--------|
| **Consistency** | % of tasks with same outcome across 5 runs (same input) | ≥70% |
| **Robustness** | % of tasks tolerating perturbations (similar but different input) | ≥75% |
| **Predictability** | Calibration: agent's confidence matches actual accuracy | ≥80% |

### Pass Metrics (from Claw-Eval)

| Metric | Definition |
|--------|------------|
| **Pass@k** | Fraction of tasks passed at least once in k trials — capability ceiling |
| **Pass^k** | Fraction of tasks passed on EVERY trial — reliability floor |
| **Gap** | Pass@k − Pass^k — larger gap = less consistent |

A large gap between Pass@k and Pass^k indicates limited consistency.

## Statistical Significance

- Binomial metrics: Wilson-score confidence intervals (95%)
- Derived metrics: Percentile bootstrap intervals
- Minimum: 5 trials per task, 3 per configuration
- Report confidence intervals alongside point estimates
