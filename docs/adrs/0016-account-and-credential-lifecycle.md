# ADR-0016: Account self-service and credential lifecycle

- Status: Accepted; implementation and verification in progress.
- Date: 2026-10-03.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Baselines: backend main `4c50f17`; frontend main `01a9543`.

## Context and audit

ADR-0003's pragmatic modular monolith, ADR-0008's controlled partner provisioning and ADR-0009's explicit organization memberships remain authoritative. ADR-0014 removed obsolete routes and established V1. Frontend ADR-0015 integrated identity reads, fleet, materials and intake. The owner confirms Railway is still pre-production and permits considering a deliberate disposable reset, without authorizing automatic destruction. We choose **OPTION A — INCREMENTAL**: these two durable credential fields require no domain restructuring, and an additive V2 avoids infrastructure recreation, checksum divergence and a coordinated deployment outage. This is a deliberate choice, not an assumption that all pre-production data is immutable. V1 remains byte-for-byte unchanged.

Current identity supports login, current-account reads, administrator partner CRUD and membership grant/revoke. Password changes/resets and self-service updates are absent. JWTs reload active identity/role but have no credential revocation generation. Organization reads are correctly membership-scoped, leaving administrative organization/membership discovery missing. The active frontend profile is read-only; unreachable legacy account pages still depend on incompatible services. Original backend Railway documentation edits are preserved in their existing worktree; this implementation uses an isolated checkout.

## Decision drivers

Preserve current `/api/v1` consumers during independent deployments; invalidate compromised credentials; avoid self privilege escalation; preserve persistent data; use existing transport, hashing, PostgreSQL and authorization boundaries; complete deployment without container intervention or new required infrastructure secrets.

## Decision

Add `PUT /api/v1/users/me`, `PUT /api/v1/users/me/password` and administrator-only `PUT /api/v1/users/{id}/password`. Profile input contains only fullName/email. Partner creation and administrative metadata updates cannot create/promote/edit administrator accounts. Reset targets partners; administrators change their own password through authenticated self-service. Exact email matching and soft deactivation remain unchanged. Unknown self-service fields cannot affect role, active state or membership.

Use the existing EncryptionPort/BCrypt policy: nonblank passwords, at least eight characters, fewer than 72 UTF-8 bytes. Credential mutations serialize on the account row, hash once and increment a monotonic `credential_version`. Every authenticated request compares the signed JWT generation with PostgreSQL. Missing generation in existing signed v1 tokens means zero; existing accounts backfill to zero, preserving rollout without reviving obsolete APIs. Changed credentials invalidate all previous tokens, including the changing session; successful self-change returns 204 and the frontend signs out. Already admitted in-flight requests may finish; this is next-request revocation, not transaction cancellation.

Administrative reset sets `must_change_password=true`. Login still returns `token`, adding `mustChangePassword`; current-account responses add the same boolean. Restricted accounts may only read their own account and change their password; all other authenticated operations return 403 with `password_change_required`. A replacement password must differ from the temporary/current password. The frontend loads the current account before memberships and forces the password screen. No password is stored in browser storage or returned by credential endpoints.

Add administrator metadata discovery at `GET /api/v1/organizations/admin` and `GET /api/v1/organizations/{id}/members/{userId}`. Existing organization list/get remain membership-scoped. The administrator selects a paginated organization then reads/grants/revokes the chosen user's association; these metadata reads do not bypass business access. Grant/revoke retain existing idempotence and transactional audit.

First-administrator bootstrap is separate, disabled by default, and uses explicit `INITIAL_ADMIN_ENABLED`, `INITIAL_ADMIN_NAME`, `INITIAL_ADMIN_EMAIL`, `INITIAL_ADMIN_PASSWORD`. Under a PostgreSQL advisory lock it creates an administrator only if no administrator (including inactive) has ever been recorded, never promotes an existing account or resets credentials. It runs without development profile, forces initial password change, and never logs secrets. Existing deployments need none of these variables. Remove optional bootstrap secrets after the first successful deployment. Development initialization remains development-only and must not overwrite an existing account's changed password on restart.

## Alternatives and rejected options

| Option | Benefit | Cost / decision |
| --- | --- | --- |
| Keep JWTs valid until expiry after password changes | No schema change | Compromised tokens retain access; rejected |
| Redis token denylist or a new authentication platform | Fine-grained session management | New lifecycle/availability burden; unnecessary while PostgreSQL is already queried per request |
| Credential generation with restricted reset state | Immediate next-request revocation, additive rollout | Requires V2 and explicit rollback restriction; selected |
| Reset without mandatory change | Old UI could immediately use temporary credentials | Leaves administrator-known passwords as permanent credentials; rejected |
| Public registration/reset or default production administrator | Easy setup | Unrequested attack surface and credential risk; rejected |
| Reuse development bootstrap in Railway | Existing code | Rewrites credentials and couples production to local settings; rejected |

## Migration, rollout and rollback

The operational baseline boundary is now V1 plus V2 for this delivery. Once a deployment must retain data, every later change uses a new Flyway version. A future disposable pre-production consolidation would require another explicit reset decision, an inventory of lost data, aligned local/CI/Railway schema and operator authorization before any destruction; it is not required or performed here. Never rewrite an applied migration in a database that will remain in use.

V2 adds non-null credential_version default 0 with a nonnegative constraint and must_change_password default false. PostgreSQL applies constant-default additive DDL transactionally; it still takes an ALTER TABLE lock, so validate against a representative restored database and monitor lock contention. No data removal, backfill based on guessed ownership, Flyway clean/repair or V1 edits. Fresh databases execute V1 then V2; existing V1 databases execute only V2 during normal startup.

Deploy backend first, verify health/liveness/readiness and existing frontend login/account/organization/intake operations, then deploy frontend. Before the new frontend is available, do not initiate resets: an older bundle cannot complete the newly required password flow. Existing normal accounts/tokens remain compatible. Migrations run automatically before Hibernate validation; ports, CORS, RSA, DB/Redis configuration and Docker execution stay unchanged.

The old backend is schema-compatible with the added columns but is **not security-compatible after credential changes or mandatory resets**: it ignores generation and restriction state. Do not roll back to that binary after enabling these actions. Prefer a forward fix or a rollback artifact retaining these security checks. Restoring V1 application logic would require an independently reviewed access-blocking/recovery procedure; do not remove V2 or manually rewrite hashes to make it start.

## Frontend, security and verification consequences

Retain React/Vite, the single apiClient/session/provider and generated OpenAPI types. Add explicit profile/password and administrator user/membership pages with pt-BR UX. Remove unreachable account services/pages and their dead dependencies only after dependency inspection. Preserve unrelated workflows; no new backend domain architecture is introduced.

Verify populated V1 upgrade and rerun on real PostgreSQL, no changed V1 checksum, generic login rejection, DTO exposure, self escalation, email conflicts, concurrent credential/metadata writes, old-token revocation, restricted reset workflow, platform guards, membership grants/revocation and cross-organization denial. Run the full Maven suite, Docker build, frontend clean install/lint/type/tests/build, generated-contract checks and local real-backend/browser workflows. Deployment evidence must distinguish isolated local rehearsal from actual Railway verification; no remote deployment or configuration mutation is authorized.
