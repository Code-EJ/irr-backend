# Routes delivery and verification record

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Final local checks: 2026-10-05; backend full verification: 2026-10-04.

## Outcome and scope

Routes is **IMPLEMENTED / READY FOR HOMOLOGATION**. Identity remains functionally complete; this delivery did not redesign authentication or credential policy. External email and public deployment are **OPERATIONAL CONFIGURATION PENDING / EXTERNAL CONFIGURATION REQUIRED**. Local acceptance does not freeze v1, approve a release, claim external inbox delivery or implement dashboards. No remote PR, merge, deployment, secret update, database reset or repository move was performed.

## Repository and source state

| Repository | Reused checkout/worktree | Branch | Routes phase base | Verified implementation source |
| --- | --- | --- | --- | --- |
| Code-EJ/irr-backend | irr-backend-identity | feat/route-planning-and-execution | d43f481713c3b602d12ce317058eeeb2eb1093ec | 6bb0a830a9778b16994a83f87e25b3eaad462c4b |
| Code-EJ/irr-frontend | irr-frontend | feat/route-planning-and-execution | f9e70547a34ddd9e8cae9cd11fa20f8346fe31ef | ae9f106a6de0e257e74d8b6b22a0e08bc77fa3ec |

The final documentation commits follow these executable sources. Obtain the exact documentation tip and working-tree status with `git rev-parse HEAD` and `git status --short`; both implementation checkouts are committed at handoff. Original backend add-railway_deploy-docs stays at 4c50f171b03f7d97487e08aadd4d80257b31bc03 with the owner's pre-existing staged/unstaged Railway documentation preserved. No main/master changes were made.

## Identity closure and operational email

Full backend verification retains Identity, credential revocation, global/local authorization, bootstrap and password recovery regressions. Frontend component and real API suites retain account/organization/membership/recovery workflows. Reset, membership selection and inactivation dialogs now share accessible native headers, contextual notices, consistent spacing/footer, close controls, busy protection and focus restoration. Platform content is Brazilian Portuguese; maintained code/docs/comments are English.

EmailSenderPort/HttpEmailSender uses Brevo HTTP delivery; local operation uses the actual Mailpit HTTP adapter without external traffic. PostgreSQL retains one-use token digests/credential changes; Redis applies abuse controls. The Brevo free plan was checked against official documentation on 2026-10-04 (300 daily sends); providers can change limits, so operators must confirm their account allowance. Disabled delivery and provider failure are not inbox acceptance. No recovery secret or API key is placed in the frontend bundle.

The backend email-recovery runbook documents local setup, provider account, approved sender/domain/DNS, key secrecy, public reset URL, variables, quota/error behavior, actual inbox tests and troubleshooting. Variables are existing MAIL_PROVIDER, MAIL_API_KEY, MAIL_FROM, MAIL_FROM_NAME, MAILPIT_URL, MAILPIT_PORT, PASSWORD_RESET_BASE_URL and PASSWORD_RESET_TTL_MINUTES; their exact defaults and associated abuse controls remain in that runbook/.env.example. External account/sender/domain/key and public release URLs remain operator dependencies. No Routes-specific service or Railway variable is added.

## Decisions and Master Plan

Backend ADR-0019 defines Routes and geographic boundaries. Backend ADR-0020 accepts the Analytics & Data direction **for planning only**. Frontend ADR-0017 executes the established incremental integration sequence. They preserve ADR-0003's pragmatic modular monolith and ADR-0014's exclusively /api/v1 contract; no strict Clean Architecture rewrite, vendor SDK or new runtime dependency was introduced.

Previous milestone: Identity, implemented/ready for homologation. Current delivered milestone: Routes, implemented/ready for homologation. Next functional milestone: native Analytics, Reports, Filters, Indicators and Dashboards after approved metric/period definitions. GPS/maps remain PLANNED_POST_DEPLOY; optional external BI remains deferred. External mail configuration runs in parallel and does not block authorized Routes development.

## Model, geography and API

Municipality is a shared stable UUID with name, UF and seven-digit IBGE code; validation checks format/UF prefix, not membership in an official imported dataset. Neighborhood belongs to one municipality; street is optional operator-declared coverage. Route belongs to an organization and one immutable municipality, with ordered coverage, status, version and optional planned km/minutes. An organization can operate multiple municipal plans without inferring geography from CITY_HALL, organization type or postal text. No authoritative neighborhood/street dataset, polygon or provider was imported.

RouteExecution is a dated assignment, separate from the reusable plan. It snapshots plan/version/name, municipality, ordered areas, vehicle, DRIVER and optional distinct crew. Start/coverage/completion/cancellation record server actual times, operator coverage, optional manual exact km, version and actor events. Plan edits preserve existing execution context. Terminal history is immutable. Partial coverage is recorded honestly; null distance is not zero. Schedule planning does not create fleet reservations or reject overlap without a product rule.

