# Best Practices & Coding Standards

This document is the central repository for generalized engineering rules, architectural patterns, and code style preferences derived from active development and task post-mortems.

The foreman updates this document dynamically whenever a task resolving a bug or code style issue requires a generalized rule to prevent future regressions.

## Rules

*(Rules generated from task completions are appended below)*

- Keep environment-specific endpoints, usernames, and passwords out of Java `@ConfigurationProperties` defaults. Bind them from Spring configuration instead so operators can override them without code changes and tests must set them explicitly.
- Keep datasource, probe endpoint, and credential fixtures for tests in test-owned property files or test property sources instead of hard-coding them inside test methods.
- Validate compatibility-shim request envelopes before routing, cache, or datasource work. Reject blank or unsupported requests through the module's established error contract instead of letting them fall through to downstream execution logic.
