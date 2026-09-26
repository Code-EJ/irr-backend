# ADR-0001: IRR architecture overhaul and delivery master plan

> Database execution update: [ADR-0007](0007-fresh-database-baseline.md) replaces the active historical migration chain with one fresh English V1. Old databases are preserved, not automatically upgraded or deleted.

> Execution update (2026-09-26): the owner confirmed no production deployment and authorized backend/database refactoring. [ADR-0005](0005-preproduction-docker-foundation.md) governs Docker Compose, pre-production compatibility, closed legacy PRs and staged implementation. Historical audit tables below remain dated evidence.

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Status: Foundation authorized and implemented as recorded in [ADR-0006](0006-backend-foundation-execution.md); later domain decisions and acceptance gates remain open.
- Recorded: 2026-09-23; repository and registry observations collected on 2026-09-22/23.
- Scope: Code-EJ/irr-backend and Code-EJ/irr-frontend.
- Decision owners: backend lead, frontend lead, product owner and release operator; individual assignments remain unconfirmed.
- Backend baseline: `develop` at `0e675044018f1cfb7c52f7c722aafc49a499d53a`.
- Frontend baseline: `main` at `96f2d3908e51c808257780cca55c6035405e4217`.
- Related: [ADR-0002](0002-database-schema-redesign.md), [ADR-0003](0003-architecture-and-dependencies.md).

## Context

IRR supports waste intake through collections and donations, material classification, sorting, pressing, inventory and sales with supporting documents. Its repositories do not currently represent a compatible, verified release pair. The React frontend still speaks the legacy Portuguese API. The active Spring backend uses English DTOs, UUID identifiers and a substantially different PostgreSQL schema.

The reference at `podiss-backend/docs/ADRS/0002/master-plan.md`, its stage instructions and stage-one report establish the quality model: identify the baseline, inventory first, distinguish internal renaming from API/schema changes, protect existing data, define migration and deployment verification, and only then refactor behavior. These records adopt that sequence and add explicit Decision Drivers, Considered Options, Pros/Cons and Consequences. The reference is a staged plan rather than a single conventional ADR template. Its MariaDB, Hostinger and production statements are not evidence about IRR.

Three backend states must remain distinct:

| State | Verified evidence | Implication |
| --- | --- | --- |
| Default `main` | `dd9559b1667aea6e25b9e71c0bdad0b725a3c57f`; initial document API, MySQL driver, historical SQL | Closed issues are not proof that `main` contains their implementation |
| `staging` | `b7ff42a34fa51f9609684c15a076b48a5c6cb340` | Different integration baseline; not asserted to be production |
| `develop` | Commit above; PostgreSQL, Flyway V1–V4, sessions, materials, vehicles, donors, donations, sorting, pressing and attachments | Primary audit and backend documentation base |

The original local backend checkout was clean on `US-004/Crud-Anexo_p2` at `1c0485dd8b9886b80960b95837df7c27f7f6c6cb`. It was switched to a new documentation branch based on fetched `origin/develop`. The original branch is preserved. The specified local frontend was clean and matches the audited `main` commit. No deployment commit, production database schema, migration history, row counts, workload or recovery objective was supplied or inferred.

## Decision drivers

1. Preserve existing records and give deployed clients an explicit compatibility window.
2. Prevent cross-owner access, duplicate inventory credits, negative stock and non-reconciling financial/material reports.
3. Deliver complete vertical workflows rather than isolated CRUD layers that appear done on a board.
4. Make builds, type checking, authorization, database upgrades and concurrency tests reproducible.
5. Reduce dependency and framework drift without mixing major upgrades with domain redesign.
6. Use English for all new identifiers, comments, documentation, commits and PRs; retain legacy wire/storage names only at documented compatibility boundaries.
7. Keep scope small enough for independent code review and reversible releases.

## Considered options

