# Documentation Framework — Guidelines

> **Version:** 2.0
> **Last updated:** 2026-04-29
> **Scope:** Process rules for creating, reviewing, and maintaining documentation in any project that uses this framework.

---

## 1. Overview

This framework organizes software documentation into 4 hierarchical levels:

```
LEVEL 1 — DIRECTION      → PROJECT_CHARTER.md
LEVEL 2 — DECISION       → ADRs (Architecture Decision Records, when trade-offs exist)
LEVEL 3 — SPECIFICATION  → Use Cases (self-contained: flow + tasks), data-model, API, sequence diagrams, glossary
LEVEL 4 — OPERATION      → Deployment, runbooks, testing, security
```

Every project that uses this framework must create its own documentation derived from the templates in `references/`.

---

## 2. When to Create Each Document

| Document | Required? | When to create |
|---|---|---|
| `PROJECT_CHARTER.md` | **YES** | First file of any project. Before any code or UC. If it does not exist, use the discovery process (Step 0) to create it. |
| `use-cases/UC-XXX-title.md` | **YES** | After charter is accepted, before coding. Each UC is self-contained and includes its own implementation plan. |
| `ADR-XXX-*.md` | Yes (if trade-off exists) | During grooming of a UC, **before** implementation. Only non-trivial decisions that affect architecture. |
| `data-model.md` | **YES** (if there is a database) | Before the first migration/schema. |
| `api-contracts/` | **YES** (if there is an API) | Before implementing the first endpoint. |
| `sequence-diagrams.md` | Optional | When the flow involves >3 services or is critical. |
| `glossary.md` | Optional | When the domain has >10 specific terms or ambiguities. |
| `deployment.md` | **YES** | Before the first deploy, even if local. |
| `runbooks/` | Optional | After the first incident or when the system goes to production. |
| `testing-strategy.md` | Optional | When there are critical NFRs (performance, consistency, security). |
| `security-guide.md` | Optional | When there is authentication, authorization, or sensitive data. |
| `setup.md` | **YES** | Always. "How to run this project in 5 minutes." |
| `CONTRIBUTING.md` | Optional | When other people can contribute. |

---

## 3. Use Cases as the Center of the Workflow

Use cases are the **primary executable specification**. Every UC file (`UC-XXX-*.md`) is a single self-contained document that includes:

1. **Flow & Requirements** — what the system must do
2. **Architecture Decisions** — references to ADRs or inline decisions
3. **Implementation Plan** — tasks broken down for AI-agent execution
4. **Acceptance Criteria** — how to verify completion

### UC Triggers ADR

A UC exposes an architectural trade-off **if**:
- It spans >2 services → potential integration pattern ADR
- It has strict latency/SLA requirements → potential caching or async ADR
- It has consistency requirements → potential data model or transaction ADR
- It introduces a new technology or pattern not yet used in the project

When a trade-off is detected:
1. Pause UC implementation planning
2. Write/update the ADR (status: Proposed → Accepted)
3. Return to UC and reference the ADR in the "Architecture Decisions" section
4. Proceed with task decomposition

### UC Does NOT Trigger ADR

A UC does **not** need an ADR **if**:
- It applies an existing ADR or Charter principle directly
- The change is localized to a single endpoint/module
- It uses a library/pattern already consolidated in the project
- It configures an existing parameter

In these cases, the UC contains an **inline decision** in its "Architecture Decisions" section, explaining why no formal ADR is needed.

---

## 4. Criteria for Writing an ADR

Use the **"Reversibility + Impact"** test:

| Write an ADR | Do not write an ADR |
|---|---|
| Choice that is hard to reverse after implementation | Use of a library/pattern already consolidated in the project |
| Affects multiple services or components | Change localized to a single endpoint/module |
| Introduces new technology or pattern | Refactoring that keeps existing decisions |
| Resolves a significant architectural trade-off | Configuration of an existing parameter |
| Violates or reinforces a Charter principle | Direct application of an already established principle |

---

## 5. Workflow

```
STEP 0: Charter Conception (Optional — only if PROJECT_CHARTER.md does not exist)
    ├── Path A: AI-Guided Discovery (Recommended)
    │         └── Agent uses charter-discovery-guide.md to interview the user
    │         └── 6 phases: Vision+Success Metrics → Scope (with MoSCoW) → Anti-Scope
    │             → Tech Stack → Assumptions & Constraints → Principles
    │         └── Language: same as the conversation
    │         └── Outputs filled charter-worksheet.md
    │         └── Generates PROJECT_CHARTER.md from charter-template.md
    │
    ├── Path B: Self-Service
    │         └── User fills charter-worksheet.md manually
    │         └── Generates PROJECT_CHARTER.md from charter-template.md
    │
    └── Path C: Charter Already Exists → Skip to Step 1
           │
           ▼
STEP 1: Extract Use Cases from Charter
    └── Convert charter FRs into UC-{NUMBER}-{title}.md files
    └── Place in docs/02-specs/use-cases/
           │
           ▼
STEP 2: Groom UC — Detect architectural trade-offs?
    ├── YES → Write ADR (status: Proposed)
    │         └── Use ADR-TEMPLATE.md
    │         └── Place in docs/01-decisions/
    │         └── Review → status: Accepted
    │         └── Return to UC, reference ADR
    │
    └── NO  → Document inline decision in UC
           │
           ▼
STEP 3: Write Implementation Plan inside UC
    └── Use UC-TEMPLATE.md sections: Architecture Decisions, Implementation Plan
    └── Decompose into Tasks 1-N (not strictly 5; adapt to UC complexity)
           │
           ▼
STEP 4: Execute tasks with AI agent
    └── For each task:
        1. Copy the prompt from the UC
        2. Paste into the agent + repository context
        3. Review the result
        4. Mark as Completed in the UC
        5. Log adjustments and lessons
           │
           ▼
STEP 5: Finalize
    └── Update UC status → Completed
    └── Update specs (data-model, api-contracts) if needed
    └── Fill in "Lessons Learned" inside the UC
    └── Mark completion checklist
```

