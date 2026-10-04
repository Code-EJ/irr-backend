# Identity & Access verification and release record

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Executed 2026-10-04.

## Result and boundaries

The expanded Identity & Access milestone is implemented and locally verified. This is not a deployed homologation release. Existing ADR-0003's pragmatic modular monolith, ADR-0014's exclusively /api/v1 contract and the existing React/TypeScript/Vite/Axios frontend are retained. No v2, compatibility aliases, new RBAC framework, municipality entity or route/reporting/GPS implementation was introduced.

Backend executable reference: **18f4891825821dae931ab7f3d44a2d0cd48c311f**. The current generated OpenAPI contains **50 paths / 95 operations**, all application paths under /api/v1. The frontend pins its SHA-256 and generates schema.d.ts from that snapshot. Swagger/OpenAPI remains the official API specification; supporting Markdown inventories do not replace it.

Both implementation checkouts use feat/identity-account-management. The original backend add-railway_deploy-docs checkout remains separate: its staged/unstaged README and Railway documentation are preserved. No main/master commit, owner-worktree modification, force push, merge or remote secret update was performed. The owner-relocated repository root is retained; no repository was moved.

## Delivered workflows and authorization

- ADMINISTRATOR is global authority, displayed as Administrador Geral; it creates/edits/deactivates/resets all account roles, including administrators, and accesses active organizations without membership. Organization headers still select business ownership; foreign IDs and inactive organizations remain inaccessible.
- MANAGER is Administrador da Organização, limited to actual active assigned organizations. It cannot create global accounts/organizations, grant memberships or change shared account roles. MEMBER retains existing permitted scoped operations.
- A PostgreSQL advisory lock, actor reload and active-administrator count protect the last active global administrator under concurrent demotion/deactivation. No unsupported direct SQL administration path is offered.
- Own profile/password changes, administrator reset and forced initial password change use credential generations to revoke previous tokens. Request DTOs reject privilege mass assignment; passwords, recovery secrets and provider credentials are excluded from safe responses/logs.
- Frontend organization CRUD and membership lookup/grant/revoke reuse the single client and membership editor. Responsible onboarding creates Organization, selects/creates User, then grants MANAGER; completed IDs survive failed grants, so retries do not duplicate accounts. Existing resources can be resumed through membership administration after leaving the wizard.
- Public recovery sends neutral 202 for existing/absent/inactive/throttled accounts. Confirmation rejects invalid/expired/used/revoked authorization, consumes once and changes credentials atomically. The UI clears URL fragments, confirms passwords and preserves authorization on a same-password validation error.
- Bootstrap uses BOOTSTRAP_ADMIN_ENABLED/NAME/EMAIL/PASSWORD, create-only, disabled by default. Existing profiles/hashes/roles are never overwritten, non-admin email collision is rejected, and prior administrator history closes bootstrap. The local rehearsal was restarted with a different bootstrap password and then disabled; existing changed credentials still authenticated.

## Executed verification

| Command / check | Executed result |
| --- | --- |
| Backend `./mvnw.cmd spotless:apply clean verify`, JDK 21 | 48 unit + 75 integration tests; zero failures/errors/skips |
| Backend `node scripts/verify-documentation.mjs --update-inventory` | 259 Java sources; documentation links/inventory valid |
| Backend dependency tree + `node scripts/audit-dependencies.mjs` | 156 coordinates; zero reported OSV findings at audit time |
| Frontend `npm ci` | Lockfile clean install succeeded |
| `npm run lint` and `npm run typecheck` | Passed |
| `npm test` | 57 tests / 7 files passed |
| `npm run test:contract` against real PostgreSQL/Redis/backend/Mailpit | 18 tests / 4 files passed |
| `npm run api:check` | 95 supported v1 operations pinned to 18f4891 |
| `npm run build` with public HTTPS /api/v1 origin | Passed; 1828 modules; JS 394.28 kB before gzip |
| `npm run verify:production` with the same origin | Public API origin and absence of supplied private credentials/local API/internal hostname verified |
| `npm audit --omit=dev` | Zero reported vulnerabilities |
| Full `npm audit` | Five high findings in Tailwind 3's build dependency chain; review gate remains open |
| Backend Docker image, development frontend image and public-HTTPS frontend image | All three builds passed |
| Running backend readiness and frontend /health | Both UP / HTTP 200; backend, database, Redis, Mailpit and frontend healthy |
| Browser inspection of running image | Global organization navigation, creation form/default Cooperativa and user/profile role presentation inspected in Portuguese |

