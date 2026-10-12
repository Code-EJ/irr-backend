# ADR-0022: Approved MVP analytics definitions and consistent fact cohorts

- Status: Accepted; implemented and locally verified. Operator acceptance and release readiness remain separate gates.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: explicit owner approval on 2026-10-10. Continues ADR-0001, ADR-0020 and ADR-0021; ADR-0014 and the pragmatic modular monolith remain authoritative.

## Context and decision drivers

The first operational report is implemented at backend 20a03c7. The owner now approves planned/actual comparisons, declared-section coverage, weighted collection productivity and live posted-sale revenue. Missing data, duplicate joins, zero denominators and reversal timing can otherwise produce misleading indicators. Existing immutable execution snapshots contain planned distance/duration and actual timestamps/km; linked collections retain execution identity. Sale fiscal documents do not establish route/municipality provenance.

## Considered options

| Option | Benefits | Costs / risks | Decision |
| --- | --- | --- | --- |
| Calculate from paginated browser lists | Small UI change | Incomplete populations, floating precision, divergent formulas | Rejected |
| Expand operations-1 with incompatible period semantics | Fewer endpoints | Mixes receipt-date mass with completion-cohort productivity | Rejected |
| Dedicated native analytics query/API using shared period/scope validation | Explicit definitions, exact SQL source grains and server math | Additional response and documented cohorts | Selected |
| Warehouse, cache, BI or new chart library | Potential scale/presentation flexibility | Unmeasured infrastructure cost and stale values | Deferred |

## Decision

Expose GET /api/v1/reports/analytics with the existing required inclusive from/to Brazilian dates (at most 366 days), optional municipality/route/vehicle filters and effective selected organization scope. Native PostgreSQL aggregation produces one internally consistent statement snapshot. Reuse period/filter validation with operations-1; retain its contract and independent receipt-date semantics. No applied V1-V4 changes, cache, new infrastructure or estimates.

Definition version: analytics-1. Include period bounds, timezone, applied filters, generated timestamp, exact sums, sample/exclusion counts and nullable results. Exact decimals serialize as strings. Ratios round HALF_UP to six decimal places only after aggregation. Source quantities and sums retain their precision. Frontend formats server results and explains definitions; it does not calculate business metrics.

### Execution cohort and comparisons

Use only COMPLETED executions whose finished_at belongs to the half-open America/Sao_Paulo date interval, with immutable municipality/route/vehicle context. Compare each metric on executions containing both planned and actual values. Distance uses planned_distance_km versus distance_km; duration converts snapshot planned_duration_minutes to seconds and compares to actual finished_at-started_at. A present zero is valid, missing values are excluded and reported. Aggregate matched planned/actual sums. Signed deviation is actual minus planned; absolute deviation is its magnitude; percentage deviation is signed deviation / planned sum * 100, unavailable when the planned sum is zero or there are no matched executions. Never mix unmatched populations.

### Coverage

Count unique (execution_id, source_area_id) pairs from the considered completed executions. A repeated route section in a different execution is a different declared work obligation. Duplicate rows with identical coverage state count once; contradictory states make coverage percentage unavailable and are explicitly counted. Covered / declared * 100 is unavailable without declarations. Report executions with/without declarations and duplicate/conflict counts. This is quantity-of-sections coverage, neither geographic area nor travelled distance. Never join collection/team rows into the coverage denominator.

### Productivity

Group active collection receipts by their linked execution before aggregating. For each metric include only considered completed executions with at least one active linked collection and a strictly positive actual denominator. Kg/h = sum of linked kg * 3600 / sum of actual seconds; kg/km = sum of linked kg / sum of reported actual km. Duration/distance each have their own matched cohort and numerator. Never average per-execution ratios. Zero kg from a real receipt remains zero; absent receipts are missing evidence and excluded, not invented zero collection.

Receipt mass follows the selected execution-completion cohort even if its realization date is elsewhere; this differs deliberately from operations-1's receipt-date total. Show eligible execution/collection counts, numerator/denominator, executions lacking linked receipts, missing denominators and zero denominators. Also expose unlinked active receipts in the date period as provenance warnings; never assign them guessed execution/municipality. Edits/inactivation recalculate subsequent reads.

### Revenue

Use organization-owned active POSTED sales with sale_date inside the same Brazilian date interval and sum authoritative BRL total_value once per sale. DRAFT and REVERSED do not count; reversal in a later period removes that sale from historical revenue on reread. Show considered sales and excluded draft/reversed counts. Empty unfiltered sale population has zero revenue. Revenue is unavailable when municipality, route or vehicle filters are applied: the model has no sale-to-execution provenance, so attributing commercial revenue to those dimensions would be fabricated. The UI states this reason. No fiscal publication, market-price estimates or tax-authority integration is implied.

## Supersession and consequences

Supersedes only PRODUCT DEFINITION REQUIRED for these four analytics families in ADR-0020/0021 and the metric register. Additional material recovery/reject ratios, stock-period closures, cross-organization comparisons, targets, alerts, GPS, scheduled delivery and exports retain their existing unresolved scope. No new roadmap or backend architecture.

Benefits: weighted, auditable cohorts and explicit unavailable states. Costs: differing productivity populations require visible sample counts; current-state reversal changes historical totals; declared coverage depends on operator facts. No GPS proof is claimed.

## Implementation and verification

Backend period/scope reuse, DTO/query/math and PostgreSQL/unit tests precede generated OpenAPI capture and frontend integration. Tests cover matched sums versus average-of-ratios, missing/zero inputs, empty sets, multi-receipt/crew fan-out, duplicate/conflicting section identities, Brazilian period edges, foreign scope and subsequent sale reversal. Inspect the actual running Swagger and containers; then verify lint/types/build/component/real API and browser loading/filter/empty/data journeys. Save real synthetic-data screenshots in docs/assets/screenshots. Record executed/failing/not-run gates in MASTER_PLAN_PROGRESS.md. Local-only execution; deployment remains a later phase.
