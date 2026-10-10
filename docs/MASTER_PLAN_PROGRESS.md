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
| L1 [~] | Valid attachment fixture and download/viewer verification; tests/fixtures, intake contract test, Documents UI | Existing documents API | Real browser upload/download passed; bytes match, strict one-page parse and Poppler render pass. In-app file:// viewing is policy-blocked; operator viewer acceptance remains. Historical invoice files preserved |
| L2 [x] | Explicit-stack consistent backup and isolated restore; backend scripts/operations | Valid L1 attachment; running local stack | V1-V4 restored in network-isolated PostgreSQL: 39 matching table counts, 20 matching file hashes/references, zero stock differences; live backend restarted healthy. See backend docs/recovery-verification.md |
| L3 [x] | Current code quality gates; Maven/Spotless, frontend lint/types/build/tests/API checks | Current Analytics and valid PDF corrections | Spotless and source-only full Maven pass; frontend lint/types/build, 127 tests, 26 actual API tests and generated-contract check pass. Any future L2 correction requires its own relevant rerun |
| L4 [x] | Reproducible synthetic route/report performance baseline; reporting services and benchmark scripts | Current operations-1/analytics-1 definitions | Deterministic isolated V1-V4 data, progressive 1,000/10,000/50,000 volumes, exact oracle/conservation, HTTP percentiles/throughput/errors and SQL/prepared plans recorded; baseline failures preserved. See backend docs/reporting-benchmarks.md; synthetic evidence is not production certification |
| L4.1 [ ] | Resolve measured Analytics prepared-plan sensitivity in the application | L4 evidence | Evaluate a bounded query/JDBC remedy, repeat identical progressive cohorts and verify unit/integration/contract behavior; no default-mode report timeout hidden by successful-only percentiles. Isolated custom-mode experiment is not a shipped fix |
| L5 [~] | Complete remaining browser journeys and accessibility review; identity/fleet/intake/routes/reports | Healthy matched local pair | Analytics empty/populated/route-filtered journeys and actual screenshots complete. Broader role/error/accessibility journeys remain; distinguish human acceptance and automated evidence |
| A3 [x] | Approved analytics-1 definitions and implementation; backend reporting/frontend reporting/data metric register | Explicit owner approval 2026-10-10 | Backend ADR-0022/frontend ADR-0021 implemented: matched comparisons, unique section-count coverage, weighted productivity, historical reversal-aware revenue; SQL/API/UI and real browser verification passed. Other families remain undefined |
| A4 [!] | PDF/spreadsheet report exports and consolidated comparisons | Approved metric/filter/export scope | UI/API/export totals match; permission and exact quantity tests; no placeholder exports |
| L6 [ ] | Final local readiness review and future deployment checklist | All applicable local tasks | Consistent docs/screenshots, no known blocking defects, repeated startup and matched contract evidence; Railway remains a separate future phase |

## Deferred and external dependencies

GPS/maps/telemetry remain PLANNED_POST_DEPLOY under the current decisions; optional BI remains deferred. External email is tested locally through Mailpit; real provider/domain/inbox acceptance remains external. Public HTTPS, production secrets, image release manifests, backup retention/off-machine storage and recovery objectives are future deployment/operator gates, not actions in this execution. GitHub Projects reconciliation lacks a supplied board/access and is outside local-only mutation scope. No legacy data import is inferred: ownership, reference closure and opening stock require explicit mapping.

## Current execution evidence

- Inspection confirmed five local service containers healthy; no database volume reset.
- Working branches: frontend chore/local-recovery-evidence; backend test/synthetic-report-benchmarks, continuing the committed Analytics milestone. Unrelated owner files retained.
- Backend 996e36f and frontend 9e25e91 are the rebuilt and running local implementation pair. No applied migration changed and no volume reset occurred.
- Backend original target resource access failed; no owner file or permission was altered. A public-source copy under the Codex workspace passed mvn clean verify: 51 unit and 95 PostgreSQL/Redis integration tests, zero failures/errors/skips. Original Spotless apply/check and Docker image build passed.
- Frontend npm run lint, npm run typecheck, npm test and npm run build passed; 127 tests in 16 files. The initial ambiguous loading assertion was corrected and the complete suite rerun. npm run api:check passed against 116 operations from actual Swagger.
- npm run test:contract passed 26 tests against the running backend, including linked-receipt denominator integrity, preserved plan snapshots, scope isolation, sale post/reverse historical recalculation and valid PDF byte equality. Disposable contract fixtures were compensated/deactivated without resetting retained data.
- Actual browser evidence covers empty, populated and route-filtered Analytics, visible definitions/calculation details and explicit unavailable revenue under geographic filtering. Frontend docs/analytics-integration.md contains the screenshots. Operator acceptance of Analytics remains separate.
- L1: browser upload/download and post-restart Documents read passed. Downloaded PDF hash 1716a8a315fe809bb0e4595dfcf0fd6c59358bcd75fbec3a455ac5145b058410 matches the fixture; strict parsing and actual downloaded-byte rendering passed. In-app file:// opening was policy-blocked, not passed; owner viewer acceptance remains. Frontend docs/document-verification.md contains the screenshot/render distinction. Existing historical invoices were not rewritten.
- L2: selected private Compose backup and isolated restore passed with identical V1-V4 checksums, 39 public-table counts, 20 attachment hashes/references and zero ledger/lot/balance differences. Six recovery guard tests pass. Only labeled rehearsal containers were removed; all five live services remain healthy. Private snapshots and keys remain ignored; public evidence contains no secrets.
- Full documentation coverage and broad code review are still in progress; this initial diagnosis does not claim an exhaustive completed audit.
- L4: backend ADR-0023 records deterministic synthetic benchmarking without changing report formulas, migrations or live configuration. Initial auto-mode 1,000/10,000 runs validated 3,200 measured responses; the first 50,000 attempt was interrupted while Docker became unavailable. Restored live services without resetting retained volumes. Repeated 50,000 auto-mode Analytics recorded four 30-second timeouts under concurrency four; preserve this failure as a concrete L4.1 release concern.
- Prepared-plan diagnostics on identical 50,000 data measured custom broad/month/route/smaller-organization SQL at 387/147/142/107 ms; generic broad exceeded 30 seconds and other generic queries took approximately 9.9–16.6 seconds. A disposable-only custom-plan HTTP repeat passed 320 responses. This experiment does not change production query/JDBC settings and does not claim production readiness; full measurements and limits are maintained in backend docs/reporting-benchmarks.md.
- Corrected JDBC positional diagnostics at 10,000 reproduced broad custom/generic SQL at 75/10,777 ms, including 5,541 aggregate rescans. Identical SQL hashes were confirmed for every volume across comparisons. Final corrected cross-volume harness verification passed 640 requests; the custom 10,000 repeat passed 1,600. Accumulated recorded measurements: 7,438 starts, 7,434 exact successful responses and four timeouts, with setup/transport interruptions separately retained. Eight benchmark and six recovery guard tests pass; sanitized public evidence contains no keys or runtime credentials.