| Option | Pros | Cons | Outcome |
| --- | --- | --- | --- |
| Rewrite both applications and replace the database in one release | Internally consistent target, no temporary adapters | Unverified data mapping, simultaneous client/server failure, difficult rollback, discards working behavior | Rejected |
| Keep generic CRUD and repair screens independently | Small immediate edits | Retains contract drift, repeated inventory logic and ambiguous ownership | Rejected as the overall strategy |
| Incremental modular monolith with a versioned contract and additive migrations | Reuses Spring/React experience; atomic stock changes; independently testable slices | Temporary adapters and careful migration bookkeeping | Selected |
| Split inventory, identity and documents into microservices now | Independent deployment and scaling | Distributed transactions and new operational burden without measured demand | Deferred until evidence demonstrates a need |

## Decision

Retain two repositories and one transactional backend. Establish the backend as the sole owner of persistence, authorization and stock accounting. The frontend owns user interaction and consumes a versioned HTTP contract; it never determines authoritative stock or access rights. Use a modular monolith organized by identity, materials, intake, processing, inventory, sales, attachments and reporting. Keep PostgreSQL and Java 21 for the next compatible backend baseline. Keep React and TypeScript with Vite and npm for the frontend. Dependency patch selection belongs to ADR-0003 and requires fresh advisory checks.

Establish a deployment manifest recording frontend commit, backend commit, database history/checksums and contract version. Start backend work from `develop` and frontend work from `main` until repository maintainers reconcile release branches. Do not merge `develop` into backend `main` merely to make the audit baseline look consistent.

### Evidence and priority register

| ID | Priority | Verified finding | Required outcome |
| --- | --- | --- | --- |
| F01 | P0 | `BaseService` and `AuthService` use legacy paths, `senha`, `access_token`, PATCH and `{data,total}`; Java uses `/api/session`, `password`, `token`, PUT and Spring `Page` | Contract matrix, adapter and real frontend/backend integration test |
| F02 | P0 | `AuthService.register` always creates REPRESENTATIVE; session registration bypasses authentication; #52 requires administrator-assigned roles | Explicit registration policy, administrator-only partner creation, role matrix and negative tests |
| F03 | P0 | `DocumentController` returns Attachment entities; download/delete find by ID without owner checks; storage deletes file before DB deletion | Response DTO, owner authorization, safe storage keys and transaction-aware cleanup |
| F04 | P0 | `inventory_balance.material_subtype_id` is not unique; first writes can insert duplicate balances despite `@Version` | Unique scoped balance and atomic initialization; real PostgreSQL concurrency test |
| F05 | P0 | Sorting credits net quantity and records a further negative reject entry; pressing records positive mass without changing balance mass and clamps volume to zero | Defined ledger semantics and conservation tests; fail on insufficient stock |
| F06 | P0 | Several reads use creator scope, but inventory is global; sorting/pressing source lookups are unscoped | Organization ownership, same-scope foreign keys and authorization tests |
| F07 | P1 | Flyway core resolves to 11.7.2 while PostgreSQL module is explicitly 10.17.0 | Align through one BOM; test fresh and previous-schema upgrade |
| F08 | P1 | Frontend ESLint crashes on `typescript.configs`; Vite build omits type checking; 519 TypeScript diagnostics | Working TypeScript-aware ESLint and zero type errors before release |
| F09 | P1 | Frontend audit reports 28 affected package entries; backend OSV finds matches on 19 of 89 resolved runtime/compile coordinates | Reachability review, compatible remediation, regenerated audit evidence |
| F10 | P1 | Startup/test keys absent; selected token tests error; integration tests need an existing database and seed rows | Ephemeral test keys and disposable PostgreSQL fixtures |
| F11 | P1 | SQL lacks XOR input ownership, most business checks/secondary indexes, scoped uniqueness and traceable ledger origins | ADR-0002 migrations and duplicate/orphan preflight |
| F12 | P1 | Compose exposes port 8081 but app defaults to 9191; debugging and mappings logging enabled; frontend uses Vite preview in image | Environment profiles, production artifact serving and release smoke tests |
| F13 | P1 | Collection/team/sales have models and DTOs but no controllers on audited `develop`; report service is incomplete | Deliver vertical workflows and report invalidation; keep unavailable UI behind feature flags |
| F14 | P2 | Duplicate HTTP clients, legacy services and highly generic forms coexist | One HTTP boundary, feature-owned request/response models and explicit forms |

