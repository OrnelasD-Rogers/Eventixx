# agent-improver Lessons

> Lessons learned from improving agents. Append after each improvement cycle.

---

## 2026-05-16

- **Agent**: code-writer
- **Change**: 4 fixes (bash patterns, skill permission, PMD thresholds, Javadoc exemptions)
- **Impact**: Not yet measured (evals needed), estimated +5-10 pp on code-writer reliability
- **Confidence**: alta
- **Lesson 1 — Bash permission patterns**: The OpenCode runtime strips `./` from command invocations before matching against permission patterns. Always add patterns both WITH and WITHOUT `./` prefix (e.g., both `"./mvnw compile*"` and `"mvnw compile*"`).
- **Lesson 2 — Skill permission block**: When a permission block exists in the frontmatter, tools NOT listed are effectively denied. If an agent needs to load skills, it must have `skill:` explicitly in the permission block. Don't assume `read: allow` implicitly allows tool access.
- **Lesson 3 — PMD thresholds must be in the prompt**: LLMs don't know project-specific PMD thresholds (e.g., CouplingBetweenObjects ≤ 20). If the thresholds aren't in the prompt, the agent writes code that violates them. Always include a thresholds table.
- **Lesson 4 — Javadoc exempt annotations**: The list of annotations exempt from Javadoc must be exhaustive. An ambiguous "etc." causes agents to guess, and they guess wrong (e.g., omitting @Component). Spell out the full list.

## Triggers for future improvements

- When bash commands silently fail (no error output but command doesn't execute), suspect `./` prefix mismatch in permission patterns
- When an agent references loading a skill in its prompt but that skill never loads, check if `skill:` is in the permission block
- When quality-runner reports CouplingBetweenObjects violations, check if the agent's prompt has the PMD threshold table
