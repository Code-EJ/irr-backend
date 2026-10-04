# ADR-0017: Global administration and the Identity release boundary

- Status: Accepted; implementation in progress.
- Date: 2026-10-04.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Context and supersession

The owner's consolidated direction establishes ADMINISTRATOR as global authority and explicitly permits administrator creation. This **supersedes the administrator-target restrictions in ADR-0008/0016 and the explicit-membership requirement for global administrators in ADR-0009/0015/0016**. Other account lifecycle, token generation, modular monolith and migration decisions remain in force. Historical decisions are retained with supersession notices, not silently rewritten as if the former rule never existed.

Organization is an operational tenant; User is an authenticatable identity; Membership grants local authority. Historical requirements FF-1 describe organization login as a combined business workflow, not a reason to place credentials on Organization. The current milestone includes the existing identity slice plus global administrator lifecycle, complete organization administration and email recovery (ADR-0018).

## Drivers and options

Global IRR oversight, local isolation, safe administrator succession, minimal roles, and reproducible deployment drive the decision. Adding a SUPER_ADMIN enum or a second local administrator role would duplicate existing concepts. A granular permission matrix has no approved product definitions. Reuse ADMINISTRATOR globally and MANAGER/MEMBER locally. A creator-only model cannot represent shared tenant operations and is superseded by organization access boundaries.

## Decision

ADMINISTRATOR is displayed as **Administrador Geral**. It can create all four user roles, update/demote/deactivate administrators, reset credentials and perform organization CRUD and membership management. Global users can list all active organizations, select any active organization and read/write its business data without a stored membership. Inactive organizations and records remain inaccessible; foreign IDs still cannot be mixed into an explicitly selected organization. Global access does not disable domain invariants, validation or concurrency protections.

Use the existing `/api/v1/organizations` list for effective visibility and remove the unreleased duplicate `/organizations/admin` metadata listing after updating consumers. The `/organizations/{id}/membership` response describes actual membership only. The frontend uses the authenticated global role for global controls; it does not invent or persist a MANAGER membership. RequestOrganizationScope uses a dedicated effective-access check for global users and existing membership checks for everyone else. Existing scoped reporting can be accessed for every tenant, but a consolidated analytical dashboard is the explicitly later reporting milestone, not an additional feature in this delivery.

MANAGER is the **Administrador da Organização**: it manages existing operational modules only in active assigned organizations. MEMBER retains existing read and explicitly permitted member operations. Account classifications CITY_HALL/ORGANIZATION/REPRESENTATIVE never substitute for membership. Global identity creation, account role changes, Organization creation and membership grants remain global-only. This avoids letting a manager change an account shared by other tenants or escalate privileges. Future granular permissions belong at the centralized access boundary after product definitions exist; no parallel RBAC is introduced.

At least one active global administrator must remain after every account demotion/deactivation. Serialize these operations using a PostgreSQL transaction advisory lock shared with bootstrap, then lock/reload the actor and target and check the active administrator count. Recheck the actor's global role after obtaining the lock so a concurrently demoted/deactivated administrator cannot complete a stale authorized mutation. Credential changes remain row-locked. This is an application-controlled invariant; direct database writes are not a supported administration path.

Bootstrap uses only BOOTSTRAP_ADMIN_ENABLED/NAME/EMAIL/PASSWORD. The prior INITIAL_ADMIN_* names are unreleased and superseded; no aliases are kept. Disabled by default, create-only, non-development, no automatic promotion, no profile/hash/role overwrite and no SQL installation step. An existing administrator history closes first-account bootstrap. Recommend disabling it after provisioning. Existing development bootstrap remains local and create-only.

Organization creation UX uses the existing organization, user and membership operations. Each successful resource is retained and shown if a later step fails. Retry the failed step without duplicating completed resources. No distributed transaction, guessed ownership, or hidden cleanup of newly created accounts.

## API and database boundaries

Only `/api/v1` is supported. Before first release/homologation its unreleased contract may be corrected with explicit documentation and consumer updates. The first release manifest freezes v1; later compatible changes remain v1, while real breaking changes require an ADR and evaluation of v2. Do not create v2 preemptively.

V1 and applied V2 stay unchanged. Global administration requires no schema change; recovery requires V3 under ADR-0018. No database reset, manual SQL patch, history deletion or automatic merge is authorized.

## Consequences and verification

Global compromise has platform-wide impact; password revocation and mandatory reset remain enforced before authorization. UI guards complement server checks. Audit every OrganizationScope use and historical negative test: replace obsolete global denial assertions with positive global access plus explicit non-global cross-tenant denial tests. Verify administrator creation/edit/deactivation, self-removal and concurrent last-admin protection, local manager boundaries, inactive organizations, real contract tests, frontend workflows and both images.

Identity & Access is NOW; first deployment/homologation is NEXT; Routes follows that gate; native reporting/filters/indicators/dashboards follows Routes; GPS remains planned post-deploy; optional BI/advanced integrations are future work. The Master Plan and requirement traceability are the central progress records. No Routes, Municipality entity, GPS provider or analytical formula is implemented here.
