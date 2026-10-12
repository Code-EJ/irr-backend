# Railway rollout and rollback

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Preparation

Review the current chore/release-readiness branches and explicit deployment authorization before any merge or remote action. Preserve unrelated edits. Review both PRs; do not merge automatically. Record source commits, image build result, tests and required infrastructure actions before release. GitHub main remains the deployment source; pushing a feature branch is not a release.

Backend gates: ./mvnw clean verify with Java 21 and Docker; dependency audit; documentation verification; Docker build; real PostgreSQL V1 upgrade and empty-schema tests. Frontend gates: npm ci, npm run lint, npm run typecheck, npm test, npm run build, npm run api:check, local contract tests and browser smoke. Both images must build from Git without local keys, .env files or uploaded artifacts.

## Merge order

1. Finalize and review the backend branch and PR.
2. Merge backend to monitored main; Railway builds the existing Dockerfile and starts Spring Boot.
3. Flyway automatically applies V2, V3 and V4 after V1; Hibernate validates the result. Watch for lock waits and startup failures. No migration file copying, SQL patching or shell access is needed.
4. Verify GET /actuator/health, /actuator/health/liveness and /actuator/health/readiness: HTTP 200 and status UP. Verify Swagger/OpenAPI when enabled.
5. Smoke the currently published frontend with an ordinary existing account: login, GET /api/v1/users/me, organization selection and a supported business workflow. Existing generation-zero v1 tokens remain accepted until credentials change.
6. Finalize and review the frontend branch and PR. Build with the public HTTPS backend /api/v1 URL.
7. Merge frontend only after the backend is healthy; Railway builds Vite assets and Nginx automatically.
8. Verify frontend GET /health (ready), / and a direct nested route return 200; assets load; CORS preflight matches the frontend origin.
9. Complete identity smoke below. Record actual environment evidence; local rehearsal is not remote deployment proof.

Do not initiate administrative resets before the new frontend is available. An older bundle cannot complete forced password change. Do not deploy the new frontend against the old backend.

## Identity smoke

Use designated disposable test accounts, never an unrelated real account. Verify own profile edit and email-conflict feedback; wrong-current-password rejection; successful change, old-token rejection and new login; administrator list/detail/create/edit/deactivate; non-administrator denial; reset with mandatory change; and explicit MEMBER/MANAGER grant and revocation. Global administrator access does not create business membership; global authority itself permits active-organization access. Local accounts require actual active associations. Recheck user access after revocation and deactivation.

All application routes use /api/v1. Credential endpoints return no password or hash. Reset sets mustChangePassword; restricted sessions can only GET /users/me and PUT /users/me/password under that prefix.

## Flyway confirmation and baseline boundary

Use startup migration/validation results and the versioned PostgreSQL tests. If operators inspect history, a read-only query such as SELECT version, description, success FROM flyway_schema_history ORDER BY installed_rank; must show successful versions 1, 2 and 3. This is diagnosis, not an installation step.

OPTION A — INCREMENTAL: V1 unchanged, V2 credential fields and V3 recovery table additive, existing database upgraded in place. Preserve Flyway history and attachment storage. After operational data must be retained, every schema change uses a new version. No clean, erased history, rewritten applied migration or automatic reset. A future deliberate pre-production reset needs a separate reviewed runbook and operator authorization.

## Rollback

V2 is additive and compatible at schema level with the previous binary. However, the previous backend ignores credential generations and mandatory-change state. **After any credential change/reset, it is not a safe security rollback target.** Use a forward fix or a reviewed artifact preserving those checks. Do not downgrade the database or edit password hashes to force startup.

Frontend rollback is safe for ordinary accounts only; accounts awaiting forced change still need a compatible password screen. Retain a security-compatible frontend artifact or forward-fix it. Restoring database backups is a separate operator-led recovery operation, including attachment consistency and access protection, not the default rollback procedure.

## Infrastructure exceptions

Current Identity completion requires mail configuration and sender verification; no new database/Redis service or reset. New empty environments optionally require BOOTSTRAP_ADMIN_* as documented. RSA rotation, new secrets, CORS/domains, network/ports, persistent storage and database recreation/restoration always require a separately listed infrastructure action. No remote changes were performed for local verification.


## Expanded identity release gate

The frontend repository's `npm run verify:homologation` checks the supplied public frontend/backend pair without authenticating or mutating records: backend health, canonical pinned OpenAPI (root server origins excluded), exact CORS, frontend health/direct SPA routes and served API origin. Set public IRR_API_URL/IRR_FRONTEND_URL and the verified frontend image source IRR_FRONTEND_COMMIT. Its ignored deployed-candidate report is partial evidence, not a release approval or automatic v1 freeze. See frontend docs/homologation-verification.md. Actual source/image provenance, migrations/storage and authenticated/email smoke remain operator gates below.

Deploy backend V1/V2/V3 and a frontend pinned to that OpenAPI. Verify global administrator creation/edit/succession, last-admin rejection, local MANAGER isolation, organization CRUD/membership/revocation, recovery external delivery/expiry/replay and create-only BOOTSTRAP_ADMIN_* restart behavior. Do not freeze v1 or start Routes until this homologation gate passes. Local evidence does not certify a Railway deployment.

Rollback must retain V3 and preserve credential-generation enforcement. A pre-recovery frontend/backend pair disables recovery, so use coordinated release manifests. Do not restore a pre-generation binary that can revive revoked bearer tokens. See [recovery rollback](../../email-recovery.md#rollout-and-rollback).