P0 means a prerequisite to exposing the affected workflow, not a claim of a confirmed production incident. Findings are from source and local verification; no live service was probed.

### Delivery milestones and acceptance gates

| Milestone | Scope and sequence | Accountable role | Entry dependency | Exit evidence |
| --- | --- | --- | --- | --- |
| M0 — Baseline and contract freeze | Identify deployed release/schema; reconcile board; snapshot API; accept ADRs; choose ownership semantics | Leads + operator + product owner | None | Signed release matrix, schema history, issue mapping, fixture contract |
| M1 — Security and reproducible builds | Resolve registration/roles, document access, CORS; isolated test keys; CI type/lint/test gates; compatible vulnerability fixes | Backend/frontend leads | M0 contract snapshot; urgent fixes may proceed on deployed baseline | Role-by-endpoint tests, clean install/build, zero type errors, scanner dispositions |
| M2 — Database integrity and inventory | Add scope/uniqueness/checks, ledger origin/idempotency, stock locking and reconciliation; replay upgrade and rollback | Backend lead + operator | M0 ownership + backup evidence; M1 test isolation | No duplicate balances, zero reconciliation delta, deterministic contention tests |
| M3 — Intake and protected attachments | Materials/fleet/team APIs; collection/donation with safe upload, historical references and report invalidation | Backend lead | M1, M2 foundations | Collection and donation end-to-end, orphan cleanup, backdated event replay |
| M4 — Processing and sales | Sorting/pressing allocations, insufficient-stock protection, invoice-gated sale posting and corrections | Backend lead | M2 ledger, M3 intake | Conservation and two-concurrent-sale tests; complete US-005/006 criteria |
| M5 — Frontend migration by feature | Repair toolchain; contract client; session/materials/fleet/intake/processing/sales screens, keyboard and error states | Frontend lead | Contract slice for each feature; UI prototypes can start earlier | E2E per enabled route, correct 401/403/409/422 handling and pagination |
| M6 — Release and deprecation | Canary, monitoring, restore rehearsal, old-client telemetry, retire adapters only after window | Operator + both leads | M1–M5 gates for release scope | Tagged compatible release pair, restore proof, no legacy traffic during agreed window |

M5 runs alongside backend slices; it does not wait for every M4 feature. Do not commit to calendar dates until owners, capacity, production facts and customer reference spreadsheets are available. The critical path is M0 → M1/M2 → M3 → M4 → M6, with matched frontend slices required at each exposed release.

### Incremental implementation packages

1. Identity and transport: deny-by-default security configuration, explicit public authentication route, protected partner registration, account scope and one principal provider. Add redaction and structured error responses. Preserve existing login through a bounded adapter where required.
2. Inventory core: one posting service, scoped unique balance, immutable movement batches, source allocations and idempotency. Replace direct balance writes in sorting/pressing only after conservation tests fail on the old behavior and pass on the new behavior.
3. Intake and attachments: draft/upload/finalize lifecycle, metadata DTOs, owner checks, safe storage adapter, compensation/outbox and retention. Invalidate reports transactionally for backdated corrections.
4. Sales: draft/posted/reversed states, required invoice before posting, atomic consumption and no destructive edits to posted transactions.
5. Frontend: fix quality gates, migrate auth and one fleet screen, then material hierarchy/intake/processing/sales. Feature flags prevent navigation to unimplemented routes. A screen is complete only with its real contract and role tests.
6. Operational closure: health/readiness, environment validation, pinned build runtime, documentation, release tags and adapter retirement.

