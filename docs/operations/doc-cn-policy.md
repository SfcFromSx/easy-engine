# Chinese Mirror Policy

English is the only canonical language for agent-facing documentation, prompts, and machine-readable state.

## Mirrored by Default

- root `README.md`
- `docs/operations/local-development.md`
- `docs/architecture/overview.md`

## Not Mirrored by Default

- prompt templates
- JSON schemas
- `tasks.md` (foreman writes this; mirrors not needed)
- `.agent/config.json`
- generated repo maps and execution logs

## Sync Model

- Chinese mirror files are human-facing summaries and translations.
- Mirror sync is run manually by the foreman as a doc-gardener stage step, using the source-target pairs configured in `.agent/config.json`.
- Do not invent additional mirror files unless the project explicitly asks for them.
