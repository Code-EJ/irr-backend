# Railway installation preparation

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Authority: [ADR-0025](../../adrs/0025-pre-release-security-and-deployment-preparation.md). Planning and local artifacts are authorized; creating resources, configuring real secrets, publishing and deploying are not. No remote action has occurred. Existing runbooks remain the detailed variable/rollback reference.

## Reviewable topology and configuration

Create separate homologation/production environments only after approval. Backend Java 21 and frontend static Nginx use existing Dockerfiles; PostgreSQL 16 and Redis 8.2 remain separate private services. Public HTTPS exposes only frontend and `/api/v1` backend. Browser traffic needs a public API origin, never private DNS. Use exact CORS origin, ports 9191/8080 and their matching health targets. Redis/database connections use environment-local service references, not exposed TCP proxies. See [private networking](https://docs.railway.com/networking/private-networking).

Mount backend attachments at `/data/uploads` and persist PostgreSQL data. Keep a single backend replica until the filesystem storage assumption changes through an explicit ADR. Test a fresh and retained upload volume as UID 10001: successful creation/download/restart is a release gate. Railway mounts volumes at runtime, not during build/pre-deploy; image-layer ownership does not prove mounted-volume permissions. Do not follow a root-process workaround without a separately reviewed secure provisioning/privilege-drop solution. See [volume behavior](https://docs.railway.com/volumes). Attached volumes can cause deployment downtime; do not promise zero downtime.

The backend reads `SERVER_PORT`, not `PORT`; set networking targets to existing listeners. Health target is `/actuator/health/readiness`; frontend `/health`. Deployment healthchecks gate activation and are not continuous monitoring. Configure restart policy, resource limits, off-host uptime alerts, disk/database/Redis/log monitoring and an incident owner before client use. See [healthcheck limitations](https://docs.railway.com/deployments/healthchecks).

Current Railway documentation deprecates config-as-code and disallows opting new services into it; legacy files cease on 2026-12-01. Therefore no new `railway.toml` is introduced. `deployment-plan.json` is a provider-neutral review document, **not executable Railway IaC**. After the owner supplies project/environment/service identifiers, review the current supported IaC API before implementing an apply-capable artifact. See [current config-as-code policy](https://docs.railway.com/config-as-code). This avoids unreviewed infrastructure libraries and accidental provisioning.

## Secrets, domains and mail

Generate environment-specific DB/Redis credentials and a matching RSA keypair. Store values in Railway secret configuration only; frontend receives only public `VITE_API_BASE_URL` at build time. Never copy `.env-dev`, Mailpit configuration or synthetic credentials into the release. Disable development profile/bootstrap. Provision the first administrator only through reviewed `BOOTSTRAP_ADMIN_*`; require password change, then remove its temporary secret/disable bootstrap. No client passwords enter manifests or logs.

The owner must choose frontend/backend domains, region, account ownership and budget. Configure DNS/HTTPS, exact CORS and recovery HTTPS URL; rebuild frontend when the origin changes. Configure the existing Brevo HTTPS adapter with approved sender/domain authentication and a backend-only API key. Test a real approved inbox, recovery expiration/single-use, failure behavior and token revocation. Local Mailpit success is not evidence of external delivery.

Review Swagger exposure, token/localStorage XSS risk, headers and log redaction before publication. Do not invent a new authentication architecture in this phase. Restrict account/cloud access and record key rotation/recovery procedures. Database migration/runtime privileges currently share the configured datasource; evaluate environment least-privilege roles without breaking Flyway startup or pretending separate credentials already exist.

## Data, backup and release pair

Preserve successful V1–V4 checksums; startup applies existing migrations and validates schema. No reset, clean, repair or direct SQL seeding. Complete [initial-data approval](../../data/initial-load.md) before import. Freeze source SHAs, final image digests, scan database/tool version, OpenAPI hash and migration checksums in the release manifest. Do not promote a different rebuild under the same tag.

Existing [local recovery evidence](../../recovery-verification.md) remains valid for its dated isolated rehearsal; remote restoration needs its own verification. Plan encrypted off-machine backups of PostgreSQL **and attachments at a consistent boundary**, separate key custody, retention, scheduling, monitoring and an isolated restore drill. Railway/database snapshots alone do not capture attachment consistency. RPO/RTO, retention and acceptable maintenance downtime require owner/client approval; no numeric promise is established by local timing.

Release sequence after approval: preserve backup → deploy backend → validate V1–V4/readiness/contract → deploy matched frontend → validate HTTPS/CORS/static routes → test administrator, member and manager journeys → verify email/attachments/analytics → client/operator acceptance. Disable automatic deployment until the reviewed release gate exists. Application rollback uses the previous compatible image pair; it does not undo migrations or data. Preserve original records and use a forward fix when compatibility requires it. Restoring data is a separate authorized incident action.

## Blocking inputs

Cloud account/project/service access; chosen domains/region/budget; public homologation URLs; secret provisioning; verified non-root storage; external email sender/inbox; approved backup/RPO/RTO policy; accepted security dispositions; unblocked browser/human homologation; client dataset/import scope. No production resources or secrets are needed to continue local reviews.
