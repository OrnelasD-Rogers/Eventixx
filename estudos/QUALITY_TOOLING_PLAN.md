# Quality Tooling Implementation Plan

- **Date:** 2026-05-01
- **Scope:** All microservices (via `eventixx-parent` POM)
- **Status:** Implemented
- **Author:** @ornelas

---

## 1. Context & Problem Statement

In 2026, AI-generated code (Copilot, Cursor, Devin) is the default for many developers. While this accelerates delivery, it introduces a new class of risks:

- **Code that compiles but breaks in production** (e.g., `@Transactional` on private methods, internal calls bypassing Spring proxies)
- **Security vulnerabilities that look correct** (e.g., SQL injection in AI-generated repository queries)
- **Architectural drift** (e.g., AI adding imports across layers, creating cyclic dependencies)
- **Inconsistent style** across services written at different times by different AI prompts

Eventixx, as a **portfolio project targeting senior-level positions**, must demonstrate not only clean architecture but also **automated quality enforcement**. This plan defines the tooling, rationale, and implementation steps to achieve that.

**Constraints:**
- All tools must be **free and self-hosted** (no SaaS subscriptions — this is a personal portfolio)
- Integration must work for **all current and future microservices** via the parent POM
- Build time impact must be reasonable for local development (`< 2 min` total overhead)

---

## 2. Objectives

1. **Detect bugs before CI:** Null pointers, resource leaks, concurrency issues, deadlocks
2. **Enforce code style:** Consistent formatting across all services (Google Java Style)
3. **Prevent architectural erosion:** No cyclic dependencies between packages, correct dependency direction (entities/repositories independent of controllers/services), no direct repository access from controllers
4. **Security baseline:** Detect common injection vulnerabilities and insecure patterns
5. **Impress recruiters:** Modern, production-grade quality stack that most portfolio projects ignore

---

## 3. Tools Matrix — Chosen Stack

| Tool | What It Detects | Why Chosen | Rejected Alternative |
|---|---|---|---|
| **SpotBugs + FindSecBugs** | Bytecode-level bugs: null pointers, deadlocks, race conditions, resource leaks, SQL injection, XSS, insecure crypto | The only free tool with deep bytecode analysis. Concurrency detection (400+ patterns) is unmatched in the open-source Java ecosystem. FindSecBugs adds security rules without extra infrastructure. | Semgrep (source-level only, no bytecode analysis), SonarQube Cloud (SaaS/paid) |
| **PMD** | Code smells: unused variables/imports, empty catch blocks, copy-paste (CPD), high cyclomatic complexity | Fast, lightweight, 400+ built-in rules, CPD (copy-paste detector) included. Integrates cleanly into Maven `verify` phase. | DeepSource (SaaS), Checkstyle alone (no smell detection) |
| **Checkstyle** | Code style and formatting: indentation, naming conventions, import order, Javadoc | Industry standard; Google Java Style is universally recognized; IDE support (IntelliJ, VS Code) is native. | No style enforcement (inconsistent AI-generated code), custom style (harder to justify in interviews) |
| **ArchUnit** | Architecture rules: cyclic dependencies, package dependency direction, naming conventions, annotation misuse | Tests architecture as unit tests — zero extra infrastructure. Spring-aware (detects `@Transactional` on private methods, `@Async` issues). Validates the layered package structure (`entities`, `repositories`, `services`, `controllers`). | jQAssistant (requires Neo4j, overkill for microservices), Spring Sentinel (too new/niche, 2026-only) |

---

## 4. Rejected Tools — Detailed Rationale

### 4.1 SaaS / Paid Platforms

| Tool | Why Rejected |
|---|---|
| **SonarQube Cloud** | Paid for private repositories. SonarQube Community (self-hosted) is our dashboard alternative, but the scanner/analysis plugins (SpotBugs, PMD) run independently. |
| **Codacy** | $15/user/month SaaS. Embeds PMD/SpotBugs but adds no value we cannot get from Maven plugins directly. |
| **CodeAnt AI** | $24-40/user/month. AI layer is nice-to-have, not essential. Deterministic rule-based tools are sufficient for a portfolio. |
| **ThreadSafe** | Commercial tool focused exclusively on concurrency. SpotBugs covers ~80% of the same patterns for free. |

### 4.2 Open Source but Not Chosen (Yet)

| Tool | Why Rejected for Phase 1 | Future Consideration |
|---|---|---|
| **Semgrep** | Excellent for security, but more generic (multi-language). For Java bytecode-level bugs, SpotBugs is superior. | Phase 2: Add Semgrep for custom security rules and CI speed. |
| **jQAssistant** | Powerful graph-based analysis (Neo4j), but heavy and complex. Build time impact is significant. ArchUnit covers 90% of architectural needs with zero overhead. | Phase 2: For legacy analysis or complex cross-service dependency visualization. |
| **OpenTaint** | Advanced taint analysis (inter-procedural, Spring-aware). However, integration into Maven build is non-trivial (standalone CLI). | Phase 2: If security audit becomes a requirement. |
| **Spring Sentinel** | Very new (2026), niche Maven plugin for Spring-specific smells (N+1, `new Thread()`, OSIV). Promising, but not yet battle-tested. | Phase 2: After core stack is stable, as a Spring-specific enhancement. |
| **ApiPosture.Java** | Good for Spring Security API scanning, but limited scope (only authorization annotations). FindSecBugs covers broader security surface. | Phase 2: If OAuth2/authorization rules become complex. |