The verified OpenAPI contains **114 operations / 63 paths**, all supported business operations under /api/v1. It includes list/detail/create territory, list/detail/create/update/inactivate plans, events, and execution scheduling/list/detail/start/area coverage/complete/cancel. Geography creation returns canonical retrievable Location URLs. Pagination is bounded; route-specific order is deterministic. Swagger remains the executable field/security reference; generated frontend types match its pinned schema.

Global administrators curate shared territory and can select active organizations. Organization MANAGER manages local plans/scheduling/cancellation. MEMBER can read and record permitted execution progress. Active organization/membership, foreign-resource 404, role 403, input 400 and stale lifecycle/version 409 remain server enforced. No frontend control grants additional authority. Scope write locks, version preconditions and PostgreSQL constraints/triggers prevent lost updates and historical corruption.

## Collection and historical consistency

Collection has an optional routeExecutionId. Linking requires a completed execution in the same organization with matching vehicle, driver and crew. The backend derives exact actual timing/km and rejects conflicting facts; an existing association cannot be removed or reassigned. A completed execution may support multiple raw receipts. Existing ad-hoc routeDescription and collections remain compatible; no old structured Routes integration existed to remove. Consumed-intake and stock rules are retained.

The frontend selects completed executions, resolves retained references, fills and locks assigned fleet/team/logistics, and omits derived actual times/distance from writes. This avoids rounding subsecond timestamps through datetime-local inputs. Displayed km uses Brazilian decimal commas while the API keeps exact decimal strings. Coverage controls have stable explicit accessible labels.

## Database and rollout

DATABASE CHANGES REQUIRED: YES. RESET REQUIRED: **NO**. Additive V4__routes_and_execution_history.sql adds municipality, neighborhood, route, route_area, route_execution, route_execution_area, route_execution_team and route_event, plus nullable collection linkage and integrity indexes/triggers. The runtime has **38 application tables plus Flyway metadata**. V1/V2/V3 are unchanged. A populated V3 upgrade preserves existing account hashes and ad-hoc collection facts; repeated startup validates V1/V2/V3/V4. Test-only future examples were renamed to V5 to avoid colliding with runtime V4.

Preserve database/Redis/upload volumes and RSA keys. Deploy backend before its matching frontend, inspect migration history/readiness and run the scoped workflow. V4 rollback must not drop history or disable constraints: after linked collections exist, use a Routes-aware candidate or forward fix; older intake code may attempt fact changes V4 correctly rejects. Local pre-V4 backup was retained privately. Owner Maven/Spotless/Docker changes were preserved. Both final local frontends (5173 and recovery-link target 5273) serve the same container image against backend 9292.

## Analytics, GPS and data readiness

The data dictionary and metric register define source grain, dimensions, units, null handling and missing provenance. Operational Routes facts now exist; metrics/charts/exports do not. Future native flow is PostgreSQL -> bounded query/application aggregation -> typed v1 API -> IRR frontend, with common definitions for UI/PDF/spreadsheets. Reporting remains outside Identity even if it later reuses email delivery. No warehouse, lake, star schema, BI dependency or empty analytics endpoint was added.

Dimensions include Organization, Municipality, period, Route/Execution, Vehicle, team, Neighborhood, Material, Buyer and Donor. Aggregate at receipt/input or execution grain before joining crew/coverage; avoid multiplying mass/distance. Preserve raw intake versus recovered/compacted/stock/commercial facts and reversals. Use measured query plans/latency/volume before read models, materialized views, caching or replicas. GPS may later attach timestamped measurements to execution/coverage identities after a separate provider/hardware/geometry decision; no SDK, map, tracking endpoint or speculative interface exists now.

Every formula remains **PRODUCT DEFINITION REQUIRED**: route/execution counts, distance, cycle duration, declared coverage, planned/actual comparison, collected mass, productivity, recovery/reject rates, pressing/stock, sales/revenue, cross-municipality trends and alerts. Define eligibility, period/timezone, null/zero, cancellation/reversal, denominator, correction/publication policy and freshness before coding indicators.

## Requirements and remaining decisions

R01/R04/R05/R06/R07/R08 Identity and R09 intake remain implemented. R12 Routes and operational multi-municipality separation in R15 are implemented/locally verified. Consolidated R15 comparison and R16 indicators remain planned after Routes; R13 GPS remains planned post-deploy; R11 processing/stock/sales frontend and further evolution retain their recorded future priority. External BI stays deferred. Original requirements are interpreted through traceability, not wholesale implementation authorization.

