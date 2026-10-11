# Railway: manual installation, resume and updates

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). CLI syntax checked with 5.64.2 on 2026-10-11.

Read [the operator prerequisites](../operator-guide.md) first. Commands below run in **PowerShell on your workstation**. Dashboard steps use an owner-controlled account. Source is uploaded manually through the official CLI; a Git push alone does not update these services. See [official CLI instructions](https://docs.railway.com/cli).

## 1. Authenticate and select the destination

```powershell
npm install --global @railway/cli@5.64.2
railway --version
railway login
railway whoami
```

Expected: the intended account appears. For a headless login use `railway login --browserless` and complete its device approval in your browser. Do not paste a token into documentation. On the existing maintainer machine, the same CLI is also available at `<backend>/.local/tools/railway/node_modules/.bin/railway.cmd`; the global installation makes the commands below usable without that private tool folder.

For the **existing paused environment**, use these non-secret identifiers and skip resource creation:

```powershell
$Project = '642ae148-48a0-4e29-bd03-583bf5af17d6'
$Environment = '8c60c102-8ef8-4d50-82f2-ac5429a4b45c'
$Database = '229d7065-6e5b-47c8-b123-e1919fade22a'
$Redis = '79a3ee49-98a5-4b6d-85d8-93b87159b483'
$Backend = '70ac22d1-7f09-457e-a816-8631d84e6339'
$Frontend = 'c35b6e56-6491-40e9-929e-97a59dbef3c3'
```

Open project `irr-homologation`, environment `homologation`, and confirm those IDs in the service settings. Preserve `lovely-wonder`. The existing public names are `backend-homologation-7600.up.railway.app` and `frontend-homologation-ffac.up.railway.app`. At the documented pause all four latest deployments were REMOVED; volumes and credentials still existed. Never recreate them simply because the app is offline.

## 2. Fresh environment only: create resources

1. In the dashboard create an empty project and an environment named `homologation`. Record their IDs and replace the variables above. Do not deploy into an automatically selected `production` environment.
2. Add four empty services named exactly `database`, `redis`, `backend`, `frontend`. Record each service ID. Keep GitHub autodeploys disabled for this manual workflow.
3. Choose US East Virginia for each service and one replica. This region is authorized only for synthetic homologation. Check actual volume locality after deployment, not just the selected region.
4. Attach separate persistent volumes: database `/var/lib/postgresql/data`, Redis `/data`, backend `/data/uploads`. Verify the account supports three volumes and sufficient capacity. Do not attach an existing project's volumes.
5. In Networking generate HTTPS domains for backend (target 9191) and frontend (target 8080). Do not create public domains or TCP proxies for database/Redis. Record their actual private DNS names from Networking.
6. Configure backend healthcheck `/actuator/health/readiness`, frontend `/health`, startup timeout 180 seconds, and bounded restart-on-failure retries (3). Use Dockerfile builds and preserve image entrypoints/start commands.
7. Set resource ceilings and inspect credit usage. The existing trial used backend 1 GB/1 CPU, database 250 MB/0.5 CPU and frontend/Redis 125 MB/0.25 CPU each. These are measured startup settings, not production capacity promises. Storage can continue billing after compute is stopped.

## 3. Configure variables before the first deploy

Use each service's Variables editor. Keep secrets out of command history. For the existing environment **retain** working passwords and matching keys. Changing POSTGRES_PASSWORD on an initialized volume does not rotate the database role password.

| Service | Required values |
| --- | --- |
| database | `POSTGRES_DB=irr`, `POSTGRES_USER=irr_app`, unique `POSTGRES_PASSWORD`, `PGDATA=/var/lib/postgresql/data/pgdata` (data lives below the mount root) |
| redis | unique `REDIS_PASSWORD`: at least 32 base64url characters (letters, digits, `_`, `-`); required by the retained startup script |
| backend | `DB_HOST` = database private DNS; `DB_PORT=5432`; `DB_NAME`, `DB_USER`, `DB_PASSWORD` matching PostgreSQL; `REDIS_HOST` = Redis private DNS; `REDIS_PORT=6379`; matching `REDIS_PASSWORD` |
| backend | `SERVER_PORT=9191`, **`PORT=9191`**, `STORAGE_DIRECTORY=/data/uploads`, `RAILWAY_RUN_UID=0`, `IRR_INIT_STORAGE_PERMISSIONS=true` |
| backend | matching `RSA_PRIVATE_KEY` (PKCS#8 PEM) and `RSA_PUBLIC_KEY` (public PEM), pasted as complete multiline values |
| backend | `CORS_ALLOWED_ORIGINS=https://FRONTEND-DOMAIN`, `PASSWORD_RESET_BASE_URL=https://FRONTEND-DOMAIN/reset-password`, `API_DOCS_ENABLED=true` |
| backend | `DEV_BOOTSTRAP_ENABLED=false`, `BOOTSTRAP_ADMIN_ENABLED=false`, no development Spring profile; `MAIL_PROVIDER=disabled` until approved email setup |
| backend | `JAVA_TOOL_OPTIONS=-Xmx384m -XX:ActiveProcessorCount=1 -XX:+ExitOnOutOfMemoryError` for the existing trial |
| frontend | `VITE_API_BASE_URL=https://BACKEND-DOMAIN/api/v1`, `PORT=8080` |

You can use Railway reference variables such as `${{database.POSTGRES_PASSWORD}}` in its dashboard instead of copying the same secret between services. Use actual service names. RSA keys must retain real newlines; literal `\n` is not a PEM newline. Generate a fresh pair with OpenSSL `genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out private.pem` then `pkey -in private.pem -pubout -out public.pem` in a protected directory, or use your approved secret-management process. Never rotate a working keypair during an ordinary restart. Full scope explanations: [variables](variables.md).

## 4. Prepare clean source archives

Set absolute checkout paths and full reviewed SHAs. The concrete SHAs below identify the published 2026-10-11 homologation snapshot, including the institutional footer; they are not a production approval or a moving latest release. For a later release replace them with its approved full SHAs. A marker such as `REPLACE_WITH_FULL_REVIEWED_BACKEND_SHA` is explanatory text, never a Git commit. Use a **new** staging directory for each release so old files cannot leak into the upload.

```powershell
$BackendRepo = 'F:\Users\EHD\Github\CODEJR\irr-backend-identity'
$FrontendRepo = 'F:\Users\EHD\Github\CODEJR\irr-frontend'
$BackendCommit = '5779be077bce4d6e586fd8ddab555a7d58b45590'
$FrontendCommit = 'a4dbac251d03a00afeec0299c9ca8733f1e3ed8b'
foreach ($repo in @($BackendRepo,$FrontendRepo)) {
  git -C $repo fetch origin
  if ($LASTEXITCODE -ne 0) { throw 'Fetch failed; verify repository access' }
}
foreach ($selection in @(@{Repo=$BackendRepo;Commit=$BackendCommit},@{Repo=$FrontendRepo;Commit=$FrontendCommit})) {
  if ($selection.Commit -notmatch '^[a-fA-F0-9]{40}$') { throw 'Use a real full 40-character commit SHA, not a placeholder' }
  git -C $selection.Repo cat-file -e ($selection.Commit + '^{commit}')
  if ($LASTEXITCODE -ne 0) { throw 'Selected commit is unavailable; verify the reviewed SHA and fetch' }
}
if ($Backend -notmatch '^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$') { throw 'Set the backend service UUID from step 1 first' }
$Stage = Join-Path $BackendRepo ('.local/railway-release-' + [guid]::NewGuid().ToString('N'))
New-Item -ItemType Directory -Path $Stage | Out-Null
foreach ($name in @('backend','frontend','database','redis')) {
  New-Item -ItemType Directory -Path (Join-Path $Stage $name) | Out-Null
}
git -C $BackendRepo archive --format=tar -o "$Stage/backend.tar" $BackendCommit
if ($LASTEXITCODE -ne 0) { throw 'Backend archive failed' }
git -C $FrontendRepo archive --format=tar -o "$Stage/frontend.tar" $FrontendCommit
if ($LASTEXITCODE -ne 0) { throw 'Frontend archive failed' }
tar -xf "$Stage/backend.tar" -C "$Stage/backend"
if ($LASTEXITCODE -ne 0) { throw 'Backend extraction failed' }
tar -xf "$Stage/frontend.tar" -C "$Stage/frontend"
if ($LASTEXITCODE -ne 0) { throw 'Frontend extraction failed' }
Copy-Item "$Stage/backend/deploy/postgres/Dockerfile" "$Stage/database/Dockerfile"
Copy-Item "$Stage/backend/deploy/redis/Dockerfile.railway" "$Stage/redis/Dockerfile"
Copy-Item "$Stage/backend/deploy/redis/railway-start.sh" "$Stage/redis/railway-start.sh"
node "$BackendRepo/scripts/prepare-railway-dockerfile.mjs" $Backend "$Stage/backend/Dockerfile"
if ($LASTEXITCODE -ne 0) { throw 'Backend cache namespace preparation failed' }
Write-Output ('Prepared release directory: ' + $Stage)
```

Run the retained helper from the backend checkout: it permits output only under that checkout's ignored `.local`. It changes only the staged Dockerfile's literal Maven cache service ID. Do not replace the owner's Dockerfile or remove its Maven/Spotless changes. Inspect the staged files: each infrastructure directory needs only the files copied above. Do not upload `$Stage` itself or use `--no-gitignore`; upload each explicit subdirectory with `--path-as-root`.

If `git archive` says `not a valid object name`, stop before upload, replace the marker with the selected real SHA, run the fetch/validation above and rerun the **whole preparation block** with a new Stage. An empty directory created by the failed attempt is not a release. No checkout reset, volume deletion or secret replacement is necessary.

## 5. Deploy or resume, in dependency order

For a full resume, start with PostgreSQL, then Redis. For an ordinary application-only update with healthy dependencies, leave those services running and upload only the changed application(s).

**Use the same PowerShell window as steps 1 and 4.** Variables such as Project, Environment and Stage exist only in that terminal session; reopening PowerShell clears them. If continuing in a new window, rerun the destination-ID block in step 1 and either rerun the complete preparation in step 4 or restore Stage to its exact printed directory. Before any upload, run:

```powershell
foreach ($value in @($Project,$Environment,$Database,$Redis,$Backend,$Frontend)) {
  if ($value -notmatch '^[a-f0-9]{8}(?:-[a-f0-9]{4}){3}-[a-f0-9]{12}$') { throw 'Destination variables are missing; rerun step 1 in this window' }
}
if ([string]::IsNullOrWhiteSpace($Stage)) { throw 'Stage is missing; rerun step 4 in this window' }
foreach ($file in @('database/Dockerfile','redis/Dockerfile','redis/railway-start.sh','backend/Dockerfile','frontend/Dockerfile')) {
  if (-not (Test-Path -LiteralPath (Join-Path $Stage $file) -PathType Leaf)) { throw 'Release preparation is incomplete; rerun step 4' }
}
```

The CLI error `a value is required for --project` means the Project variable was empty; it does not mean the project or its data must be recreated.

```powershell
railway up "$Stage/database" --path-as-root --project $Project --environment $Environment --service $Database --detach
if ($LASTEXITCODE -ne 0) { throw 'Database upload failed' }
railway deployment list --project $Project --environment $Environment --service $Database --limit 1
```

**Wait before continuing:** use Deployments and Logs in the dashboard until the target deployment is SUCCESS and PostgreSQL reports ready for connections. An upload exit code only confirms submission. If failed, inspect logs and use step 8; do not recreate storage.

```powershell
railway up "$Stage/redis" --path-as-root --project $Project --environment $Environment --service $Redis --detach
if ($LASTEXITCODE -ne 0) { throw 'Redis upload failed' }
railway deployment list --project $Project --environment $Environment --service $Redis --limit 1
```

Wait for SUCCESS and Redis ready. The Railway-specific image prepares mount-root permissions without recursively touching lost+found and uses authenticated AOF/noeviction. Do not substitute the plain VPS Redis Dockerfile here.

```powershell
railway up "$Stage/backend" --path-as-root --project $Project --environment $Environment --service $Backend --detach
if ($LASTEXITCODE -ne 0) { throw 'Backend upload failed' }
railway deployment list --project $Project --environment $Environment --service $Backend --limit 1
```

Wait for SUCCESS and `https://BACKEND-DOMAIN/actuator/health/readiness` returning HTTP 200/UP. Logs must show normal Flyway validation/application, not repair/reset. Java must run as UID 10001 after the entrypoint's mount initialization. Then upload the frontend:

```powershell
railway up "$Stage/frontend" --path-as-root --project $Project --environment $Environment --service $Frontend --detach
if ($LASTEXITCODE -ne 0) { throw 'Frontend upload failed' }
railway deployment list --project $Project --environment $Environment --service $Frontend --limit 1
```

Wait for SUCCESS and `https://FRONTEND-DOMAIN/health` returning HTTP 200. Open `/login` and a direct nested route. The frontend public API URL is compiled during this build; changing its variable requires a new build/upload. Record all deployment IDs and source SHAs. Execute [common acceptance](../operator-guide.md#4-common-acceptance-checklist).

## 6. First administrator and email

For an **empty new database only**, temporarily enable `BOOTSTRAP_ADMIN_ENABLED=true` with explicit name, email and a unique password in backend Variables, then deploy. Sign in, complete the required password change and sign in again. Set bootstrap false, remove name/email/password inputs and redeploy. Check ordinary login still works. The existing paused environment already has accounts: do not bootstrap it again. Provision an organization and scoped operator through the application; share operator access for routine client evaluation. Store the owner administrator credential privately.

Follow [the Brevo procedure](../../email-recovery.md) before promising recovery email. Disabled mail returns 503; `.invalid` test logins cannot receive email. Changing an existing password is separate from email recovery and works without the provider.

## 7. Pause, update, rollback and backups

Pause in this order to finish application work before dependencies disappear:

```powershell
foreach ($Service in @($Frontend,$Backend,$Redis,$Database)) {
  railway down --project $Project --environment $Environment --service $Service --yes
  if ($LASTEXITCODE -ne 0) { throw 'Stop failed; inspect the selected service' }
  railway deployment list --project $Project --environment $Environment --service $Service --limit 1
}
```

Expected latest status: REMOVED (wait if still removing). This retains volumes/service settings; it is not a backup. Storage and other projects can still consume shared credit. To resume use steps 4–5; do not rely on Restart for a removed deployment.

For an update, select the new reviewed SHA, prepare a new archive and upload the changed service. Record the old deployment ID first and take a coordinated backup for valuable data. For a code rollback, re-upload the previously approved compatible commit using the same variables and volumes; the frontend must match the backend contract. Do not roll back to credential-generation-unaware code, downgrade PostgreSQL, or undo migrations by deleting history. A failed migration needs investigation, not automatic rollback of data.

For synthetic Railway recovery, open each volume's Backups tab and inspect plan support, retention and restore controls in [Railway's backup guide](https://docs.railway.com/volumes/backups). Stop backend writes before taking coordinated database/attachment checkpoints, label the pair and restart only after both complete. Provider snapshots are not an independent off-provider backup or an automatically atomic multi-volume snapshot. Rehearse restoration into an isolated destination before retaining valuable data. The Docker-host backup script does **not** operate on Railway remotely; it must not be pointed at your local development stack as a substitute. A portable off-provider Railway export/restore remains a separate operational gate, not a verified feature of this guide.

## 8. Troubleshooting

| Symptom | Action |
| --- | --- |
| CLI Unauthorized | Run login, finish device approval, verify whoami; confirm project membership |
| Maven cache ID rejected | Re-run the staged Dockerfile helper with this backend service UUID |
| Java started but deploy health fails | Set both PORT and SERVER_PORT to 9191, check readiness dependencies and target port |
| Redis AOF permission error | Use the Railway Dockerfile and its startup script, preserve /data mount and entrypoint |
| Database authentication error | Match configured password to the initialized role; changing only an env value does not rotate it |
| Frontend calls localhost or CORS fails | Correct public build URL with /api/v1 and exact frontend CORS origin, then rebuild |
| Attachment permission error | Check /data/uploads volume and opt-in initializer; do not run Java as root |
| Application offline after pause | Expected; follow full resume steps, not database recreation |
| Exit 137/out of memory | Inspect service metrics/logs; review actual resource limits before increasing spend |
| Flyway checksum mismatch | Stop; compare deployed source and existing history, preserve database and logs |

Additional diagnosis: [troubleshooting](troubleshooting.md). For support, provide deployment ID, time, status and redacted error text, never secret variable exports.
