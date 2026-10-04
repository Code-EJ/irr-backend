# ADR-0019: Route planning, execution and geographic boundaries

- Status: Accepted for incremental implementation; verification pending.
- Date: 2026-10-04.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Context

The owner closes Identity as IMPLEMENTED / READY FOR HOMOLOGATION and explicitly authorizes Routes development while external email configuration remains pending. This supersedes only ADR-0017/0018's former development-sequencing gate, not identity, authorization, bootstrap, recovery or security decisions. Continue ADR-0003's pragmatic modular monolith, ADR-0014's /api/v1 contract and additive Flyway. [The Master Plan](0001-master-plan.md) records the phase change; [ADR-0020](0020-analytics-reporting-and-data-architecture.md) plans subsequent analytics without authorizing dashboards now.

## Decision drivers

Represent internal route coverage and dated work without depending on GPS; isolate organizations; support multiple municipalities; preserve planned versus executed context; reuse fleet/team/intake; prevent silent historical edits and lost updates; maintain exact units and stable references; avoid external geography infrastructure or a framework rewrite.

## Current domain constraints and audit

Collection already stores vehicle, DRIVER TeamMember, collection_team, realizationDate, exact totalWeightKg, raw material inputs, routeDescription, paired departure/arrival and optional distanceKm. Consumed intake cannot be replaced. Vehicle and TeamMember are organization-owned reference data retained while referenced. Organization is an operational tenant with name/classification; CITY_HALL is an account classification. Donor PostalAddress has city/region text tied to one donor. None represents a territorial identity or reusable route. No Municipality, Neighborhood, Route, execution, geographic dataset or GPS provider exists at the audited baseline. V1/V2/V3 are applied; their checksums remain untouched.

## Requirements interpretation

Historical FF-4 and the platform vision describe municipality/neighborhood/street coverage and later GPS. Interpret them as normalized internal planning and dated execution, not an immediate map/tracking integration. Preserve history and allow explicit area coverage recording. Do not interpret persistent completion-toast wording as an obligation to interrupt the operator indefinitely. Do not derive territorial identity from an organization/account name or fabricate telemetry.

## Considered options

| Option | Benefits | Costs / reason |
| --- | --- | --- |
| Continue free-text routeDescription only | Small immediate CRUD change | No stable territory, coverage or reusable execution context; rejected |
| Route directly owns one permanent vehicle/team | Simple lookup | Confuses reusable planning with dated assignments; rejected |
| Route plus normalized areas and immutable execution snapshot | Supports history, assignment variation and future facts without external providers | Adds a small operational module and explicit transitions; selected |
| GIS/telemetry service, JSON geometry and generic permission engine now | Broad theoretical extension | Unneeded dependencies/undefined product semantics; deferred |

## Decision and domain model

Route is an organization-owned reusable plan with municipalityId, name, DRAFT/ACTIVE status, isActive, optimistic version, optional plannedDistanceKm/plannedDurationMinutes, actor and creation/update metadata. Coverage uses ordered RouteArea rows referencing a stable Neighborhood plus optional street text. One neighborhood-only row means a declared area, not GPS geometry or proof of collection. A street narrows that declared coverage; it is not a national street identifier. Require at least one area, enforce positive sequence/no duplicates and validate every neighborhood against the route municipality. Retain superseded area rows inactive rather than deleting referenced planned facts.

RouteExecution is a dated assignment of an ACTIVE route: routeId/version/name, municipality identity/name/UF/code snapshot, plannedStartAt, assigned existing vehicle/driver/team and their display snapshots, lifecycle/version, actual start/finish/cancel timestamps and optional manually reported distanceKm. ExecutionArea copies ordered neighborhood/street identities and labels from the selected plan and records covered state. ExecutionTeam retains existing TeamMember IDs plus display/role snapshots. These are concrete required relationships, not speculative RouteStop/RouteStreet/Team aggregates.

## Organization scope

All operational route, area, execution, crew and event records have non-null organization_id. Reuse OrganizationScope's server-authorized selection and organization write lock. Composite foreign keys reject mixed organizations even if a query omits a filter. Lists are bounded; get/list/history always apply selected organization. Global administrators can select any active tenant; no fake membership. Inactive organization access remains denied.

## Municipality model