### API deprecation policy

Capture deployed contracts before changing them. Add `/api/v1` as an explicit English contract alongside any verified deployed endpoints. A gateway/controller adapter may translate legacy paths and payloads to the same application use cases. Do not maintain two independent business implementations. Within the frontend, normalize legacy and v1 responses at one client boundary.

Announce the replacement and sunset in release notes, provide deprecation headers where supported, instrument legacy-route calls without logging tokens or payloads, and keep compatibility for at least two coordinated releases and 30 days after the first supported replacement release, whichever is longer. Extend the window if consumers remain or ownership is unknown. Remove only after the client inventory is accounted for, agreed observation period is quiet, and rollback rehearsals succeed. A schema contract removal is a separate migration, never bundled with the first client switch.

### Release, backup and rollback procedure

Before database work, record the real database engine/version, applied migrations and checksums, current frontend/backend artifact IDs, attachments storage location, table sizes, traffic and integration consumers. Obtain a consistent database backup and matching attachment manifest, verify checksums, and restore into an isolated environment. The operator must define and measure acceptable recovery time and data-loss objectives; this audit cannot invent production RTO/RPO.

Rehearse previous-release → expanded-schema → new-backend → new-frontend. Run old backend against expanded schema as a compatibility check. Deploy additive schema first, compatible backend second, feature-flagged frontend third. Validate login, every role boundary, one intake transaction, one sorting/pressing operation, one sale, attachment authorization/download and reconciliation totals. Monitor authorization failures, 5xx, conflict rates, deadlocks, slow queries and ledger mismatch. Thresholds are set against measured baseline before rollout.

On application regression, disable the feature and restore the previous compatible artifact pair while retaining additive columns. For incorrect postings, stop writes to the affected scope, inspect immutable operation records, and use a reviewed compensating transaction. Restore a backup only under the operator's recovery procedure, accounting for writes accepted since the backup. Never blindly run down migrations, clear Flyway history, `clean`, or checksum repair. A failed concurrent index requires explicit inspection/cleanup before retry; see ADR-0002.

### Branch and commit policy

| Repository | Branch | Base and purpose |
| --- | --- | --- |
| Backend | `docs/master-plan-adrs` | Audited `develop`; these ADRs and audit appendices |
| Backend | `refactor/backend-architecture-overhaul` | Audited `develop`; future reviewed modularization slices |
| Backend | `chore/dependency-audit` | Audited `develop`; future compatible dependency changes |
| Frontend | `docs/master-plan-adrs` | Audited `main`; local frontend plan and shared decisions |
| Frontend | `refactor/frontend-architecture-overhaul` | Audited `main`; future contract and UI slices |
| Frontend | `chore/dependency-audit` | Audited `main`; future compatible package/tooling changes |

Branches are created locally. Documentation commits use `docs(adr): ...`. Future implementation uses small `fix(auth): ...`, `refactor(inventory): ...`, `chore(deps): ...` commits. Stage explicit paths, inspect diffs and never commit directly to `main`/`master`. A documentation branch does not certify acceptance or deployment. No refactor or dependency implementation is claimed merely because its branch exists.

## Consequences

Positive: decisions are traceable to code and tickets; stock consistency is a database/application invariant; frontend delivery can proceed one compatible feature at a time. Existing work is retained and reassessed against acceptance criteria.

Negative: adapters and parallel contract versions create temporary maintenance work. Ownership backfill and historical ledger reconstruction need product/operator input. Quality gate repair precedes visible feature delivery.

Risks: historical records may not support exact provenance; an active production legacy database may require a separate import path rather than an in-place Flyway upgrade. Open PRs may overlap planned work. Mitigate through snapshots, explicit reconciliation approval and rebasing small PRs.

## Validation and completion evidence

