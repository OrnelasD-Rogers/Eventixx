# Trace Format

> Schema and collection mechanism for agent execution traces.

---

## Overview

Every agent execution generates a structured trace. Traces are the foundation
for all metrics, debugging, and continuous improvement. Traces are stored as
JSON Lines (`.jsonl`) files in `.opencode/evals/traces/`.

## Trace Schema

```json
{
  "schema_version": "1.0",
  "trace_id": "uuid-v4",
  "session_id": "session-reference",
  "timestamp": "2026-05-16T12:00:00Z",
  "agent": {
    "type": "orchestrator|code-writer|librarian|quality-runner|docs-updater",
    "mode": "primary|subagent",
    "config_hash": "sha256-of-agent-file",
    "model": "model-identifier"
  },
  "parent_trace_id": "uuid-or-null",
  "task": {
    "input": "original user request or task spec",
    "intent": "implementation|research|debug|review|documentation",
    "expected_output": "description of expected result (eval only)",
    "complexity": "low|medium|high"
  },
  "execution": {
    "status": "success|fail|error|timeout",
    "duration_ms": 12345,
    "tokens_in": 1500,
    "tokens_out": 800,
    "total_tokens": 2300,
    "steps": [
      {
        "step_id": 1,
        "action": "read|edit|bash|websearch|webfetch|task|skill|grade",
        "target": "file-path-or-url-or-subagent-type",
        "duration_ms": 500,
        "status": "success|fail",
        "tokens_in": 100,
        "tokens_out": 50,
        "error": null
      }
    ]
  },
  "result": {
    "output": "agent's final response or synthesis",
    "files_created": ["path1", "path2"],
    "files_modified": ["path3"],
    "quality_passed": true,
    "quality_details": {
      "spotless": "PASS",
      "checkstyle": "PASS",
      "pmd": "PASS",
      "spotbugs": "PASS",
      "tests": "PASS (15/15)"
    },
    "docs_updated": ["doc-path-1"]
  },
  "evaluation": {
    "eval_id": "eval-reference-or-null",
    "pass": true,
    "score": 0.85,
    "failure_categories": ["spec_incomplete", "wrong_routing"],
    "notes": "human-or-llm-judge notes"
  },
  "errors": [
    {
      "type": "compilation|violation|timeout|routing|api_mismatch",
      "message": "descriptive error",
      "step_id": 2,
      "recovered": true
    }
  ]
}
```

## Required Fields

| Field | Required | Notes |
|-------|----------|-------|
| `trace_id` | ✅ | UUID v4, generated at trace start |
| `session_id` | ✅ | From the runtime session |
| `timestamp` | ✅ | ISO 8601 UTC |
| `agent.type` | ✅ | Must match agent name |
| `task.input` | ✅ | Full input text |
| `execution.status` | ✅ | One of: success, fail, error, timeout |
| `execution.duration_ms` | ✅ | Wall-clock time in ms |
| `execution.tokens_in` | ✅ | Input tokens |
| `execution.tokens_out` | ✅ | Output tokens |
| `result.output` | ✅ | What the agent produced |
| `evaluation.pass` | ✅ | For eval runs; null for production |
| `parent_trace_id` | ✅ | null for orchestrator, orchestrator's trace_id for subagents |

## Optional Fields

| Field | When to Include |
|-------|-----------------|
| `execution.steps` | Always for detailed analysis, optional for production monitoring |
| `errors` | Always when errors occur |
| `result.quality_details` | When quality-runner was invoked |
| `evaluation.failure_categories` | When eval grading is performed |
| `agent.model` | When model varies across runs |
| `agent.config_hash` | When tracking config changes |

## Collection Mechanism

### Automatic Collection

Traces are written by each agent at the end of execution:

```python
# Pseudocode for trace writing
trace = build_trace(agent, task, execution, result, errors)
append_to_file(".opencode/evals/traces/{session_id}.jsonl", trace)
```

### File Organization

```
.opencode/evals/traces/
├── {session_id}-{yyyy-mm-dd}.jsonl    # Production traces
├── eval-{eval_id}-run-{n}.jsonl       # Eval suite traces
└── archive/                            # Rotated after 30 days
```

### Aggregation

For analysis, traces are aggregated into metrics:

```python
# Load all traces from a session
traces = load_jsonl(".opencode/evals/traces/session-*.jsonl")

# Group by agent type
for agent_type in ["orchestrator", "code-writer", ...]:
    agent_traces = [t for t in traces if t.agent.type == agent_type]
    metrics = compute_metrics(agent_traces)  # From framework.md
    r_score = compute_r_score(metrics)
```

### Integrity Checks

| Check | Description |
|-------|-------------|
| Parent-child linking | Every subagent trace must have a valid parent_trace_id |
| Completeness | No required field missing |
| Time ordering | Steps within a trace must be monotonic |
| Size limit | Single trace ≤ 1MB, single file ≤ 100MB |
