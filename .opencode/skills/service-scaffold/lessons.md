# Service Scaffold — Lessons Learned

## 2026-05-14 — Initial Creation

- The service name transformation table is critical — `check-in-service` becomes `CheckInService` (PascalCase) not `CheckinService`, and `checkinservice` (flat package). Verify each convention before generating files.
- Package name derivation: remove ALL hyphens and dots. `saga-orchestrator` → `sagaorchestrator`, `check-in-service` → `checkinservice`.
- When a service has NO REST controllers, still create the `controllers/` directory as an empty package (Spring Boot scans the package tree). Alternatively, exclude from component scan explicitly.
- For events-only services (no DB), the `application-dev.yml` must still include `logging.level.com.eventixx.<package>: DEBUG` even if there's no datasource.
- Services without Eureka must set `eureka.client.enabled: false` explicitly in dev profile to avoid startup errors.
- The parent pom.xml `<modules>` section lists modules alphabetically. Always insert in the correct position.
- After adding a new module to parent pom.xml, verify the relativePath in the child pom.xml is correct: `../../pom.xml` for services at the `services/<name>/` level.
- Port allocation: check ALL docker-compose files (not just AGENTS.md) to avoid port conflicts, since some compose files may define services not yet listed in AGENTS.md.
- If the user asks for a service that already exists in AGENTS.md but hasn't been built yet, warn them and scaffold anyway — AGENTS.md is the source of truth for what should exist.
