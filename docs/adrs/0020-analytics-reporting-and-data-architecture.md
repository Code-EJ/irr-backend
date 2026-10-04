# ADR-0020: Analytics, reporting and data architecture

- Status: Accepted planning decision; analytics implementation deferred until after Routes.
- Date: 2026-10-04.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Context and decision drivers

Historical reports/dashboard requirements and multi-municipality direction need reliable operational facts. [ADR-0019](0019-route-planning-execution-and-geographic-boundaries.md) introduces planning/execution/territory without GPS or analytics now. Keep ADR-0003's modular monolith, exact quantities, immutable stock movements and explicit tenant/global authority. Metrics need approved meaning, units, periods, completeness and correction policies; attractive dashboards cannot supply missing data or invented formulas.

## Considered options

PostgreSQL queries/application aggregation are operationally simple and auditable. A mandatory external BI product would make core reporting dependent on a vendor. A warehouse/lake/stream platform adds unproven cost and replication consistency. Precomputed/cache/read models may later be justified by measured query latency and data volume. Choose native PostgreSQL-backed reports first; optional BI and further storage layers require evidence and a later ADR.

## Decision: analytics-first, not analytics-now

Operational features produce analytics-ready data: stable UUIDs, explicit Organization/Municipality, timestamped and known lifecycle states, execution snapshots, exact kg/km and money, and traceable material/stock/commercial relationships. PostgreSQL operational data remains the source of truth. No dimensional warehouse tables, empty analytics endpoints, scheduled export/report delivery, chart package or dashboards are introduced in this task.

## Target architecture and boundaries

PostgreSQL → application/query layer → explicit aggregation → typed /api/v1 API → IRR frontend → native report/dashboard. Keep reporting/query definitions outside Identity and route mutation services. Reuse verified organization scope; global views use explicit global authority and selected filters rather than pretending a tenant is global. UI and future PDF/spreadsheet exports use the same query/metric definitions and metadata. External BI is an optional adapter, never required for core platform use.

## Dimensions and metric domains

Organization, Municipality, Period, Route, RouteExecution, Vehicle, Team, Material, Buyer and Donor are operational dimensions, not new star-schema tables. Routes use execution snapshots for historical municipality/coverage/crew, not mutable current plan labels. Plan groups for route/execution/distance/time/coverage; collection mass/period/provenance; material input/sorting/pressing/recovery/reject/stock; commercial quantity/revenue/buyer/material; and cross-municipality trends/comparisons. Multi-municipality comparisons require consistent definitions and available facts, not account CITY_HALL classifications.

## Definitions, filters and data quality

Maintain [the data dictionary](../data/README.md) and [metric register](../data/metrics.md): meaning, source grain, unit, dimensions, filters, formula and status. Unapproved formulas are PRODUCT DEFINITION REQUIRED. Required future filters include period, municipality, organization, route, vehicle, neighborhood, material and buyer. Define reporting time zone/half-open interval semantics, cancellation/reversal handling, plan-versus-actual denominators, missing/null/zero behavior and correction policy before implementing aggregates.

Aggregate each fact at its source grain before joining many-to-many team/coverage/input records to avoid fan-out. Collection mass is not stock credited by sorting, compacted mass or sales quantity. Immutable stock history/reversals remain authoritative for ledger facts; current-status summaries and closed/published period reports need an explicit product definition. Missing km/geometry is unavailable, not zero. Store exact units and identify manual versus future provider measurements. Do not derive unobserved vehicle travel, street length or employee performance.

## Performance evolution

Begin with bounded filters, correct PostgreSQL indexes, query plans and application aggregates. Measure representative volume, p95 latency and transaction contention. If evidence demonstrates insufficient performance, evaluate query changes/indexes first, then materialized views/read models/scheduled aggregation and generation-aware caching. Document freshness, invalidation, ownership and reconciliation before introducing them. A warehouse, replication/ETL or BI platform is a later evidence-driven choice, not this implementation.

## History, consistency and auditability

Persist actual execution timestamps/status/crew/territory snapshots and traceable collection links. Preserve terminal facts and actor/version audit records. Future analytics responses should expose definition/version, interval, units, data freshness and completeness. Reconcile totals against operational facts and ledger, and test concurrent writes, reversals, null facts, boundaries and multitenant isolation. Administrative permissions do not waive aggregate correctness.

## Exports and email delivery

Plan PDF and spreadsheet outputs after Routes and metric definitions. Their filters and totals must match the UI/API definitions. A future Reporting-owned delivery service may use the existing EmailSenderPort transport boundary after a second actual consumer exists; report logic, scheduling, consent and attachments do not belong inside PasswordRecoveryService. No unused mail abstraction or export implementation is added now.

## Consequences, acceptance and deferred work

Benefits: a single auditable source, native functionality, stable dimensions and gradual performance choices. Costs: formal metric definitions and data-quality work before dashboards, explicit missing-data reporting and retained history. This ADR is accepted when the operational Routes design preserves needed facts and the data dictionary identifies missing definitions. It does not certify analytics, dashboards or exports as implemented. After Routes, approve formulas and reporting semantics, implement native filtered query/API/UI slices, verify totals and only then assess optional BI/GPS extensions.
