---
name: docs-sync
description: >-
  !!! ALWAYS INVOKE THIS SKILL AFTER ANY CODE CHANGE, BEFORE COMMITTING !!!
  Synchronizes project documentation after code changes. Use this whenever code has been modified (controllers, entities, DTOs, services, config, pom.xml, tests, Docker, security) and documentation needs updating. Triggers on: git diff showing changes to Java files, "precisa alterar algo na documentação?", updating docs before/after commits, "docs sync", "sincronizar documentação", or when the user asks what documentation needs updating. Covers all doc-framework layers: charter, ADRs, use-cases, data-model, api-contracts, testing-strategy, setup, glossary, deployment, security-guide, and sequence-diagrams. Includes self-improvement via lessons learned. Don't let the user manually figure out which docs changed — run this skill to analyze every changed file against the doc tree.
license: MIT
metadata:
  version: "1.2.0"
  domain: documentation
  triggers: after implementation, pre-commit, code changes, git diff, documentation update, docs sync, documentacao
  role: tech-writer
  related-skills: doc-framework
---

# Docs Sync

> **⚠️ MUST RUN AFTER EVERY CODE CHANGE — SEE "When to Use" BELOW**

Synchronizes project documentation with code changes. Covers all 4 layers of the doc-framework.

## When to Use

Use this skill whenever code changes are made and documentation needs to stay in sync. Triggering conditions:
- After implementing a feature or fixing a bug
- Before creating a commit that includes code changes
- When asked "what docs need updating?" or "precisa alterar algo na documentação?"
- After adding/modifying entities, endpoints, dependencies, tests, or config

## Core Workflow

### Step 0: Load Lessons Learned

Read previous discoveries to avoid repeating mistakes.

```markdown
Check if `<skill-path>/lessons.md` exists.
If yes: Read it and apply relevant guidance during execution.
If no: Proceed with default workflow.
```

*Validation checkpoint:* Lessons are loaded before any analysis begins.

### Step 1: Analyze Code Changes

Run these commands to understand what changed:

```bash
# Overview of all changes
git diff --stat

# Full diff for detailed analysis
git diff

# New untracked files
git status --short
```

Categorize **every changed file** into one or more of these types:

| Category | Matches Files Like |
|----------|-------------------|
| `entity` | `entities/*.java`, `*Entity.java` |
| `endpoint` | `controllers/*.java` |
| `dto` | `dto/**/*.java` |
| `service` | `services/**/*.java` |
| `repository` | `repositories/*.java` |
| `mapper` | `mappers/*.java` |
| `config` | `config/*.java`, `application*.yml`, `application*.yaml` |
| `dependency` | `pom.xml`, `build.gradle` |
| `test` | `src/test/**/*.java`, `src/test/**/*.yml` |
| `infra` | `docker-compose*.yml`, `Dockerfile`, `*.sql` |
| `security` | `Security*`, `Jwt*`, `Auth*`, `User*` |
| `docs` | `docs/**/*.md` |

*Validation checkpoint:* Every changed file is categorized. If a file matches multiple categories, list all.

### Step 2: Map to Documentation

For each category found, identify which docs need updating:

| Category Found | Doc to Update | What to Update |
|----------------|---------------|----------------|
| `entity` | `docs/02-specs/data-model.md` | Add/modify table definition, columns, FKs, constraints |
| `entity` + enum | `docs/02-specs/data-model.md` | Add enum values and CHECK constraints |
| `endpoint` | `docs/02-specs/api-contracts/` | Add/modify OpenAPI contract for the endpoint |
| `dto` | `docs/02-specs/api-contracts/` | Add/modify request/response schema |
| `service` | `docs/02-specs/use-cases/UC-*.md` | Update Execution Log, Lessons Learned, task status |
| `repository` | `docs/02-specs/data-model.md` | Add custom query documentation |
| `mapper` | `docs/04-implementation/setup.md` (if new mapping pattern) | Document MapStruct pattern if novel |
| `dependency` | `docs/04-implementation/setup.md`, `AGENTS.md` | Update versions, build commands, new plugins |
| `test` | `docs/03-operations/testing-strategy.md` | Update test patterns, coverage, new test types |
| `infra` | `docs/03-operations/deployment.md` | Update Docker config, ports, volumes |
| `security` | `docs/03-operations/security-guide.md` | Update auth flow, roles, endpoints |
| New feature/scenario | `docs/02-specs/use-cases/UC-*.md` | Update or create use case |
| Architectural trade-off | `docs/01-decisions/ADR-*.md` | Create new ADR or update existing |
| Scope/principle change | `docs/00-charter/PROJECT_CHARTER.md` | Update vision, scope, or principles |
| New domain term | `docs/02-specs/glossary.md` | Add definition |
| Cross-service flow | `docs/02-specs/sequence-diagrams.md` | Add/modify sequence diagram |

