# Analytics query planning correction

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Continues Master Plan L4.1 and [ADR-0024](adrs/0024-analytics-execution-fact-aggregation.md). The original [synthetic baseline](reporting-benchmarks.md) is retained as historical before-change evidence.

## Implementation and semantics

Backend commit `ebea9f2` replaces the receipt aggregate joined back into execution facts with a scoped execution/collection left join grouped once per execution. The existing collection organization/execution index supports individual receipt lookup when the planner chooses nested loops. Completed executions without active linked receipts still contribute to comparison/quality populations with a receipt count of zero and unavailable receipt mass. Multiple receipts contribute their mass once, while duration/distance denominators remain one per eligible execution.

Coverage continues at unique execution/source-section grain; fiscal revenue remains independently bounded by sale date and current posting state. Nulls, zeros, exact decimal serialization, weighted ratios, original date boundaries, authorization and `analytics-1` are unchanged. The generated Swagger hash matches the previous application. No schema/dependency or datasource-wide planning option was introduced.

## Verification method

Use the maintained benchmark scripts with the actual corrected image, seed 669850, the original 1,000/10,000/50,000 logical volumes and the same four filters. Normal `auto` planning is mandatory for the HTTP workload. Hashes must match the original datasets. Independently validate every successful response; retain failed before-change evidence. Forced generic/custom diagnostics must use distinct positional occurrences matching named JDBC expansion and report actual aggregate loops, not only elapsed time.

```powershell
docker build -t irr-analytics-planning:ebea9f2 .
node scripts/benchmarks/prepare.mjs --image irr-analytics-planning:ebea9f2
# Replace with the exact directory printed by preparation.
$planningRun = '.local/benchmarks/irr-bench-REPLACE_WITH_PRINTED_ID'
node scripts/benchmarks/run.mjs $planningRun --seed 669850 --volumes 1000,10000,50000 --samples 20 --warmup 10 --concurrency 1,4
node scripts/benchmarks/plan-modes.mjs $planningRun 50000
node scripts/benchmarks/summarize.mjs $planningRun results.json
node scripts/benchmarks/cleanup.mjs $planningRun
```

Twenty samples make tail percentiles exploratory; p99 is the maximum. The original 1,000/10,000 baseline has 100 samples per combination, while its 50,000 repeat has 20 and stopped on errors. This difference is explicit; timing comparisons are indicative, not a statistical capacity or production SLO proof. The same host also runs development services. Initial smaller-volume measurements overlapped verification processes; the large-volume measurement starts after those tests ended.

## Java and local runtime verification

Original-repository Spotless apply/check and Linux Docker build passed with the existing configuration. A public-source verification copy passed full Maven `clean verify`: 51 unit and 96 PostgreSQL/Redis integration tests, no failures/errors/skips. The weighted/cohort correction test now exercises seven report reads and live receipt correction under both forced prepared modes on a transaction-bound connection; its planner setting expires with that test transaction.

The initial Windows Maven process failed inside the JVM during Spotless with `EXCEPTION_ACCESS_VIOLATION`. The source copy also initially lacked `.gitattributes` and reported newline differences. Verification was completed after including the repository attributes and using temporary `MAVEN_OPTS=-XX:TieredStopAtLevel=1 -Xmx1024m` for the local Maven process. Neither workaround modifies the repository's Maven configuration or the runtime image. The successful full verification, rather than either failed attempt, is the Java acceptance evidence.

Eight benchmark regression tests and six recovery guard tests pass. The local selected-stack backup completed and restored backend health before the application rollout.

## Corrected performance evidence

The [sanitized measurement record](data/analytics-query-planning-2026-10-10.json) contains complete percentiles, throughput, errors, query buffer summaries, image IDs, migration checksums and aggregate loop counts. All three volumes passed all 16 combinations: 960 successful measured HTTP responses, zero errors and exact independent expected totals. Dataset SQL hashes match the original baseline for every volume. The application used normal `auto` planning throughout this workload.

Broad Analytics HTTP results, milliseconds:

| Executions | Clients | Samples | p50 | p95 | p99 | Requests/s | Errors |
| ---: | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1,000 | 1 | 20 | 25.86 | 38.31 | 57.82 | 35.68 | 0 |
| 1,000 | 4 | 20 | 54.14 | 82.16 | 98.61 | 68.91 | 0 |
| 10,000 | 1 | 20 | 95.26 | 107.09 | 117.17 | 10.40 | 0 |
| 10,000 | 4 | 20 | 200.16 | 203.62 | 206.47 | 19.93 | 0 |
| 50,000 | 1 | 20 | 397.62 | 471.23 | 511.44 | 2.50 | 0 |
| 50,000 | 4 | 20 | 881.64 | 989.37 | 997.29 | 4.45 | 0 |

The original broad 10,000 p95 was approximately 9.5 seconds sequential and 21.1 seconds with four clients, with 100 samples. The original 50,000 concurrent repeat timed out four requests; the corrected repeat completed every request. Sample/warm-up counts and process contention are explicit above; this is evidence of a corrected bottleneck, not production capacity certification.

At 50,000, prepared diagnostics matching JDBC positional expansion:

| Scenario | Custom SQL (ms) | Generic SQL (ms) | Maximum actual aggregate loops |
| --- | ---: | ---: | ---: |
| Larger organization, all months | 376.871 | 449.016 | 1 |
| Larger organization, September | 172.532 | 139.102 | 1 |
| Larger organization, one route | 151.205 | 110.300 | 1 |
| Smaller organization, all months | 101.545 | 130.196 | 1 |

All eight diagnostic executions completed within the bounded statement limit. The original corrected-positional 10,000 generic broad plan had 5,541 aggregate rescans; the new larger-volume plans perform each aggregate once. Existing indexes and normal planner settings suffice for this tested workload.

## Local rollout and retained data

The verified image `sha256:a7df8a827cf2e0b7f430eb99be02e22b7d1f94f1980afcfb33070dfec2d72111` was tagged `irr-identity-backend`, then only the selected local backend service was recreated with `--no-build --pull never`. The database, Redis, Mailpit, frontend, private configuration and mounted upload volume were preserved. A selected-stack private backup was taken first and records the previous immutable image for recovery.

Before the frontend contract fixtures ran, all 39 public-table counts exactly matched the backup. After verification, V1-V4 checksums and every original attachment file hash still matched (21 files). The live database reports `plan_cache_mode=auto`, and the live backend image matches the measured corrected image. No runtime planner override, database reset or migration rewrite occurred.

The frontend's actual generated Swagger comparison passed after removing only environment-specific server URLs; `npm run api:check` passed for 116 supported versioned operations. `npm run test:contract` passed all 26 cases against the corrected live backend; disposable fixtures were compensated/deactivated through the existing test cleanup. No frontend types/services change was necessary because the supported contract and definitions are identical. This is API integration evidence; it does not claim new browser/operator acceptance.

Only purpose-labeled benchmark resources were removed; evidence remains ignored locally and sanitized in Git. All five development services are healthy. L4.1 is complete for the locally reproduced problem. Remaining Master Plan browser/accessibility, audit, product and deployment gates continue separately.
