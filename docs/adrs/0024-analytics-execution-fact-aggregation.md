# ADR-0024: Aggregate Analytics receipt facts once per execution

- Status: Proposed; candidate implementation and verification in progress.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: Master Plan L4.1, continuing [ADR-0022](0022-approved-mvp-analytics.md) and [ADR-0023](0023-reproducible-synthetic-report-benchmarks.md). ADR-0003's modular monolith and ADR-0014's `/api/v1` contract remain authoritative.

## Context and decision drivers

The synthetic baseline found a prepared-plan bottleneck in the Analytics receipt population. At 10,000 executions, the reproduced generic plan executed the grouped receipt subquery 5,541 times and rejected 15,015,234 rows in a nested-loop join. The broad HTTP p95 reached 9.5 seconds sequential and 21.1 seconds with four clients. At 50,000, four concurrent requests timed out after 30 seconds. Exact totals remained correct in successful responses.

The isolated custom-plan experiment demonstrates that query planning matters, but configuring all application statements to use custom plans would affect unrelated workloads. We need a bounded query correction preserving independent fact grains, scoped ownership, nullable quantities, weighted denominators and the existing response/definition version.

## Considered options

| Option | Benefits | Costs / risks | Decision |
| --- | --- | --- | --- |
| Force custom plans across the datasource/database | Demonstrated local improvement | Global planner policy and additional planning cost for unrelated statements | Rejected as the default application remedy |
| Add speculative indexes/cache | May help selected access paths | Existing linked-collection index already exists; stale reports and unmeasured write cost | Deferred |
| Materialize the receipt aggregate only | Prevents repeated aggregate execution | Still permits a poorly estimated nested-loop join between two large populations | Considered, not selected initially |
| Aggregate the scoped execution/collection left join directly at execution grain | Eliminates the repeatedly evaluated grouped subquery; existing index supports bounded receipt lookup | Grouping includes each execution snapshot field; compare both prepared modes | Selected candidate |

## Decision

Replace the separate receipt aggregate joined back into execution facts with one scoped left join from the completion cohort to active linked collections, grouped by execution identity and its snapshot measures. Count actual collection IDs, sum collection weights and retain executions without receipts. All comparison/productivity totals consume this one-row-per-execution fact population. Coverage, revenue, date boundaries, organization validation, null handling and backend ratio formulas retain ADR-0022 semantics.

The correction is limited to reporting SQL. It introduces no new dependency, migration, cache, API version, planner setting or architecture layer. Keep normal `auto` planning in application verification, and explicitly inspect forced generic/custom plans in disposable diagnostics. Tests exercise weighted multiple-receipt behavior under both prepared modes on a transaction-bound connection.

## Acceptance and consequences

- Existing domain/integration/contract tests must pass; verify Spotless and build the actual application image.
- Reuse ADR-0023's exact seeded scenarios, immutable image metadata and independent oracle at 1,000/10,000/50,000 volumes. Keep before/after evidence distinct.
- No failed/partial baseline is rewritten. Require no HTTP/correctness errors in the corrected workload and inspect actual aggregate loop counts under generic planning.
- SQL grouping may use memory or temporary disk at large volumes; retain buffer plans and do not infer production capacity from local synthetic data.
- Mark L4.1 complete only after real HTTP and prepared-plan evidence confirms the candidate. Record the resulting implementation and remaining release gates in the existing Master Plan register.
