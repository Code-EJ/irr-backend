# ADR-0004: Implementation readiness and backend entry criteria

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Status: Historical readiness review, superseded in deployment assumptions and PR disposition by [ADR-0005](0005-preproduction-docker-foundation.md).
- Review date: 2026-09-26.
- Scope: readiness of ADR-0001 through ADR-0003 against both repositories and all repository issues.
- Related: [master plan](0001-master-plan.md), [schema redesign](0002-database-schema-redesign.md), [architecture/dependencies](0003-architecture-and-dependencies.md).
- Outcome: suitable strategic plan; ready for backend verification foundations, not for unconditional execution of the complete redesign or production migrations.

> Subsequent owner instruction on 2026-09-26: IRR is not in production; backend/database refactoring is authorized. The three PRs below were then closed without merging. The following review records the evidence before that instruction. Production-discovery gates no longer block development.

## Context and reviewed evidence

The GitHub connector returned all repository issues through the paginated REST collection: 73 combined issue/PR records for the backend and 65 for the frontend, each below page size 100. Excluding PRs yields 51 backend issues (25 open, 26 closed) and five frontend issues (all closed). No issue-state changes were observed relative to the previous inventory. All three open backend PRs were inspected through their metadata and changed-file patches; none has a submitted PR review in the connector results. This does not prove that no informal review occurred.

Backend integration remains `develop` at `0e675044018f1cfb7c52f7c722aafc49a499d53a`. The local backend branch is now `general-backend-refactor` at `6bf8333`, containing the three ADRs over that code baseline. Frontend local branch is `refactor-froned-general`; fetched `origin/main` is now `42ad50f340cdb5b2bf4b555b44127aec2f541dec`, also containing the ADRs. Comparing application source/build files against the previous audit baselines showed no changes. Preserve these user-selected non-main branches; previous branch descriptions in ADR-0001 are historical audit context, not an instruction to overwrite current work.

The prior build/test/scanner observations remain dated 2026-09-22/23. They were not rerun simply to repeat results against unchanged application sources. In particular, the 519 TypeScript diagnostics, eight token-test setup errors, 28 npm affected-package entries and 19 OSV-matched backend coordinates are historical observations, not newly collected September 26 scan results. Refresh vulnerability data before selecting remediation versions.

GitHub Projects draft/archived/project-only cards and production artifacts/schema were not accessible in this review. Repository issue coverage must not be described as a complete Projects or production audit. No board status, milestone, issue state, PR or production system was modified.

## Decision drivers

1. Avoid treating a closed issue or mergeable PR as proof of compatible, complete functionality.
2. Start useful backend work without waiting for unrelated frontend repairs or production-only inputs.
3. Stop schema and business behavior changes where requirements still permit incompatible implementations.
4. Turn the architectural roadmap into small changes with objective acceptance criteria.
5. Preserve existing code/data and maintain the separation between proposal, implementation and deployment.

## Considered options

| Option | Pros | Cons | Outcome |
| --- | --- | --- | --- |
| Apply all three ADRs immediately | Fast start | Silently selects catalog ownership, lifecycle and deployment assumptions; risks duplicate work from old PRs | Rejected |
| Block all backend work until every production/business input is resolved | Avoids assumptions | Unnecessarily blocks isolated tests, contract characterization and dependency convergence work | Rejected |
| Begin verification foundations; gate domain/schema/release work separately | Useful progress with measurable scope; preserves unresolved decisions | Requires explicit gates for each slice | Selected |

## Findings that affect readiness

### R1 — Existing PRs cannot be merged as the planned implementation

