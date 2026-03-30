# HUMAN OPERATING RULES

This repository is designed for autonomous, agent-driven development. Humans remain responsible for goals, approvals, and recovery, but should not make ad hoc edits that break agent continuity.

## Allowed Human Actions

- Update goals and priorities through [tasks.json](/Users/sfc/Documents/projects/engine/tasks.json) and plan docs under [docs/exec-plans/](/Users/sfc/Documents/projects/engine/docs/exec-plans/).
- Pause or resume the autonomous loop with `.agent/PAUSE`.
- Provide credentials, environment variables, and infrastructure.
- Review diffs, run extra validation, approve merges, and archive finished plans.
- Resolve Git topology issues such as initializing the root repository or flattening nested repositories.
- Confirm or reject entries collected in [docs/product/issues-inbox.md](/Users/sfc/Documents/projects/engine/docs/product/issues-inbox.md) before they become implementation tasks.

## Forbidden During Active Loops

Do not perform any of the following while the loop is running:

- manual code edits outside the active task,
- branch rewrites or force-pushes,
- ad hoc file moves or directory renames,
- direct edits to `.agent/config.json`, `.agent/history/`, `.agent/lock.json`, or prompt templates,
- edits to generated docs under `docs/generated/` unless the loop is paused,
- silent deletion of task files, schema files, or validation scripts.

## Pause Before Manual Work

If a human needs to modify code, prompts, task state, or repo layout:

1. Create `.agent/PAUSE`.
2. Wait for the active loop iteration to stop.
3. Make the manual change.
4. Record the reason in the relevant plan or backlog doc.
5. Remove `.agent/PAUSE` only after the workspace is stable again.

## Emergency Recovery

### Dirty Working Tree

1. Pause the loop.
2. Inspect uncommitted changes.
3. Decide whether to keep, revert, or move them into a new task.
4. Resume only after `doctor` passes.

### Failed Validation

1. Pause the loop if repeated failures continue.
2. Preserve logs from `.agent/history/`.
3. Add or update a task with the failure evidence.
4. Resume after the failure mode is documented.

### Git Conflict or Topology Drift

1. Pause the loop.
2. Fix the Git state at the repository root.
3. Review archived or newly introduced nested repository metadata before resuming autonomous work.
4. Re-run `python3 scripts/agent_loop.py doctor`.

## Intentional Constraints

- English documentation is canonical.
- Chinese mirrors are for human convenience only.
- Agents should always be able to recover context from `docs/`, `tasks.json`, and `.agent/config.json`.
- Human convenience must not override agent continuity.
