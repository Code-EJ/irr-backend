# Local Master Plan execution progress

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Updated 2026-10-09.

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
- Analytics has only operations-1 in the native frontend. Existing backend operational JSON/CSV summaries have distinct UTC/current-status semantics; they must not be silently represented as the Brazilian-period report.
- Broader metric formulas, alert thresholds, global comparisons and export semantics remain product decisions. A clarification was requested; no answer is inferred.
- Current full dependency/OS-image review and representative-volume report plans remain unverified in this audit. Existing build-chain advisory dispositions are not a fresh clean scan.

## Ordered execution tasks

| ID / state | Objective and modules | Dependency | Acceptance and verification |
| --- | --- | --- | --- |
| A1 [~] | Complete documentation/code traceability audit; docs/adrs, requirements, README, source/controllers/tests | Existing decisions | Inventory each applicable requirement; distinguish implemented, partial, future and blocked; do not infer implementation from documentation |
| A2 [~] | Synchronize current documentation and this register in both repositories | A1 findings | Current baseline, local commands and capabilities agree with source; preserve dated ADR history |
| L1 [~] | Valid attachment fixture and download/viewer verification; tests/fixtures, intake contract test, Documents UI | Existing documents API | Valid single-page synthetic PDF renders; downloaded bytes match upload; authenticated browser download renders; keep existing sale history unchanged |
| L2 [ ] | Explicit-stack consistent backup and isolated restore; backend scripts/operations | L1 valid attachment; running local stack | Backup current V1-V4 and matching files/config privately; restore in network-isolated disposable PostgreSQL; all referenced files exist; zero ledger/lot/balance differences; live services recover |
| L3 [ ] | Fresh existing quality gates; Maven/Spotless, frontend lint/types/build/tests/API checks | L1/L2 corrections | Record exact passed, failed and not-run commands; no unrelated lockfile/formatter rewrite |
| L4 [ ] | Representative route/report query and workload verification; reporting service/IT | Current operations-1 definition | Bounded local data set with query plans, measured timings and exact expected totals; document workload limits before choosing indexes/cache |
| L5 [ ] | Complete remaining browser journeys and accessibility review; identity/fleet/intake/routes/reports | Healthy matched local pair | Real browser positive/error/role journeys, synthetic data and screenshots in docs/assets/screenshots; distinguish human acceptance and automated evidence |
| A3 [!] | Additional analytics definitions and implementation; backend reporting/frontend reporting/data metric register | Owner definitions pending | Explicit denominator, date, null/zero, correction and scope rules, then API/SQL/frontend tests; no invented formulas |
| A4 [!] | PDF/spreadsheet report exports and consolidated comparisons | Approved metric/filter/export scope | UI/API/export totals match; permission and exact quantity tests; no placeholder exports |
| L6 [ ] | Final local readiness review and future deployment checklist | All applicable local tasks | Consistent docs/screenshots, no known blocking defects, repeated startup and matched contract evidence; Railway remains a separate future phase |

## Deferred and external dependencies

GPS/maps/telemetry remain PLANNED_POST_DEPLOY under the current decisions; optional BI remains deferred. External email is tested locally through Mailpit; real provider/domain/inbox acceptance remains external. Public HTTPS, production secrets, image release manifests, backup retention/off-machine storage and recovery objectives are future deployment/operator gates, not actions in this execution. GitHub Projects reconciliation lacks a supplied board/access and is outside local-only mutation scope. No legacy data import is inferred: ownership, reference closure and opening stock require explicit mapping.

## Current execution evidence

- Inspection confirmed five local service containers healthy; no database volume reset.
- Working branches: frontend chore/local-completion-audit; backend chore/local-recovery-validation. Unrelated owner files retained.
- Full documentation coverage and broad code review are still in progress; this initial diagnosis does not claim an exhaustive completed audit.
