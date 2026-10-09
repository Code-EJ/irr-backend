# Operational reporting verification

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Recorded 2026-10-09. Scope: backend ADR-0021 / frontend ADR-0018, continuing ADR-0020 and the existing Master Plans.

## Executable baseline and changes

Backend source: 20a03c7cc8cb31e338056917877c733bdda99b6e on feat/operational-reporting. The first approved report adds GET /api/v1/reports/operations, exact decimal-string totals, effective organization scope, required inclusive Brazilian dates and optional historical municipality/route/vehicle filters. No applied V1-V4 migration or database volume was reset. The current Swagger exposes 115 supported operations across 64 versioned paths. Unversioned aliases remain absent.

Frontend implementation f063a7c uses the generated contract, existing Axios transport and existing React/CSS stack. It adds Portuguese filters/results, completeness/provenance, safe errors/retry and empty states. The public local frontend API URL was corrected from the stopped legacy port 9191 to the selected identity worktree port 9292; private credentials were preserved.

## Commands and actual results

| Gate | Result |
| --- | --- |
| Maven Spotless apply/check, compile, package and full verify | PASS: 48 unit tests and 89 PostgreSQL/Redis integration tests, including five operational report tests; zero failures/errors/skips |
| Original worktree Spotless apply | PASS: 276 Java files; existing plugin 3.10.3 retained, check bound to validate |
| node scripts/verify-documentation.mjs --update-inventory | PASS: all 276 main/test Java sources indexed |
| Backend Docker Compose build/start | PASS; backend, PostgreSQL, Redis and Mailpit healthy after restart |
| Frontend api:capture, api:generate, api:check | PASS: actual running Swagger pinned to source 20a03c7; 115 v1 operations |
| npm run lint / npm run typecheck | PASS: ESLint zero-warning gate and application/build/test TypeScript checks |
| npm test | PASS: 83 tests across 10 files, including nine reporting checks |
| npm run test:contract | PASS: 21 tests across five files against local backend, Redis, PostgreSQL and Mailpit |
| npm run build / frontend Docker Compose build/start | PASS: Vite production build and healthy Nginx container |
| npm audit --omit=dev --audit-level=high | PASS: zero production dependency vulnerabilities in this registry observation |
| npm run verify:homologation with explicit local mode | PASS: six read-only pair checks, pinned schemas/security, readiness, CORS, direct report SPA route and served API origin |

The original Windows target directory contains a locked generated V4 resource. Maven clean/resource copying there failed with AccessDeniedException; no migration, permissions or user files were changed to bypass it. Full Maven verification ran from a source-only build snapshot under the chat workspace, containing public src, pom and Maven wrapper files, with no Git metadata or private environment files. Spotless normalized line endings in that snapshot before clean verify. This is verified source evidence, not a claim that clean verify passed in the locked original target directory. Docker builds also passed independently.

The production-only bundle verifier requires a real public HTTPS API origin and intentionally rejects this loopback development configuration; it was not used as local acceptance. Local pair verification is the applicable gate. External homologation and production bundle verification remain pending.

## Coverage and remaining work

Browser acceptance also passed against the running containers: existing administrator sign-in; required organization selection; empty report with unknown distance; switch to retained Routes fixture history; route/vehicle filter application; exact 8.125 km, 63.485280 seconds and 1.2345 kg from one execution and one receipt. Organization switching cleared the prior report. The full-page layout was visually reviewed; private local evidence is saved as frontend .local/operational-report.png. These read-only browser checks created no new business fixtures.

Tests cover Brazil midnight boundaries, decimal precision, missing versus measured-zero distance, source-grain independence with multiple linked receipts/crew, live edits and inactive collection removal, retained historical filters, foreign scope, bounded dates and current membership validation. Real API tests execute completed work, linked receipt entry, route inactivation and scoped report lookup; foreign route filters return 404. The only contract correction was documenting nullable applied filter IDs in Swagger. No additional backend domain capability was required.

No replaced legacy integration was needed: this slice adds a native report using the existing v1-only transport. Existing commercial summaries/CSV are retained with their distinct semantics. Unapproved coverage/productivity formulas, comparative charts, exports, fiscal publication, alerts/targets, GPS and BI remain outside operations-1. Review realistic-volume query plans/latency before adding indexes or caches. Existing build-tool advisories and external email/deployment approval remain separate release gates.
