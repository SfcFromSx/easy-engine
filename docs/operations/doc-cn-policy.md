# Chinese Mirror Policy

English is the only canonical language for agent-facing documentation, prompts, and machine-readable state.

## Mirrored by Default

- root `README.md`
- `HUMAN.MD`
- `docs/operations/local-development.md`
- `docs/architecture/overview.md`

## Not Mirrored by Default

- prompt templates
- JSON schemas
- `tasks.json`
- `.agent/config.json`
- generated repo maps and execution logs

## Sync Model

- Chinese mirror files are human-facing summaries and translations.
- `sync-doc-cn` refreshes mirror metadata and verifies coverage for the configured source-target pairs in `.agent/config.json`.
- The harness should not invent additional mirror files unless the project explicitly asks for them.
