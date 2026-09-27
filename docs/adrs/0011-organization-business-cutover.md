# ADR-0011: Organization-owned business API cutover

- Status: Accepted; implementation and regression verification in progress.
- Date: 2026-09-27.
- Author: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Related: [master plan](0001-master-plan.md), [membership foundation](0009-organization-scope-and-catalog-integrity.md), [stock posting](0012-transactional-stock-ledger.md).

## Context

The owner approved a catalog and stock per organization with explicit user associations. Previously, creator_id served both audit and access control, so members could not share business records. PostgreSQL V1–V3 are already the local baseline and must retain their checksums. The project is pre-production, but existing rows must not silently be assigned to organizations using their creator identity.

## Decision drivers

1. Explicit ownership and immediate membership revocation across every business operation.
2. Shared organization data with separately recorded authenticated actors.
3. No platform-administrator bypass for business data.
4. Database enforcement of child relationships, safe history retention and a reviewable migration.
5. A predictable frontend cutover documented in Swagger.

## Considered options

| Option | Benefits | Costs and risks | Decision |
| --- | --- | --- | --- |
| Keep creator-based isolation | No contract change | Violates shared ownership and duplicates catalogs | Rejected |
| Infer organization from the creator | Automatic backfill | A user can belong to multiple organizations; attribution would be invented | Rejected |
| Explicit organization header and nullable legacy ownership | Auditable selection, preserves existing rows | Clients must select an organization; legacy attribution needs a deliberate import | Selected |
| Database per organization | Strong physical separation | Operational overhead and many migration targets without current need | Deferred |

## Decision

V4 adds organization_id to the existing business tables. Unmapped rows remain null and are excluded from the new scoped APIs. No columns are dropped, and no existing organization attribution is guessed. Foreign keys pair organization_id with referenced record IDs, preventing cross-organization relationships. A collection-team trigger covers the legacy join table. Organization-scoped uniqueness includes inactive names/documents so soft deletion does not silently reuse historical identifiers.

Business requests require X-Organization-Id and a bearer token. The request adapter resolves active organization membership from PostgreSQL for each use case. MEMBER can read and operate intake/processing; MANAGER can also maintain catalog, vehicles, donors, buyers and team records. Platform account roles remain separate. Creating an organization does not grant implicit membership. Attachment bytes and metadata are shared with active members; deletion requires the uploader or a MANAGER and rejects referenced documents. The creator remains the audit actor.

Writes take the organization row lock and recheck membership inside the transaction. This serializes related operations, permission revocation, source allocation and reference deactivation within an organization. Reads do not take this write lock. It is an intentional throughput tradeoff for the current modular monolith; per-aggregate locks may replace it only with equivalent concurrency tests and deterministic lock ordering.

Catalog updates retain optimistic versions. Referenced operational parents cannot be deactivated, including through bulk vehicle updates, because JPA currently filters inactive entities. Processed intake cannot replace or deactivate its consumed child items. Historical rows are retained rather than hidden behind broken lazy associations.

## HTTP transition and deprecation

Existing business routes gain /api/v1 aliases and both aliases enforce the same organization policy. New modules use /api/v1 exclusively. This is a coordinated pre-production breaking change: old clients without the header receive 400; invalid or foreign scopes receive 404. Deploy the matching frontend organization selector before declaring the application usable. Keep unversioned aliases until the frontend and integration smoke tests use /api/v1; remove them only in a separately documented change. Authentication and platform-user routes do not require an organization header.

Swagger is the official operation/schema reference and must expose the required header. No endpoint may accept organization ownership from a request body as a substitute for the verified header.

## Migration and recovery

1. Back up PostgreSQL and attachment storage and preserve the ignored .env and signing keys.
2. Run Flyway V1–V5 on an empty disposable database and upgrade V1–V3 in integration tests.
3. Deploy V4 and the matching API in a maintenance window; never edit deployed migration checksums.
4. Create organizations, grant explicit associations, and use the new APIs for clean development data.
5. If legacy data must be imported, prepare a reviewed ownership mapping for complete relationship graphs and reconcile quantities before exposing those rows. A creator is not sufficient evidence.
6. Roll back application and database together from the backup if needed. Running the old creator-based API against newly shared data is not a supported rollback.

## Consequences

Authorization becomes explicit and shared data is no longer tied to its author. Organization serialization limits write throughput, and null legacy rows remain an intentional migration backlog. Historical names can be edited by managers, but referenced identities cannot be removed. This decision does not claim that every master-plan CRUD, dependency remediation or frontend screen is already complete.

## Verification

Regression coverage checks scoped catalog pagination and optimistic updates, attachment rollback/deletion, denied foreign sources, required headers, immediate membership revocation, Flyway upgrade behavior and preserved unmapped rows. A complete verification run passed after the scope and ledger changes; subsequently added CRUDs and lifecycle checks must pass their own integration tests before deployment.
