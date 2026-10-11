# Reporting extensions: product proposal

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Status: PROPOSED; no new implementation or formula authorized. Continue [ADR-0020](adrs/0020-analytics-reporting-and-data-architecture.md) and approved [ADR-0022](adrs/0022-approved-mvp-analytics.md), rather than redefining metrics.

| Proposed increment | Exact boundary for owner review | Acceptance requirement |
| --- | --- | --- |
| Operational/Analytics export | Export the selected organization, approved date/filter cohort, displayed aggregate values, units, missing-data counts and definition version. Decide CSV versus spreadsheet/PDF formats and whether detailed rows are required | Same backend formulas/nullable values/exact quantities as the UI; preserve dates/timezone/filters; authorized scope only; formula-injection escaping for spreadsheets; downloadable file validity |
| Period comparison | Compare two explicitly selected, non-overlapping equivalent periods within one authorized organization; display both cohorts/completeness and mathematically defined differences | Missing values remain unavailable; zero baselines have undefined percentage; no hidden calendar alignment or simple mean of productivity |
| Consolidated organization comparison | Global administrator opt-in only, with explicitly selected organizations and approved confidentiality policy; no tenant-level implicit cross-organization access | Sum facts/denominators before ratios; independently scoped cohort validation; completeness and comparability disclosed |

Owner decisions needed: file formats, detail/aggregate scope, row/period limits, retention/download behavior, calendar comparison rules, cross-organization permissions and acceptance examples. Native reports remain supported without these extensions. Threshold alerts, targets and rankings require additional product definitions. GPS/provider telemetry, maps and advanced external BI remain deferred; no speculative dependencies or placeholders are introduced.