Product decisions still required: mandatory versus optional collection linkage; whether complete coverage is required; append-only terminal corrections/reactivation; resource overlap/reservations; report timezone and period boundaries; reviewed legacy territorial mapping; indicator formulas, benchmarks/targets/alerts; published monthly report recalculation; authoritative cartography/provider. Current safe behaviors and alternatives are recorded in Routes architecture/data documentation; no unapproved additional workflow is invented.

## Executed checks and limits

| Check | Result / scope |
| --- | --- |
| Backend `./mvnw.cmd spotless:apply clean verify` with Java 21 | BUILD SUCCESS; 48 unit + 84 integration tests, zero failures/errors/skips; Identity/intake/ledger retained |
| PostgreSQL RoutesIT | Eight tests covering CRUD, pagination, global/MANAGER/MEMBER/inactive/foreign authority, invalid/duplicate geography, concurrency, lifecycle, snapshots, collection facts and database history constraints |
| PostgreSQL RoutesMigrationIT | Populated V3 -> V4 upgrade without reset, preserved hashes/ad-hoc facts and repeated migration startup |
| Runtime Flyway/readiness | V1/V2/V3/V4 all successful; backend, PostgreSQL, Redis and Mailpit healthy |
| Frontend `npm run lint`, `npm run typecheck` / `npm run build` | Passed; 1,835 transformed modules; no new dependencies |
| Frontend `npm test` | 74 tests / nine files passed, including accessible dialogs, stable coverage labels, exact PT-BR distance display, version/error/scope and collection integration |
| Frontend `npm run test:contract` | 20 tests / five files passed against the running backend, including two real Routes workflows; safe unique local fixture cleanup |
| `npm run api:check` | Pinned/generated 114-operation v1 contract matches backend source 6bb0a83 |
| Backend and frontend Docker builds | Passed; final image health, direct SPA entry points and actual API origin verified |
| Read-only `verify:homologation` on both local origins | Six checks passed for each: liveness/readiness, normalized OpenAPI, exact CORS, frontend health/SPA and served bundle API origin; apiFrozen remains false |
| Browser business workflow | Created plan/scheduled execution/started/marked coverage/completed with manual 8,125 km; submitted collection through UI in a uniquely owned test organization |
| Persisted browser collection | API confirmed exactly one linked collection, completed version-3 execution, attended coverage and exact matching timestamps/distance/fleet; owner's business organization untouched |
| Browser presentation evidence | Plan form, completion dialog and earlier Identity reset/membership dialog screenshots were reviewed. After Docker/session restart the browser error-page protocol policy prevented the final visual revisit; no bypass or final mobile visual claim |
| Dependency/security checks | Production npm audit: zero findings; existing build chain: five high findings (braces/chokidar/fast-glob/micromatch/Tailwind); no forced stack upgrade. Backend 156-coordinate OSV audit: zero findings on 2026-10-04 |
| Packaging/credentials | Public-HTTPS production-origin rehearsal passed; local API origin restored. Supplied private credentials absent from tracked/unignored source; ignored .env/.env-dev/.local retained |
| Documentation | Source inventory/link checks and changed-file whitespace checks completed before documentation commit |

The production-origin rehearsal used an example.invalid public-shaped URL to test packaging, not an actual deployed API. The final presentation changes do not alter transport, dependency or credential handling. Component/API checks verify those changes; a final live-browser revisit remains an operator acceptance check after the browser connection issue. Full remote global/local/member acceptance, actual inbox delivery, public source/image provenance, persistent upload/backup verification and build-advisory disposition are still release gates.

## Verification evidence and operation

Private local evidence is under .local/verification and .local test/build/audit logs, ignored by Git. It includes the pre-V4 dump, schema inventory, source-pair JSON, browser fixture/result IDs and screenshots; none is committed or copied into a public report. Contract tests refuse non-loopback targets and redact secret-bearing transport errors. They soft-inactivate only uniquely created fixture identities/organizations; historical rows and clearly named shared local test territories are retained, not certified geographic data. Browser fixture history is retained in its own clearly named active test organization. Automatic execution review rejected the requested fixture-organization inactivation with only a policy-block reason; cleanup remains an operator action. The owner organization was not modified. Do not reset production or owner data for fixture cleanup.

Local source/image evidence: backend 6bb0a83 with image sha256:ad86a1e7c1c6dbf3c96dc8a24f7af7634dfca3f36a4c48af2ae27764aa58c497; frontend ae9f106 with image sha256:11d7447e61d8d2343e80f3561ab64b91459efcb2a8e487a74d23cee22b18523b. This records locally built images, not signed registry releases. Documentation-only commits following these sources do not change executable files.

## Suggested pull requests

Backend title: `feat(routes): add scoped municipal planning and immutable execution history`

