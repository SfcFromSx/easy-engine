# Validation Matrix

The foreman workflow should prefer module-scoped validation over blanket repository-wide validation unless a task changes shared contracts.

Use [testing-standard.md](/Users/sfc/Documents/projects/engine/docs/operations/testing-standard.md) for the required checklist on scenario coverage and structured-artifact verification after those commands run.

## Default Validation Commands

### Manager

```bash
bash scripts/with-java8.sh mvn -q -pl analyze,manager -am test -Dspring.mvc.pathmatch.matching-strategy=ant_path_matcher
npm --prefix manager/frontend run test
npm --prefix manager/frontend run build
```

### Query

```bash
bash scripts/with-java8.sh mvn -q -pl analyze,query -am test
```

### Benchmark

```bash
bash scripts/with-java8.sh mvn -q -f benchmark/pom.xml test
npm --prefix benchmark/frontend run build
```

### Optional Smoke

```bash
bash scripts/benchmark-smoke.sh
```

## Escalate Validation Scope When

Run the Maven commands above from the repo root so the reactor can build shared
module `analyze` alongside `query` or `manager`.

- a task changes cross-service interfaces,
- a task changes routing or cache semantics,
- a task touches build or infrastructure scripts,
- a task changes shared docs or machine-state files used by the harness.
