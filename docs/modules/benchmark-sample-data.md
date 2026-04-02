# Benchmark Sample Data

## Public TPC-H / Presto Import Sample

Easy Engine now keeps a reproducible public TPC-H / Presto-style test-set sample under:

- `benchmark/src/main/resources/samples/test-sets/tpch-presto-raw/`: raw upstream Trino TPC-H query templates plus `LICENSE` and provenance notes.
- `benchmark/src/main/resources/samples/test-sets/tpch-presto-import.preview.csv`: text preview of the normalized rows.
- `benchmark/src/main/resources/samples/test-sets/tpch-presto-import.xlsx`: benchmark-ready import workbook.

### Upstream provenance

- Primary upstream source: `https://github.com/trinodb/trino/tree/master/core/trino-parser/src/test/resources/tpch/queries`
- License visible in the upstream repository root: Apache License 2.0

### Normalization choices

- Every imported SQL row is prefixed with `/* YH_TARGET_ENGINE=presto_local */` so benchmark runs can route through `query` to the local Presto target.
- Rows are normalized into the benchmark test-set import contract columns: `sql`, `label`, `weight`, `execution_mode`, `param_json`.
- The committed workbook uses `STATEMENT` mode with `weight = 1` for every row.
- Query 15 is rewritten into a single `WITH revenue0 AS (...) SELECT ...` statement because the upstream template uses create-view / drop-view scaffolding that does not fit one-row-per-statement benchmark imports.

### Re-import

Upload the sample workbook into SQL Lib against a local benchmark service on port `8091`:

```bash
curl -fsS \
  -F "file=@benchmark/src/main/resources/samples/test-sets/tpch-presto-import.xlsx" \
  http://127.0.0.1:8091/api/v1/sql-lib/upload
```

Then create a test set in the Benchmark UI and add the imported SQL Lib rows to that test set, or call `POST /api/v1/test-sets/{id}/items/add-sql-lib` with the ordered SQL Lib IDs you want to attach.

Then verify the imported SQL Lib row count in MySQL:

```bash
mysql -h127.0.0.1 -P3307 -uengine -pengine123 engine_db \
  -e "SELECT COUNT(*) AS sql_lib_count FROM benchmark_sql_template WHERE source_filename = 'tpch-presto-import.xlsx';"
```
