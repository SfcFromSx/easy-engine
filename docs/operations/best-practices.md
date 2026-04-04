# Best Practices & Coding Standards

This document is the central repository for generalized engineering rules, architectural patterns, and code style preferences derived from active development and task post-mortems.

The foreman updates this document dynamically whenever a task resolving a bug or code style issue requires a generalized rule to prevent future regressions.

## Rules

*(Rules generated from task completions are appended below)*

- Keep all environment-specific configuration in the module's three Spring profile files: `application-dev.yml`, `application-test.yml`, and `application-pro.yml`. Do not hard-code environment-specific endpoints, ports, datasource URLs, credentials, Redis settings, Flyway settings, or similar deployment/test configuration in Java code, test code, shell scripts, Maven defaults, or per-test property files.
- When a checked-in table structure changes, rewrite the canonical historical `CREATE TABLE` migration to the final shape and remove patch-style `ADD COLUMN` drift from the fresh-schema path. If legacy upgrades still need schema normalization, use an explicit table rebuild flow instead of accumulating incremental DDL patches.
- Validate compatibility-shim request envelopes before routing, cache, or datasource work. Reject blank or unsupported requests through the module's established error contract instead of letting them fall through to downstream execution logic.
- Use `MEDIUMTEXT` (16MB) instead of default `TEXT` (64KB) for all database columns that store raw SQL text, complex JSON snapshots, error samples, or evaluation reports. This ensures storage capacity for large, multi-thousand-line SQL queries and associated audit trails without truncation.