Real integration coverage includes administrator succession/concurrency, global access without fabricated membership, local manager/foreign-scope denial, organization updates and revocation, profile/current-password/reset/token invalidation, additive migrations, ledger/catalog regressions, and actual local recovery email capture followed by one-use confirmation/replay rejection. Provider protocol tests verify the fixed Brevo HTTPS request without sending external email. Unit/component cases include partial onboarding retries, neutral recovery, expiry/used links, same-password retry, unavailable mail service, membership confirmation and organization update/deactivation. Browser inspections do not claim password-changing or permission-granting end-to-end browser execution; these mutations are covered by API/component tests.

The compiled React Router dependency contains a bare localhost placeholder for relative-URL parsing. It is not an API network destination. The production gate checks actual localhost /api URLs rather than modifying vendor code or promising zero occurrences of an unrelated string. Images were built successfully; an image vulnerability scanner was not run.

Private logs, fixture secrets and screenshots are ignored under .local. Development credentials reside in ignored frontend .env-dev, never VITE_* values or committed examples. They were aligned with the current local rehearsal, preserving an ignored previous-values backup. Neither plaintext recovery tokens nor credentials are included in this report.

## Database and deployment

**Reset required: NO.** Existing V1 is unchanged. V2 adds credential_version/must_change_password; V3 adds password_recovery_token with user foreign key, nonnegative generation, expiry validation and expiry/user indexes. Runtime: 30 application tables plus Flyway history. Test-only migration examples now start at V4. No migration repair, history rewrite, column drop or volume deletion was performed.

Deploy backend first with backup, immutable V1/V2/V3 and current PostgreSQL/Redis configuration. Provision the first administrator only when needed, change its initial password, then disable bootstrap/remove temporary bootstrap secrets. Validate readiness, the administrator invariant, current OpenAPI, local/global access and revocation. Configure the transactional provider and public frontend reset URL. Deploy frontend with the verified public HTTPS /api/v1 build argument and exact CORS origin; test the public reset path through the SPA fallback. Execute deployed smoke/recovery and write a release manifest recording image digests, commits, OpenAPI digest, migration versions and approvals. That first homologation manifest freezes v1; this local record does not.

Railway: retain existing backend/PostgreSQL/Redis infrastructure. Configure MAIL_PROVIDER=brevo, MAIL_API_KEY, MAIL_FROM, MAIL_FROM_NAME, PASSWORD_RESET_BASE_URL and PASSWORD_RESET_TTL_MINUTES. Set BOOTSTRAP_ADMIN_* only for explicit first provisioning. Local Mailpit is not a production mail relay and does not require a Railway service. No remote infrastructure or variables were modified in this execution.

Rollback must preserve V2/V3 and credential generations; do not run undo/reset migrations or recreate volumes. Prefer rolling back to a tested image with the current identity/revocation policy. A pre-generation or old global-role-policy binary is not a safe security rollback. Disable recovery via MAIL_PROVIDER=disabled if required; authenticated login remains available, public recovery explicitly returns unavailable. Tokens consumed and passwords changed before rollback remain changed. Retain a secure database backup and validate compatibility before any binary rollback.

## Email and remaining operational gates

The chosen production adapter is Brevo HTTPS with a small port and no provider SDK. Its documented free plan allows 300 emails/day; API rate limits are distinct from that quota. Local Mailpit captures messages without paid or external delivery. Redis applies hashed-email/client cooldowns, a global 300/rolling-24-hour acceptance budget and confirmation limits; it fails closed. PostgreSQL stores only SHA-256 authorization digests with expiry/address/generation checks. A bounded asynchronous sender is not a durable outbox: process crashes may lose queued mail; users retry after cooldown. Reverse proxy deployment must review direct-client-address throttling because forwarded headers are deliberately not trusted automatically.

