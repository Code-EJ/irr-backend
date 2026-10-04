# Metric register

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). All entries are planned. An available field is not an implemented indicator.

| Metric | Business meaning requiring confirmation | Required source data / grain | Unit | Dimensions / filters | Formula | Status |
| --- | --- | --- | --- | --- | --- | --- |
| Route count | Active plans versus all registered plans | Route identity/lifecycle/organization/municipality | routes | organization, municipality, period, status | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Execution count | Scheduled, completed or eligible operational work | Execution identity/status/actual/planned timestamps | executions | organization, municipality, route, vehicle, team, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Route distance | Actual reported travel versus planned travel | Execution reported km, plan km snapshot, measurement provenance | km | organization, municipality, route, execution, vehicle, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Cycle duration | Work interval and cancellation/idle treatment | Actual start/finish/cancel timestamps at execution grain | minutes | organization, municipality, route, vehicle, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Declared coverage | Area/street/length denominators and partial work | Ordered planned snapshot entries, covered flags; measured geometry not available | approved denominator required | organization, municipality, route, neighborhood, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Planned versus executed | Appropriate distance/time/coverage comparison | Snapshot plan km/duration/areas, actual execution facts | km/minutes/approved coverage unit | organization, municipality, route, execution, vehicle, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Collected mass | Raw receipt mass and eligible status/period | Collection + input exact kg, optional execution provenance | kg | organization, municipality when linked, route, vehicle, material, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Route productivity | Output versus time/distance/crew denominator | Eligible collection mass, actual execution duration/km, crew | approved ratio required | organization, municipality, route, vehicle, team, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Recovery/reject rates | Valid input/output/reject mass balance | Sorting provenance, recovered output, reject facts and reversals | kg / approved percentage | organization, material, period, municipality when traceable | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Pressing/stock | Compacted output versus available posted stock | Pressing/bales/immutable movements/lots/reversals | kg/m3 | organization, material, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Sales/revenue | Posted/reversed/fiscal versus current commercial facts | Sale/item/buyer/material exact quantities/prices/currency | approved currency / kg | organization, buyer, material, period, municipality when traceable | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Cross-municipality trends | Comparable units/periods and completeness | Above facts with explicit immutable municipality context | approved comparable units | organization, municipality, period | PRODUCT DEFINITION REQUIRED | PLANNED_AFTER_ROUTES |
| Delay/variation alerts and targets | Thresholds, baselines and exclusions | Approved metric definitions + trustworthy route timing/history | approved units | organization, municipality, route, vehicle, period | PRODUCT DEFINITION REQUIRED | DEFERRED |

No query/API/chart/export is added from this register. Do not replace null distance with zero, count joined crew/coverage rows as additional executions, infer historic municipality from free text, or equate raw mass with saleable inventory. GPS measurement, street geometry and authoritative neighborhood mapping remain future dependencies.
