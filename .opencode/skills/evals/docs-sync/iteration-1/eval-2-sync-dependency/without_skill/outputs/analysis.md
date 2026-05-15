# Analysis: MapStruct version bump (1.6.0 → 1.7.0)

**Trigger:** `pom.xml` line 30 changed `<mapstruct.version>1.6.0</mapstruct.version>` → `1.7.0`

---

## Files requiring updates

### 1. `AGENTS.md` — MUST update

**Location:** Line 204, Key Versions table

**Current:**
```markdown
| MapStruct | 1.6.0 |
```

**Change to:**
```markdown
| MapStruct | 1.7.0 |
```

**Why:** This table is the single source of truth for dependency versions used by AI agents and developers. A stale version here directly causes incorrect code generation and confusion.

---

## Files verified — NO change needed

| File | Reason |
|------|--------|
| `docs/04-implementation/setup.md` | References MapStruct conceptually (annotation processor ordering, troubleshooting) but does not mention a specific version number. No update needed. |
| `docs/02-specs/use-cases/UC-001-event-catalog.md` | Mentions MapStruct as a required dependency for the event catalog service but does not pin a version. No update needed. |
| `.opencode/instructions/coding-rules.md` | Documents MapStruct coding conventions (`@Mapper(config = MapStructConfig.class)`) — no version reference. No update needed. |
| `.opencode/skills/docs-sync/SKILL.md` | Documents the docs-sync pattern for new dependencies but contains no hardcoded version. No update needed. |
| `.opencode/skills/service-scaffold/SKILL.md` | References `MapStructConfig.java` pattern only. No version reference. No update needed. |
| `KAFKA_GUIA_ESTUDO.md` | Mentions `EventDocumentMapper` class name only. No version reference. No update needed. |

---

## Summary

**1 file must change** — only `AGENTS.md`.

The version lives in exactly two places: `pom.xml` (the changed file) and the Key Versions table in `AGENTS.md`. All other documentation references MapStruct conceptually without pinning a version, so they remain correct.
