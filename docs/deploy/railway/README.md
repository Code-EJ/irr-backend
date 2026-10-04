# Railway deployment

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Git, Dockerfiles, Flyway and environment configuration are the source of truth. Railway runs the resulting artifacts. No source patches, JAR uploads, copied migrations or post-deployment SQL are part of normal installation.

This runbook incorporates the existing owner-authored deployment notes into the identity feature branch. The original staged and unstaged notes were preserved in their original checkout. Local verification does not establish the current Railway service state.

## Service topology

- Backend: existing Java 21 Dockerfile, non-root process, internal port 9191.
- PostgreSQL: persistent service reached through DB_* reference variables.
- Redis: private service reached through REDIS_* reference variables; not browser-accessible.
- Attachments: mount persistent storage at STORAGE_DIRECTORY and preserve it across releases.
- Frontend: existing Vite build and unprivileged Nginx Dockerfile, internal port 8080, /health and SPA fallback.
- Browser API traffic uses the backend's public HTTPS origin followed by /api/v1. Private Railway hostnames are for server-to-server traffic only.

Keep the intentional Maven/Spotless and Docker cache changes already present on main. The backend Dockerfile cache mount identifier is build infrastructure, not an API configuration setting. Do not replace it incidentally.

## Database strategy — OPTION A: INCREMENTAL

[ADR-0016](../../adrs/0016-account-and-credential-lifecycle.md) deliberately keeps ADR-0014's V1 byte-for-byte unchanged and adds V2__account_credential_lifecycle.sql. Existing V1 databases upgrade during startup; empty databases apply V1, V2 and V3. ADR-0018 adds the recovery table after the credential upgrade. No Railway recreation, Flyway clean/repair or manual SQL is required.

The operational baseline is V1 + V2 + V3 (password recovery). Once operational data must be retained, every change requires a new migration. Never rewrite V1, erase flyway_schema_history or reset PostgreSQL as normal maintenance. The owner still considers pre-production data disposable, but a future consolidation needs its own explicit reset decision and operator authorization. This delivery does not authorize or perform that reset.

## First administrator

For an empty environment only, supply BOOTSTRAP_ADMIN_ENABLED=true and explicit BOOTSTRAP_ADMIN_NAME, BOOTSTRAP_ADMIN_EMAIL, BOOTSTRAP_ADMIN_PASSWORD. Use a secret generated for that environment, at least eight characters and fewer than 72 UTF-8 bytes. Do not enable the development profile.

Startup takes a PostgreSQL transaction advisory lock and creates one administrator only when no administrator, including inactive history, exists. An existing email belonging to another account is rejected; it is never promoted. Existing administrators are never reset or recreated. The new administrator must change the temporary password before using business or administrative endpoints. No organization membership is inferred.

Deploy the identity-capable frontend before using this temporary account. After the first successful change, disable BOOTSTRAP_ADMIN_ENABLED and remove the initial password from service configuration. Removing optional bootstrap inputs is an operator action, not a normal per-release step. Existing environments with administrators need none of these new variables.

## Runbooks

- [Variables and secrets](variables.md)
- [Rollout and rollback](rollout.md)
- [Troubleshooting](troubleshooting.md)
- [Deployment checklist](checklist.md)
- [Development operations](../../operations.md)

Identity completion: **Railway configuration changes required: mail provider, sender, API key, public recovery URL and TTL**. Existing database/Redis/storage services remain; V3 applies automatically without reset. Follow [email recovery operations](../../email-recovery.md). New environments still need their normal infrastructure and optional first-administrator provisioning. No remote settings, secrets, deployments or merges were changed by the local implementation.

## Identity closure and external delivery

Identity is IMPLEMENTED / READY FOR HOMOLOGATION. Email provider deployment is OPERATIONAL CONFIGURATION PENDING / EXTERNAL CONFIGURATION REQUIRED: approved Brevo account, sender/domain DNS verification, backend-only API key and public reset URL. Follow [the implemented email runbook](../../email-recovery.md) for exact variables, local no-internet tests, real-inbox smoke and troubleshooting. This operational configuration does not block owner-authorized Routes development. No external credentials or Railway changes were performed locally.
