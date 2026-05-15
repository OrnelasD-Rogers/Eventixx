# Analysis: 2FA Security Filter Chain for Admin Endpoints

**Change:** New `SecurityFilterChain` requiring 2FA for all POST/PUT/DELETE under `/api/v1/admin/`.
**Git diff scope:** `services/event-catalog-service/src/main/java/com/eventixx/eventcatalog/config/SecurityConfig.java` (only file changed).

---

## Summary

| Layer | File | Must update? | Impact |
|-------|------|-------------|--------|
| **Security** | `docs/03-operations/security-guide.md` | **YES** | Currently empty (3 lines). Needs full 2FA + admin endpoint documentation. |
| **Charter** | `docs/00-charter/PROJECT_CHARTER.md` | **YES** | Security stack entry must mention 2FA; admin operations are now gated. |
| **Use Case** | `docs/02-specs/use-cases/UC-001-event-catalog.md` | **YES** | Inline decision about write auth needs updating; acceptance criteria out of date. |
| **Testing** | `docs/03-operations/testing-strategy.md` | **YES** | New test patterns needed for 2FA scenarios. |
| **Glossary** | `docs/02-specs/glossary.md` | Optional | Add "2FA" definition. |
| **Setup** | `docs/04-implementation/setup.md` | Optional | If 2FA requires env vars or external authenticator service. |
| **Deployment** | `docs/03-operations/deployment.md` | No | Pure code change — no infra impact. |
| **Data Model** | `docs/02-specs/data-model.md` | No | No schema change. |
| **Sequence Diagrams** | `docs/02-specs/sequence-diagrams.md` | No | Flow is a single-service concern. |
| **ADRs** | `docs/01-decisions/ADR-*.md` | No (borderline) | Change is localized, uses existing Spring Security pattern → inline decision, not an ADR. |

---

## Detailed Changes Required

### 1. `docs/03-operations/security-guide.md` — FULL UPDATE (high priority)

Currently a 3-line placeholder. Must be expanded to document:

#### 1.1 Authentication Model
- JWT-based authentication for all endpoints (existing)
- **New:** 2FA enforced on `/api/v1/admin/**` for POST/PUT/DELETE

#### 1.2 Filter Chain Architecture
- **SecurityFilterChain #1** (default): applies to all endpoints
  - `GET /api/v1/events/**`, `GET /api/v1/venues/**`, `GET /api/v1/categories/**` → permitAll
  - `GET /api/v1/admin/**` → requires `SCOPE_admin` or equivalent role
  - Mutating endpoints outside `/admin/` → requires JWT (existing)
- **SecurityFilterChain #2** (admin-2fa): new chain ordered before default
  - Path: `/api/v1/admin/**`
  - Methods: POST, PUT, DELETE
  - Requires: valid JWT **AND** 2FA code verification

#### 1.3 2FA Verification Flow
1. Client authenticates with JWT (username/password → token)
2. For admin write ops, client sends `X-2FA-Code` header or `X-2FA-Token` header
3. `SecurityFilterChain` extracts the 2FA code from the header
4. Code is verified against a TOTP-based authenticator (or mock for development)
5. On success: request proceeds; on failure: `401 Unauthorized` with `X-2FA-Required: true`

#### 1.4 Endpoint Matrix

| Path Pattern | Methods | Auth Required | 2FA Required |
|---|---|---|---|
| `/actuator/**` | ALL | None | No |
| `/swagger-ui/**`, `/v3/api-docs/**` | GET | None | No |
| `/api/v1/events/**` | GET | None | No |
| `/api/v1/venues/**` | GET | None | No |
| `/api/v1/categories/**` | GET | None | No |
| `/api/v1/admin/**` | POST, PUT, DELETE | JWT | **Yes** |
| `/api/v1/admin/**` | GET | JWT | No |
| Any other request | ALL | JWT | No |

#### 1.5 Development vs Production
- **Dev:** 2FA can be bypassed via `application-dev.yml` flag or always-valid mock code
- **Prod:** TOTP-based (Google Authenticator, Authy) with per-user secret stored in User Service

---

### 2. `docs/00-charter/PROJECT_CHARTER.md` — Section 2.2 (tech stack)

**Line 56** currently reads:
```markdown
- **Security:** Spring Security, JWT (io.jsonwebtoken)
```

Must be updated to:
```markdown
- **Security:** Spring Security, JWT (io.jsonwebtoken), 2FA (TOTP-based authenticator)
```

---

### 3. `docs/02-specs/use-cases/UC-001-event-catalog.md` — Multiple sections

