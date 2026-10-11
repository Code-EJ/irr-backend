# Isolated homologation execution — 2026-10-11

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
