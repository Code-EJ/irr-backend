# Operational data and future analytics

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Updated 2026-10-10. Status: operations-1 implemented; owner-approved analytics-1 implementation and verification recorded in the central progress register.

## Current implemented reporting boundary

PostgreSQL remains the source of truth. Operations-1 provides completed execution count, reported km/completeness, actual elapsed duration and active collection mass with independent receipt dates. Analytics-1 adds matched planned/actual distance and duration, unique declared-section coverage, weighted linked-receipt kg/h and kg/km, and live sale-date POSTED revenue. Formula approval is explicit on 2026-10-10. Dates use America/Sao_Paulo, with bounded inclusive calendar dates converted to half-open instants. Different cohorts are exposed rather than silently combined.

Missing/zero denominators, absent declarations and unsupported geographic revenue attribution are unavailable, not estimated zero. Active linked collections aggregate once per execution; sales aggregate once per sale. Reversal recalculates historical revenue. Ratios round once on the backend to six decimal places; frontend presentation preserves authoritative strings. The metric register distinguishes these approved families from unresolved targets, alerts, recovery/stock closures and global comparisons.

The planning sections and dated readiness checkpoint below retain architectural guidance and historical evidence. Their earlier statements that no analytics exists are superseded by this current boundary.

Operational features should produce analytics-ready data. PostgreSQL remains the source of truth; no warehouse, lake, dimensional schema or mandatory BI is introduced. See [the metric register](metrics.md) for unapproved formulas. Native application/query aggregation, typed API and IRR frontend reports follow Routes. UI and future PDF/spreadsheet exports must share definitions.

| Operational source / grain | Units and dimensions | Integrity / missing-data interpretation |
| --- | --- | --- |
| Municipality / one shared reference | UUID, seven-digit IBGE identity, UF | Operator-verified code/name; shape validation does not certify membership in the official table |
| Neighborhood / one municipality reference | UUID, Municipality | Stable identity; no presumed official polygon or street dataset |
| Route / reusable plan | Organization, Municipality, version; optional planned km/minutes | Plan status is not evidence of execution; superseded coverage retained |
| RouteArea / one ordered declared coverage entry | Neighborhood, optional street | Text is declared coverage, not measured street geometry |
| RouteExecution / one dated assignment | Organization, Municipality snapshot, Route/version, Vehicle, DRIVER, period | Actual timestamps and terminal status; reported km may be null; snapshot plan distance/duration retained |
| ExecutionArea / one snapshot coverage entry | Neighborhood/street snapshot, covered flag | Operator declaration, not GPS proof; partial coverage is not auto-completed |
| ExecutionTeam / one assigned existing TeamMember | Execution, staff UUID/name/role snapshot | Do not multiply execution distance/collection totals when joining crew |
| Collection / one raw receipt with input lines | Exact kg, optional execution, Vehicle, period | Existing ad hoc rows have no guessed Municipality; sum at receipt/input grain before other joins |
| Processing / sorting and pressing provenance | Exact kg/m3, Material, Organization | Raw input, recovered output, compacted stock and rejects are distinct facts |
| Inventory / immutable posted movements and reversal | Exact kg/m3 by lot/material/organization | Reconciliation validates projection; never rewrite history to fit a chart |
| Sale / commercial operation and allocated items | Exact quantity/currency, Buyer, Material, period | Posting/reversal/current status and published-period interpretations require definitions |
| Donor / intake supplier | Stable identity and Organization | Donor postal city text is not a Municipality FK or a geographic inference |

Before reports, define time zone/interval boundaries, null versus zero, cancellation/reversal, stale/reference labels, actual-versus-planned denominators, currency, completeness, corrections and publication versions. Required filters: period, Municipality, Organization, Route, Vehicle, Neighborhood, Material and Buyer. Retain source IDs and actor timestamps; never include credentials or recovery links in analytics.

Start with bounded PostgreSQL queries/indexes and application aggregates. Measure latency/volume before read models, materialized views, caching or scheduled aggregation. Document freshness/reconciliation and access boundaries before any replica/warehouse. Optional BI is a separate future adapter. Reporting delivery stays outside Identity even if it later reuses an email transport.

## Operational source readiness — 2026-10-05

Routes V4 and its API/frontend now produce the plan, execution, coverage, team and optional collection provenance described above. These are implemented source facts, not implemented analytics. Municipality is attached through Route and immutable execution context; an organization may operate in multiple municipalities without deriving territory from its type or postal text. [Verification](../routes-verification.md) records exact-history and authorization checks. Formulas, period policy, targets, publication and export semantics remain PRODUCT DEFINITION REQUIRED.
