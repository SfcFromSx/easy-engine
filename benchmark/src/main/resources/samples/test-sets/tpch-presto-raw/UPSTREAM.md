## Upstream Source

- Repository: `trinodb/trino`
- Source URL: `https://github.com/trinodb/trino/tree/master/core/trino-parser/src/test/resources/tpch/queries`
- Download method: extracted from `https://github.com/trinodb/trino/archive/refs/heads/master.zip`
- Snapshot commit: `efd62efff42aefa0cf75c4d04e07f68100457012`
- Visible license: Apache License 2.0 (`LICENSE` copied from the upstream repository root)

## Notes

- `q01.sql` through `q22.sql` are the raw upstream TPC-H query templates copied without local edits.
- The benchmark-ready workbook at `../tpch-presto-import.xlsx` is a local normalization of those public queries for Easy Engine's test-set import contract.
- Normalization adds the `ENGINE=presto_local` routing comment to every SQL row and rewrites query 15 into a single statement because benchmark test-set items expect one executable SQL statement per row.
