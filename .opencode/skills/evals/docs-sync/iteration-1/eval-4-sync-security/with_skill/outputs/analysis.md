# Docs Sync Analysis — Eval 4: 2FA Security Filter

**Date:** 2026-05-15
**Skill version:** 1.1.0
**Task:** A developer added a new `SecurityFilterChain` requiring 2FA for all POST/PUT/DELETE endpoints under `/api/v1/admin/`. Only `config/SecurityConfig.java` changed.

---

## Step 0: Lessons Learned

Loaded from `<skill-path>/lessons.md`. Relevant guidance:
- **Security filter changes → update security-guide.md** (from Reference Guide decision table: "Security filter added → Update security-guide.md")
- **Glossary updates are easy to forget** — every new security mechanism that introduces a domain term should trigger a glossary check
- **UC inline decisions** — UC-001 has a relevant inline decision ("Write operations require JWT") that may need synchronizing
- **Do not rewrite docs** — surgical edits only

---

## Step 1: Analyze Code Changes

### Changed Files

| File | Status | Categories |
|------|--------|-----------|
| `services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/config/SecurityConfig.java` | Modified | `config`, `security` |

### What Changed (Reconstructed from Scenario)

The existing `SecurityConfig.java` has a single `SecurityFilterChain` bean that permits all requests (no authentication). The change adds a **second** `@Ordered` `SecurityFilterChain` bean that:

1. Matches paths `/api/v1/admin/**`
2. Requires 2FA (Two-Factor Authentication) for `POST`/`PUT`/`DELETE` HTTP methods
3. Continues to permit `GET` requests (for listing/admin reads)
4. The original filter chain continues to handle all other paths as before

This is a **dual-category** change: `config` (because it's in the `config/` package) and `security` (because it introduces SecurityFilterChain rules for authentication).

### Validation Checkpoint

Every changed file categorized: ✅ `config/SecurityConfig.java` → [`config`, `security`]

---

## Step 2: Map to Documentation

### Category-to-Doc Mapping

| Category | Doc to Update | What to Update |
|----------|---------------|---------------|
| `security` | `docs/03-operations/security-guide.md` | Currently empty (3 lines, header only). Must document the 2FA filter chain, endpoint-level rules, and general security architecture |
| `security` | `docs/02-specs/glossary.md` | Currently empty (3 lines, header only). Add "2FA (Two-Factor Authentication)" definition |
| `security` | `docs/02-specs/use-cases/UC-001-event-catalog.md` | Update inline decision "Write operations require JWT" to mention that admin mutating operations additionally require 2FA |
| `config` | No direct doc. The config change is a security concern, covered by security-guide.md | — |

### Docs NOT Needing Updates (and why)

| Doc | Reason |
|-----|--------|
| `docs/00-charter/PROJECT_CHARTER.md` | Charter already lists "Security: Spring Security, JWT". 2FA is an extension of existing security, not a new principle or scope change |
| `docs/01-decisions/ADR-*.md` | No new ADR needed — adding 2FA to admin endpoints is an inline enhancement to an existing security mechanism, not a new architectural trade-off with reversibility concerns. If the implementation were a significant architectural decision (e.g., replacing JWT with OAuth2), an ADR would be warranted |
| `docs/04-implementation/setup.md` | No new dependencies added (only SecurityConfig.java changed). No new setup steps |
| `docs/02-specs/data-model.md` | No new entity or column added. 2FA state (if persisted) would be in the User Service's database, not event-catalog's |
| `docs/02-specs/api-contracts/` | No new endpoints added. The `/api/v1/admin/*` endpoints already existed; only security constraints changed |
| `docs/03-operations/deployment.md` | No infra changes |
| `docs/03-operations/testing-strategy.md` | No test pattern changed (unless new tests for 2FA filter were added, but scenario says only SecurityConfig.java changed) |
| `docs/02-specs/sequence-diagrams.md` | No cross-service flow changed |
| `AGENTS.md` | No new build commands, dependencies, or service entries |

### Validation Checkpoint

Every category has a mapping decision: ✅
- `security` → security-guide.md, glossary.md, UC-001-event-catalog.md
- `config` → no doc update needed (covered by security mapping)

---

## Step 3: Proposed Edits

### Doc 1: `docs/03-operations/security-guide.md`

**Action:** Populate file with security architecture description (currently empty).

**Sections to add:**
- Overview of authentication model (JWT + 2FA)
- Filter chain architecture (two chains: general + admin-2FA)
- Endpoint security matrix table
- 2FA verification flow description

**See `proposed-edits.md` for exact content.**

### Doc 2: `docs/02-specs/glossary.md`

**Action:** Add new glossary entry.

**Change:**
- Insert after existing header:
  ```markdown
  | Term | Definition |
  |------|------------|
  | 2FA (Two-Factor Authentication) | Authentication method requiring two distinct verification factors. In Eventixx, 2FA is enforced on all mutating operations under `/api/v1/admin/` (POST, PUT, DELETE) as an additional security layer beyond JWT authentication. |
  ```

### Doc 3: `docs/02-specs/use-cases/UC-001-event-catalog.md`

**Action:** Update existing inline decision.

**Current text (line 45):**
```
- **Write operations require JWT:** Only authenticated organizers can create/modify events.
```

**Proposed text:**
```
- **Write operations require JWT; admin operations require 2FA:** Authenticated organizers can create/modify events via standard endpoints. Mutating operations under `/api/v1/admin/` additionally require Two-Factor Authentication (2FA) verification for elevated security.
```

---

## Step 4: Verify Consistency

No cross-reference concerns identified:
- `AGENTS.md` base build commands unchanged
- `PROJECT_CHARTER.md` security section already accurate ("Spring Security, JWT")
- `setup.md` dependency list unchanged
- No dead links introduced (no new doc files created)

---

## Summary

| Doc | Action | Status |
|-----|--------|--------|
| `docs/03-operations/security-guide.md` | Populate with security architecture | 🔲 Proposed |
| `docs/02-specs/glossary.md` | Add "2FA" term | 🔲 Proposed |
| `docs/02-specs/use-cases/UC-001-event-catalog.md` | Update inline decision | 🔲 Proposed |
| `docs/00-charter/PROJECT_CHARTER.md` | No change needed | ✅ |
| `docs/04-implementation/setup.md` | No change needed | ✅ |
| `docs/02-specs/data-model.md` | No change needed | ✅ |
| `docs/03-operations/deployment.md` | No change needed | ✅ |
| `docs/03-operations/testing-strategy.md` | No change needed | ✅ |
| `docs/01-decisions/ADR-*.md` | No change needed | ✅ |
| `docs/02-specs/sequence-diagrams.md` | No change needed | ✅ |
