# ADR-0021: Native operational reporting first slice

- Status: Accepted; first slice implemented and locally verified.
- Date: 2026-10-09.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Context and drivers

Routes is locally verified. The owner explicitly approved the first reporting definitions in the conversation and requested continuation. This executes backend ADR-0020 and the existing Master Plan; it adds no new architecture. Preserve the modular monolith, PostgreSQL source facts, React/TypeScript frontend, /api/v1, effective organization scope and additive migration discipline.

## Considered options and decision

Choose bounded native aggregate queries and a typed report screen. Counting client-loaded pages loses facts and leaks precision; a warehouse/BI/chart framework or precomputed report snapshot adds unjustified infrastructure. Reuse PostgreSQL/JDBC, Swagger generation, Axios, exact decimal presentation and current application cards/filters.

Approved metrics: completed execution count; sum of reported km on completed executions with measured/missing counts; sum of actual elapsed duration on completed executions; sum of kg from active collection receipts. Use America/Sao_Paulo calendar dates. Executions belong to the period by finished_at; collections by realization_date. Inclusive UI dates become a half-open instant interval from local midnight through midnight after the final date. Live reports recalculate after receipt edits/inactivation. Distance with no measurements remains null, including empty execution sets; reported zero is a measurement. No productivity or coverage percentage is approved.

Filters: period, municipality, route and vehicle, within the selected authorized organization. Municipality/route collection filtering uses immutable linked execution context; unlinked receipts remain included without territorial/route filters and cannot be assigned guessed geography. Vehicle filtering applies to each source fact. Retain inactive plans/fleet as valid historical filter identities. No organization-wide/global consolidation is introduced. Count each execution/receipt once, without joining coverage/team/input rows into metric totals.

## Contract, sequence and verification

Add GET /api/v1/reports/operations with required from/to ISO calendar dates (inclusive, maximum 366 days) and optional municipalityId/routeId/vehicleId. Return interval/timezone/definition version/generated timestamp, exact decimal sums, counts and missing-provenance metadata. No cache or persistent report schema is needed for this slice. Use one aggregate statement for internally consistent source totals; inspect query plans before introducing new indexes. No reset or edits to applied V1-V4.

Backend first: scoped query/API plus PostgreSQL tests for day boundaries, exact quantities, multiple crew/receipts, null versus zero, foreign/inactive filters and live edits. Build/run backend, capture actual Swagger, regenerate frontend types, integrate the report with Portuguese content and test loading/error/empty/filter states. Run Spotless, ESLint, type/build/component/real-backend tests and Docker checks; compilation alone is insufficient. The report is operational, not a published fiscal statement. Exports, additional dimensions, consolidated comparisons, targets and GPS remain subsequent decisions.

## Consequences and remaining risks

[Verification evidence](../reporting-verification.md) records backend source 20a03c7, five report integration tests, the full 48 unit/89 integration test suite, actual Swagger capture and frontend acceptance. Broader Analytics remains pending.

Benefits: auditable facts, exact sums, no double counting and incremental delivery. Costs: live totals change after edits; ad-hoc receipts lack municipal provenance; manual km completeness must remain visible. External email/homologation and existing build-chain advisories remain separate release gates. Unapproved metric families in the register retain PRODUCT DEFINITION REQUIRED. Record actual acceptance evidence before marking implementation complete.