- Backend package: `mvn -B -DskipTests package` succeeded with JDK 21.0.10; this compiles tests but does not run them or certify startup.
- Selected isolated tests: 33 executed, 25 passed, eight token-test setup errors because `public.key`/`private.key` resources are absent. `AuthServiceTests` currently contributes no discovered `@Test` methods. No existing DB-backed test was pointed at the user's database.
- Frontend: `npm ci --ignore-scripts --no-audit --no-fund` and `npm run build` succeeded. `npm run lint` failed in configuration loading. `tsc --noEmit -p tsconfig.app.json` produced 519 diagnostics. These are baseline failures, not introduced by documentation.
- Dependency tree, dependency analysis, versions inspection, npm audit and OSV coordinate matching completed; interpretations and reproducible commands are in ADR-0003.
- No application runtime, schema, migration, package manifest or lockfile was changed by this documentation phase.
- GitHub issue inventory is complete for the two named repositories at snapshot time: 51 backend issues (25 open, 26 closed), five frontend issues (all closed). REST pages contained 73 and 65 combined issue/PR records respectively, both fewer than the requested 100; PR records were excluded from issue counts.
- All comments on backend issues with comments (#9, #56 and #67) were retrieved. #67 reports 36 passing tests in its historical context; that does not override the clean-checkout results above.
- GitHub Projects is not fully audited: no Projects connector capability; `gh` unauthenticated; browser unauthenticated. The user's supplied list confirms open issues but contains no project-only, archived or draft cards. Milestone creation/reassignment and project-card mutations were not performed. The mapping below is the reviewable desired state, not a claim of remote completion.

## Unresolved inputs and acceptance

Confirm organization membership and material catalog sharing, actual deployed release/database, retained historical schemas and their migration histories, invoice/document requirements, production storage and identity policies, project-board URL/access, and responsible owners. Accept this ADR only after those decisions are assigned and M0 exit evidence is linked. New evidence must amend the proposed record; after acceptance, superseding decisions receive a new ADR number.

## Appendix A: Backlog reconciliation

The following mapping covers every repository issue, including closed work. Issue numbers link to original records; English descriptions are normalized audit summaries. Closed historical tasks remain closed unless a separate remediation is deliberately created or the team reopens the original. No issue is silently deleted, no PR is merged, and no assignee is changed by this map.

Open backend PRs at the snapshot are #50 (drivers/users), #66 (donations) and #73 (pressing). Inspect their diffs before implementation; do not duplicate unfinished work. #69 incorrectly names US-005 in its body: its sales acceptance criteria belong to US-006/#57. Backend #62 belongs to the frontend delivery milestone and should link to the frontend implementation issue while preserving its history.

Board target workflow: Backlog → Ready → In progress → Review → Validation → Done, with Blocked as an explicit status. Proposed fields: Area, Milestone, Priority, ADR, Contract Version and Blocking Issue. Define Done as merged into the agreed integration branch, accepted criteria, passing checks and linked release evidence; track deployment separately. Audit project-only drafts and archived cards before claiming complete backlog consolidation.

| Issue | English scope | Snapshot state | Milestone | Priority | Disposition / acceptance debt |
| --- | --- | --- | --- | --- | --- |
| [BE#4](https://github.com/Code-EJ/irr-backend/issues/4) | Document CRUD | open | M3 | P0 | Consolidate under #63; secure metadata and owner-scoped downloads/deletion. |
| [BE#5](https://github.com/Code-EJ/irr-backend/issues/5) | Media CRUD | open | M3 | P1 | Legacy media overlaps attachments; map IDs/storage before retiring the API. |
| [BE#6](https://github.com/Code-EJ/irr-backend/issues/6) | Vehicle CRUD | closed | M3 | P1 | CRUD exists on develop; add database plate uniqueness and history/role tests. |
| [BE#7](https://github.com/Code-EJ/irr-backend/issues/7) | Driver entity | closed | M3 | P2 | Legacy driver now represented by TeamMember; verify driver role and historical mapping. |
| [BE#8](https://github.com/Code-EJ/irr-backend/issues/8) | User entity | closed | M1 | P1 | User entity exists; organization membership and partner-role workflow remain. |
| [BE#9](https://github.com/Code-EJ/irr-backend/issues/9) | Driver CRUD | open | M3 | P1 | Reconcile open PR #50 with TeamMember model; no DriverController on develop. |
| [BE#10](https://github.com/Code-EJ/irr-backend/issues/10) | CRUD epic | open | M0 | P2 | Replace generic completion measure with linked workflow milestones; preserve epic history. |
| [BE#11](https://github.com/Code-EJ/irr-backend/issues/11) | Initial entities | closed | M2 | P2 | Historical entity completion superseded by current model; verify migration lineage. |
| [BE#12](https://github.com/Code-EJ/irr-backend/issues/12) | Output material entity | closed | M2 | P1 | Legacy output material maps to sale/ledger concepts; no automatic one-to-one conversion. |
| [BE#13](https://github.com/Code-EJ/irr-backend/issues/13) | Input material entity | closed | M2 | P1 | InputItem now exists; add exactly-one-source and quantity invariants. |
| [BE#14](https://github.com/Code-EJ/irr-backend/issues/14) | Typology entity | closed | M2 | P1 | Legacy typology superseded by three-level hierarchy; preserve an explicit ID map. |
| [BE#15](https://github.com/Code-EJ/irr-backend/issues/15) | Collection entity | closed | M3 | P1 | Collection model exists but controller absent; route/timing/distance fields still needed. |
| [BE#16](https://github.com/Code-EJ/irr-backend/issues/16) | Pressing entity | closed | M4 | P0 | Pressing entity is insufficient; repair volume clamp, mass delta and source allocation. |
| [BE#17](https://github.com/Code-EJ/irr-backend/issues/17) | Sale entity | closed | M4 | P0 | Sale model lacks posting workflow/controller; implement atomic stock and invoice gate. |
| [BE#18](https://github.com/Code-EJ/irr-backend/issues/18) | Sorting entity | closed | M4 | P0 | Sorting entity/controller exist; ledger conservation and repeat consumption remain. |
| [BE#19](https://github.com/Code-EJ/irr-backend/issues/19) | Invoice entity | closed | M4 | P1 | Invoice becomes attachment evidence; enforce at posting and retain historical references. |
| [BE#20](https://github.com/Code-EJ/irr-backend/issues/20) | Donation entity | closed | M3 | P1 | Donation implementation exists; prevent editing consumed inputs without correction workflow. |
| [BE#21](https://github.com/Code-EJ/irr-backend/issues/21) | Donor entity | closed | M3 | P1 | Donor model exists; address requirement and normalized identity policy remain. |
| [BE#22](https://github.com/Code-EJ/irr-backend/issues/22) | Subtype entity | closed | M2 | P1 | Subtype exists; enforce scoped identity, hierarchy and historical inactive reads. |
| [BE#23](https://github.com/Code-EJ/irr-backend/issues/23) | Vehicle entity | closed | M3 | P1 | Vehicle exists; service duplicate check lacks SQL uniqueness. |
| [BE#24](https://github.com/Code-EJ/irr-backend/issues/24) | Database migrations | closed | M2 | P0 | Historical completion is not upgrade proof: compare old/current V1 and deployed checksums. |
| [BE#25](https://github.com/Code-EJ/irr-backend/issues/25) | JWT authentication | closed | M1 | P0 | JWT exists; expiry/key isolation and route/object authorization still need integration tests. |
| [BE#26](https://github.com/Code-EJ/irr-backend/issues/26) | User CRUD | open | M1 | P1 | Partner lifecycle and administrator-assigned roles; reconcile PR #50 before duplication. |
| [BE#30](https://github.com/Code-EJ/irr-backend/issues/30) | Database documentation | open | M0 | P1 | ADR-0002 inventories source schema; live catalog/history remains an operator input. |
| [BE#32](https://github.com/Code-EJ/irr-backend/issues/32) | Backend README | closed | M0 | P2 | README exists on develop, not main; revise from tested setup and release baseline. |
| [BE#33](https://github.com/Code-EJ/irr-backend/issues/33) | System architecture documentation | closed | M0 | P1 | Document implemented boundaries honestly; current ports do not form strict Clean Architecture. |
| [BE#34](https://github.com/Code-EJ/irr-backend/issues/34) | Domain inventory | open | M0 | P1 | Use schema/DTO inventory and frontend mapping; require product terminology review. |
| [BE#35](https://github.com/Code-EJ/irr-backend/issues/35) | API route documentation | open | M0 | P1 | Use controller inventory; generate/version OpenAPI before contract migration. |
| [BE#36](https://github.com/Code-EJ/irr-backend/issues/36) | Frontend/backend/database consistency | open | M0 | P0 | Track route, DTO, pagination and enum mismatch matrix; gate compatible release. |
| [BE#37](https://github.com/Code-EJ/irr-backend/issues/37) | Weekly meeting 1 record | closed | M0 | P2 | Meeting artifact is external; closed status cannot establish content or implementation. |
| [BE#38](https://github.com/Code-EJ/irr-backend/issues/38) | Planning meeting 1 record | open | M0 | P2 | Obtain original meeting record; retain outside critical implementation path. |
| [BE#39](https://github.com/Code-EJ/irr-backend/issues/39) | Authentication type and JWT expiry defects | closed | M1 | P1 | Historical type fix does not certify clean key setup or expiry behavior; eight token tests error. |
| [BE#43](https://github.com/Code-EJ/irr-backend/issues/43) | Planning meeting 2 record | open | M0 | P2 | Obtain original meeting decisions; do not invent requirements from a title. |
| [BE#46](https://github.com/Code-EJ/irr-backend/issues/46) | Hardcoded registration role | open | M1 | P0 | Administrator default is no longer present, but fixed REPRESENTATIVE still fails partner-role selection. |
| [BE#52](https://github.com/Code-EJ/irr-backend/issues/52) | US-001: Partner access management | closed | M1 | P0 | Closed yet unmet: public registration and no role input conflict with admin-only partner creation. |
| [BE#53](https://github.com/Code-EJ/irr-backend/issues/53) | US-002: Material hierarchy | closed | M3 | P1 | Hierarchy CRUD exists; writes are admin-only while story includes partner roles; resolve policy and test. |
| [BE#54](https://github.com/Code-EJ/irr-backend/issues/54) | US-003: Fleet management | closed | M3 | P1 | Fleet CRUD exists; driver/team assignment and database uniqueness require follow-up. |
| [BE#55](https://github.com/Code-EJ/irr-backend/issues/55) | US-004: Collections and donations | open | M3 | P1 | Deliver collection, donation, attachments and durable report recalculation as one tested workflow. |
| [BE#56](https://github.com/Code-EJ/irr-backend/issues/56) | US-005: Sorting, pressing and stock | open | M4 | P0 | Existing processing code needs scoped ledger, idempotency and conservation repairs. |
| [BE#57](https://github.com/Code-EJ/irr-backend/issues/57) | US-006: Sales with fiscal evidence | open | M4 | P0 | Invoice requirement, no negative stock, filters/downloads and concurrent sale tests. |
| [BE#58](https://github.com/Code-EJ/irr-backend/issues/58) | User registration authorization | open | M1 | P0 | Admin-only partner registration plus explicit approved bootstrap path. |
| [BE#59](https://github.com/Code-EJ/irr-backend/issues/59) | Vehicle authorization | open | M1 | P1 | Role annotations already exist; validate ownership, inactive bindings and regression matrix. |
| [BE#60](https://github.com/Code-EJ/irr-backend/issues/60) | Material CRUD | open | M3 | P1 | Reconcile existing three-level CRUD; avoid duplicate implementation; verify permissions/version conflicts. |
| [BE#61](https://github.com/Code-EJ/irr-backend/issues/61) | Database redesign | open | M2 | P0 | Implement ADR-0002 only after deployed schema/backfill approval and restore rehearsal. |
| [BE#62](https://github.com/Code-EJ/irr-backend/issues/62) | Frontend prototype | open | M5 | P1 | Frontend-owned feature prototypes tied to real contract; preserve backend issue link. |
| [BE#63](https://github.com/Code-EJ/irr-backend/issues/63) | Attachment CRUD | open | M3 | P0 | Complete authorization, safe storage, DTOs and cleanup; existing upload is not done. |
| [BE#64](https://github.com/Code-EJ/irr-backend/issues/64) | Collection CRUD | open | M3 | P1 | Collection use case/controller, logistics fields and validated document links. |
| [BE#65](https://github.com/Code-EJ/irr-backend/issues/65) | Donation CRUD | open | M3 | P1 | Compare existing donation code and open PR #66; retain missing acceptance criteria. |
| [BE#67](https://github.com/Code-EJ/irr-backend/issues/67) | Sorting CRUD | closed | M4 | P0 | Closed but ledger mismatch, first-insert race and source ownership remain; historical test claim is insufficient. |
| [BE#68](https://github.com/Code-EJ/irr-backend/issues/68) | Pressing CRUD | open | M4 | P0 | Compare PR #73 against merged processing code; reconcile mass/volume and source allocation. |
| [BE#69](https://github.com/Code-EJ/irr-backend/issues/69) | Sales CRUD | open | M4 | P0 | Correct parent from US-005 to US-006/#57; atomic posted sale and fiscal evidence. |
| [FE#27](https://github.com/Code-EJ/irr-frontend/issues/27) | Local-development CORS | closed | M1 | P1 | Retest origin allowlist and preflight against actual target; old closure is environment-specific. |
| [FE#37](https://github.com/Code-EJ/irr-frontend/issues/37) | Missing donor CRUD | closed | M3 | P1 | DonorController exists on develop, but frontend legacy endpoint/fields still differ. |
| [FE#42](https://github.com/Code-EJ/irr-frontend/issues/42) | Recurring CORS failure | closed | M1 | P1 | Duplicate symptom of #27; validate one shared CORS configuration and deployment origin. |
| [FE#50](https://github.com/Code-EJ/irr-frontend/issues/50) | Paginated entity lists | closed | M5 | P1 | Closed pagination remains incompatible: page/limit + data/total vs page/size + Spring Page; generic controls still assume arrays. |
| [FE#64](https://github.com/Code-EJ/irr-frontend/issues/64) | Frontend README | closed | M0 | P2 | README merged; align documented setup with failing lint/type checks and real backend baseline. |

### Proposed debt work items

| Key | Milestone | Finding | Acceptance |
| --- | --- | --- | --- |
| D01 | M1 | F02/F06: identity and organization policy | Non-admin partner creation denied; cross-organization read/write blocked; audit actor retained. |
| D02 | M2 | F04/F05: stock posting correctness | Unique balance; race tests; retry-idempotency; movement sum equals projection. |
| D03 | M3 | F03: protected attachment lifecycle | No entity serialization; authorized download/delete; failed DB write cleans pending upload. |
| D04 | M1/M5 | F01/F08: frontend contract and gates | Strict typecheck/lint pass; matched login, pagination and 204 contracts. |
| D05 | M1 | F07/F09: dependency remediation | Aligned Flyway; reviewed advisory dispositions; fresh scans and real migration tests. |
| D06 | M6 | F10/F12: reproducible deployment | Ephemeral test keys; isolated DB fixtures; health/port/static serving verified. |
