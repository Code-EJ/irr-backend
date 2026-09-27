# ADR-0009: Organization-owned catalogs and stock, explicit membership foundation

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Current implementation reference: [ADR-0013](0013-backend-operational-completion.md) records the completed backend and verified local operations. This ADR retains its decision-time evidence; later records supersede pending implementation statements.


- Status: Organization ownership model accepted by the owner on 2026-09-27; membership foundation implemented; catalog/operational ownership cutover remains pending.
- Author and documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Related: [master plan](0001-master-plan.md), [target database topology](0002-database-schema-redesign.md), [identity boundary](0008-identity-and-attachment-boundaries.md).

## Context

The owner explicitly selected a separate catalog and stock for each organization, with associated users. The former open product decision in ADR-0002 is therefore resolved. Existing creator_id columns identify actors, not organization ownership; one actor may belong to several organizations. Creating an organization per user would fabricate an unverified mapping.

Independent inspection also found that type/subtype name filters were ignored and update services assigned client versions directly to Hibernate-managed entities. Those defects can be corrected without changing ownership or importing historical data.

## Decision drivers

1. Distinguish organizational ownership, platform roles and the actor responsible for a record.
2. Require explicit active associations instead of interpreting a supplied organization UUID as permission.
3. Revoke organization access immediately and record administrative changes transactionally.
4. Preserve existing domain records while preparing a deliberate organization cutover.
5. Make catalog paging/filtering and concurrent edits behave consistently before scope changes.

## Considered options

| Option | Advantages | Disadvantages | Outcome |
| --- | --- | --- | --- |
| Global shared catalog and organization stock | Less catalog duplication | Does not match the owner's selected policy | Rejected |
| Organization-specific catalogs and stock | Explicit ownership and independent operations | Requires catalog initialization and scoped reference migration | Accepted |
| Infer organization from creator_id | Fast apparent backfill | Confuses an actor with an owner and fails multi-organization membership | Rejected |
| Explicit organizations and memberships before business-data cutover | No fabricated ownership; independently testable foundation | Intermediate system still contains legacy creator-scoped APIs | Selected |
| Cache membership in JWT/Redis | Fewer database reads | Revocation and role changes can remain stale | Rejected for this stage |

## Decision and implementation

### Feature boundary

The new organizations feature follows api -> application -> domain, with infrastructure implementing OrganizationStore using JdbcTemplate. OrganizationService owns transactions and method authorization. The organization records contain only safe identity/membership metadata. The persistence port keeps SQL out of the application service. Existing catalog services retain their current packages until their organization-aware rewrite.

Runtime V3 adds organization, organization_membership and organization_access_audit. PostgreSQL enforces organization/user references, a unique composite membership key, nonblank organization metadata and allowed local membership roles. The schema now has 25 application tables. V1 and V2 remain immutable. No existing record is moved, reassigned or deleted.

organization_type is a bounded descriptive classification, not a permission. Platform ADMINISTRATOR is required to create organizations or grant/revoke memberships. MEMBER and MANAGER are organization-local roles reserved for explicitly defined scoped permissions; neither grants platform administration, membership management or an implicit catalog write permission.

Creating an organization does not silently enroll its creator. An administrator must explicitly grant the first and subsequent memberships. Ordinary organization list/get and the reusable requireMembership boundary require an active organization, active association and active user, even for a platform administrator. Foreign, inactive and missing scopes all return 404. Paginated lists expose only the current user's organizations and accept size 1-100 with nonnegative page indexes.

Membership administration is an explicit platform capability across organizations. Each CREATE, changed GRANT or effective REVOKE records organization, actor, optional subject, old/new role and timestamp in the same transaction. Organization row locks serialize competing membership changes. Repeating the same active grant or revoking an already revoked membership creates no duplicate effect/event. Revocation retains the association row and takes effect on the next organization access check without waiting for token expiration.

The audit table has no mutation API. It is not yet cryptographically tamper-evident or protected from a database administrator; separate database roles/immutability enforcement remain release-hardening work. It records committed administrative effects, not every failed authorization attempt.

