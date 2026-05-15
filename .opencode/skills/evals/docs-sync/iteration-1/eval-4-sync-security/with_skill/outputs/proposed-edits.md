# Proposed Edits — Eval 4: 2FA Security Filter

**Date:** 2026-05-15
**Skill version:** 1.1.0

---

## Edit 1: `docs/03-operations/security-guide.md`

**Action:** Rewrite empty file with security architecture content.

**Current content (3 lines):**
```markdown
# Security Guide

> Authentication, authorization, and data protection.
```

**Proposed content:**

```markdown
# Security Guide

> Authentication, authorization, and data protection.

## Authentication Model

Eventixx uses a multi-layered authentication model:

1. **JWT Authentication** — Stateless bearer tokens issued by the User Service. Required for all authenticated requests.
2. **Two-Factor Authentication (2FA)** — Additional verification factor required for sensitive admin operations.

## Filter Chain Architecture

Each service configures Spring Security `SecurityFilterChain` beans. The event-catalog-service defines two filter chains:

### Chain 1: General Filter Chain (default)

| Property | Value |
|----------|-------|
| Order | Default (lowest priority) |
| Matcher | All requests not matched by Chain 2 |
| GET endpoints | Public (`/api/v1/events/**`, `/api/v1/venues/**`, `/api/v1/categories/**`) |
| Swagger/Actuator | Public (`/swagger-ui/**`, `/v3/api-docs/**`, `/actuator/**`) |
| All other requests | Permitted (no auth) |

### Chain 2: Admin 2FA Filter Chain

| Property | Value |
|----------|-------|
| Order | Higher priority (e.g., `@Order(1)`) |
| Matcher | `/api/v1/admin/**` |
| GET requests | Permitted (read-only admin views) |
| POST/PUT/DELETE requests | **2FA required** — caller must present a valid 2FA token in addition to JWT |
| 2FA verification | Enforced via a custom `OncePerRequestFilter` added to the filter chain that validates the 2FA code/header before allowing the request to proceed |

## Endpoint Security Matrix

| Method | Path Pattern | Authentication | 2FA Required |
|--------|-------------|---------------|--------------|
| GET | `/api/v1/events/**` | None | No |
| GET | `/api/v1/venues/**` | None | No |
| GET | `/api/v1/categories/**` | None | No |
| POST/PUT/DELETE | `/api/v1/events/**` | JWT | No |
| POST/PUT/DELETE | `/api/v1/venues/**` | JWT | No |
| POST/PUT/DELETE | `/api/v1/categories/**` | JWT | No |
| GET | `/api/v1/admin/**` | JWT | No |
| POST/PUT/DELETE | `/api/v1/admin/**` | JWT | **Yes** |
| GET | `/actuator/**` | None | No |
| GET | `/swagger-ui/**`, `/v3/api-docs/**` | None | No |

## 2FA Verification Flow

1. Client authenticates with JWT as usual
2. For mutating operations on `/api/v1/admin/**`, the Admin 2FA Filter intercepts the request
3. Client must provide a valid 2FA token (e.g., via `X-2FA-Code` request header)
4. The filter validates the token against the stored 2FA secret (associated with the user's account in the User Service)
5. If valid: request proceeds to the controller
6. If invalid/missing: respond with `401 Unauthorized`

## Security Configuration (event-catalog-service)

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    @Order(1)
    public SecurityFilterChain admin2faFilterChain(HttpSecurity http) throws Exception {
        http.securityMatcher("/api/v1/admin/**")
            .csrf(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET).permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new Admin2faFilter(), BasicAuthenticationFilter.class);
        return http.build();
    }

    @Bean
    public SecurityFilterChain defaultFilterChain(HttpSecurity http) throws Exception {
        http.csrf(AbstractHttpConfigurer::disable)
            .formLogin(AbstractHttpConfigurer::disable)
            .httpBasic(AbstractHttpConfigurer::disable)
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/actuator/**").permitAll()
                .requestMatchers("/swagger-ui/**", "/v3/api-docs/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/events/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/venues/**").permitAll()
                .requestMatchers(HttpMethod.GET, "/api/v1/categories/**").permitAll()
                .anyRequest().permitAll());
        return http.build();
    }
}
```

---

## Edit 2: `docs/02-specs/glossary.md`

**Action:** Add "2FA" entry.

**Current content (3 lines):**
```markdown
# Glossary

> Domain-specific terminology and ubiquitous language.
```

**Edit:** Append after the existing content:

```markdown
| Term | Definition |
|------|------------|
| 2FA (Two-Factor Authentication) | Authentication method requiring two distinct verification factors. In Eventixx, 2FA is enforced on all mutating operations under `/api/v1/admin/` (POST, PUT, DELETE) as an additional security layer beyond JWT authentication. |
```

---

## Edit 3: `docs/02-specs/use-cases/UC-001-event-catalog.md`

**Action:** Update inline decision on line 45.

**Old text (line 43-45):**
```
### Inline Decisions (no ADR needed)
- **Public read access:** `GET /api/v1/events/**` requires no authentication to simulate a real ticketing platform.
- **Write operations require JWT:** Only authenticated organizers can create/modify events.
```

**New text:**
```
### Inline Decisions (no ADR needed)
- **Public read access:** `GET /api/v1/events/**` requires no authentication to simulate a real ticketing platform.
- **Write operations require JWT; admin operations require 2FA:** Authenticated organizers can create/modify events via standard endpoints. Mutating operations under `/api/v1/admin/` additionally require Two-Factor Authentication (2FA) verification for elevated security.
```

---

## Lessons Learned

After executing this sync, append to `<skill-path>/lessons.md`:

```markdown
## 2026-05-15

- Security-only changes (only `config/SecurityConfig.java` modified) map to 3 docs: `security-guide.md`, `glossary.md`, and the relevant UC inline decisions. Do not over-update `setup.md` or `data-model.md` if no dependencies or entities changed.
- An empty `security-guide.md` should be populated rather than just patched — the first security change becomes the foundation for future ones.
- Adding 2FA is an inline decision enhancement, not an ADR-worthy trade-off, unless the implementation changes the auth provider (e.g., JWT → OAuth2) or introduces a new infrastructure dependency.
```
