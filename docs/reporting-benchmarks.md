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

## Domain and expected values

The default seed is 669850. Business dates span June through September 2026; report bounds are inclusive Brazilian dates converted to half-open UTC timestamps at America/Sao_Paulo. Execution states are approximately 70% completed and 10% each planned, in progress and cancelled. Organization ownership is approximately 80/20. Fixed modular distributions intentionally combine absent/zero measurements, repeated sections, multiple receipts/crew members, inactive receipts, unlinked collections and reversals after original sale dates. This is a designed workload, not a claim about customer distributions.

The generator follows real parent keys, execution lifecycle constraints and organization boundaries. Sales have rounded line totals and stock provenance; opening donations/sorting create lots, posted sales debit the ledger and reversed sales compensate it. Attachment references have a maintained valid PDF fixture. Triggers and foreign keys remain enabled. An SQL hash identifies the deterministic logical scenario; operational database metadata and physical timings are not deterministic. Volume counts and conservation results are saved in `scenario-N.json`.

The independent oracle uses generated logical facts, scaled BigInt arithmetic and explicit cohort selection. It does not import backend calculations or execute reporting SQL. All measured responses must match exact quantities, nulls, inclusion/exclusion counts and six-place HALF_UP ratios, plus organization, date bounds, timezone and definition version. Unit examples exercise weighted ratios, duplicate sections, independently dated receipts, reversed revenue, null/zero distinction and rejected wrong totals. SQL conservation checks additionally compare collection inputs, sale lines, stock lots, balances and movements.

## Measurement method and evidence

HTTP latency covers the real authenticated request through receipt of the complete response body. Parsing and correctness assertions follow the latency timestamp but are included in the closed-loop elapsed time and throughput. Successful latencies determine percentiles; error count and messages are separate. Four scenario filters cover the larger organization over all four months, its September subset, one route and the smaller organization. Each endpoint/scenario/concurrency combination warms up and measures separately. This is read-only HTTP workload, not write throughput or a browser render benchmark.

The runner captures actual Flyway checksums, normalized Swagger hash, source commit, image IDs, Docker/Node/OS metadata and resource limits. PostgreSQL and backend each have two CPUs and 1 GiB; PostgreSQL shared buffers are 128 MB, work memory 4 MB and max connections 40; Java heap is 256–512 MiB and Hikari maximum pool 10. Redis is isolated and authenticated but does not cache these reports. Three direct SQL plans per endpoint/scenario include execution time and buffer details. Prepared-plan diagnostics compare session-local custom/generic modes without changing the application.

Full private evidence lives beneath `.local/benchmarks/`. Public results and the final interpretation are recorded here after measurement. No keys, bearer tokens, runtime Compose environment or database dump should be published.

## Boundaries and remaining work

No production performance proof, SLO, cold-cache or steady-arrival capacity claim follows from this baseline. Development services share the Docker host; CPU/storage contention and Docker Desktop virtualization affect results. Preparation/loading duration is not report latency. Fixtures are loaded directly rather than replaying all command workflows; command receipts and write concurrency are not measured. The generator supports up to one million executions as a validation limit, not a promised memory/performance capacity; start with progressive bounded volumes.

Investigate measured SQL/HTTP differences and repeat an identical scenario before changing indexes, query shape, JDBC settings or introducing cache. Production-specific hosting, hardware, data distributions, concurrent writes and recovery objectives still require separate acceptance. The benchmark is kept in the repository for future comparisons; CI runs deterministic correctness tests, not brittle timing thresholds on shared runners.