Backend description: Add organization-owned municipal route plans, dated vehicle/team execution snapshots and versioned start/coverage/completion/cancellation under /api/v1. Additive Flyway V4 preserves existing data and links completed execution facts to optional collection receipts. Document email operations, geographic boundaries and the next native analytics/data phase. Validated with 48 unit/84 PostgreSQL/Redis integration tests, Docker, OpenAPI and the matched frontend's real/browser workflows. No reset or new Railway variable; operator homologation, external sender setup and product metric definitions remain pending.

Frontend title: `feat(routes): integrate planning execution and completed collection workflows`

Frontend description: Integrate the verified v1 Routes contract with Portuguese planning, history, scheduling and field-operation screens, scope/role/version handling and completed-execution collection selection. Improve accessible Identity/operation dialogs and exact localized distance display; preserve compatible ad-hoc intake. Validated with lint/typecheck/build, 74 component/unit tests, 20 real-backend contract tests, Docker/direct SPA/CORS checks and the browser workflow. No new dependency or backend rewrite. Final remote/inbox/browser acceptance and build-advisory review remain release gates.

## Documentation entry points

See [Routes architecture/operations](architecture/routes.md), [email operations](email-recovery.md), [Railway rollout](deploy/railway/README.md), [data dictionary](data/README.md), [metrics](data/metrics.md) and [requirements](requirements/traceability.md).

## Routes implementation commits in this repository

```text
615a9a4 docs(adr): define Routes and analytics data architecture
aea3342 feat(routes): add additive territory and execution history schema
462f8aa feat(routes): expose scoped planning and execution commands
76f2a7f feat(intake): preserve completed route execution references
55fccdd test(routes): verify authorization concurrency and historical integrity
6bb0a83 fix(routes): resolve created territory at canonical resource URLs
6f7bdf1 docs(routes): document operational contract and additive V4 rollout
```

Final documentation commits are separate from these implementation responsibilities; exact handoff tips are supplied with the delivery report.

## Changed-file inventory (relative to the Routes phase base)

```text
M README.md
M docs/adrs/0001-master-plan.md
A docs/adrs/0019-route-planning-execution-and-geographic-boundaries.md
A docs/adrs/0020-analytics-reporting-and-data-architecture.md
M docs/api_documentation.md
M docs/architecture/readme.md
A docs/architecture/routes-audit.md
A docs/architecture/routes.md
A docs/data/README.md
A docs/data/metrics.md
M docs/deploy/railway/README.md
M docs/deploy/railway/checklist.md
M docs/entities_documentation.md
M docs/enums_documentation.md
M docs/operations.md
M docs/requirements/traceability.md
A docs/routes-verification.md
M docs/source-inventory.md
M src/main/java/org/code/api/domain/models/collection/Collection.java
M src/main/java/org/code/api/dto/collection/request/CollectionCreateRequestDTO.java
M src/main/java/org/code/api/dto/collection/response/CollectionResponseDTO.java
M src/main/java/org/code/api/infrastructure/documentation/OpenApiConfiguration.java
M src/main/java/org/code/api/intake/application/CollectionService.java
M src/main/java/org/code/api/parties/application/TeamMemberService.java
A src/main/java/org/code/api/routes/api/GeographyContract.java
A src/main/java/org/code/api/routes/api/GeographyController.java
A src/main/java/org/code/api/routes/api/RouteContract.java
A src/main/java/org/code/api/routes/api/RouteController.java
A src/main/java/org/code/api/routes/api/RouteExecutionContract.java
A src/main/java/org/code/api/routes/api/RouteExecutionController.java
A src/main/java/org/code/api/routes/application/GeographyService.java
A src/main/java/org/code/api/routes/application/RouteAccess.java
A src/main/java/org/code/api/routes/application/RouteExecutionService.java
A src/main/java/org/code/api/routes/application/RouteService.java
A src/main/java/org/code/api/routes/infrastructure/RouteStore.java
M src/main/java/org/code/api/services/ReferenceLifecycleGuard.java
A src/main/resources/db/migrations/V4__routes_and_execution_history.sql
M src/test/java/org/code/api/infrastructure/AccountLifecycleIT.java
M src/test/java/org/code/api/infrastructure/InventoryBalanceSchemaIT.java
M src/test/java/org/code/api/infrastructure/InventoryMigrationIT.java
A src/test/java/org/code/api/infrastructure/RoutesIT.java
A src/test/java/org/code/api/infrastructure/RoutesMigrationIT.java
D src/test/resources/db/migration-examples/invalid/V4__example_failed_upgrade.sql
A src/test/resources/db/migration-examples/invalid/V5__example_failed_upgrade.sql
D src/test/resources/db/migration-examples/valid/V4__example_incremental_index.sql
A src/test/resources/db/migration-examples/valid/V5__example_incremental_index.sql
```

A = created, M = modified, D = removed. The removed V4 test examples were renamed unchanged to V5; runtime V1/V2/V3 remain unchanged. No replaced structured frontend Routes code existed; ad-hoc collection integration remains supported.
