# ADR-0023: Reproducible isolated synthetic report benchmarks

- Status: Accepted; measurement evidence is maintained separately.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: owner's synthetic performance phase; continues ADR-0001, ADR-0020, ADR-0021 and ADR-0022. ADR-0014's versioned contract and applied V1-V4 remain unchanged.

## Context and decision drivers

Development records are neither a representative workload nor safe benchmark targets. Report correctness depends on completion cohorts, independent receipt dates, scoped ownership, nullable measurements, distinct sections and historical sale reversals. An uncontrolled random fixture can hide incorrect joins and make before/after comparisons meaningless. Performance evidence must preserve the same logical data and identify the exact executable image, contract, migrations and resource limits.

## Considered options

| Option | Benefits | Costs / risks | Decision |
| --- | --- | --- | --- |
| Benchmark retained development data | Little setup | Uncontrolled volume, privacy and destructive-reset risk | Rejected |
| Drive every fixture through public write workflows | Exercises write orchestration | Slow setup, nondeterministic identifiers/times; confounds read workload | Deferred to a separate write benchmark |
| Deterministic bulk fixtures in disposable migrated PostgreSQL plus actual authenticated HTTP reads | Repeatable cohorts, real constraints, independent totals, existing infrastructure | Generator and oracle need maintained tests; not write-path verification | Selected |
| Introduce distributed load infrastructure or a warehouse | Larger load generation | Unnecessary complexity before a local baseline | Deferred |

## Decision

Use Node built-ins and existing Docker/PostgreSQL/Redis/application images. Create a unique purpose-labeled Compose project, private ephemeral signing keys, a new database volume and a loopback-only API port. PostgreSQL and Redis publish no ports. Require exact resource ownership before loading or deleting anything; no arbitrary database URL is accepted. Apply real Flyway migrations through application startup. Never disable constraints or change the live schema/history.

Seed configurable execution volumes from an integer seed using stable identifiers, integer scaled quantities and fixed business timestamps. Include two organizations with skew, routes/crew snapshots, completed/planned/in-progress/cancelled states, positive/missing/zero measures, active/inactive linked and unlinked collections, duplicate section declarations, draft/posted/reversed sales, compensating stock movements and valid attachment bytes. SQL conservation checks reconcile receipt weights, line-rounded sales, stock lots, balances and the ledger.

Benchmark operations-1 and analytics-1 using their actual `/api/v1` endpoints and organization scope. Every measured response must match an independent BigInt oracle; production SQL is not used to calculate expectations. Test broad, monthly, route-filtered and second-organization cohorts. Warm up, then measure closed-loop concurrency levels with nearest-rank p50/p95/p99, throughput and error counts. Persist checkpoints and failed runs rather than discarding interruptions.

Explain the maintained SQL with PostgreSQL `EXPLAIN (ANALYZE, BUFFERS)` and separately inspect custom/generic prepared plan sensitivity when direct SQL and repeated HTTP results diverge. Diagnostic sessions do not alter application or global database configuration. Any optimization requires measured evidence and a fresh identical-workload comparison; no speculative cache, index, migration or query rewrite is part of this decision.

## Consequences and verification

- Generated data and credentials stay ignored. Retain generator, tests, preparation/execution/cleanup scripts and sanitized evidence in Git.
- Source commit and executable image may differ because benchmark tooling evolves independently; record both explicitly.
- Bulk loading verifies constraints and final read semantics, not command idempotency, event creation through services, concurrent writes or fiscal publication.
- Fixed data does not make timings deterministic: Docker, host scheduling, caches and prepared plans affect latency. Synthetic benchmarks do not prove production capacity or readiness.
- Small samples make tail percentiles exploratory; closed-loop throughput is not a constant-arrival saturation test or an agreed SLO.
- Keep L4's findings distinct from remaining browser, product, audit and deployment gates. See [the benchmark runbook](../reporting-benchmarks.md) for commands, evidence and limitations.
