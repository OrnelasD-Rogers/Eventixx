# Reliability Dashboard

> Continuous monitoring, alerts, and benchmark refresh for agent reliability.

---

## Monitoring Cadence

| Activity | Frequency | Owner |
|----------|-----------|-------|
| Run full eval suite | Every 10 dev sessions | quality-runner |
| R_geral computation | After each eval run | quality-runner |
| Regression detection | After each eval run | quality-runner |
| Lessons review | Weekly | Human |
| Benchmark refresh | Monthly | Human |
| Metric threshold review | Monthly | Human |

## Metrics to Track

### Per-Agent (over time)

```
Agent: orchestrator
├── O1 Task Decomposition:  ████████░░ 82%  (trend: ↑)
├── O2 Subagent Selection:  █████████░ 91%  (trend: →)
├── O3 Spec Completeness:   ███████░░░ 78%  (trend: ↑)
├── O4 Error Recovery:      ████████░░ 80%  (trend: →)
└── O5 Result Validation:   ████████░░ 83%  (trend: ↑)
R_orchestrator: 82.8%  (target: ≥80% ✅)

Agent: code-writer
├── C1 First-Pass Compilation: ████████░░ 84%  (trend: →)
├── C2 Convention Compliance:  █████████░ 92%  (trend: ↑)
├── C3 Edge Case Coverage:     ████████░░ 81%  (trend: ↑)
├── C4 API Correctness:        █████████░ 88%  (trend: →)
└── C5 Avg Rework Cycles:      1.8         (trend: ↓)
R_code_writer: 81.4%  (target: ≥80% ✅)
```

### Composite

```
R_geral: 82.1%  (target: ≥80% ✅)
├── R_orchestrator:  82.8%  (weight: 0.30 → 24.8 pp)
├── R_code_writer:   81.4%  (weight: 0.25 → 20.4 pp)
├── R_librarian:     84.2%  (weight: 0.15 → 12.6 pp)
├── R_quality_runner: 90.1% (weight: 0.10 → 9.0 pp)
├── R_docs_updater:  79.5%  (weight: 0.10 → 8.0 pp)
└── R_integrado:     72.3%  (weight: 0.10 → 7.2 pp)
```

## Alerts

| Condition | Severity | Action |
|-----------|----------|--------|
| R_geral drops below 75% | 🔴 Critical | Block all commits, investigate |
| Single agent drops below 70% | 🔴 Critical | Isolate agent, run diagnostic |
| R_geral drops 5+ pp in one eval | 🟡 Warning | Review changes since last eval |
| Consistency drops below 60% | 🟡 Warning | Check for non-determinism |
| Integration metrics below 65% | 🟡 Warning | Review orchestration DAG and handoffs |
| No eval run in 15+ sessions | 🟡 Warning | Quality process gap |

## Regression Detection

After every code change that modifies agent behavior:

1. Run eval suite on previous known-good config
2. Run eval suite on new config
3. Compare per-agent and composite scores
4. If any metric drops below threshold → block and report

```bash
# Regression check
evaluate --baseline .opencode/evals/baseline-runs/latest.jsonl
         --candidate .opencode/evals/traces/current.jsonl
         --output .opencode/evals/regression-report.json
```

## Benchmark Refresh

To prevent saturation and contamination:

| Action | Frequency | Trigger |
|--------|-----------|---------|
| Review eval tasks for memorization | Monthly | Check if pass rates exceed 95% consistently |
| Replace 20% of eval tasks | Monthly | Rotate low-discrimination tasks |
| Add new scenarios | As needed | New feature, new agent capability |
| Retire saturated tasks | Monthly | Tasks where all agents score >95% |

### Task Discrimination

Use Item Response Theory (IRT): keep tasks with pass rates between 30-70%.
Tasks outside this band have low discriminative power.

```python
# Task selection for eval suite
if task_pass_rate < 0.30:
    candidate_for_retirement = True  # Too hard or broken
elif task_pass_rate > 0.70:
    candidate_for_replacement = True  # Too easy or memorized
else:
    keeper = True  # Good discriminative power (30-70% band)
```

## Improvement Cycles

```
1. Run eval suite → generate metrics
2. Identify agent with lowest R_score
3. Apply targeted improvement (from troubleshooting guide)
4. Re-run eval suite
5. Verify R_geral improved AND no regressions elsewhere
6. Repeat until R_geral ≥ 80%
```

## Reporting

Weekly report should include:

1. Current R_geral and trend (↗︎ / → / ↘︎)
2. Per-agent R scores with changes from previous week
3. Top 3 failure categories with frequency
4. Recent improvements and their measured impact
5. Eval suite health (task discrimination, saturation)
6. Action items for next week
