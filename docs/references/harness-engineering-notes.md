# Harness Engineering Notes

These notes adapt ideas from OpenAI's [Harness Engineering](https://openai.com/index/harness-engineering/) article to this repository.

## Repo-Specific Takeaways

- Keep the fast-start context tiny. `AGENTS.md` should be a compact operational map, not a long handbook.
- Push durable detail into `docs/` and let agents discover deeper context only when needed.
- Keep the repository root lean; deep navigation belongs under `docs/`, not in multiple root shim files.
- Treat documentation gardening as a recurring engineering task, not a one-time cleanup.
- Make state explicit. `tasks.json`, `.agent/config.json`, and iteration logs should be machine-readable and stable.
- Prefer deterministic harness logic for task selection and stop conditions so model behavior does not need to invent control flow.
- Guard long-running autonomy with clear pause, lock, retry, and Git-boundary rules.

## Easy Engine Implications

- The harness should refuse to run fully autonomously until root Git is valid.
- `query`, `manager`, `benchmark`, and `kylin-jdbc-cache` need concise module docs so agents can scope changes quickly.
- Chinese translations should stay limited to human-facing docs so the agent-facing surface stays compact and canonical.
