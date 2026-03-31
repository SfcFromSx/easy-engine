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

Run the existing benchmark upload path against a local benchmark service on port `8091`:

```bash
curl -fsS \
  -F "file=@benchmark/src/main/resources/samples/test-sets/tpch-presto-import.xlsx" \
  -F "name=tpch_presto_public" \
  http://127.0.0.1:8091/api/v1/test-sets/upload
```

If `tpch_presto_public` already exists, delete the older test set first through the Benchmark UI or `DELETE /api/v1/test-sets/{id}` so the name stays unique in MySQL-backed local runs.

Then verify the imported row count in MySQL:

```bash
mysql -h127.0.0.1 -P3307 -uengine -pengine123 engine_db \
  -e "SELECT s.name, COUNT(i.id) AS item_count FROM benchmark_test_set s LEFT JOIN benchmark_test_set_item i ON i.test_set_id = s.id WHERE s.name = 'tpch_presto_public' GROUP BY s.id, s.name;"
```
