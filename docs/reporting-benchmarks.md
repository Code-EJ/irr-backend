# Synthetic report performance baseline

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Decision: [ADR-0023](adrs/0023-reproducible-synthetic-report-benchmarks.md). Report semantics remain [ADR-0021](adrs/0021-native-operational-reporting.md) and [ADR-0022](adrs/0022-approved-mvp-analytics.md).

## Prepare, execute and clean up

Run from the backend repository with Node 24, Docker Desktop's Linux engine and locally available `irr-identity-backend`, `postgres:16-alpine` and `redis:8.2-alpine` images. The backend image must contain both supported report endpoints and the current migrations. Preparation pins immutable local image IDs; it does not download images. These scripts do not read the development `.env`, JWT key or database. Keep the generated private folder out of Git and published artifacts.

```powershell
node --test scripts/benchmarks/benchmark.test.mjs
node scripts/benchmarks/prepare.mjs --image irr-identity-backend
# Set this to the exact directory printed by preparation.
$benchmarkRun = '.local/benchmarks/irr-bench-REPLACE_WITH_PRINTED_ID'
node scripts/benchmarks/seed.mjs $benchmarkRun --seed 669850 --executions 1000
node scripts/benchmarks/run.mjs $benchmarkRun --seed 669850 --volumes 1000,10000,50000 --samples 100 --warmup 10 --concurrency 1,4
# Run diagnostics after the workload ends, with the currently loaded volume.
node scripts/benchmarks/plan-modes.mjs $benchmarkRun 50000
# Retains evidence files; removes only labeled disposable containers/volume/network.
node scripts/benchmarks/cleanup.mjs $benchmarkRun
```

For a bounded exploratory large-volume run use `--volumes 50000 --samples 20 --warmup 2`. At 20 successful samples p99 equals the slowest observation; use more samples/repetitions for release tail-latency decisions. `run.mjs` reseeds each volume, so a separate seed command is optional. Archive `results.json` before another run in the same directory, because its checkpoints intentionally replace that file. Prefer a newly prepared stack for an independent before/after comparison. Cleanup is also required after failures; never run `docker system prune` for this task.

To compare a prepared-plan configuration against the identical already loaded data, first archive the baseline and then use the controlled disposable-database experiment:

```powershell
Copy-Item "$benchmarkRun/results.json" "$benchmarkRun/baseline-results.json"
node scripts/benchmarks/set-plan-mode.mjs $benchmarkRun force_custom_plan
node scripts/benchmarks/run.mjs $benchmarkRun --volumes 50000 --seed 669850 --samples 20 --warmup 2 --concurrency 1,4 --reuse
Copy-Item "$benchmarkRun/results.json" "$benchmarkRun/custom-results.json"
node scripts/benchmarks/summarize.mjs $benchmarkRun baseline-results.json custom-results.json
```

`--reuse` accepts one volume, checks the deterministic SQL hash and every public-table count, repeats grouped conservation checks and regenerates independent expected values. It does not silently reuse old expectations. In this mode `seedMs` is verification/preparation time, not initial bulk-loading time. The experiment changes only the labeled disposable database and restarts only its backend. It preserves the dataset and fixed image/resources; it does not configure the retained development application. The restart resets the connection pool and requires fresh warm-up; this is an explicit experimental difference. Docker may reassign an ephemeral host port on restart, so scripts re-inspect the owned loopback binding.

## Domain and expected values

The default seed is 669850. Business dates span June through September 2026; report bounds are inclusive Brazilian dates converted to half-open UTC timestamps at America/Sao_Paulo. Execution states are approximately 70% completed and 10% each planned, in progress and cancelled. Organization ownership is approximately 80/20. Fixed modular distributions intentionally combine absent/zero measurements, repeated sections, multiple receipts/crew members, inactive receipts, unlinked collections and reversals after original sale dates. This is a designed workload, not a claim about customer distributions.