#### 3.1 Inline Decisions (lines 43-49)
The existing entry at line 45:
```markdown
- **Write operations require JWT:** Only authenticated organizers can create/modify events.
```
Must be updated to:
```markdown
- **Write operations require JWT:** Only authenticated organizers can create/modify events.
- **Admin write operations require 2FA:** POST/PUT/DELETE under `/api/v1/admin/**` require
  both JWT authentication and a valid 2FA code (TOTP). Implemented as a dedicated
  `SecurityFilterChain` in `SecurityConfig.java`, ordered before the default chain.
  The 2FA code is passed via `X-2FA-Code` request header. In dev mode, 2FA can be
  disabled via spring profile flag.
```

#### 3.2 Exceptions table (lines 17-25)
Add new row:
```markdown
| 2FA code missing or invalid | 401 | 2FA code is required for admin write operations |
```

#### 3.3 Acceptance Criteria (lines 159-171)
Add:
```markdown
- [ ] Admin write endpoints require 2FA code; missing/invalid code returns 401
```

#### 3.4 Architecture Decisions section
Add reference to the 2FA filter chain decision. This could be an inline decision (since it's localized, uses existing Spring Security pattern) — no ADR needed per GUIDELINES criteria.

---

### 4. `docs/03-operations/testing-strategy.md` — New test patterns

#### 4.1 Test Package Updates (section 4)
Add new test categories:

```
web/
├── SecurityConfigWebTest.java       # Tests for 2FA filter chain
├── AdminControllerWebTest.java      # Tests for admin endpoints (if applicable)
```

```
integration/
├── SecurityIntegrationTest.java     # 2FA challenge-response flow
```

#### 4.2 New 2FA Test Patterns (section 7)
Add test examples:

```java
// Web test — 2FA required for admin POST
@WebMvcTest(AdminController.class)
class SecurityConfigWebTest {
    @Autowired private MockMvc mockMvc;
    @MockitoBean private TwoFactorService twoFactorService;

    @Test
    void shouldRejectAdminPostWithout2FA() throws Exception {
        mockMvc.perform(post("/api/v1/admin/events")
                .header("X-User-Id", "admin-1"))
            .andExpect(status().isUnauthorized())
            .andExpect(header().string("X-2FA-Required", "true"));
    }

    @Test
    void shouldAllowAdminPostWithValid2FA() throws Exception {
        when(twoFactorService.verify(anyString(), anyString())).thenReturn(true);
        mockMvc.perform(post("/api/v1/admin/events")
                .header("X-User-Id", "admin-1")
                .header("X-2FA-Code", "123456"))
            .andExpect(status().isCreated());
    }
}
```

#### 4.3 Quality Gates (section 8)
Add:
| Gate | Criteria |
|------|----------|
| **Security** | All admin write endpoints tested with missing/invalid/valid 2FA; public GETs still pass without 2FA |

---

### 5. `docs/02-specs/glossary.md` — Optional

Add entry:
```markdown
**2FA (Two-Factor Authentication):** Authentication method requiring both a JWT token (something you
have) and a time-based one-time password (TOTP, something you know). Enforced on all POST/PUT/DELETE
operations under `/api/v1/admin/**`.
```

---

### 6. `docs/04-implementation/setup.md` — Conditional

Only update if the 2FA implementation introduces:
- New environment variables (e.g., `APP_2FA_ENABLED=true`)
- New external dependency (e.g., TOTP library like `com.eatthepath:java-otp`)
- New Docker service (e.g., authenticator app — unlikely for a filter-level change)

If none of the above, no setup.md changes needed.

---

## Files NOT Affected (with rationale)

| File | Why not affected |
|------|-----------------|
| `docs/02-specs/data-model.md` | No database schema changes — 2FA is a filter-level concern, not persisted in event-catalog-db |
| `docs/03-operations/deployment.md` | No infrastructure changes — filter chain runs in-process |
| `docs/02-specs/sequence-diagrams.md` | Single-service security concern — no multi-service flow |
| `docs/01-decisions/ADR-*.md` | Per GUIDELINES section 3 (UC Does NOT Trigger ADR): change is localized to one config file, uses existing Spring Security pattern. Document as inline decision in UC-001 instead. |

---

## Potential Issues & Risks

1. **Backward compatibility:** Existing clients hitting `/api/v1/admin/**` will break if they don't send 2FA codes. Migration strategy needed — could phase in via `X-2FA-Required: true` response header warning before enforcing.
2. **Dev experience:** Developers need a way to bypass 2FA locally (profile flag or mock). Must be documented in both `security-guide.md` and `setup.md`.
3. **Test isolation:** 2FA filter chain must not interfere with existing public GET endpoints. Integration tests must verify public endpoints remain accessible without any auth headers.
