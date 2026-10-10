# Local Master Plan execution progress

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Updated 2026-10-10.

## Authority and completion rule

This execution register continues the existing ADR-0001 sequence and the owner's local-only consolidation request. No Railway configuration or deployment is authorized in this phase. Preserve the modular monolith, current frontend stack, /api/v1, applied V1-V4 migrations, private configuration and retained data. English engineering documentation; Brazilian Portuguese product content. Historical ADR observations are not current implementation status.

A task is complete only with implementation, successful relevant validation and documentation. `[ ]` pending, `[~]` in progress, `[x]` complete and validated, `[!]` blocked. Test evidence below distinguishes historical passes from commands rerun in this audit. No status here certifies production readiness.

## Initial evidence-based diagnosis

The active frontend has feature modules and enabled routes for identity, organizations, fleet, materials, parties, documents, intake, routes/executions, processing, inventory, buyers/sales and operations reporting. Backend controllers and PostgreSQL integration suites exist for those boundaries. Runtime SQL contains V1 baseline, V2 credential lifecycle, V3 recovery and V4 Routes; test-only V5 examples are not runtime migrations. Backend reference remains 20a03c7; frontend starts this audit after 3276bd4.

Prior verified evidence: 121 frontend tests, 25 real API tests, backend 48 unit/89 integration tests, and six local pair checks. These are dated observations, not fresh full-suite results. The owner accepted the local processing/sales walkthrough; this does not accept all other workflows.

Concrete gaps found by source inspection:

- Frontend README opening/setup and both data guides contain stale baseline/capability statements; historical checkpoints need current summaries.
- `tests/contract/intake.test.ts` and the private browser fixture use an invalid 76-byte PDF. The download assertion checks only its header and cannot establish viewer usability.
- `scripts/backup-development.mjs` selects default Compose/.env/key paths, while the running stack uses an explicit private Compose file. Current V1-V4 restore evidence is still needed.
- Initial Analytics had only operations-1. The owner-approved analytics-1 block now has a separate native screen and backend query, preserving the distinct existing report semantics.
- The owner explicitly approved comparison, section-count coverage, weighted productivity and posted-sale revenue on 2026-10-10. Other metric formulas, alert thresholds, global comparisons and export semantics remain product decisions.
- Current full dependency/OS-image review and representative-volume report plans remain unverified in this audit. Existing build-chain advisory dispositions are not a fresh clean scan.

## Ordered execution tasks

| ID / state | Objective and modules | Dependency | Acceptance and verification |
| --- | --- | --- | --- |
| A1 [~] | Complete documentation/code traceability audit; docs/adrs, requirements, README, source/controllers/tests | Existing decisions | Inventory each applicable requirement; distinguish implemented, partial, future and blocked; do not infer implementation from documentation |
| A2 [~] | Synchronize current documentation and this register in both repositories | A1 findings | Current baseline, local commands and capabilities agree with source; preserve dated ADR history |
| L1 [~] | Valid attachment fixture and download/viewer verification; tests/fixtures, intake contract test, Documents UI | Existing documents API | Valid single-page synthetic PDF renders; downloaded bytes match upload; authenticated browser download renders; keep existing sale history unchanged |
| L2 [ ] | Explicit-stack consistent backup and isolated restore; backend scripts/operations | L1 valid attachment; running local stack | Backup current V1-V4 and matching files/config privately; restore in network-isolated disposable PostgreSQL; all referenced files exist; zero ledger/lot/balance differences; live services recover |
| L3 [x] | Current code quality gates; Maven/Spotless, frontend lint/types/build/tests/API checks | Current Analytics and valid PDF corrections | Spotless and source-only full Maven pass; frontend lint/types/build, 127 tests, 26 actual API tests and generated-contract check pass. Any future L2 correction requires its own relevant rerun |
| L4 [ ] | Representative route/report query and workload verification; reporting service/IT | Current operations-1 definition | Bounded local data set with query plans, measured timings and exact expected totals; document workload limits before choosing indexes/cache |
| L5 [~] | Complete remaining browser journeys and accessibility review; identity/fleet/intake/routes/reports | Healthy matched local pair | Analytics empty/populated/route-filtered journeys and actual screenshots complete. Broader role/error/accessibility journeys remain; distinguish human acceptance and automated evidence |
| A3 [x] | Approved analytics-1 definitions and implementation; backend reporting/frontend reporting/data metric register | Explicit owner approval 2026-10-10 | Backend ADR-0022/frontend ADR-0021 implemented: matched comparisons, unique section-count coverage, weighted productivity, historical reversal-aware revenue; SQL/API/UI and real browser verification passed. Other families remain undefined |
| A4 [!] | PDF/spreadsheet report exports and consolidated comparisons | Approved metric/filter/export scope | UI/API/export totals match; permission and exact quantity tests; no placeholder exports |
| L6 [ ] | Final local readiness review and future deployment checklist | All applicable local tasks | Consistent docs/screenshots, no known blocking defects, repeated startup and matched contract evidence; Railway remains a separate future phase |

## Deferred and external dependencies

GPS/maps/telemetry remain PLANNED_POST_DEPLOY under the current decisions; optional BI remains deferred. External email is tested locally through Mailpit; real provider/domain/inbox acceptance remains external. Public HTTPS, production secrets, image release manifests, backup retention/off-machine storage and recovery objectives are future deployment/operator gates, not actions in this execution. GitHub Projects reconciliation lacks a supplied board/access and is outside local-only mutation scope. No legacy data import is inferred: ownership, reference closure and opening stock require explicit mapping.

## Current execution evidence

- Inspection confirmed five local service containers healthy; no database volume reset.
- Working branches: frontend/backend feat/native-analytics-mvp. Unrelated owner files retained.
- Backend 996e36f and frontend 9e25e91 are the rebuilt and running local implementation pair. No applied migration changed and no volume reset occurred.
- Backend original target resource access failed; no owner file or permission was altered. A public-source copy under the Codex workspace passed mvn clean verify: 51 unit and 95 PostgreSQL/Redis integration tests, zero failures/errors/skips. Original Spotless apply/check and Docker image build passed.
- Frontend npm run lint, npm run typecheck, npm test and npm run build passed; 127 tests in 16 files. The initial ambiguous loading assertion was corrected and the complete suite rerun. npm run api:check passed against 116 operations from actual Swagger.
- npm run test:contract passed 26 tests against the running backend, including linked-receipt denominator integrity, preserved plan snapshots, scope isolation, sale post/reverse historical recalculation and valid PDF byte equality. Disposable contract fixtures were compensated/deactivated without resetting retained data.
- Actual browser evidence covers empty, populated and route-filtered Analytics, visible definitions/calculation details and explicit unavailable revenue under geographic filtering. Frontend docs/analytics-integration.md contains the screenshots. Operator acceptance of Analytics remains separate.
- L1: the synthetic PDF is valid, renders through Poppler, and survives real API download byte-for-byte. Browser document-viewer verification remains pending; existing historical invoice files were not rewritten.
- Full documentation coverage and broad code review are still in progress; this initial diagnosis does not claim an exhaustive completed audit.
