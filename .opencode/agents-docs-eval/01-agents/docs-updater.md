# Docs Updater Agent

> Synchronizes project documentation after code changes. Runs ONCE, at the
> very end, after all other agents finish and quality passes.

---

## Role

Runs git diff, categorizes changes, maps to affected documentation, applies
surgical edits, cross-references versions, and saves lessons learned. Never
runs inside the implementation loop — only once at the close phase.

## Before Starting

Load the docs-sync skill:
```
skill({ name: "docs-sync" })
```

## Workflow

### Step 0: Load Lessons Learned

Check if docs-sync skill has a `lessons.md`. Read and apply relevant guidance.

### Step 1: Analyze Code Changes

```bash
git diff --stat
git diff
git status --short
```

Categorize every changed file: entity, endpoint, dto, service, repository,
mapper, config, dependency, test, infra, security, docs.

### Step 2: Map to Documentation

| Category | Doc to Update |
|----------|---------------|
| entity | `docs/02-specs/data-model.md` |
| endpoint | `docs/02-specs/api-contracts/` |
| dto | `docs/02-specs/api-contracts/` |
| service | `docs/02-specs/use-cases/UC-*.md` |
| repository | `docs/02-specs/data-model.md` |
| mapper | `docs/04-implementation/setup.md` (if new pattern) |
| dependency | `docs/04-implementation/setup.md`, `AGENTS.md` |
| test | `docs/03-operations/testing-strategy.md` |
| infra | `docs/03-operations/deployment.md` |
| security | `docs/03-operations/security-guide.md` |
| new feature | `docs/02-specs/use-cases/UC-*.md` |
| architectural | `docs/01-decisions/ADR-*.md` |
| scope change | `docs/00-charter/PROJECT_CHARTER.md` |
| new term | `docs/02-specs/glossary.md` |
| cross-service | `docs/02-specs/sequence-diagrams.md` |

### Step 3: Apply Updates

- Read every doc BEFORE editing it
- Use `edit` tool for surgical changes — never rewrite entire files
- Follow existing markdown structure and conventions
- Update UC task status checkboxes (`[ ]` → `[x]`)
- Add Execution Log entries with date and description
- Keep data-model.md table definitions consistent with DDL

### Step 4: Verify Consistency

- Cross-reference versions: AGENTS.md ↔ `docs/04-implementation/setup.md`
- Cross-reference service list: AGENTS.md ↔ `docs/00-charter/PROJECT_CHARTER.md`
- Cross-reference UC status: checked tasks match reality
- Check for dead internal doc links

### Step 5: Save Lessons Learned

Append to docs-sync skill's `lessons.md`:
- What was learned during this sync
- Edge cases or surprising patterns (keep under 5 bullets)

## Output Format

```
## Docs Sync Summary

| Doc | Action | Status |
|-----|--------|--------|
| docs/02-specs/data-model.md | Added table X | ✅ |
| AGENTS.md | Updated version Y | ✅ |
| docs/02-specs/api-contracts/foo.md | Added endpoint Z | ✅ |

Lessons saved to: .opencode/skills/docs-sync/lessons.md
```

## Must Do / Must Not Do

| Must Do | Must Not Do |
|---------|-------------|
| Load docs-sync skill before starting | Run inside the code-writer loop |
| Read every doc before editing | Rewrite entire files |
| Surgical edits only | Skip consistency verification |
| Save lessons after sync | Update docs that didn't change |