| PR | Inspected evidence | Required disposition |
| --- | --- | --- |
| [#50](https://github.com/Code-EJ/irr-backend/pull/50) | Targets `staging`; legacy Driver with Integer ID; UserService copies request password into `senha` and includes it in response mapping; no BCrypt call on that create path; user controller has no method-level administrator check; mergeability reported false | Do not integrate unchanged. Reimplement/reuse reviewed requirements against current UUID/User/TeamMember model, hashing, response DTO and authorization tests. This finding concerns the proposed PR code, not the current deployed service. |
| [#66](https://github.com/Code-EJ/irr-backend/pull/66) | Targets historical `main`; new `donations` table uses MySQL `AUTO_INCREMENT`, BIGINT IDs and DOUBLE weight; endpoint `/donations`; material IDs are scalar fields | Do not merge into PostgreSQL develop. Current donation work already exists from merged PR #72. Port only missing requirements such as donor-address handling after comparison. Git mergeability is not semantic compatibility. |
| [#73](https://github.com/Code-EJ/irr-backend/pull/73) | Targets historical `main`; Portuguese Prensagem/Tipologia/Subtipologia entities; updates `volumeCompactadoEstoque` on subtype, clamps some subtraction results to zero; no new SQL migration among 16 changed files | Do not introduce a second inventory model. Current processing work already exists from merged PR #71. Reconcile acceptance criteria against one posting service and explicit migration path. |

These are audit recommendations, not remote PR closures, rebases or reviews submitted on behalf of the user. Preserve contributors' work and extract useful requirements/tests rather than blindly cherry-picking legacy implementations.

### R2 — Closed access/material/processing stories still have acceptance gaps

- [#52](https://github.com/Code-EJ/irr-backend/issues/52) requires administrator-created partners with selected roles. Current `/api/session/register` is bypassed by BearerFilter and creates only REPRESENTATIVE. The proposed protected partner workflow is necessary; bootstrap/self-signup policy must be explicit.
- [#53](https://github.com/Code-EJ/irr-backend/issues/53) describes partner access to material management, while current material mutation methods require ADMINISTRATOR. Donation input resolution also restricts subtype to the current creator. Merely adding organization tables cannot decide whether catalog data is shared or partner-owned.
- [#54](https://github.com/Code-EJ/irr-backend/issues/54) is not proof of database-enforced license-plate uniqueness or complete driver/team workflows.
- [#67](https://github.com/Code-EJ/irr-backend/issues/67) is closed, but sorting credits net stock and writes an additional negative rejection entry, and first balance insertion has no unique key. Existing-row optimistic locking does not close those gaps.
- Frontend [#50](https://github.com/Code-EJ/irr-frontend/issues/50) is closed but describes the legacy pagination response. It does not prove compatibility with current Spring Page serialization.

Keep historical closed states as evidence of prior work. Add linked remediation or deliberately reopen after backlog review; do not count them as release acceptance merely because they are closed.

### R3 — Multi-stage sorting and source allocation need a state model

Current SortingType permits GROSS, PRIMARY and FINE. ADR-0002's statement that accepted sorting output creates saleable stock must not be implemented as a fresh credit at every stage. Define whether these are alternative classifications or successive processing stages, which transition releases saleable inventory, and how rejection/reclassification transfers previously credited quantities.

Required invariants before inventory implementation: one source quantity cannot be consumed twice; saleable release happens once for the accepted allocation; re-sorting previously credited stock consumes/transfers the previous output instead of recreating it; pressing consumes a bounded source allocation and conserves mass; sales allocate available stock in the correct material state. The current proposed ERD is conceptual and omits exact allocation columns/constraints, so it is not yet an implementation-ready DDL specification. Do not derive those semantics from enum names alone.

### R4 — Opening-balance equation requires one unambiguous convention

Clarify ADR-0002: an approved opening balance is represented as an OPENING movement in the new immutable ledger. Therefore the implementation invariant is:

`inventory_balance = SUM(all committed stock_movement deltas, including OPENING)`

Equivalently, `opening + SUM(non-opening deltas)` may be used in reporting. Never add opening separately and also include its movement in the same sum. Legacy inventory_log is retained historical evidence, not replayed as new movements without approved reconciliation.

### R5 — Business decisions and production migration evidence remain separate gates

Before organization/schema work, confirm catalog sharing, organization membership, administrator cross-scope actions, creator-to-organization mapping, and how existing records are assigned. Before stock posting, confirm lifecycle rules above, cancellation/reversal behavior, quantity units and source allocation. Before fiscal posting, confirm required evidence/currency/rounding and handling of missing legacy invoices.

Before production migration, identify the deployed backend/frontend pair, actual DB engine/version and Flyway history, backups/restoration, data anomalies and active clients. A local empty database test cannot replace this evidence. These requirements do not block pure unit-test fixtures or source contract inventories.

### R6 — Backend acceptance must not depend on repairing the whole frontend first

ADR-0001's broad M1 gate includes frontend type/lint repairs. Split it into backend and frontend gates. Backend verification and additive compatible changes can proceed once their own checks pass; a coordinated public release still requires the consuming frontend slice to pass its contract tests. Do not wait for all 519 frontend diagnostics before establishing backend unit/integration tests.

## Decision and executable backend sequence

The master plans are sufficient to start BE0 below. They are not accepted as blanket authorization for schema migration or guessed business policies. Preserve the current feature branches and begin with a small backend-only change set. Keep the major package/domain rewrite after behavior characterization.

| Slice | Concrete deliverable | Acceptance | Blockers |
| --- | --- | --- | --- |
| BE0.1 — Reproducible unit tests | Generate ephemeral RSA test key pairs in test code, inject them into JWT tests, remove reliance on absent classpath private/public keys; add actual AuthService tests | Selected unit suites discover intended tests and pass from clean checkout; no real keys or DB connection; no production authentication change | None from domain or production |
| BE0.2 — Isolated PostgreSQL verification | Test-only datasource/fixtures; disposable PostgreSQL; startup/schema and independent-transaction concurrency harness | No development/production DB fallback; deterministic fixtures; startup and existing migration results recorded | Disposable database runtime available in local/CI environment |
| BE0.3 — Dependency convergence | Remove conflicting Flyway PostgreSQL module override through the chosen Boot BOM; capture resolved tree | Core/module aligned; clean database migration and startup test pass; no applied SQL changed | BE0.2; fresh compatible-version/advisory check |
| BE0.4 — Backend CI and contracts | Java 21 checks, unit/integration separation, current DTO/path/error characterization and source inventory | Pipeline fails on test failure; fixture setup automatic; no private key committed; current HTTP behavior captured | BE0.1/2 |
| BE1 — Identity and attachment protection | Implement approved role matrix, principal consistency, safe response DTOs and object-level checks; repair storage lifecycle separately | Positive/negative role and ownership tests; no password/storage path leakage; no destructive file operation before DB commit | Current behavior characterized; bootstrap/ownership policies for changed access |
| BE2 — Scoped data and stock accounting | Detailed DDL/allocation/state design, new migrations, one posting service and reconciliation | Unique first insertion, idempotency, conservation, rejection/re-sorting, two concurrent sales, reversal and old-schema upgrade tests | R3–R5 decisions; BE0/1 |
| BE3 — Complete workflows | Collection/team, donation corrections, pressing and invoice-gated sales with report invalidation | Each issue's acceptance criteria tied to executable tests; legacy PR work reconciled | BE2 and matched contract slice |

The smallest first PR is BE0.1, not a package-wide rename or a new V1. No runtime code was changed as part of this readiness review. Add test-only dependencies only when needed by concrete BE0 gates. Do not weaken security merely to start a test application; tests must supply their own isolated secrets and datasource.

## Required acceptance decisions

| Decision | Evidence owner | Needed before |
| --- | --- | --- |
| Public signup vs administrator-provisioned partners; first-admin bootstrap | Product owner + backend lead | BE1 access behavior |
| Shared catalog vs organization-owned catalog; organization membership and data mapping | Product owner + data owner | BE2 scope migration |
| Sorting stages, saleable release, re-sorting and allocation lifecycle | Domain owner + backend lead | BE2 stock posting |
| Posted-operation correction/cancellation, invoice/currency/rounding | Domain owner | BE2/BE3 sales |
| Real deployed schema/history/consumers and restore evidence | Release/database operator | Any production migration/cutover |

This table records missing decisions without pretending that engineering review can answer product questions. Existing stories already establish several requirements (administrator-controlled partner creation, no negative stock, required sale invoice); uncertainty is limited to their operational details, migration and contradictory existing behavior.

## Consequences

Positive: backend work can start immediately on verification foundations; known PR incompatibilities cannot silently reintroduce legacy models; the plan has explicit stop points for business and production assumptions. The inventory equation is implementable without double-counting opening balances.

Negative: full schema and workflow implementation still needs domain input. PR reconciliation requires contributor coordination. This review does not turn existing failing checks into successful validation or certify a deployed system.

## Validation and limitations

Fresh evidence: fetched Git refs; retrieved complete repository issue collections, open PR metadata/diffs and review lists; compared application source/build files against the previous baselines; rechecked authentication, material permissions, donation source scoping, sorting accounting, attachment operations and test fixtures. No source/build drift was found in those baseline comparisons. No new application test run is claimed.

Remaining limitations: live Projects cards, production database/deployment and original domain spreadsheets remain unverified. The previous dependency audit is a dated snapshot and must be refreshed for upgrade selection. The readiness conclusion is conditional by slice, not a release approval.