---

## 5. Integration Architecture

```
┌─────────────────────────────────────────┐
│          eventixx-parent POM            │
│  ┌─────────────────────────────────┐    │
│  │  Properties (tool versions)     │    │
│  │  Dependency (ArchUnit test)     │    │
│  │  Plugin Management              │    │
│  └─────────────────────────────────┘    │
└─────────────────────────────────────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
┌───────────┐ ┌───────────┐ ┌───────────┐
│  event-   │ │  ticket-  │ │  future-  │
│  catalog  │ │  service  │ │  service  │
│  -service │ │           │ │           │
└───────────┘ └───────────┘ └───────────┘
        │           │           │
        └───────────┴───────────┘
                    │
        ┌───────────┼───────────┐
        ▼           ▼           ▼
   SpotBugs       PMD     Checkstyle
   (verify)    (verify)   (verify)
        │           │           │
        └───────────┴───────────┘
                    │
            ArchUnit Tests
            (maven-test phase)
```

### Shared Configuration

All tool configurations live in `build-tools/` at project root to avoid duplication:

```
Eventixx/
├── build-tools/
│   ├── checkstyle/
│   │   └── checkstyle.xml          # Google Java Style adapted
│   ├── pmd/
│   │   └── pmd-ruleset.xml         # Spring-aware rules
│   └── spotbugs/
│       └── spotbugs-exclude.xml    # False-positive filters
```

Each child POM references these via relative path (`${project.basedir}/../build-tools/...`).

---

## 6. Implementation Plan

### Step 1: Create `build-tools/` Directory and Config Files