---

## Charter Conception Details

### When to Run Step 0

Run Step 0 **only** if `PROJECT_CHARTER.md` does not exist or is a placeholder with more than 50% of fields empty/TBD. If the user says "I already know what I want" but has no written charter, still run a lightweight discovery — 3-5 targeted questions are usually enough to prevent scope drift later.

### Path A: AI-Guided Discovery

1. The agent reads `references/charter-discovery-guide.md`.
2. The agent conducts a structured interview across 6 phases:
   - **Phase 1:** Vision, Purpose, and Success Metrics (with Jobs-to-be-Done framing)
   - **Phase 2:** Functional Scope with MoSCoW prioritization
   - **Phase 3:** Anti-Scope (Out-of-Scope)
   - **Phase 4:** Tech Stack
   - **Phase 5:** Assumptions & Constraints (timeline, budget, compliance, risks)
   - **Phase 6:** Architectural Principles
3. Between every phase, the agent applies the **confirmation loop**: summarize what was understood, ask the user to confirm or correct, and only then proceed.
4. The agent adapts questions based on project type (web, mobile, API, data pipeline, AI/ML, etc.).
5. After the interview, the agent fills `references/charter-worksheet.md` as a living draft.
6. The agent reads `references/charter-template.md` and generates the final `PROJECT_CHARTER.md`.
7. The agent presents the charter and iterates based on user feedback.

**Language:** The entire interview happens in the same language the user is speaking. The final charter may be generated in the project's preferred language (often English for technical teams, but not required).

### Path B: Self-Service

1. The user reads `references/charter-worksheet.md`.
2. The user fills the worksheet at their own pace.
3. The user hands the completed worksheet back to the agent (or uses it themselves with `charter-template.md`).
4. The agent converts the worksheet into a formatted `PROJECT_CHARTER.md`.

This path is useful for:
- Asynchronous workflows (the user is not available for a live interview)
- Users who prefer to think in writing
- Refining an existing rough draft

### Path C: Skip

If `PROJECT_CHARTER.md` already exists and is accepted, skip directly to Step 1 (Extract Use Cases).

### Quality Expectations

A charter is "good enough" when:
- The vision can be explained in one sentence using the JTBD frame
- At least 1 success metric with baseline and target is defined
- The scope has 3-5 named functional areas, each with MoSCoW priority
- The anti-scope has at least 2 deliberate exclusions
- The stack is plausible for the domain
- Assumptions & constraints are documented (timeline, budget/resource, at least 1 assumption)
- There is at least 1 testable architectural principle

If any of these are weak, the agent should suggest refinement but not block progress. It is better to ship a draft charter and revisit it during UC grooming than to spend hours perfecting it upfront.

---

## 6. Naming Conventions

### ADRs
- Format: `ADR-{NUMBER}-{short-title}.md`
- Example: `ADR-001-jwt-rs256-jwks.md`
- Sequential numbering, does not restart between projects

### Use Cases
- Format: `UC-{NUMBER}-{short-title}.md`
- Example: `UC-004-reserve-tickets.md`
- Sequential numbering, does not restart between projects

### Directories
```
docs/
├── 00-charter/         → Vision and purpose
├── 01-decisions/       → ADRs (pure decision records)
├── 02-specs/           → Use Cases (self-contained: flow + tasks), data-model, api-contracts, sequence-diagrams, glossary
├── 03-operations/      → Deploy, runbooks, security
└── 04-implementation/  → Setup, contributing
```

---

## 7. Review and Maintenance

- **Charter:** Review when there is a scope or principles change
- **ADRs:** Status must reflect reality (Deprecated, Superseded when applicable). ADRs are immutable once Accepted — if the decision changes, write a new ADR that supersedes the old one.
- **Use Cases:** Status reflects implementation state (Draft → In Progress → Completed). Update whenever implementation diverges from the plan.
- **Specs:** Update whenever implementation diverges from the plan

---

## 8. Integration with Agile

| Agile Artifact | Framework Document | Relationship |
|---|---|---|
| Feature / Epic | Charter | The Charter defines the scope of the Feature |
| User Story | Use Case (UC-XXX-*.md) | Each UC details system behavior and contains the implementation plan for the agent |
| Technical task | UC Implementation Plan | Each task in the UC becomes a prompt for the agent |
| Spike / Research | ADR | The result of the spike is recorded as an ADR |
| Retrospective | Lessons Learned (inside UC) | Implementation feedback becomes documented knowledge |

---

> *"Living documentation is better than perfect documentation. Prefer updating frequently over trying to get it right the first time."*
