# Operational reporting architecture

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Implements [ADR-0021](../adrs/0021-native-operational-reporting.md), continuing ADR-0020 and the pragmatic modular monolith.

## Boundaries and request flow

OperationalReportController exposes GET /api/v1/reports/operations in Swagger. OperationalReport is the safe response record; exact decimal quantities serialize as strings and unrestricted filter identities and unknown distance serialize as null. OperationalReportService owns period/filter validation and the read-only aggregate query. It reuses OrganizationScope and Spring JDBC; no new port, repository hierarchy, cache or infrastructure is needed.

Authentication and active account verification precede effective selected organization/membership validation. Global administrators can select any active organization; permitted organization members can read. Foreign or unavailable organization/filter identities return 404 under the existing non-disclosure policy. Dates must be ordered ISO calendar dates, at most 366 inclusive days. Territory is a shared reference; routes and fleet must belong to the selected organization. Historical inactive filters remain valid. Mismatched route/municipality filters return 400.

## Exact fact grains

America/Sao_Paulo computes the local start of the first date and the exclusive start of the day after the last date, including historical daylight-saving boundaries. One PostgreSQL statement aggregates completed executions and active collection receipts in independent CTEs, then combines one aggregate row from each. Statement snapshot consistency prevents mixed reads; no crew, coverage or material join multiplies facts. This query uses the ordinary read-only transaction; existing commercial report/export services retain their documented snapshot policy.

Execution count, known km and duration use finished_at in the interval. Missing km are counted and never estimated; no measurements produce null distance, while an explicitly measured zero contributes zero. Duration sums exact PostgreSQL epoch seconds between actual start and finish. Collections use realization_date independently, sum declared total_weight_kg, and exclude inactive receipts. Municipality/route filters use the linked execution's immutable territory/route. Unlinked receipts are included without those filters and counted as missing provenance. Vehicle filtering uses each fact's vehicle identity.

Reports are live, definition operations-1, with generated timestamp and applied interval/filter metadata. They are not fiscal closing statements. Edits and lifecycle changes recalculate subsequent reads. No schema migration, Redis cache, materialized view, data reset or modification to V1-V4 is required. Realistic-volume EXPLAIN/latency review remains a performance follow-up before introducing indexes or caching.

## Validation and evolution

PostgreSQL tests cover Brazilian midnight boundaries, exact decimals, zero/null completeness, multiple receipt and crew facts, independent linked receipt dates, live edits/inactivation, effective scope and foreign/inactive filters. Frontend real-backend tests cover the actual Swagger-derived transport and linked receipt workflow. See [delivery verification](../reporting-verification.md). Exports, comparisons, productivity/coverage formulas, targets and GPS retain their remaining product definitions.
