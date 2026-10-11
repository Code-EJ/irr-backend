# Isolated homologation execution — 2026-10-11

## Current checkpoint: resumed by owner

The owner subsequently requested activation. All four services were deployed successfully and the new frontend credits, public contract, administrator/operator login and retained PDF were verified. [The resume record](resume-2026-10-11.md) gives exact source SHAs, deployment/image IDs and remaining limitations. The pause text below is history, not the current service state.

## Historical checkpoint: owner-requested pause

The owner requested a temporary shutdown to preserve trial compute credit before scheduling the client session with Paulo. The official CLI `railway down --project <project-id> --environment <environment-id> --service <service-id> --yes` was executed sequentially for frontend, backend, Redis and PostgreSQL, using only the isolated project/environment/service identifiers below. A subsequent `railway deployment list` for each service confirmed its latest deployment is **REMOVED**. No volume, service definition, domain, variable or retained record was deleted. The existing `lovely-wonder` project was not stopped or modified.

The public application is currently offline. Persistent storage can still consume credit: Railway bills used volume storage independently of active application compute, including filesystem metadata ([volume pricing](https://docs.railway.com/volumes/reference#pricing)). Other preserved projects may also consume the account's shared trial credit. This pause is not a zero-cost guarantee or a backup.

Before the scheduled session, resume PostgreSQL and Redis first, wait for their readiness, then deploy the reviewed backend and frontend from clean versioned sources using [the installation procedure](homologation-installation.md). Preserve the existing volumes, private secrets, PORT, exact CORS and disabled bootstrap settings. Include the latest frontend institutional-footer commit in the next upload; it has not been published during this pause. Repeat the six public pair checks, operator login, organization selection and persisted PDF download before sending an availability confirmation. Redeployment and resuming compute require an owner-requested session; do not silently restart services while documenting or preparing PRs.

The two coordinated PRs target `main` from `chore/release-readiness`: [backend #79](https://github.com/Code-EJ/irr-backend/pull/79) and [frontend #67](https://github.com/Code-EJ/irr-frontend/pull/67). They remain drafts with explicit acceptance/security/mail gates; neither has been merged. Credentials remain only in the ignored private handoff file. The client message is a draft for the owner to send, not an email sent by automation.

## Verified runtime checkpoint before pause

The separate synthetic environment is now running all four services. Backend deployment `ddfaec01-b072-44a2-8614-b064d48ff9a8` succeeded after PORT=9191 was aligned with SERVER_PORT and the public target. Backend source is `84fe4f1` with only its literal Maven cache namespace generated for service `70ac22d1-7f09-457e-a816-8631d84e6339`; the owner Dockerfile stays unchanged. The retained generator is `scripts/prepare-railway-dockerfile.mjs` in `b3077ef`. Source upload excludes ignored credentials and does not enable GitHub autodeploys. The earlier cloud failures and preparation-only statements below are historical.

- PostgreSQL deployment `d078c812-d441-46f6-8783-52fd719dcf53`: PostgreSQL 16.15 ready; private network only.
- Redis deployment `1b4a7869-2731-4a37-b580-6a6e88d8832d`: Redis 8.2.10 with authenticated AOF, 64 MB maxmemory and noeviction. A root-owned mount containing lost+found initially prevented upstream repair. The bounded mount-root fix in `84fe4f1` preserves that directory and passes the retained secret-safe startup test. Actual PID 1 UID is 999, with no capability bounds and no-new-privileges.
- Frontend deployment `9563f266-6ee7-4682-a0d1-3854c77f0a26`: source `bdcafa3`, public API origin baked into the bundle; HTTPS and direct SPA routes verified.
- Actual backend PID 1 UID/GID is 10001/10001, with zero capability bounds and no-new-privileges. All three 500 MB volumes now read back `us-east4-eqdc4a`; PostgreSQL runtime also reports Virginia. V1–V4 were applied normally, without reset or repair.

The six actual public pair checks passed: health, semantic pinned OpenAPI, exact CORS, frontend health/direct routes and served bundle origin. Operator authentication, organization selection, six report/reconciliation reads and a real PDF upload/download passed. After replacing the backend with bootstrap disabled, operator login and the persisted PDF download passed with the same SHA-256. This is measured API/runtime evidence, not completed browser/operator acceptance. The initial admin password was rotated and bootstrap variables cleared. A synthetic MANAGER operator is scoped to **Homologação IRR — Dados Sintéticos**. Private owner/admin/operator credentials are in ignored `.local/railway-homologation/.env-homologation`, never documentation or Git. A dedicated personal Railway SSH key was registered for these checks; its private key remains ignored locally.

[Client session instructions](client-homologation.md) describe the ready functional evaluation scope. Mail remains disabled and actual recovery delivery is pending approved provider configuration. Raw image findings, final risk review and client/operator acceptance remain open; no production deployment or main merge is claimed. The previous project was preserved. Backend CI at `cc25efe` completed successfully; later deployment-tooling commits require their own checks.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Authority: [ADR-0028](../../adrs/0028-isolated-trial-homologation.md). This is a provisioning record, not a successful deployment or merge approval. The existing `lovely-wonder` project was not modified. No application image, GitHub source or automatic deployment was connected to the new services.

## Created resources

Project: `irr-homologation`, ID `642ae148-48a0-4e29-bd03-583bf5af17d6`.
Environment: `homologation`, ID `8c60c102-8ef8-4d50-82f2-ac5429a4b45c`.
The provider-created `production` environment in this new project remains empty and unused.

| Service | Service ID | CPU ceiling | RAM ceiling | Persistent mount |
| --- | --- | --- | --- | --- |
| database | `229d7065-6e5b-47c8-b123-e1919fade22a` | 0.5 vCPU | 250,000,000 bytes | `/var/lib/postgresql/data` |
| redis | `79a3ee49-98a5-4b6d-85d8-93b87159b483` | 0.25 vCPU | 125,000,000 bytes | `/data` |
| backend | `70ac22d1-7f09-457e-a816-8631d84e6339` | 1 vCPU | 1,000,000,000 bytes | `/data/uploads` |
| frontend | `c35b6e56-6491-40e9-929e-97a59dbef3c3` | 0.25 vCPU | 125,000,000 bytes | None |

Each volume reports a 500 MB size. Limits are ceilings, not reserved capacity, demonstrated performance, or a monthly cost guarantee. PostgreSQL and Redis have no public domain or TCP proxy. Backend healthcheck is `/actuator/health/readiness`; frontend healthcheck is `/health`, both with a 180-second startup timeout. Configuration retains one replica, no application sleeping and at most three restart retries.

Reserved domains, **not yet serving IRR**:

- Backend: `https://backend-homologation-7600.up.railway.app`, target port 9191.
- Frontend: `https://frontend-homologation-ffac.up.railway.app`, target port 8080.

## Region verification

All four environment service configurations read back `deploy.multiRegionConfig = {"us-east4-eqdc4a":{"numReplicas":1}}`, the [documented Virginia identifier](https://docs.railway.com/deployments/regions). The legacy `ServiceInstance.region` field reads null even after setting its mutation input; do not use a successful mutation response alone as region proof. The CLI scale command also misparsed explicit project arguments in this execution; the authenticated GraphQL update and configuration readback were used instead.

The volume-instance API still reports `sfo` for the newly created, empty volumes. No workload has ever deployed. Virginia runtime and volume locality are therefore **pending effective deployment verification**. Do not label the region fully verified, populate records, or migrate any retained environment based only on desired service configuration. Inspect the selected environment's region/volume transition before attaching final images. Preserve the old project's volumes.

## Variables and secrets

Fresh 32-byte random database and Redis passwords and a matching 3072-bit RSA pair were generated privately and configured only in the new environment, using `variableCollectionUpsert` with `replace=false` and `skipDeploys=true`. No secret values are recorded here, printed in command output, staged, or placed in frontend variables. Private provisioning files are under the ignored `.local/railway-homologation/` directory; protect them as credentials, not evidence to publish.

Backend DB/Redis hosts use the new services' private names and ports 5432/6379. The browser API build variable uses the reserved public backend URL with `/api/v1`; backend CORS is the exact reserved frontend origin. Recovery URL ends in `/reset-password`. Mail remains `disabled`: recovery delivery is an **incomplete gate** pending the approved Brevo sender/key and real-inbox verification. Development and first-admin bootstraps are disabled; no remote administrator has been provisioned.

Backend storage permission initialization is configured as specified in ADR-0026, with the Java process required to drop to UID/GID 10001. This remains a real Railway runtime verification gate. A candidate `JAVA_TOOL_OPTIONS` bounds heap to 384 MB and active processor count to one with exit on out-of-memory; its cloud startup/load behavior has not yet been verified.

## Local evidence before deployment

| Command | Result |
| --- | --- |
| Java 21: `mvn --batch-mode --no-transfer-progress clean verify` | Passed: 51 unit tests and 98 integration tests, no failures/errors/skips |
| `node --test scripts/tests/initial-data.test.mjs scripts/tests/recovery.test.mjs scripts/benchmarks/benchmark.test.mjs` | 22 tests passed |
| `node scripts/verify-container-storage.mjs irr-backend-storage:candidate` | Seven isolated permission/privilege scenarios passed |
| `node scripts/verify-hosted-compose.mjs` | Portable deployment boundaries passed |
| Frontend `npm run lint`, `npm run typecheck`, `npm run test`, `npm run build` | Passed; 136 tests across 17 files |
| Frontend `npm run api:check` | Passed: 116 supported v1 operations against pinned backend contract `996e36f` |

The initial PowerShell loop incorrectly used a colon in the API-check log filename and printed a misleading pass. The command was rerun separately with a safe filename and explicit exit-code propagation; the result above refers to that successful rerun. The pinned API check is not a remote Railway smoke test.

## Remaining gates and execution order

1. Resolve or formally review remaining framework and image findings in [the security inventory](../../security/README.md), selecting exact compatible image references. Maven security CI still reports the two documented Spring MVC advisories; verification success does not make that audit green. No audit suppression or forced merge was performed.
2. Verify Virginia effective service/volume locality; connect reviewed PostgreSQL/Redis images with authenticated Redis persistence and validate private readiness. No database recreation or history edits are needed: use actual V1–V4 migrations.
3. Deploy reviewed backend/frontend source from explicit feature-branch commits, excluding ignored credentials and unrelated owner files. Verify first-admin provisioning/password change, HTTPS/CORS, `/api/v1` contract, upload persistence after restart, and startup privilege drop. Then disable bootstrap and remove its temporary password.
4. Configure the approved mail provider and verify actual delivery/reset behavior. Run synthetic scoped workflows and obtain separate operator acceptance. Export necessary synthetic evidence before trial expiry.
5. Push review branches, open PRs, review actual CI results and release gates before merging. Homologation provisioning neither changes main nor proves production readiness.

The account usage snapshot was approximately USD 0.03765; it is not remaining-credit evidence or a forecast. Trial credits are shared with the preserved project. No payment method, paid plan, workspace-wide spending limit or production resource was changed. Monitor actual account usage during bounded homologation sessions; the Free plan cannot retain this three-volume topology.

## GitHub and stateful candidate follow-up

The branches were pushed and draft review PRs opened: [backend #79](https://github.com/Code-EJ/irr-backend/pull/79) and [frontend #67](https://github.com/Code-EJ/irr-frontend/pull/67). No main merge occurred. At backend commit `04454b2`, workflow run `38135252086` passed Java, documentation and container jobs but failed the dependency-audit step; its recovery, initial-data and benchmark guard tests passed. Frontend workflow run `38135253800` at `701ccc0` completed successfully. These are dated commit-specific results, not automatic proof for subsequent commits.

[The stateful remediation evidence](../../security/README.md) now records PostgreSQL 16.15 and Redis 8.2.10 patched candidate images, each with zero critical/high scan matches and four residual findings requiring review. The retained stateful rehearsal checks authentication and persistent restart without published ports or development-volume changes. These candidates have not been deployed to Railway; the dependency/security and effective-region gates remain open. Docker Desktop was restarted locally and the existing backend/frontend/database/Redis/Mailpit containers returned healthy.
