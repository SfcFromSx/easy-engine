# Human Collaboration

This document describes what humans should do in the task-driven foreman workflow.

## Allowed Human Actions

- Update active goals and priorities through [tasks.md](/Users/sfc/Documents/projects/engine/tasks.md), and review completed-task history through [tasks-done.md](/Users/sfc/Documents/projects/engine/tasks-done.md), plus plan docs under [docs/exec-plans/](/Users/sfc/Documents/projects/engine/docs/exec-plans/).
- Pause work by telling the foreman to stop; resume by directing it to continue.
- Provide credentials, environment variables, and infrastructure.
- Review diffs, run extra validation, approve merges, and decide whether to push or publish.
- Resolve Git topology issues such as initializing the root repository or flattening nested repositories.
- Confirm or reject entries collected in [INBOX.md](/Users/sfc/Documents/projects/engine/INBOX.md) when they genuinely require human judgment before they become implementation tasks.
- Directly request harness-framework changes as tasks when the desired outcome is already decided; use the inbox only for unplanned harness issues that still need human judgment.

## Avoid During Active Work

Do not perform any of the following while the foreman is actively working a task:

- manual code edits outside the active task,
- branch rewrites or force-pushes,
- ad hoc file moves or directory renames,
- direct edits to `.agent/config.json`,
- edits to generated docs under `docs/generated/` unless work is paused,
- silent deletion of task files, schema files, or validation scripts.

## Pause Before Manual Changes

If a human needs to modify code, prompts, task state, or repo layout:

1. Tell the foreman to stop and wait for the current stage to finish.
2. Make the manual change.
3. Record the reason in the relevant plan, backlog, or inbox entry.
4. Resume only after the workspace is stable again.

## Recovery

### Dirty Working Tree

1. Tell the foreman to stop.
2. Inspect uncommitted changes.
3. Decide whether to keep, revert, or move them into a new task.
4. Resume once the working tree is clean.

### Failed Validation

1. Stop the foreman if failures keep repeating.
2. Add or update a task in `tasks.md` with the failure evidence.
4. Resume after the failure mode is documented.

### Git Conflict or Topology Drift

1. Stop the foreman.
2. Fix the Git state at the repository root.
3. Review archived or newly introduced nested repository metadata before resuming work.

## Constraints

- English documentation is canonical.
- Chinese mirrors are for human convenience only.
- Agents should always be able to recover context from `docs/`, `tasks.md`, `tasks-done.md`, `INBOX.md`, and `.agent/config.json`.
- Human convenience must not override agent continuity.
