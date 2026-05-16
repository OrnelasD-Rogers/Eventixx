# Agent System Overview

> Architecture, modes, permissions, and delegation flow for the Eventixx
> multi-agent system.

---

## Agent Architecture

```
┌──────────────┐
│  opencode.json │  ← Agent registration & permissions
└──────┬───────┘
       │
       ▼
┌──────────────────────────────────────────────────────┐
│  Primary Agent: orchestrator                         │
│  • Top-level router                                  │
│  • Decomposes tasks into DAG                         │
│  • Delegates to subagents                            │
│  • Never executes work directly                      │
└──────────┬───────────────────────────────────────────┘
           │
    ┌──────┴──────┬──────────┬─────────────┬──────────┐
    ▼             ▼          ▼             ▼          ▼
┌────────┐ ┌──────────┐ ┌──────────┐ ┌────────────┐ ┌───────────┐
│ explore│ │ librarian│ │code-writer│ │quality-    │ │docs-      │
│(built- │ │(custom)  │ │(custom)  │ │runner      │ │updater    │
│ in)    │ │          │ │          │ │(custom)    │ │(custom)   │
└────────┘ └──────────┘ └──────────┘ └────────────┘ └───────────┘
 PHASE 1     PHASE 1      PHASE 2      PHASE 2       PHASE 3
 RESEARCH    RESEARCH     IMPLEMENT    VERIFY        CLOSE
```

## Agent Modes

| Mode | Description | Permissions |
|------|-------------|-------------|
| **primary** | Routes tasks, never executes. Has access to `task()` tool for subagent delegation | read, glob, grep, task, skill, todowrite, question |
| **subagent** | Specialized executor. Limited to specific tools needed for its role | Varies per agent — see individual docs |

## Permission Model

Permissions are declared in `.opencode/agents/<name>.md` and enforced by the
platform. The orchestrator's `opencode.json` defines which subagent types and
skills are allowed.

### Orchestrator Permissions (from opencode.json)

```json
"task": {
  "*": "deny",
  "explore": "allow",
  "librarian": "allow",
  "code-writer": "allow",
  "quality-runner": "allow",
  "docs-updater": "allow"
},
"skill": {
  "orchestrator": "allow",
  "edge-case-hunter": "allow",
  "docs-sync": "allow",
  "javap-inspector": "allow"
}
```

### Subagent Permission Patterns

| Agent | read | edit | bash | webfetch | websearch | task |
|-------|------|------|------|----------|-----------|------|
| orchestrator | ✅ | ❌ | ❌ | ❌ | ❌ | ✅ |
| code-writer | ✅ | ✅ | ✅(limited) | ❌ | ❌ | ❌ |
| librarian | ✅ | ❌ | ❌ | ✅ | ✅ | ❌ |
| quality-runner | ✅ | ❌ | ✅(mvn only) | ❌ | ❌ | ❌ |
| docs-updater | ✅ | ✅ | ✅(git only) | ❌ | ❌ | ❌ |

## Delegation Flow

```
1. Orchestrator receives user request
2. Parses intent (implementation / research / debug / review / documentation)
3. Decomposes into atomic subtasks (DAG with dependencies)
4. Maps subtasks to subagents using routing heuristics
5. Dispatches tasks via task() tool
6. Validates each result before forwarding
7. Loops code-writer → quality-runner until pass
8. Runs docs-updater once at the end
9. Synthesizes final response
```

## Phases

### Phase 1: RESEARCH (parallel)

Explore codebase (explore) + research APIs (librarian) run simultaneously.
Orchestrator waits for both before proceeding.

### Phase 2: IMPLEMENT (sequential loop)

code-writer → compile → spotless → quality-runner. Loop until all checks pass.
Orchestrator validates code-writer output BEFORE sending to quality-runner.

### Phase 3: CLOSE (once, at the end)

docs-updater runs git diff, categorizes changes, maps to docs, applies edits,
verifies cross-references, and saves lessons learned.

## Skills Registration

Skills are loaded at runtime via `skill({ name: "<skill>" })`. Each skill
provides specialized workflows and references:

| Skill | Registered In | Loaded By |
|-------|---------------|-----------|
| orchestrator | orchestrator skill file | orchestrator |
| edge-case-hunter | edge-case-hunter skill file | code-writer |
| docs-sync | docs-sync skill file | docs-updater |
| javap-inspector | javap-inspector skill file | code-writer |
