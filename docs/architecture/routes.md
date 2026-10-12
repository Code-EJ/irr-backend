# Routes operational architecture

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Decision: [ADR-0019](../adrs/0019-route-planning-execution-and-geographic-boundaries.md). Subsequent analytics: [ADR-0020](../adrs/0020-analytics-reporting-and-data-architecture.md). This describes implemented operational behavior; it does not introduce a second roadmap.

## Ownership and transaction boundary

The feature lives under `routes/api`, `routes/application` and `routes/infrastructure`. Controllers validate typed commands. Services reuse effective global/local authority and the existing OrganizationScope write lock. JdbcTemplate shares Spring's established transaction/data source with JPA intake. RouteStore reads bounded plan/execution context and appends transactional events. This is ADR-0003's pragmatic modular monolith; no framework-free domain/hexagonal rewrite is required.

Shared territory references have stable UUIDs. IBGE codes have seven digits and a matching UF prefix; operator review against the official table is still required. Global administrators register territory after selecting an active organization. Municipalities/neighborhoods support paginated list, reference resolution and creation; reference correction/removal is deferred. Every operational route has exactly one immutable municipality. Organizations can operate in several municipalities through their routes; geography grants no organization access.

## Source of truth

| Record | Grain / responsibility | Historical behavior |
| --- | --- | --- |
| municipality | Shared municipality UUID, unique IBGE code, name, UF | Stable territorial reference |
| neighborhood | Named reference within one municipality | Normalized name uniqueness |
| route | One reusable organization-owned municipal plan | Optimistic version, DRAFT/ACTIVE, soft inactivation |
| route_area | Ordered neighborhood and optional declared street | Replaced planning areas retained inactive |
| route_execution | One dated assignment of a current active plan | Immutable plan/territory/fleet/driver context |
| route_execution_area | One copied ordered planned area | Only covered flag may change during IN_PROGRESS |
| route_execution_team | Existing TeamMember assignment and copied label/role | Snapshot retained; driver is separate |
| route_event | One actor/action/version transaction fact | Append-only; includes lifecycle/coverage events |
| collection.route_execution_id | Optional many-to-one completed execution | Same organization, matching logistics/crew, immutable reference |

A route can be edited after scheduling. The already copied execution plan is unchanged. Vehicle/driver/team vary between executions. Global administrators may select any active organization; MANAGER may manage only an authorized tenant; MEMBER may read and start/cover/complete existing executions. Scheduling, cancellation, plan mutation and shared territory creation use their documented stronger authorities. See the executable Swagger for request/response fields.

## Commands and invariants

Every business request uses `/api/v1` and `X-Organization-Id`. Read lists use fixed deterministic name/identity ordering for plans/territory and planned-start descending/identity for execution. Pagination is bounded by the existing server maximum. Arbitrary sort parameters do not alter these fixed route query orders.

POST creates plans/executions. PUT route replacement requires the current version and complete ordered coverage. DELETE route requires `?version=<current>` and never deletes history. Execution start/complete/cancel and coverage bodies require the last returned version; 409 requires explicit reload/review before retry. Transactions serialize current organization writes, verify authority again, validate version and append events before commit. Actual timestamps and events use the server wall clock after acquiring the existing write lock.

PLANNED -> IN_PROGRESS -> COMPLETED; PLANNED or IN_PROGRESS -> CANCELLED with a reason. No terminal rewrite/reopening. Cancellation is management-only. Null measured km remains missing, never zero. Partial declared coverage is retained; completion does not mark pending areas attended. No vehicle overlap/reservation or mandatory all-covered rule has been invented.

Collection can remain ad hoc. When an execution is selected, it must be completed and fleet/driver/crew must match. The server derives exact start/finish/distance; it rejects conflicting supplied values. The frontend omits copied actual values when saving the association, avoiding timestamp precision loss. Both command checks and database integrity protect the link; deferred crew constraints validate the final JPA join-table state at commit. Existing processed-intake restrictions remain active. Fleet/team referenced by any execution cannot be deactivated, and referenced staff roles cannot be reassigned.