The generator follows real parent keys, execution lifecycle constraints and organization boundaries. Sales have rounded line totals and stock provenance; opening donations/sorting create lots, posted sales debit the ledger and reversed sales compensate it. Attachment references have a maintained valid PDF fixture. Triggers and foreign keys remain enabled. An SQL hash identifies the deterministic logical scenario; operational database metadata and physical timings are not deterministic. Volume counts and conservation results are saved in `scenario-N.json`.

The independent oracle uses generated logical facts, scaled BigInt arithmetic and explicit cohort selection. It does not import backend calculations or execute reporting SQL. All measured responses must match exact quantities, nulls, inclusion/exclusion counts and six-place HALF_UP ratios, plus organization, date bounds, timezone and definition version. Unit examples exercise weighted ratios, duplicate sections, independently dated receipts, reversed revenue, null/zero distinction and rejected wrong totals. SQL conservation checks additionally compare collection inputs, sale lines, stock lots, balances and movements.

## Measurement method and evidence

HTTP latency covers the real authenticated request through receipt of the complete response body. Parsing and correctness assertions follow the latency timestamp but are included in the closed-loop elapsed time and throughput. Successful latencies determine percentiles; error count and messages are separate. Four scenario filters cover the larger organization over all four months, its September subset, one route and the smaller organization. Each endpoint/scenario/concurrency combination warms up and measures separately. This is read-only HTTP workload, not write throughput or a browser render benchmark.

The runner captures actual Flyway checksums, normalized Swagger hash, source commit, image IDs, Docker/Node/OS metadata and resource limits. PostgreSQL and backend each have two CPUs and 1 GiB; PostgreSQL shared buffers are 128 MB, work memory 4 MB and max connections 40; Java heap is 256–512 MiB and Hikari maximum pool 10. Redis is isolated and authenticated but does not cache these reports. Three direct SQL plans per endpoint/scenario include execution time and buffer details. Prepared-plan diagnostics compare session-local custom/generic modes without changing the application.

Full private evidence lives beneath `.local/benchmarks/`. Public results and the final interpretation are recorded here after measurement. No keys, bearer tokens, runtime Compose environment or database dump should be published.

The committed [sanitized measurement record](data/synthetic-report-benchmark-2026-10-10.json) contains complete recorded metric rows, counts/hashes, immutable images, environment, migration checksums, SQL buffer summaries and prepared-plan loop details. It distinguishes failed/partial runs and the isolated configuration experiment. Review the artifact before sharing it; never substitute the private run folder.

## Observed baseline and investigation, 2026-10-10

The retained application image contains backend source 996e36f; benchmark tooling has its own later commits. The host reports Windows 10.0.19045 x64, Node 24.12.0, Docker 29.2.1, 12 Docker CPUs and 16,745,209,856 bytes of Docker memory. Live development services share that host. Applied Flyway V1-V4 checksums and exact immutable image IDs are captured in the evidence.

The initial progressive `auto` plan-mode run completed 1,000 and 10,000 executions: 100 samples for each of 16 endpoint/scenario/concurrency combinations per volume, 3,200 successful measured requests, zero HTTP or expected-total errors. Its 50,000 stage failed with `fetch failed`; Docker was subsequently observed unavailable. This is an infrastructure interruption, not a measured endpoint capacity result. After restoring Docker, all five development services became healthy without resetting their volumes.

Bulk loading 50,000 executions applied real constraints successfully. The first conservation implementation used correlated subqueries without suitable fixture-side access paths and was taking minutes; only that disposable verification statement was cancelled. The harness now uses grouped conservation checks, including missing child rows via `IS DISTINCT FROM`, and passed with zero mismatches on the same committed dataset. This tooling correction does not optimize production reporting SQL.

The repeated 50,000 `auto` baseline passed broad operational requests and sequential Analytics totals. Four concurrent Analytics requests later reached the 30-second client timeout. That measurement was stopped after 18 started requests (14 successful, four timeouts) and the run is explicitly failed. Successful-only percentiles must not hide those timeouts. Remaining baseline scenarios were not measured after the failure; their later diagnostic/custom-mode results are separate evidence.

