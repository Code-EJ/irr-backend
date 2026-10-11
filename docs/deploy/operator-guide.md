# Deployment operator guide

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Reviewed 2026-10-11.

This is the starting point for a human operator. No AI session, private chat history or generated one-off script is required. Keep this guide with the release artifacts. Technical documentation is English; application labels are Brazilian Portuguese.

## 1. Choose a procedure

| Goal | Follow |
| --- | --- |
| Resume the existing paused synthetic Railway environment | [Railway manual, steps 1 and 4–8](railway/operator-guide.md) |
| Install a fresh Railway environment | [Railway manual, all steps](railway/operator-guide.md) |
| Install on Hostinger or another Linux VPS | [VPS manual](vps-operator-guide.md), plus [Hostinger procurement](hostinger/README.md) |
| Update or roll back an existing deployment | The update section of the selected provider manual |
| Back up or recover a Docker host | [Backup and recovery](backup-and-recovery.md) |
| Configure real recovery email | [Email procedure](../email-recovery.md) |

Railway homologation is synthetic-only in Virginia. Production requires an approved Brazilian location, independent secrets and approved client data. As of this review the Railway deployments are stopped, while their volumes remain. Writing this guide does not start any workload. Hostinger is not installed. See [execution evidence](railway/execution-2026-10-11.md) for what was actually tested.

For another provider, a Linux VPS can follow the same VPS commands unchanged except for account, firewall and DNS screens. A managed container platform must provide private PostgreSQL 16 and authenticated persistent Redis 8.2, writable persistent attachment storage, supported image entrypoints/UIDs, HTTPS routing to ports 9191/8080, secrets, healthchecks and backups. Map those capabilities explicitly before deploying; an ephemeral container or static-site-only plan cannot host the complete application. Do not silently substitute ephemeral disk or expose internal services to make an incompatible platform work.

## 2. Prepare an operator handoff

Before running a deploy, record these non-secret values in the release ticket:

- Environment, owner, maintenance window, provider/account/project, region and budget.
- Backend and frontend **full commit SHAs**, reviewed PRs and their CI results.
- Public frontend and backend HTTPS names; the browser API URL ends in `/api/v1`.
- For a VPS: immutable image references for all five services and their CPU architecture.
- Current/previous image or deployment IDs, migration history, backup ID and restore evidence.
- Mail sender approval, monitoring recipient, backup schedule/retention, recovery objectives and client acceptance status.

Store credentials in an owner-controlled password manager or protected secret store. The existing homologation credentials are in the backend's ignored `.local/railway-homologation/.env-homologation` on the owner's machine; they are not available from a fresh Git clone. Recover them from the owner/secret store rather than inventing a default login. Never copy development `.env` into hosting, expose secrets through `VITE_*`, or publish resolved Compose configuration. Keep administrator credentials separate from the organization's test-operator account.

## 3. Obtain reviewed source

Install Git, Node.js 24/npm and Docker Engine/Desktop with Compose v2 on the build workstation. Java 21 is needed for local Maven checks; cloud/container builds already include their build JDK. Use a supported Linux x86-64 build host or Docker Linux containers. Verify `git --version`, `node --version`, `docker version` and `docker compose version` before continuing.

On a **new build workstation**, clone both repositories using your own GitHub access:

```sh
git clone https://github.com/Code-EJ/irr-backend.git
git clone https://github.com/Code-EJ/irr-frontend.git
```

In each repository run `git fetch origin`, inspect `git status --short`, and select the reviewed commit in a separate build checkout or archive. Do not reset a working directory containing someone else's changes. The manuals use `git archive` so ignored credentials, local data and uncommitted files never enter the release. A commit on a feature branch can be tested in homologation; production requires the agreed review/release approval. Do not assume `main` already contains an open PR.

## 4. Common acceptance checklist

Run this **from the frontend repository on the workstation**, after installing its locked dependencies with `npm ci`. Replace every example and record the exact deployed frontend source SHA. PowerShell:

```powershell
$env:IRR_FRONTEND_URL = 'https://FRONTEND-DOMAIN'
$env:IRR_API_URL = 'https://BACKEND-DOMAIN/api/v1'
$env:IRR_FRONTEND_COMMIT = 'REPLACE_WITH_FULL_FRONTEND_COMMIT_SHA'
Remove-Item Env:IRR_VERIFICATION_MODE -ErrorAction SilentlyContinue
npm run api:check
if ($LASTEXITCODE -ne 0) { throw 'Pinned contract verification failed' }
npm run verify:homologation
if ($LASTEXITCODE -ne 0) { throw 'Public pair verification failed' }
```

Bash equivalent:

```sh
export IRR_FRONTEND_URL=https://FRONTEND-DOMAIN
export IRR_API_URL=https://BACKEND-DOMAIN/api/v1
export IRR_FRONTEND_COMMIT=REPLACE_WITH_FULL_FRONTEND_COMMIT_SHA
unset IRR_VERIFICATION_MODE
npm run api:check && npm run verify:homologation
```

Expected: successful pinned contract and six public-pair checks. The verifier needs generated OpenAPI; temporarily enable `API_DOCS_ENABLED=true` during controlled verification if the deployed policy disables it. Restore the approved setting afterwards. A passing command does not exercise every business workflow.

In a browser, verify login and mandatory password change, organization selection, operator versus administrator access, a direct nested URL, the footer credits, theme/contrast/font controls, a genuine small PDF upload/download, and persistence after an application restart. Run the frontend `docs/final-operator-review.md` worksheet and relevant business workflows. Real email recovery requires a controlled deliverable inbox; `.invalid` synthetic addresses cannot receive mail. See [client instructions](railway/client-homologation.md). Record failures and expected exclusions before inviting the client.

## 5. Stop conditions and release record

Stop on a failed migration, unmatched keys, missing storage, wrong environment selection, failed healthcheck, unreviewed security finding, or mismatched frontend API origin. Use the provider troubleshooting table. Do not delete volumes, run Flyway clean/repair, rewrite released migrations or bypass authentication to make a deployment pass.

Keep a dated record containing the values from step 2, command outcomes, tested URLs, role/PDF/email outcomes, backup/restore IDs and unresolved limitations. Never include passwords, bearer tokens or reset links. Security review, recovery proof and client approval remain separate from a green build. The scripts and Docker syntax were checked locally; this documentation update did not provision a VPS or repeat a live Railway deployment.

Documentation verification on 2026-10-11: 9 PowerShell and 19 Bash blocks parsed without executing deployment commands; CLI upload/status options and Compose create options inspected locally; hosted Compose boundary verifier passed with synthetic configuration; documentation link/Java inventory verifier passed (285 entries). The existing Railway database PGDATA was read back as `/var/lib/postgresql/data/pgdata` without printing secrets. These checks validate syntax/configuration alignment, not a live full-host disaster-recovery rehearsal.
