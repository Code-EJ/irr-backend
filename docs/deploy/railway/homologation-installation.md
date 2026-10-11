# Fresh Railway homologation installation

Current execution authority is [ADR-0028](../../adrs/0028-isolated-trial-homologation.md). [The dated provisioning record](execution-2026-10-11.md) identifies which steps have actually run and which remain open. Earlier preparation-only statements below are historical; reserved domains and configured secrets do not establish a successful installation.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Authority: [ADR-0027](../../adrs/0027-provider-independent-deployment.md), retaining ADR-0026 storage initialization. Owner clarification permits Railway Virginia only for synthetic homologation. Production is planned separately on Hostinger in Brazil. This is a concrete installation checklist; steps below have not been executed remotely. Refer to [release gates](../../release-readiness.md) before creating billable resources.

## 1. Account, costs and region

Use US East Virginia (`us-east4-eqdc4a`) for all homologation services, provider domains and **synthetic data only**, as explicitly approved by the owner. Brazil/São Paulo remains mandatory for production. The current [compute region list](https://docs.railway.com/deployments/regions) has no Brazil option; [Railway staff confirms São Paulo is CDN only](https://station.railway.com/feedback/request-for-brazil-hosting-region-availa-64766196). No real client data, passwords, attachments or production backup may enter this foreign homologation environment.

Use an owner-controlled workspace, project `irr` and environment `homologation` after access and reviewed spending approval. Keep volumes, keys and secrets independent from local development and future production. A successful test environment does not authorize production procurement or publication.

Evaluate any provider against the total BRL 130/month ceiling, including persistent storage, backups, payment fees and currency variation. A subscription credit is not an all-inclusive four-service hosting price. No USD conversion or foreign spending limit has been approved. For any later compatible Railway plan, [a hard limit suspends workloads](https://docs.railway.com/pricing/cost-control); communicate the availability tradeoff. Do not enable Railway Agent consumption unnecessarily. Review actual usage after 24 hours and one week rather than promise a fixed monthly bill.

Keep one replica per service. Start with backend 1 GiB maximum RAM / 1 vCPU, frontend 256 MiB / 0.5 vCPU, PostgreSQL 512 MiB / 0.5 vCPU and Redis 256 MiB / 0.5 vCPU as **proposed limits requiring measurement**, not tested guarantees or reserved/billed consumption. On the backend, consider `JAVA_TOOL_OPTIONS=-XX:MaxRAMPercentage=60.0 -XX:ActiveProcessorCount=2`, then verify startup and representative requests inside those limits. Do not shrink limits after a single idle measurement. Keep services awake during acceptance; sleeping and scaling are separate changes.

## 2. Persistent private infrastructure

Provision PostgreSQL 16 and authenticated Redis 8.2 using reviewed compatible images. Preserve their exact final digests and security dispositions. Do not accept a template's accidental major upgrade. Attach PostgreSQL storage at its image's documented data directory. Use private networking, no public TCP proxy and unique generated passwords. Configure Redis persistence and a no-eviction policy consistent with [existing operations](../../operations.md); retain it across restarts. Verify reference variables against the actual service names instead of assuming template field names.

Do not deploy Mailpit or copy development data. Destination initial-data scope remains an owner decision; until resolved, no import is authorized. Use real V1–V4 startup migrations and no reset/repair/SQL seeding.

## 3. Backend

Select the reviewed backend source commit/image, existing Dockerfile and disable automatic deploys. Keep target port 9191 and readiness healthcheck `/actuator/health/readiness`. Set restart-on-failure with a bounded retry count, then configure independent uptime/disk alerts; deployment healthchecks are not continuous monitoring.

Set DB/Redis references and environment-specific RSA keys using [the variable table](variables.md). No keys/passwords go into Git, chat, frontend variables or build arguments. Set `DEV_BOOTSTRAP_ENABLED=false` and do not activate the development profile. Mount the attachment volume at `/data/uploads`; set `STORAGE_DIRECTORY=/data/uploads`.

For a root-owned fresh Railway volume, explicitly set `RAILWAY_RUN_UID=0` and `IRR_INIT_STORAGE_PERMISSIONS=true`. Keep the Dockerfile entrypoint: **do not override it with a direct Java start command**. The entrypoint prepares only the canonical directory and uses `setpriv` to start Java as UID/GID 10001 with no capability bounds and no-new-privileges. Verify actual process identity and successful attachment create/download/restart on Railway. If capability restrictions prevent initialization, stop and provision ownership through a reviewed platform operation; do not run Java as root or recursively change original files.

Local reproduction:

```powershell
docker build -t irr-backend-storage:candidate .
node scripts/verify-container-storage.mjs irr-backend-storage:candidate
```

The script creates/removes only a uniquely named, purpose-labeled disposable volume. It never mounts the development database or attachment volume.

Generate the backend Railway HTTPS domain. Enable Swagger for homologation (`API_DOCS_ENABLED=true`) as the executable reference. Recheck `/api/v1` only, liveness/readiness and migration success. Provision the first administrator only after empty-environment scope approval; use a unique temporary password and enforce its mandatory change. Disable bootstrap and remove the temporary password after provisioning.

## 4. Frontend and mail

Generate the frontend Railway HTTPS domain and target port 8080, healthcheck `/health`. Build the existing frontend Dockerfile with `VITE_API_BASE_URL=https://BACKEND-DOMAIN/api/v1`; this is a public build argument, not a secret. Update backend `CORS_ALLOWED_ORIGINS=https://FRONTEND-DOMAIN` and `PASSWORD_RESET_BASE_URL=https://FRONTEND-DOMAIN/reset-password`. Rebuild static frontend when the backend origin changes. Preserve its UID 101 and SPA fallback.

Configure Brevo only with an approved sender and backend-only key, then test the approved real inbox. Keeping `MAIL_PROVIDER=disabled` permits an explicitly incomplete temporary environment but **does not pass recovery or delivery acceptance**. No Mailpit substitute counts as public delivery evidence.

## 5. Evidence and promotion gate

Record source SHAs, immutable image digests, OpenAPI/migration hashes and security review in a new release record; do not reuse the historical local record as remote evidence. In the frontend repository, run:

```powershell
$env:IRR_FRONTEND_URL = 'https://FRONTEND-DOMAIN'
$env:IRR_API_URL = 'https://BACKEND-DOMAIN/api/v1'
$env:IRR_FRONTEND_COMMIT = 'EXACT_40_CHARACTER_IMAGE_SOURCE_SHA'
Remove-Item Env:IRR_VERIFICATION_MODE -ErrorAction SilentlyContinue
npm run verify:homologation
npm run api:check
```

These checks are read-only; they do not establish operator/accessibility/mail acceptance. Execute the frontend final operator worksheet, valid PDF viewing, real password recovery and retained upload restart. Configure agreed off-machine coordinated database/attachment backups, verify alerts and perform an isolated remote restore before claiming delivery readiness. Never expose development `.env` credentials. Freeze the contract only after the matched deployed pair and remaining gates pass.

Pending owner inputs: Brazil-capable provider choice/cloud access, initial-data/admin scope, approved Brevo sender/inbox, RPO/RTO/retention and operational owner. Budget is BRL 130/month; provider-specific cost compatibility is not verified. Pending technical evidence: remaining image/vendor risks, complete operator/accessibility acceptance and real cloud behavior. Production remains a separate later decision.
