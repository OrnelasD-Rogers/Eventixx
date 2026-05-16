# Agents Docs & Evaluation

> Documentation for the Eventixx multi-agent system: agent definitions, workflows,
> evaluation framework, and reliability metrics.

---

## Structure

```
agents-docs-eval/
├── README.md                           # This file
├── 01-agents/                          # Agent definitions and workflows
│   ├── overview.md                     # Architecture, modes, permissions, flow
│   ├── orchestrator.md                 # DAG decomposition, spec template, error recovery
│   ├── code-writer.md                  # Write loop, javap, edge-case-hunter
│   ├── librarian.md                    # Web research, templates, confidence scoring
│   ├── quality-runner.md               # Fast-fail pipeline, Maven verification
│   └── docs-updater.md                 # Docs-sync workflow, cross-reference verification
├── 02-evaluation/                      # Evaluation and reliability
│   ├── framework.md                    # Metrics, R_geral formula, per-agent dimensions
│   ├── suite.md                        # Eval Suite: task validity, outcome validity, stats
│   ├── trace-format.md                 # Trace schema, fields, collection
│   └── dashboard.md                    # Continuous monitoring, alerts, benchmark refresh
└── 03-troubleshooting/
    └── common-failures.md              # Failure patterns per agent, diagnosis, resolution
```

## Quick Reference

| Agent | Mode | Permission Scope | Key Constraint |
|-------|------|-----------------|----------------|
| orchestrator | primary | read/glob/grep/task/skill/todowrite | NEVER edits or executes directly |
| code-writer | subagent | read/edit/bash(limited) | javap every API, compile per file |
| librarian | subagent | webfetch/websearch/read | NEVER writes code |
| quality-runner | subagent | read/bash(limited) | NEVER modifies code, only reports |
| docs-updater | subagent | read/edit/bash(limited) | Runs ONCE at the end |

## Core Flow

```
User Request
    │
    ▼
┌─────────────────────────────────────────────────┐
│  Orchestrator (primary)                         │
│  • Parse & classify intent                      │
│  • Decompose into DAG                           │
│  • Route to subagents                           │
│  • Validate results                             │
│  • Synthesize output                            │
└──────┬──────────────────────────────────────────┘
       │
       ├── PHASE 1: RESEARCH (parallel)
       │    ├── explore (codebase discovery)
       │    └── librarian (web research)
       │
       ├── PHASE 2: IMPLEMENT (sequential loop)
       │    ├── code-writer → compile → spotless
       │    └── quality-runner (fast-lint → full verify)
       │    └── (loop until pass)
       │
       └── PHASE 3: CLOSE (once)
            └── docs-updater (sync docs)
```

## Key Skills

| Skill | Used By | Purpose |
|-------|---------|---------|
| orchestrator | orchestrator | Decomposition and delegation workflow |
| edge-case-hunter | code-writer | Edge case analysis + test generation |
| docs-sync | docs-updater | Documentation synchronization |
| javap-inspector | code-writer | Bytecode inspection for API verification |