External operator work remains: establish/verify the sender/domain, provision the Brevo key, confirm current account quota, configure Railway public HTTPS origins and secrets, then verify actual inbox delivery and expiry/replay in deployed homologation. No external delivery is claimed. Official references: [Brevo free plan](https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan), [API limits](https://developers.brevo.com/docs/api-limits), [transactional sending](https://developers.brevo.com/docs/send-a-transactional-email).

The full frontend audit exposes five high development-chain findings rooted in [the braces advisory](https://github.com/advisories/GHSA-vfj7-8cjw-p6xm). Production dependencies report zero. Do not hide this by forcing an unrelated Tailwind major upgrade; review the current build-input exposure and select an explicitly verified remediation before release approval. No browser runtime safety claim or production-readiness certification follows from the production-only audit.

## Master Plan and remaining product decisions

NOW Identity & Access is locally delivered. NEXT is first deploy/homologation. THEN Routes; AFTER Routes native reports, filters, indicators and dashboards; GPS is later and external BI optional. Processing/stock/sales frontend remains gated for later approved module work. Legacy removal includes 78 unreachable frontend files from b156abb and removal of the unreleased /organizations/admin consumer in favor of the canonical effective-visibility endpoint; no compatibility service remains.

Six historical requests remain product decisions, not implementation authorization: delegated non-global account creation; report recalculation/published-period semantics; persistent street-completion notifications; authoritative neighborhood cartography; alert/benchmark/indicator formulas; and bulk market-price editing. Multi-municipality ownership/mapping must be defined during Routes discovery; Organization classification/CITY_HALL is not a municipality model. [Requirements traceability](requirements/traceability.md) records the source and status of each.

## Reviewable pull request text

These are prepared descriptions, not remotely created or merged pull requests.

Backend title: `feat(identity): complete global administration and secure email recovery`

Backend description: Complete the current Identity & Access milestone using global ADMINISTRATOR authority and tenant-local MANAGER/MEMBER roles. Add administrator succession protection, profile/password/reset revocation, create-only bootstrap and single-use email recovery through PostgreSQL, Redis and Brevo/Mailpit. Preserve V1 and apply additive V2/V3 without resetting accounts or stock; retain the modular monolith and exclusively /api/v1 contract. Validation: 48 unit and 75 integration tests, current Swagger, Docker build and dependency audit. External sender/Railway configuration and deployed homologation remain release gates; no remote secrets or production reset are included.

Frontend title: `feat(identity): integrate global administration organization onboarding and recovery`

Frontend description: Integrate the current /api/v1 Swagger contract through the existing transport and React/Vite stack. Add global account/organization administration, actual membership management, responsible onboarding with partial retries, profile/password/reset and public recovery in Brazilian Portuguese. Remove replaced unreachable legacy integrations and verify production API configuration without embedding private development credentials. Validation: lint, typecheck, 57 component/unit tests, 18 real-backend contract tests, production/development Docker builds and browser inspection. Full npm audit retains five high build-tool findings requiring release review; external homologation remains pending.

## Repository review inventory

Base: `4c50f171b03f7d97487e08aadd4d80257b31bc03`. Verified implementation HEAD at record creation: `18f4891825821dae931ab7f3d44a2d0cd48c311f`. Final documentation commits follow this recorded executable baseline; obtain final HEAD/status with git before merge.

### Commits from base

~~~text
380a6e0 docs(identity): document additive account and credential lifecycle
10cfe72 feat(identity): add profile and credential lifecycle with scoped administration
201f2fd feat(bootstrap): provision the first administrator without resetting accounts
dd3e80f test(identity): verify concurrent password changes have one winner
2b333a1 docs(identity): record incremental rollout and Railway operations
2b876f7 docs(adr): define global administration and email recovery
9cc5fe1 feat(identity): enforce global administration and last administrator protection
9041307 docs(identity): align global authority and requirements traceability
4b471b7 feat(identity): add one-use email recovery and shared abuse limits
0ea2a2c docs(identity): document email configuration and recovery rollout
18f4891 test(identity): verify Brevo HTTPS request without external delivery
~~~

### Created, modified and deleted paths from base

The inventory includes current documentation changes before their final commit; A/M/D are added/modified/deleted. This record itself is a new documentation file.

~~~text
M	.env.example
M	README.md
M	docker-compose.yml
M	docs/adrs/0001-master-plan.md
M	docs/adrs/0008-identity-and-attachment-boundaries.md
M	docs/adrs/0009-organization-scope-and-catalog-integrity.md
M	docs/adrs/0014-final-preproduction-contract-and-baseline.md
A	docs/adrs/0016-account-and-credential-lifecycle.md
A	docs/adrs/0017-global-administration-and-identity-release.md
A	docs/adrs/0018-password-recovery-and-transactional-email.md
M	docs/api_documentation.md
M	docs/architecture/readme.md
M	docs/dependencies.md
A	docs/deploy/railway/README.md
A	docs/deploy/railway/checklist.md
A	docs/deploy/railway/rollout.md
A	docs/deploy/railway/troubleshooting.md
A	docs/deploy/railway/variables.md
A	docs/email-recovery.md
M	docs/entities_documentation.md
M	docs/operations.md
A	docs/requirements/traceability.md
M	docs/source-inventory.md
M	src/main/java/org/code/api/controllers/SessionController.java
M	src/main/java/org/code/api/controllers/UserController.java
M	src/main/java/org/code/api/domain/models/user/Session.java
M	src/main/java/org/code/api/domain/models/user/User.java
M	src/main/java/org/code/api/dto/session/request/LoginRequestDTO.java
M	src/main/java/org/code/api/dto/session/response/LoginResponseDTO.java
A	src/main/java/org/code/api/dto/user/ChangePasswordRequest.java
M	src/main/java/org/code/api/dto/user/CreatePartnerRequest.java
A	src/main/java/org/code/api/dto/user/ResetPasswordRequest.java
A	src/main/java/org/code/api/dto/user/UpdateProfileRequest.java
M	src/main/java/org/code/api/dto/user/UserResponse.java
M	src/main/java/org/code/api/filter/BearerFilter.java
A	src/main/java/org/code/api/identity/recovery/EmailSenderPort.java
A	src/main/java/org/code/api/identity/recovery/HttpEmailSender.java
A	src/main/java/org/code/api/identity/recovery/PasswordRecoveryController.java
A	src/main/java/org/code/api/identity/recovery/PasswordRecoveryService.java
A	src/main/java/org/code/api/identity/recovery/RecoveryConfiguration.java
A	src/main/java/org/code/api/identity/recovery/RecoveryThrottle.java
M	src/main/java/org/code/api/infrastructure/development/DevelopmentAdministratorInitializer.java
M	src/main/java/org/code/api/infrastructure/documentation/OpenApiConfiguration.java
M	src/main/java/org/code/api/infrastructure/repositories/UserRepository.java
M	src/main/java/org/code/api/infrastructure/security/BCryptEncryptionProvider.java
A	src/main/java/org/code/api/infrastructure/security/InitialAdministratorInitializer.java
M	src/main/java/org/code/api/infrastructure/security/JWTTokenProvider.java
M	src/main/java/org/code/api/infrastructure/security/RequestOrganizationScope.java
M	src/main/java/org/code/api/infrastructure/security/SecurityConfig.java
M	src/main/java/org/code/api/infrastructure/web/GlobalExceptionHandler.java
M	src/main/java/org/code/api/organizations/api/OrganizationController.java
M	src/main/java/org/code/api/organizations/application/OrganizationService.java
M	src/main/java/org/code/api/organizations/domain/OrganizationStore.java
M	src/main/java/org/code/api/organizations/infrastructure/JdbcOrganizationStore.java
M	src/main/java/org/code/api/services/AccountLifecycleService.java
M	src/main/java/org/code/api/services/AuthService.java
A	src/main/java/org/code/api/services/GlobalAdministratorGuard.java
M	src/main/java/org/code/api/services/PartnerProvisioningService.java
A	src/main/java/org/code/api/services/PasswordPolicy.java
M	src/main/resources/application.properties
A	src/main/resources/db/migrations/V2__account_credential_lifecycle.sql
A	src/main/resources/db/migrations/V3__password_recovery.sql
A	src/test/java/org/code/api/identity/recovery/HttpEmailSenderTest.java
A	src/test/java/org/code/api/identity/recovery/RecoveryThrottleTest.java
A	src/test/java/org/code/api/infrastructure/AccountLifecycleIT.java
M	src/test/java/org/code/api/infrastructure/DevelopmentAdministratorInitializerTest.java
M	src/test/java/org/code/api/infrastructure/FinalBaselineSchemaIT.java
A	src/test/java/org/code/api/infrastructure/GlobalAdministratorIT.java
M	src/test/java/org/code/api/infrastructure/IdentityAttachmentIT.java
A	src/test/java/org/code/api/infrastructure/InitialAdministratorInitializerTest.java
M	src/test/java/org/code/api/infrastructure/InventoryBalanceSchemaIT.java
M	src/test/java/org/code/api/infrastructure/InventoryMigrationIT.java
M	src/test/java/org/code/api/infrastructure/OrganizationMembershipIT.java
A	src/test/java/org/code/api/infrastructure/PasswordRecoveryIT.java
M	src/test/java/org/code/api/infrastructure/repository/MockedUserRepository.java
R100	src/test/resources/db/migration-examples/invalid/V2__example_failed_upgrade.sql	src/test/resources/db/migration-examples/invalid/V4__example_failed_upgrade.sql
R100	src/test/resources/db/migration-examples/valid/V2__example_incremental_index.sql	src/test/resources/db/migration-examples/valid/V4__example_incremental_index.sql
~~~

### Decision and operations documents

New backend decisions: [ADR-0017](adrs/0017-global-administration-and-identity-release.md) and [ADR-0018](adrs/0018-password-recovery-and-transactional-email.md). Current runtime configuration is documented in [email recovery](email-recovery.md), [operations](operations.md), [Railway rollout](deploy/railway/rollout.md) and [the HTTP map](api_documentation.md). Frontend counterpart: docs/adrs/0016-global-identity-and-recovery-integration.md in the frontend repository.