Composite ownership/territory foreign keys, exact numeric checks, lifecycle/timestamp checks and PostgreSQL history triggers complement application policy. The schema is not a public write API: authorization and command/version policy still require the application service. No new database access is granted to browser clients.

## Rollout and operation

DATABASE CHANGES REQUIRED: YES. Runtime V4 is additive: eight tables plus the nullable collection execution reference and integrity indexes/triggers. Existing V1/V2/V3 hashes are unchanged. No reset/backfill/IBGE inference from legacy free text. Apply the backend before its matching frontend; Flyway applies V4 on normal startup and validates repeated startup. The populated V3 upgrade test preserves existing account hashes and ad-hoc collection facts.

Keep PostgreSQL, Redis and attachment volumes and RSA keys. No new service, secret, provider or Railway environment variable is required for Routes. Before remote rollout, take the existing consistent backup, verify retained storage/migration history/readiness, deploy the matched pair and execute the route workflow under global/local/member accounts. No remote deployment is performed by the local validation. Once linked execution data exists, prefer a Routes-aware rollback candidate or forward fix: an old intake binary may attempt to overwrite facts that V4 correctly rejects. Never downgrade by dropping V4 or disabling its history triggers.

## Future data and maps

GPS/telemetry can attach timestamped measurements to stable execution/area identities after a separate requirement/provider decision. Current distance is manual and coverage is operator-declared. No map SDK/provider contract, empty tracking endpoint or speculative provider interface exists. The operational UUIDs, exact kg/km, plan snapshots and actual times support later native aggregation. [Data definitions](../data/README.md) describe grains and prevent collection mass fan-out when joining crew/coverage. Metric formulas remain PRODUCT DEFINITION REQUIRED; no new analytics endpoint/chart/export has been added.

## Product decisions required

| Question / context | Alternatives and impact | Technical recommendation |
| --- | --- | --- |
| Must every collection be linked to execution? Legacy/ad-hoc intake currently works. | Optional provenance vs mandatory scheduling; mandatory needs workflow/migration exceptions. | Keep additive optional linkage until operations approve coverage. |
| Must all planned areas be attended before completion? Real operations may be partial. | Block completion vs record partial coverage; the former may prevent accurate history. | Preserve actual declared flags; approve separate reason policy if needed. |
| How should terminal corrections/reactivation work? History is currently immutable. | Audited correction records vs reopening; reopening can invalidate collection facts. | Design an append-only correction workflow before implementing one. |
| Should fleet/team schedules reject overlap? Current planning creates no reservations. | Advisory warnings vs hard reservation rules; resource capacity and timezone rules are required. | Obtain actual scheduling requirements before enforcing overlap constraints. |
| Which timezone and period define each indicator? Offset timestamps are stored accurately. | Organization-specific vs common report timezone; affects period boundaries. | Approve period semantics with metric definitions. |
| How should legacy free-text intake map to municipality? Identity is currently unknown. | Reviewed mapping vs inferred names; inference can corrupt geographic analytics. | Keep unknown provenance until a reviewed mapping workflow exists. |

## Verification

PostgreSQL RoutesIT verifies CRUD, bounded pagination, invalid/duplicate territory, global/MANAGER/MEMBER authority, inactive tenant rejection, foreign references, stale concurrent updates, lifecycle, cancellation, coverage, immutable snapshots, resource guards and completed collection association. RoutesMigrationIT verifies a populated V3 -> V4 upgrade without reset. Full Maven verification also covers unchanged Identity, recovery and inventory behavior. Frontend component/API and real local contract evidence are recorded in the integration verification report; an ADR or successful compilation alone is insufficient acceptance.

Final local acceptance and remaining release gates: [Routes verification record](../routes-verification.md).
