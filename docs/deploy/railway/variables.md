# Railway variables

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Never copy resolved secrets into source, issues, logs or this runbook. Reference the actual Railway service names configured in your project.

| Variable | Source or example | Scope |
| --- | --- | --- |
| DB_HOST / DB_PORT / DB_NAME / DB_USER / DB_PASSWORD | Postgres PGHOST / PGPORT / PGDATABASE / PGUSER / PGPASSWORD reference variables | Backend runtime; password secret |
| REDIS_HOST / REDIS_PORT / REDIS_PASSWORD | Redis reference variables | Backend runtime; password secret |
| RSA_PUBLIC_KEY / RSA_PRIVATE_KEY | Matching inline PEM strings, with real line breaks, or mounted file: resource locations | Backend runtime; private key secret |
| CORS_ALLOWED_ORIGINS | https://your-frontend-public-domain | Backend runtime; comma-separated exact origins |
| STORAGE_DIRECTORY | /data/uploads with persistent volume at that path | Backend runtime |
| API_DOCS_ENABLED | true for official pre-production Swagger; review exposure per environment | Backend runtime |
| SERVER_PORT | 9191 (existing default and health-check target) | Backend runtime |
| VITE_API_BASE_URL | https://your-backend-public-domain/api/v1 | Frontend build-time, public |
| BOOTSTRAP_ADMIN_ENABLED | false by default; true only for first-environment provisioning | Optional backend runtime |
| BOOTSTRAP_ADMIN_NAME / BOOTSTRAP_ADMIN_EMAIL / BOOTSTRAP_ADMIN_PASSWORD | Explicit operator-supplied first-account inputs | Optional bootstrap; password secret |

Do not confuse PGPORT with PostgreSQL's password variable: a missing DB_PORT otherwise falls back to the local 5435 default. Railway internal networking should use the service reference value, normally 5432.

The current application reads SERVER_PORT, not Railway PORT. Keep the existing backend target port 9191 and Nginx target 8080. Changing listeners also requires matching health checks/networking; this delivery makes no such change.

CORS accepts origins only, without /api/v1 or trailing paths. No wildcard workaround. Browser bearer authentication does not require credentialed cookies. Rebuild the frontend after a VITE_API_BASE_URL change: runtime environment edits cannot rewrite static JavaScript. Never use localhost or *.railway.internal in the deployed browser API URL.

DEV_BOOTSTRAP_ENABLED, DEV_ADMIN_* and SPRING_PROFILES_ACTIVE=development are local development settings. They are not required for Railway. The separate BOOTSTRAP_ADMIN_* bootstrap is described in [the deployment guide](README.md).

Recovery requires the mail configuration listed below. Optional first-administrator inputs apply only to a newly provisioned environment. Key rotation, CORS/domain/network changes, storage volumes, backup restoration and database recreation remain explicit infrastructure actions outside a normal Git-only release.


## Recovery variables

| Variable | Required value / default | Placement |
| --- | --- | --- |
| MAIL_PROVIDER | brevo for homologation; application default disabled | Backend runtime |
| MAIL_API_KEY | Dedicated Brevo API key | Backend secret; never frontend |
| MAIL_FROM / MAIL_FROM_NAME | Verified sender address / IRR display name | Backend runtime |
| PASSWORD_RESET_BASE_URL | Public frontend HTTPS URL ending /reset-password | Backend runtime; never internal hostname |
| PASSWORD_RESET_TTL_MINUTES | 30 default; permitted 5–60 | Backend runtime |
| MAILPIT_URL / MAILPIT_PORT | Local catcher only | Development; not needed for Brevo |

Sender account/domain approval and DNS authentication are operator prerequisites. No Railway secrets were modified locally. See [email setup](../../email-recovery.md).
