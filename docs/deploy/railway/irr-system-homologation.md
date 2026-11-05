# Operate IRR homologation in the Institute workspace

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Updated 2026-11-05.

Use this guide for `irr-system` → `homologation` in the Instituto de Referência em Resíduos IRR workspace. The previous `irr-homologation` project is a separate recovery source. Do not deploy to its IDs or to the new project's unused `production` environment.

## 1. Authenticate and verify the target

```powershell
railway whoami
$Project = '59bc315f-4921-462a-9d0e-c460b187fe99'
$Environment = '2158f474-a314-4763-bde6-e042af274a33'
$Database = '0b8f0768-6ae2-4524-a8ec-b33889af426e'
$Redis = '4aaa99c6-e109-49bf-9e64-d6c5a5c761bc'
$Backend = '14dcc446-7492-43ff-9b25-0591cb782dce'
$Frontend = '59da6075-bf72-4353-8c79-2cfcb1637fc0'
```

Expect the Institute account or another authorized member. If switching accounts, use `railway logout`, then `railway login` and check `railway whoami` again. For persistent local linking, run `railway link --project $Project --environment $Environment` in a dedicated ignored operations directory and confirm `railway status`. Run the variable block again in each new PowerShell session; the link persists but the shell variables do not.

- Application: [login](https://frontend-homologation-9c49.up.railway.app/login).
- Backend: [readiness](https://backend-homologation-fe62.up.railway.app/actuator/health/readiness) and [Swagger](https://backend-homologation-fe62.up.railway.app/swagger-ui/index.html).
- Dashboard: [irr-system / homologation](https://railway.com/project/59bc315f-4921-462a-9d0e-c460b187fe99?environmentId=2158f474-a314-4763-bde6-e042af274a33).

## 2. Resume or deploy

1. Keep the existing PostgreSQL `/var/lib/postgresql/data`, Redis `/data` and backend `/data/uploads` volumes. Check the workspace's active plan and remaining credit; the complete layout requires three volumes. Do not create new volumes to resume.
2. Use the source-preparation block in [the general manual](operator-guide.md#4-prepare-clean-source-archives) with the **new IDs above** and the reviewed Git release: backend `5c178366afdeb332caa4c41b47ed72f03cd4e508`, frontend `a31e6e82b9ef4c687ae8a6758feb71cd253c43fa` (`v0.1.0-rc.1` in each repository). Prepare the backend Dockerfile cache namespace for the new `$Backend`. Upload only the four prepared source directories, never the operations directory or a dump.
3. Use the general manual's [upload commands](operator-guide.md#5-deploy-or-resume-in-dependency-order) with these new IDs. Wait for database and Redis first, then start backend, then frontend. A removed deployment may return `No deployment found for service` from `railway redeploy`; in that case upload the staged source again with `railway up`. This reuses the existing volumes.
4. Confirm private database/Redis names, backend ports `9191`, frontend port `8080`, exact frontend CORS origin and the frontend build argument `https://backend-homologation-fe62.up.railway.app/api/v1`. Recovery email remains disabled unless a provider is configured and verified.
5. Confirm backend readiness, frontend `/health`, `/login` and direct SPA routes, then run the [public pair checks](../operator-guide.md#4-common-acceptance-checklist). Verify an existing account and a known attachment. Credentials must be obtained privately; a migrated account may have changed its password since the old local handoff was created.

## 3. Restore only when creating a replacement database

The `irr-system` database was restored from the validated source dump before backend startup. A normal redeploy does **not** require another restore. For disaster recovery, start PostgreSQL 16 with an empty target database, keep backend writes disabled, verify the dump hash, and restore with `pg_restore --no-owner --no-privileges --exit-on-error --single-transaction`. Confirm V1–V4 history and counts against the backup; keep `BOOTSTRAP_ADMIN_ENABLED=false` for a restored database. Use a private SSH tunnel or binary-safe transfer; Windows PowerShell text redirection can corrupt a binary archive.

The dump includes attachment metadata only. Copy the original files separately to `/data/uploads` by their stored names and verify downloads. A missing file must remain an explicit migration exception; do not replace it with an unrelated file. Keep original backups privately until the destination is verified. Preserve the original workspace and its volumes during migration.

### Migration verification — 2026-11-05

The owner explicitly authorized importing the retained records and original attachments into this Institute homologation environment, including the real attachment. This is specific to this migration and does not change the production hosting decision. The restored PostgreSQL 16 database has 39 public tables, V1–V4 and two attachment records. Both files were recovered and matched their original SHA-256 through authenticated downloads. All four services were online and the six public pair checks passed. The existing administrator credentials worked; the previously saved operator password returned HTTP 401 and was not reset. Recovery email remains disabled. No credentials, database dumps or attachment contents are stored in Git.

## 4. Stop safely

From the [dashboard](https://railway.com/project/59bc315f-4921-462a-9d0e-c460b187fe99?environmentId=2158f474-a314-4763-bde6-e042af274a33), open each service's **Deployments**, remove the active deployment, and verify no deployment remains active. Order: frontend, backend, Redis, database. Preserve the services and volumes. For CLI commands see [pause and resume](operator-guide.md#7-pause-update-rollback-and-backups) and use the new IDs above. Storage may still consume credit.