Session-local prepared-plan diagnostics on the identical 50,000 dataset used one positional parameter per named value. This first diagnostic is an approximation: JDBC repeats positional placeholders for repeated occurrences. Both the approximation and the subsequently corrected expansion are explicitly labeled in public evidence.

| Analytics scenario | Custom execution time (ms) | Generic execution time (ms) |
| --- | ---: | ---: |
| Larger organization, all months | 387.176 | Exceeded 30,000; estimated plan retained, no actual runtime claimed |
| Larger organization, September | 147.322 | 15,428.604 |
| Larger organization, one route | 142.380 | 9,883.802 |
| Smaller organization, all months | 106.653 | 16,617.811 |

Repeating diagnostics with distinct positional occurrences matching named JDBC expansion on the 10,000 dataset confirmed the same sensitivity:

| Analytics scenario | Custom execution time (ms) | Generic execution time (ms) |
| --- | ---: | ---: |
| Larger organization, all months | 74.532 | 10,776.757 |
| Larger organization, September | 27.589 | 558.608 |
| Larger organization, one route | 24.408 | 421.608 |
| Smaller organization, all months | 18.918 | 711.634 |

Generic plans contain nested loops with very small estimated execution populations, despite large actual cohorts. Optional parameter predicates and cohort selectivity make this query sensitive to prepared planning; direct literal SQL timings alone miss this behavior. The initial progressive broad 10,000 Analytics HTTP p95 was 9,506 ms sequential and 21,073 ms with four clients, while direct SQL execution was 85–161 ms. Custom/generic diagnostics support prepared-plan sensitivity as a concrete bottleneck. Connection history, mixed parameter cohorts and statistics/cache state mean that fresh larger-volume timings need not increase monotonically over earlier measurements.

The corrected 10,000 generic broad plan re-executed an aggregate 5,541 times and reported 15,015,234 rows removed by a nested-loop join filter. This is concrete repeated aggregation work, not simply slow network transport. The public plan summary retains actual loops/rows/timing for these nodes; the full private plans retain SQL operator and buffer detail.

No production query, migration, index, cache or connection setting has been changed by this investigation. The isolated custom-mode HTTP repeat and complete sanitized measurement records are the next evidence below. A production remedy must compare a bounded query-shape/JDBC planning solution against the same scenarios and verify domain and integration tests; a global custom-plan setting is not automatically the preferred application design.

### Broad Analytics HTTP comparison

All latencies are milliseconds. Throughput is started requests/elapsed seconds; errors are included. Quantiles are successful responses only. `auto` is the original disposable configuration; `custom` is the isolated experiment. All successful bodies matched independent expected values.

| Executions | Mode | Clients | Requests | p50 | p95 | p99 | Requests/s | Errors |
| ---: | --- | ---: | ---: | ---: | ---: | ---: | ---: | ---: |
| 1,000 | auto | 1 | 100 | 92.17 | 121.43 | 145.59 | 12.23 | 0 |
| 1,000 | auto | 4 | 100 | 210.09 | 280.14 | 294.16 | 18.06 | 0 |
| 10,000 | auto | 1 | 100 | 8,034.55 | 9,506.09 | 11,867.71 | 0.12 | 0 |
| 10,000 | auto | 4 | 100 | 19,483.46 | 21,073.45 | 22,241.73 | 0.22 | 0 |
| 50,000 | auto | 1 | 20 | 361.45 | 455.90 | 541.03 | 2.63 | 0 |
| 50,000 | auto | 4 | 18 | 977.28 | 1,019.71 | 1,019.71 | 0.52 | 4 timeouts |
| 1,000 | custom | 1 | 100 | 17.54 | 21.80 | 22.55 | 55.37 | 0 |
| 1,000 | custom | 4 | 100 | 18.40 | 36.36 | 54.93 | 190.68 | 0 |
| 10,000 | custom | 1 | 100 | 54.59 | 72.69 | 83.06 | 17.09 | 0 |
| 10,000 | custom | 4 | 100 | 123.20 | 196.32 | 201.55 | 28.50 | 0 |
| 50,000 | custom | 1 | 20 | 360.45 | 399.94 | 405.78 | 2.71 | 0 |
| 50,000 | custom | 4 | 20 | 1,020.03 | 1,185.73 | 1,210.59 | 3.80 | 0 |

