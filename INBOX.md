# Inbox

This is the canonical repo-root inbox for agent-found issues and suggestions that still require human review, judgment, approval, or prioritization before they become implementation work.

## Rules

- Record items here only when they are not already tracked in `tasks.md`.
- Keep entries evidence-based and concise.
- Use this inbox for defects, harness/process concerns, and improvement suggestions that still need human judgment.
- Task-specific failures that are already fully captured in a task's progress log do not need a duplicate inbox entry.
- Systemic harness, tooling, process, or documentation issues discovered while working another task belong here only when they still need human judgment, approval, prioritization, or task shaping.
- **Do not** log generalized coding practices, codebase styling rules, or post-mortem fixes here. These belong in task progress logs and `docs/operations/best-practices.md`.
- Do not add audit-only entries for deterministic task intake, task closeout, harness-document edits, or best-practice bookkeeping when the human has already requested the work and no further decision is needed.
- When a task blocks at max attempts, its latest rejected or revalidation note in `tasks.md` must include `Escalation: none` or `Escalation: INBOX-...` so humans can tell whether the issue stayed task-local or was escalated here.
- Do not convert inbox entries directly into implementation tasks without explicit human confirmation.
- For tooling or infrastructure changes that still need approval, prefer logging the issue here before changing the harness.
- Legacy harness audit-trail entries below are historical records; do not keep creating new entries in that style.

## Open Items

No open inbox items at the moment. Re-checked historical entries should be recovered from Git history or the completed-task archive rather than restored here unless a fresh issue still needs human judgment.

## Template

```md
### INBOX-YYYYMMDD-001
- Area:
- Related task(s):
- Summary:
- Evidence:
- Impact/Risk:
- Suggested next step:
- Human decision: pending
```
