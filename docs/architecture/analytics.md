# Native MVP Analytics

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Implements [ADR-0022](../adrs/0022-approved-mvp-analytics.md), continuing the existing Master Plan.

Swagger is the official contract: GET /api/v1/reports/analytics. Dates are required, inclusive America/Sao_Paulo calendar dates with a 366-day maximum. Selected organization and historical municipality/route/vehicle filters reuse ReportQueryScope; inactive dimensions remain valid and foreign references remain unavailable. The existing operations-1 report keeps its independent receipt-date semantics.

AnalyticsReportService reads a single PostgreSQL statement snapshot. It selects completed executions, aggregates active linked receipts per execution, independently deduplicates execution/source-section coverage and aggregates sale-date revenue. Team, receipt and coverage joins never multiply execution denominators or commercial values. AnalyticsMath computes final ratios with exact BigDecimal inputs and six-place HALF_UP rounding. Frontend arithmetic does not decide indicators.

Comparison sums use matched planned/actual populations; signed and absolute deviations are distinct. Zero planned totals retain sums/deviation but percentage is null. Productivity requires at least one linked active receipt and a positive actual denominator. Hourly and distance productivity expose their own matched sample, numerator, denominator and exclusions; receipt dates may differ from execution completion dates. Missing receipts, missing km and zero denominators are distinct exclusions.

Coverage deduplicates (execution_id, source_area_id), rather than treating repeated work on the same plan as one obligation. Contradictory duplicate coverage states invalidate the percentage. Missing declarations make coverage unavailable. All declarations are operator facts, not GPS or area measurements.

Revenue includes active POSTED sales only, by sale_date. Reversal later removes earlier revenue on a live reread. Applied municipality/route/vehicle filters make revenue unavailable because sales have no execution provenance. The unavailable reason is part of the response; a zero is never substituted for unsupported attribution.

AnalyticsReportIT uses disposable PostgreSQL to test weighted populations, active receipt corrections, null/zero values, duplicate/conflicting sections, Brazilian day boundaries, sale reversal and authorization. AnalyticsMathTest covers precision and defined/undefined ratios. The existing OperationalReportIT remains a regression gate after validation reuse.

## Local execution evidence — 2026-10-10

Spotless apply/check passed. Targeted Maven verification passed: three math tests, six analytics integration tests and five existing operations integration tests, zero failures/errors/skips. The original target resource remains locked; Maven verification used a source-only copy under the Codex workspace, with public src/pom only. Full-suite, container, generated Swagger, frontend and browser results are recorded subsequently in the central progress register. No migration, private configuration or volume reset is required.
