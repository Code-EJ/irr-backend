# Metric register

## Current approval — 2026-10-10

The owner explicitly approved planned/actual comparison, section-count coverage, weighted kg/h and kg/km, and live posted-sale revenue under analytics-1. Backend ADR-0022 and frontend ADR-0021 specify the exact rules. These supersede the earlier PRODUCT DEFINITION REQUIRED cells for those families below. Operations-1 remains implemented. The historical entries for other formulas, dimensions, exports and alerts remain pending.

| Approved analytics-1 indicator | Fact cohort / formula | Missing-data rule |
| --- | --- | --- |
| Planned/actual distance and duration | Completed executions in the Brazilian completion period with both snapshot planned and actual values; matched sums, signed delta, absolute magnitude, signed delta / planned * 100 | Exclude/report missing pairs; percent unavailable at planned zero |
| Declared-section coverage | Unique execution/source-area pairs; covered / declared * 100 | No declarations or contradictory duplicate states: unavailable; identical duplicates counted once |
| Weighted productivity | Active receipts grouped by considered execution; sum matched kg * 3600 / sum positive actual seconds, and sum matched kg / sum positive actual km | Separate matched cohorts; report missing receipts, missing/zero denominators; no average of ratios |
| Revenue | Sum active POSTED sale header total once per sale, by sale_date; later reversal recalculates history | Drafts/reversed excluded; geographic/vehicle filters make revenue unavailable without execution provenance |

These four families are implemented and locally tested through SQL/API and the generated frontend contract. Backend 996e36f and frontend 9e25e91 are the implementation references; final browser evidence is in the Analytics integration record. Ratios round HALF_UP to six decimal places after aggregation; exact sums retain precision. The pending classifications below are historical for these approved families only.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The four operations-1 metrics below are owner-approved and implemented for local verification under backend ADR-0021 and frontend ADR-0018. Other entries remain planned; an available field alone is not an approved indicator.

| Metric | Business meaning requiring confirmation | Required source data / grain | Unit | Dimensions / filters | Formula | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Route count | Active plans versus all registered plans | Route identity/lifecycle/organization/municipality | routes | organization, municipality, period, status | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Execution count | Completed work by completion instant in the approved Brazilian period | One completed execution | executions | selected organization, municipality, route, vehicle, inclusive dates | COUNT completed executions with finished_at in [Brazil midnight, midnight after end date) | IMPLEMENTED / LOCAL VERIFICATION |
| Route distance | Sum of reported actual travel, retaining missing measurements | Completed execution distance_km | km | same execution filters | SUM non-null distance_km; null when no measurements; measured/missing counts | IMPLEMENTED / LOCAL VERIFICATION |
| Cycle duration | Actual elapsed execution time | Completed execution started_at/finished_at | seconds | same execution filters | SUM exact finished_at - started_at; empty sum is zero | IMPLEMENTED / LOCAL VERIFICATION |
| Declared coverage | Area/street/length denominators and partial work | Ordered planned snapshot entries, covered flags; measured geometry not available | approved denominator required | organization, municipality, route, neighborhood, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Planned versus executed | Appropriate distance/time/coverage comparison | Snapshot plan km/duration/areas, actual execution facts | km/minutes/approved coverage unit | organization, municipality, route, execution, vehicle, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Collected mass | Declared active collection receipt mass by realization instant | One active collection total_weight_kg; independent execution grain | kg | selected organization, vehicle, inclusive dates; municipality/route require linked immutable execution context | SUM exact total_weight_kg; empty sum is zero; expose unlinked count | IMPLEMENTED / LOCAL VERIFICATION |
| Route productivity | Output versus time/distance/crew denominator | Eligible collection mass, actual execution duration/km, crew | approved ratio required | organization, municipality, route, vehicle, team, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Recovery/reject rates | Valid input/output/reject mass balance | Sorting provenance, recovered output, reject facts and reversals | kg / approved percentage | organization, material, period, municipality when traceable | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Pressing/stock | Compacted output versus available posted stock | Pressing/bales/immutable movements/lots/reversals | kg/m3 | organization, material, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Sales/revenue | Posted/reversed/fiscal versus current commercial facts | Sale/item/buyer/material exact quantities/prices/currency | approved currency / kg | organization, buyer, material, period, municipality when traceable | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Cross-municipality trends | Comparable units/periods and completeness | Above facts with explicit immutable municipality context | approved comparable units | organization, municipality, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Delay/variation alerts and targets | Thresholds, baselines and exclusions | Approved metric definitions + trustworthy route timing/history | approved units | organization, municipality, route, vehicle, period | PRODUCT DEFINITION REQUIRED | DEFERRED |

The approved operations-1 and analytics-1 queries/APIs/frontend are implemented. The table above retains historical classifications; the current approval section supersedes the four approved families. Other metric families, charts and exports are not authorized by this register. Do not replace null distance with zero, count joined crew/coverage rows as additional executions, infer historic municipality from free text, or equate raw mass with saleable inventory. GPS measurement, street geometry and authoritative neighborhood mapping remain future dependencies.