The 50,000 auto/concurrent successful-only p95 is not a healthy result: four requests exceeded 30 seconds and are absent from that quantile. The custom experiment completed all 16 combinations at 50,000 (320 requests), and 100-sample combinations at 1,000/10,000 (1,600 each). The initial custom 1,000 stage passed completely, but its next-volume warm-up failed on a stale HTTP socket; its overall result remains failed. That transport preparation bug was corrected by draining close events after synchronous loading, without retrying measured failures. The separate 10,000 repeat passed completely. Failed/partial and complete runs remain distinguishable.

Broad operational HTTP auto-mode p95 was 16.73/66.14 ms at 1,000, 19.66/36.40 ms at 10,000 and 62.63/154.16 ms at 50,000 for one/four clients respectively; no measured operational errors occurred. Complete p50/p95/p99, throughput, errors, dataset hashes/counts and SQL buffer summaries for every recorded scenario are retained in sanitized evidence. This is a configuration comparison with explicit restart/warm-up differences, not an application optimization release.

### Reproducibility and final harness verification

The final corrected cross-volume run regenerated 1,000 then 10,000 executions in the same stack, with 20 samples, two warm-ups and one/four clients. All 32 combinations and 640 measured requests passed. This specifically validates the transition after synchronous preparation that previously exposed stale keep-alive state; the earlier failed run is retained rather than rewritten.

All recorded comparisons have byte-identical deterministic SQL per volume:

| Executions | Collections | Sales | SQL SHA-256 |
| ---: | ---: | ---: | --- |
| 1,000 | 1,343 | 200 | `f7815dda4eb85bff9aa983733a865105bae03855dec1d2692e6903a8e1ac16fd` |
| 10,000 | 13,488 | 2,000 | `0705608f816787c03bd3275f4262a296e2e6b5bcc5b9b08a12576c5bf233b1c5` |
| 50,000 | 66,968 | 10,000 | `e16c193aff6d5df65636e8b938bb099ef4ecb4783a461f9cc8720eed303c6f31` |

Across recorded measured runs, 7,438 requests were started, 7,434 succeeded with exact expected totals and four timed out. These are accumulated measurements including repeats/experiments, not one sustained load run. Warm-up/initial-validation/setup failures are separately recorded and are not silently added to measured sample denominators. The SQL dataset hashes remained identical across before/after experiments. Eight benchmark regression tests and six recovery guard tests passed. No Java or frontend executable code changed, so this phase did not rerun unrelated full Maven/browser suites; their previous evidence remains dated.

Final cleanup passed: only the purpose-labeled benchmark containers, volume and network were removed; private evidence files remained. The corrected plan-mode helper successfully restored `auto` in the disposable database and refreshed its reassigned loopback port before cleanup. All five retained development services remained healthy; backend readiness at port 9292 and frontend login at port 5173 returned HTTP 200. Neither the retained development database nor its configuration was reset or benchmark-seeded.

## Boundaries and remaining work

No production performance proof, SLO, cold-cache or steady-arrival capacity claim follows from this baseline. Development services share the Docker host; CPU/storage contention and Docker Desktop virtualization affect results. Preparation/loading duration is not report latency. Fixtures are loaded directly rather than replaying all command workflows; command receipts and write concurrency are not measured. The generator supports up to one million executions as a validation limit, not a promised memory/performance capacity; start with progressive bounded volumes.

Investigate measured SQL/HTTP differences and repeat an identical scenario before changing indexes, query shape, JDBC settings or introducing cache. Production-specific hosting, hardware, data distributions, concurrent writes and recovery objectives still require separate acceptance. The benchmark is kept in the repository for future comparisons; CI runs deterministic correctness tests, not brittle timing thresholds on shared runners.
