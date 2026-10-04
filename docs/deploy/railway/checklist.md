# Railway identity deployment checklist

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Unchecked items are operator release gates, not claims of remote verification.

## Before merge

- [ ] Feature branches reviewed; unrelated local changes preserved; no automatic merge.
- [ ] Java 21 ./mvnw clean verify, real PostgreSQL/Testcontainers and documentation checks pass.
- [ ] Backend advisory audit recorded and Docker image builds.
- [ ] V1 checksum unchanged; additive V2/V3 upgrade and fresh V1 + V2 + V3 verified.
- [ ] npm ci, lint, typecheck, test, build, api:check and real-backend contract tests pass.
- [ ] Frontend dependency findings reviewed; no unapproved forced major upgrade.
- [ ] Both images build from tracked source; no secrets or local-only source dependencies.
- [ ] Public HTTPS VITE_API_BASE_URL present at build time; no backend credentials in bundle.
- [ ] Railway configuration changes listed: BOOTSTRAP_ADMIN_* for first provisioning only, MAIL_* and PASSWORD_RESET_* for public recovery. Existing PostgreSQL/Redis infrastructure is retained; no additional Railway service is required for Brevo HTTPS.
- [ ] Database and upload storage persistence confirmed; no reset planned.
- [ ] Security-compatible rollback artifact or forward-fix procedure available.

## Backend first

- [ ] Merge backend reviewed PR, then verify Railway rebuilt that exact commit.
- [ ] Flyway versions 1, 2 and 3 successful; Hibernate schema validation succeeds.
- [ ] Health, liveness and readiness return 200/UP.
- [ ] RSA, Redis and PostgreSQL load without credential exposure.
- [ ] Existing frontend ordinary-account login, organization and business workflow work.
- [ ] No resets initiated before the new frontend is deployed.

## Frontend second

- [ ] Merge frontend reviewed PR after backend smoke succeeds.
- [ ] Nginx /health is ready; assets and direct nested routes load on port 8080.
- [ ] Browser uses public HTTPS /api/v1 and valid CORS; no startup exceptions.
- [ ] Profile edit and conflict feedback work; no self role escalation.
- [ ] Password change revokes old tokens; new credentials work.
- [ ] Administrator list/detail/create/edit/deactivate work; ordinary accounts are denied.
- [ ] Reset requires password change before business access.
- [ ] MEMBER and MANAGER grants work; revocation removes access.
- [ ] Deactivation rejects existing tokens and new login.

## New empty environments only

- [ ] Explicit BOOTSTRAP_ADMIN_* provided without development profile or default credentials.
- [ ] First administrator changes temporary password and signs in again.
- [ ] Bootstrap disabled and temporary bootstrap secret removed afterward.
- [ ] Restart does not recreate accounts, reset passwords or overwrite edited profile data.

## Data and completion

- [ ] Existing users, organizations, documents and Flyway history preserved.
- [ ] Operational V1 + V2 + V3 boundary recorded; future changes require new migrations.
- [ ] No Railway reset, manual SQL patch, source patch or secret disclosure occurred.
- [ ] Actual remote smoke results recorded separately from local evidence.


## Expanded Identity gate

- [ ] Global administrator lifecycle and last-administrator race protection verified.
- [ ] Global organization access and local MANAGER isolation verified.
- [ ] Organization CRUD and responsible-user onboarding verified, including partial-failure retry.
- [ ] V3 applied with V1/V2 unchanged; no database reset.
- [ ] Brevo account/sender/domain approved; mail settings and public recovery URL configured.
- [ ] Real inbox recovery, expiry/replay and old-password/session rejection verified.
- [ ] Current frontend/backend release manifests recorded before freezing v1.