- `checkstyle.xml`: Based on Google Java Style with relaxations:
  - Line length: 120 chars (vs. Google's 100)
  - No Javadoc required for private methods, getters/setters, and test methods
  - No Javadoc required for classes in `*.config`, `*.dto`, `*.mapper` packages
- `pmd-ruleset.xml`: Includes best practices, error-prone, multithreading; excludes Lombok false positives (`UnusedPrivateField`, `BeanMembersShouldSerialize`)
- `spotbugs-exclude.xml`: Excludes `EI_EXPOSE_REP`/`EI_EXPOSE_REP2` in DTOs/Records; excludes `THROWS_METHOD_THROWS_RUNTIMEEXCEPTION` in `@ExceptionHandler`; excludes `MC_OVERRIDABLE_METHOD_CALL_IN_CONSTRUCTOR` in JPA entities

### Step 2: Update Root `pom.xml`

Add to `<properties>`:
```xml
<spotbugs.version>4.9.2</spotbugs.version>
<spotbugs-maven-plugin.version>4.9.2.0</spotbugs-maven-plugin.version>
<findsecbugs-plugin.version>1.13.0</findsecbugs-plugin.version>
<pmd.version>7.24.0</pmd.version>
<maven-pmd-plugin.version>3.26.0</maven-pmd-plugin.version>
<checkstyle.version>10.23.0</checkstyle.version>
<maven-checkstyle-plugin.version>3.6.0</maven-checkstyle-plugin.version>
<archunit.version>1.4.0</archunit.version>
```

Add ArchUnit dependency (test scope, inherited):
```xml
<dependency>
    <groupId>com.tngtech.archunit</groupId>
    <artifactId>archunit-junit5</artifactId>
    <version>${archunit.version}</version>
    <scope>test</scope>
</dependency>
```

Add plugins in `<build>`:
- `spotbugs-maven-plugin` (bound to `verify`, with FindSecBugs plugin dependency)
- `maven-pmd-plugin` (bound to `verify`, includes CPD)
- `maven-checkstyle-plugin` (bound to `verify`, config from `build-tools/`)

All plugins configured with `failOnViolation=true` (build breaks on critical issues).

### Step 3: Create ArchUnit Test Template

Create `services/event-catalog-service/src/test/java/com/eventixx/eventcatalog/arch/ArchitectureTest.java` with rules aligned to the **Layered Architecture** (`controllers`, `dto`, `entities`, `exceptions`, `repositories`, `services`, `config`):

1. **No Package Cycles:** No cyclic dependencies between top-level packages
   ```java
   slices().matching("com.eventixx.eventcatalog.(*)..").should().beFreeOfCycles()
   ```

2. **Entities Independence:** `entities` must not depend on `controllers` or `services`
   ```java
   noClasses().that().resideInAPackage("..entities..")
       .should().dependOnClassesThat()
       .resideInAnyPackage("..controllers..", "..services..")
   ```

3. **Repositories Independence:** `repositories` must not depend on `controllers`
   ```java
   noClasses().that().resideInAPackage("..repositories..")
       .should().dependOnClassesThat()
       .resideInAnyPackage("..controllers..")
   ```

4. **DTOs Independence:** `dto` must not depend on `entities` (mappers are the only allowed bridge)
   ```java
   noClasses().that().resideInAPackage("..dto..")
       .should().dependOnClassesThat()
       .resideInAPackage("..entities..")
   ```

5. **Controller Naming:** All `@RestController` classes end with `Controller`
   ```java
   classes().that().areAnnotatedWith(RestController.class)
       .should().haveSimpleNameEndingWith("Controller")
   ```

6. **Service Naming:** All `@Service` classes end with `Service`
   ```java
   classes().that().areAnnotatedWith(Service.class)
       .should().haveSimpleNameEndingWith("Service")
   ```

7. **No System.out:** Use SLF4J
   ```java
   noClasses().should().callMethod(System.out.getClass(), "println")
   ```

8. **Spring Proxy Rules:** `@Transactional` and `@Async` methods must be `public` and not `final`
   ```java
   methods().that().areAnnotatedWith(Transactional.class)
       .should().bePublic().andShould().notBeFinal()
   ```

### Step 4: Baseline and Adjust

1. Run `./mvnw verify` on `event-catalog-service`
2. Document all violations in a temporary suppression file if needed
3. Adjust `spotbugs-exclude.xml` and `pmd-ruleset.xml` for legitimate false positives
4. Re-run until clean
5. Remove temporary suppressions (fix real issues or keep justified exclusions)

### Step 5: Documentation

Update `docs/04-implementation/setup.md` with:
```bash
# Run all quality checks
./mvnw verify

# Run only static analysis (skip tests)
./mvnw verify -DskipTests

# Run only ArchUnit tests
./mvnw test -pl services/event-catalog-service -Dtest=ArchitectureTest
```

---

## 7. Success Criteria

| # | Criterion | How to Verify |
|---|---|---|
| 1 | `./mvnw verify` executes SpotBugs, PMD, and Checkstyle without plugin errors | Run command, check build success |
| 2 | SpotBugs reports zero `High`/`Critical` bugs in production code | Check `target/spotbugsXml.xml` |
| 3 | PMD reports zero `Priority 1-2` violations | Check `target/pmd.xml` |
| 4 | Checkstyle reports zero errors (warnings allowed during transition) | Check `target/checkstyle-result.xml` |
| 5 | ArchUnit tests pass in `event-catalog-service` | `./mvnw test -Dtest=ArchitectureTest` |
| 6 | New service added to parent inherits all plugins automatically | Create dummy module, run `./mvnw verify` |
| 7 | Build time overhead `< 2 minutes` for `event-catalog-service` | Measure `time ./mvnw verify` |

---

## 8. Decisions & Scope

### What This Is
- **Infrastructure/Tooling configuration** — standard practice for professional Java projects
- **Portfolio differentiator** — most GitHub portfolios have zero quality tooling

### What This Is NOT
- **An ADR** — This is a tooling choice, not an architectural system decision (no trade-off affecting service boundaries, data model, or communication patterns)
- **A Use Case** — There is no business requirement, user story, or acceptance criteria from an end-user perspective
- **A Runbook** — Operational procedures (deploy, rollback, incident response) are documented in `docs/03-operations/`

### Why No ADR?

The "Reversibility + Impact" test from `GUIDELINES.md`:
- **Reversibility:** Removing a Maven plugin is a one-line change. Fully reversible.
- **Impact:** Affects build pipeline only; does not change runtime behavior, API contracts, or data models.

Therefore, this plan lives as a standalone implementation document, not as a formal ADR.

---

## 9. Future Enhancements (Phase 2)

| Enhancement | Tool | Trigger |
|---|---|---|
| Custom security rules for endpoints | Semgrep | When API surface grows > 20 endpoints |
| Taint analysis for complex input flows | OpenTaint | When payment/checkout flow is implemented |
| Spring-specific smell detection (N+1, OSIV) | Spring Sentinel | When JPA complexity increases |
| Cross-service dependency visualization | jQAssistant | When > 5 services exist |
| Mutation testing | PIT (PITest) | When test coverage > 70% |

---

## 10. References

- [SpotBugs Documentation](https://spotbugs.readthedocs.io/)
- [FindSecBugs Patterns](https://find-sec-bugs.github.io/bugs.htm)
- [PMD Rules](https://pmd.github.io/pmd/pmd_rules_java.html)
- [Checkstyle Google Style](https://github.com/checkstyle/checkstyle/blob/master/src/main/resources/google_checks.xml)
- [ArchUnit User Guide](https://www.archunit.org/userguide/html/000_Index.html)
- [Eventixx Project Charter](../docs/00-charter/PROJECT_CHARTER.md)
- [Eventixx Documentation Guidelines](../docs/GUIDELINES.md)

---

> *"Tools don't write good code — developers do. But tools catch the mistakes that fatigue and AI hallucinations introduce."*