### Catalog corrections

All three hierarchy levels now apply creator, optional parent and case-insensitive name predicates in PostgreSQL before page selection/counting. Percent, underscore and the escape character are literal name-search characters. Name casing follows PostgreSQL lowercase semantics; no new identifier normalization or uniqueness rule is introduced.

An update compares the supplied version with the loaded persisted version and returns 409 on mismatch, including stale no-op submissions. It never assigns to the managed version field. saveAndFlush executes optimistic validation before response mapping so the response contains the current server-managed version. A real concurrent-transaction test proves exactly one writer succeeds. Application-service method security mirrors the existing administrator-only catalog write policy; another administrator still cannot read or modify a foreign creator's catalog.

The old category search helper is retained as unused legacy code until the catalog module extraction. Race-safe catalog uniqueness, cross-level scoped FKs and the unsafe historical administrator deactivation exception are not solved by these query/version corrections.

## API contracts and transition

| Operation | Access and behavior |
| --- | --- |
| POST /api/organizations | Platform administrator; 201, audited creation, no implicit membership |
| GET /api/organizations | Active memberships of the caller; bounded page and totalElements |
| GET /api/organizations/{id} | Active member only; no platform-admin read bypass |
| PUT /api/organizations/{id}/members/{userId} | Platform administrator; active target account; MEMBER or MANAGER; audited idempotent grant |
| DELETE /api/organizations/{id}/members/{userId} | Platform administrator; 204; audited revocation with retained row |

Swagger is the official schema/operation reference. No organization CRUD endpoint has been added to the frontend yet.

The current /api/materials, intake, processing, inventory and attachment APIs remain legacy creator-scoped where previously implemented. Membership revocation therefore does not yet revoke access to a user's legacy creator-owned records. Do not claim that all business data is organization-isolated. No organization header is currently accepted as a shortcut to cross-owner catalog access.

Next, introduce explicit organization-scoped catalog contracts, organization_id keys and matching composite foreign keys. Supply an explicit legacy-record mapping or retain unmapped rows in a restricted legacy path. Migrate intake/processing/stock references and define scoped MEMBER/MANAGER actions before removing creator-only filters. Document deprecation of legacy routes before enabling organization sharing. Only then activate the organization ledger/allocation workflow; never double-credit raw intake and saleable stock.

## Migration and rollback

Flyway applies V3 additively to the already deployed V1/V2 database. No backfill is attempted. Fresh startup applies V1-V3. Migration tests upgrade an existing V1 account through current migrations and retain it; test-only V4 examples prove successful/failed incremental DDL behavior. Runtime images contain no example migrations.

An application rollback must retain the added tables and audit history. Do not drop V3 tables or rewrite Flyway history to make an older binary start; validate compatibility in an isolated database and prefer a forward corrective release. Business-data mapping is a separate future change with its own reconciliation checks.

## Consequences and verification

The ownership decision is settled and an explicit, audited association mechanism is available. Full catalog/stock isolation is still an implementation gate. Organization tests cover no implicit access, nonadministrator denial, local-manager non-escalation, idempotent grants/revocation, same-token revocation, inactive organizations/accounts, rollback with audit preservation, concurrent grants and bounded inputs. Catalog tests cover all-level filter counts, literal wildcard handling, creator/service boundaries, updated versions, stale requests and concurrent transactions.

## Executed evidence (2026-09-27)

- Full Maven verify passed: 44 unit tests and 43 integration tests, zero failures/errors/skips (87 total).
- Docker database migrated through V1/V2/V3 without dropping the existing volume.
- Runtime Swagger exposes 49 operations; login with the actual ignored .env succeeded.
- Runtime organization creation, explicit self-membership grant, read, revoke and same-token denial passed. Exactly three CREATE/GRANT/REVOKE audit events were verified; only that temporary test organization, association and audit fixture were removed afterward.
- Catalog fix committed independently as 4e012c9; organization foundation as 25c3481. Organization-scoped business-data migration remains pending.