For each doc identified:
1. Read the current file content
2. Identify the exact sections that need changes
3. Determine whether to: add new section, modify existing, or deprecate

*Validation checkpoint:* Every category has a mapping decision (even if "no doc update needed"). No doc is updated without being read first.

### Step 3: Apply Updates

For each doc that needs changes:

1. **Use the `edit` tool** to make surgical changes — never rewrite entire files
2. Follow the existing markdown structure and conventions of that doc
3. Update these UC-specific sections when applicable:
   - Task status checkboxes (`[ ]` → `[x]`)
   - Execution Log (add entry with date and description)
   - Lessons Learned (add new insights)
4. For `data-model.md`: keep table definitions consistent with actual DDL (columns, types, constraints, FKs)
5. For `setup.md`: keep versions, commands, and dependency lists accurate
6. For `testing-strategy.md`: document new patterns, not every test case

*Validation checkpoint:* After each edit, re-read the file to confirm correctness.

### Step 4: Verify Consistency

Ensure the documentation is consistent:

1. **Cross-reference versions** — `AGENTS.md` key versions table must match `docs/04-implementation/setup.md`
2. **Cross-reference service list** — `AGENTS.md` service table must match `docs/00-charter/PROJECT_CHARTER.md`
3. **Cross-reference UC status** — If a UC is marked completed, all referenced tasks should be checked
4. **Check for dead links** — Ensure internal doc references resolve to existing files

*Validation checkpoint:* No inconsistencies between docs/. Report any found with suggested fix.

### Step 5: Save Lessons Learned

Append new discoveries to `<skill-path>/lessons.md`:

```markdown
## YYYY-MM-DD

- [Insight about what was learned during this sync]
- [Edge case or pattern that should be handled differently next time]
- [File, decision, or workflow that was surprising]
```

Use this format:
- One bullet per distinct lesson
- Be specific: mention file paths, patterns, or decisions
- Keep under 5 bullets per session

If `lessons.md` does not exist, create it with a header and the first entry.

*Validation checkpoint:* Lessons are saved before the skill completes.

## Reference Guide

### Doc Change Decision Table

| Scenario | Action | Example |
|----------|--------|---------|
| New JPA entity added | Add table to data-model.md | `Event.java` → add `events` table |
| New field on existing entity | Update table in data-model.md | `Event.status` → add column + CHECK |
| New REST controller | Add contract to api-contracts/ | `CategoryController` → `api-contracts/categories.md` |
| New dependency in pom.xml | Update setup.md and AGENTS.md | MapStruct → add version + config |
| Test pattern changed | Update testing-strategy.md | TestRestTemplate → RestTestClient migration |
| Docker service added | Update deployment.md | New PostgreSQL instance → add service + port |
| New enum added | Add to data-model.md (CHECK section) | `EventStatus.DRAFT, PUBLISHED, CANCELLED, ENDED` |
| Security filter added | Update security-guide.md | `SecurityFilterChain` → document new rule |

### File Location Reference

| Layer | Directory | Files |
|-------|-----------|-------|
| Charter | `docs/00-charter/` | `PROJECT_CHARTER.md` |
| Decisions | `docs/01-decisions/` | `ADR-*.md` |
| Specs | `docs/02-specs/` | `use-cases/UC-*.md`, `data-model.md`, `api-contracts/*.md`, `glossary.md`, `sequence-diagrams.md` |
| Operations | `docs/03-operations/` | `deployment.md`, `runbooks/*.md`, `security-guide.md`, `testing-strategy.md` |
| Implementation | `docs/04-implementation/` | `setup.md` |
| Agent Guide | root | `AGENTS.md` |

## Constraints

### MUST DO
- Read every doc before editing it
- Categorize every changed file
- Update UC task status when implementation advances
- Cross-reference versions for consistency after editing
- Save lessons learned on every run

### MUST NOT DO
- Rewrite entire doc files (make surgical edits only)
- Update docs without reading them first
- Leave UC tasks checked without verifying they match reality
- Remove content that is still accurate
- Create documents that should not exist per GUIDELINES.md

## Output

After execution, provide a summary table:

```markdown
## Docs Sync Summary

| Doc | Action | Status |
|-----|--------|--------|
| `docs/02-specs/data-model.md` | Added `events.status` column | ✅ |
| `docs/03-operations/testing-strategy.md` | Updated Testcontainers pattern | ✅ |

**Lessons saved to:** `<skill-path>/lessons.md`
```
