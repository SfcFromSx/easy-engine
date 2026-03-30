# Benchmark Module

`benchmark` manages benchmark datasources, jobs, templates, test sets, and execution runs. It is the preferred operator interface for benchmark-driven validation of the cached JDBC path.

## Responsibilities

- Manage benchmark datasources.
- Manage SQL templates with statement and prepared execution modes.
- Manage test sets and Excel imports.
- Launch benchmark runs and store structured run reports.
- Expose preflight checks for Kylin and Presto dependencies.

## Standard Path

```text
benchmark -> kylin-jdbc-cache -> query -> Kylin or Presto
```

## Prepared Execution Notes

- Jobs without a linked test set execute from the global template pool.
- Jobs with a linked test set execute the test-set rows instead of the global templates.
- `execution_mode` supports `STATEMENT` and `PREPARED_STATEMENT`.
- `STATEMENT` executes raw SQL text.
- `PREPARED_STATEMENT` expects ordered `param_json` arrays for parameter binding.
- Template authoring and test-set import both expose `execution_mode` and `param_json` for prepared workflows.
- Benchmark runs support prepared execution more fully than the datasource debug endpoint.

## Operator Notes

- Datasources, jobs, and test sets load full lists in the frontend and now expose client-side keyword or select filters over those loaded arrays.
- Templates keep their existing request-backed keyword search and pagination behavior.
- Runs keep job selection as the primary request-backed filter; changing the selected job resets pagination to page 1 before reloading `/runs`.

## Run

```bash
docker compose up -d postgres redis
cd /Users/sfc/Documents/projects/engine/query && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/manager && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark && mvn spring-boot:run
cd /Users/sfc/Documents/projects/engine/benchmark/frontend && npm run dev
```

## Verify

```bash
mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

## Related Docs

- [docs/architecture/overview.md](/Users/sfc/Documents/projects/engine/docs/architecture/overview.md)
- [docs/operations/local-development.md](/Users/sfc/Documents/projects/engine/docs/operations/local-development.md)
- [docs/operations/validation-matrix.md](/Users/sfc/Documents/projects/engine/docs/operations/validation-matrix.md)