Add a shared Municipality reference with UUID, unique seven-digit IBGE code, name and UF. Organization does not represent a geographic authority; organizations can operate in several municipalities and a municipality can contain several organizations. Represent this many-to-many operational relationship through their Routes, without an unused organization_municipality association or geographic entitlement inference. Neighborhood has its own UUID and municipality FK with normalized name uniqueness. This is the minimum stable territorial identity required for comparable coverage.

IBGE documents [seven-digit municipality codes](https://www.ibge.gov.br/explica/codigos-dos-municipios.php), whose first two digits identify the UF. Validate shape and UF-prefix consistency; validation is not proof that a code/name is an official current municipality. Global administrators enter verified references from the official table. Do not import or call an external dataset at runtime. Keep the code/UF stable. A Route's municipality is immutable after creation: a plan in another territory is a distinct route, preserving stable coverage foreign keys and historical identity. Existing donor city text is not silently converted. Shared territorial writes are global-only; tenant members can read references after scope verification. Territorial catalog corrections/retirements beyond first creation are deferred to a reviewed reference-maintenance workflow.

## Route lifecycle

DRAFT is an editable complete plan not available for execution. ACTIVE is executable; either status can be edited by manager/global authority with the current version. DELETE performs soft inactivation with an expected version, never hard deletion. Reject deactivation while PLANNED/IN_PROGRESS executions exist; finish/cancel them first. Completed/cancelled history permits inactivation and remains readable. Reusing an inactive plan requires a separately approved reactivation workflow, not hidden DELETE reversal.

## Execution lifecycle

PLANNED → IN_PROGRESS → COMPLETED; PLANNED or IN_PROGRESS → CANCELLED with a nonblank reason. Start records server time; completion records server finish time and optional explicitly reported distance, without inventing missing measurements. Covered-area updates are allowed only during IN_PROGRESS and increment execution version. Terminal executions and their historical snapshots are immutable. No reopening or terminal corrections are exposed; future correction policy needs an ADR/audit path.

Partial area coverage is retained explicitly. Completion does not fabricate all-covered flags or assume GPS proof. Whether every area must be covered before completion is PRODUCT DECISION REQUIRED; current implementation records actual declared coverage without inferring that business requirement. Planned timing is scheduling context, not a vehicle-booking guarantee. Overlap/capacity policy remains a product decision, not an invented uniqueness rule.

## Collection integration

Add nullable collection.route_execution_id, permitting one execution to support several collection receipts. Existing ad hoc collections and routeDescription remain compatible; there is no forced data import or legacy alias. A new association must reference a COMPLETED execution in the same organization and match its vehicle/driver/team. Copy actual execution timing/distance into the existing collection logistics fields; conflicting supplied facts are rejected. Once associated, a collection cannot silently change/remove that reference. Editing a Route never changes an execution's names/areas/vehicle/team snapshots or the context of an associated collection. Processed-intake immutability remains unchanged.

Requiring structured execution for every future collection, time-zone/reporting-period semantics and historical ad hoc ownership mapping remain product decisions. No municipality is guessed for legacy routeDescription text.

## Vehicle and team integration

Vehicle, DRIVER and other TeamMember assignments belong to executions. Plans have no permanent vehicle dependency. Validate active same-organization references, DRIVER role, unique non-driver crew and keep snapshots. Extend existing lifecycle guards to reject deactivation/role replacement while execution history retains those IDs. Do not create Driver, Collector or Team duplicates. Routes do not introduce automatic fleet reservations.

## Geographic model, GPS extension point and map boundary

Neighborhood IDs and optional street text provide declared operational coverage. Geometry and coordinates are absent until a demonstrated map/tracking use case selects reference systems, privacy and quality rules. Future providers must adapt external geometry/telemetry to stable RouteExecution/area IDs and timestamped facts outside planning/identity policies. Do not create an unused GeometryProvider/TelemetryProvider interface now. Manual distance has explicit km and is not claimed to be GPS-derived. No Google Maps, Mapbox, hardware or telemetry API is integrated.

## Authorization

ADMINISTRATOR globally or local MANAGER may create/edit/inactivate plans and schedule/cancel executions. Active local MEMBER may read routes/executions and record permitted execution start/coverage/completion, consistent with existing member intake operations. MEMBER cannot edit reusable plans, register shared territory or escalate membership. Authorization is enforced in services, independent of frontend guards; unknown/foreign resources return 404, insufficient operations 403, invalid input 400 and stale versions/invalid transitions/history conflicts 409.

## API contract

All new operations use /api/v1 and current bearer/X-Organization-Id conventions. Municipality and neighborhood list/create support bounded selection and explicit shared-reference registration. Routes support list/get/create/PUT/versioned soft DELETE. Route-executions support list/get/create, start/complete/cancel commands and versioned area coverage replacement. Use typed UUIDs, OffsetDateTime, English fields, exact decimal strings and generated Swagger. No unversioned endpoint, external tracking endpoint or empty analytics API is added.

## Frontend responsibilities

Continue existing React/Vite/Axios, SessionProvider, generated DTOs and reference pickers. Provide scoped list/detail/create/edit/inactivate, municipality/neighborhood/street coverage, status labels and exact planned units. Provide execution scheduling/detail/lifecycle/coverage and completed-execution selection in Collection, populating its matching logistics. Respect manager/member guards but display actual server errors. Preserve loading, empty, pagination, retry and 400/401/403/404/409 states. Reuse the improved native modal; do not add a map library or rewrite Identity.

## Persistence and data integrity

Add V4 only. PostgreSQL tables cover municipality, neighborhood, route, route_area, route_execution, route_execution_area, route_execution_team and route_event, plus the nullable collection association. Use non-null scope, composite FKs, normalized active name/area uniqueness, status/time/nonnegative checks and route/execution version checks. Shared services use JDBC for this module, as existing query/audit boundaries do; shared intake remains JPA. Preserve V1–V3 and existing rows. No reset, migration archaeology consolidation or manual SQL patch.

## Auditing

Route events record action, actor, organization, route, optional execution, version and timestamp in the same transaction. Historical snapshots/event records cannot be deleted or rewritten. Plan edits retain superseded areas and executions reference original versions; event facts are not a complete generic event-sourcing system or an analytics endpoint. No passwords, mail secrets or speculative tracking PII is recorded.

## Testing strategy

Use real PostgreSQL/Testcontainers for territory uniqueness/FKs, organization/global/local/member boundaries, invalid references, pagination, create/edit/deactivate, version conflicts, inactivation with open work, transition/time checks, coverage, retained snapshots and collection association/mismatch/processed history. Re-run Identity and ledger regression gates. Frontend tests cover active flows, permissions, loading/empty/selection/errors, version conflicts and data-preserving retries. Build both images, capture actual Swagger, regenerate/check DTOs and verify a real local frontend/backend pair. An ADR or successful build alone does not mark Routes implemented.

## Analytics compatibility

Preserve explicit Organization/Municipality/Period/Route/Execution/Vehicle/Team/Material identities, actual timestamps, exact distance/weight units and source provenance. Missing distance remains null. Collection weights remain raw kg facts; multiple input/team/coverage joins must not multiply those totals. Snapshot territory permits historical comparison despite later reference labels. [ADR-0020](0020-analytics-reporting-and-data-architecture.md) identifies metrics and unresolved formulas, not current dashboard work.

## Consequences and deferred work

Advantages: reusable planning, substitutable fleet/team, stable territory, history and testable isolation without provider dependencies. Costs: more explicit forms and snapshot rows, global territorial curation, version-aware client commands and additive rollout. Defer GPS/maps, automatic scheduling, reservations, polygon/street datasets, global analytical dashboards, imports, terminal correction/reactivation policies and formally defined coverage/productivity formulas. Record open product questions with recommendations rather than silently inventing them.

## Rollout and acceptance criteria

Review ADR first; add schema/domain/contract/tests; capture OpenAPI; incrementally integrate frontend/Collection; document actual evidence. Deploy backend V4 before the matched frontend; confirm migrations/history, references, health and retained storage. Old clients omit the optional execution reference and remain valid. Rollback retains V4/history; prefer a matched security-compatible pair and forward correction. No remote merge/reset is automatic.

Accept when scoped CRUD and typed geography work; global/local/member policies hold; execution lifecycle and version conflicts are verified; historical snapshots survive plan/reference edits; collection linkage retains matching terminal facts; frontend flows work against the running API; Identity regressions pass; documentation/traceability/data dictionary/operations agree. External email configuration is tracked separately and does not block development. Status is updated only with executed evidence.
