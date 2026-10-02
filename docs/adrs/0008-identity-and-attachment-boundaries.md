# ADR-0008: Administrator provisioning and creator-scoped attachment lifecycle

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Current implementation reference: [ADR-0013](0013-backend-operational-completion.md) records the completed backend and verified local operations. This ADR retains its decision-time evidence; later records supersede pending implementation statements.


- Status: Accepted and implemented for the pre-production backend; organization scope and remaining domain gates are not completed by this slice.
- Date: 2026-09-26.
- Author and documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Related: [Master plan](0001-master-plan.md), [database topology](0002-database-schema-redesign.md), [Docker foundation](0005-preproduction-docker-foundation.md), [migration discipline](0007-fresh-database-baseline.md).
- Supersedes: the historical public-registration exception and unscoped attachment behavior described in earlier audit snapshots.

## Context

The previous filter exempted the entire session prefix. Self-registration created representatives and issued their tokens; no administrator provisioning boundary existed. The authenticated actor adapter returned an empty role list. Attachment endpoints serialized JPA metadata including internal relationships, looked up arbitrary IDs without creator checks, and deleted bytes before the database could reject a referenced attachment. A failed or rolled-back metadata deletion could therefore destroy a document still referenced by an operational record.

The owner confirmed there is no production deployment and authorized refactoring. Development database V1 is already applied, so this slice must preserve it and add a reviewed V2. Docker Compose remains sufficient; the cleanup workload does not justify Kubernetes, a message broker or another service.

## Decision drivers

1. Only an administrator may create a partner with an explicitly selected supported role.
2. Active identity and current roles come from the database on each request, not stale JWT claims.
3. Foreign attachment IDs must not grant reads, deletion, internal paths or user metadata.
4. Relational restrictions and rollbacks must never trigger premature physical deletion.
5. Failed cleanup must survive application restarts and support safe retries.
6. Swagger must describe the executable contracts; JavaDoc and supporting English ADRs explain implementation decisions.

## Considered options

| Option | Advantages | Disadvantages | Outcome |
| --- | --- | --- | --- |
| Keep self-registration and trust role claims | Small change | Violates controlled partner creation and keeps stale privileges | Rejected |
| Administrator provisioning with current database identity | Explicit authorization, immediate role/deactivation effect | Database lookup per request; no invitation/password-reset workflow yet | Selected |
| Give administrators access to every attachment | Convenient support access | Invents a cross-owner policy not established by domain requirements | Rejected for this slice |
| Creator ownership for every role | Testable, conservative boundary matching current creator columns | Organization sharing and audited support access require later explicit policies | Selected |
| Synchronous file deletion or only an after-commit callback | Simple | Transaction rollback or process failure can lose work or bytes | Rejected |
| PostgreSQL cleanup queue in the metadata transaction | Durable, no new dependency, supports concurrent claims | Polling delay, operational retries and filesystem reconciliation remain necessary | Selected |
| Broker/object-storage platform now | Rich managed lifecycle features | Infrastructure complexity before deployment and ownership decisions | Deferred |

## Decision

### Authentication and provisioning

Security is stateless and deny-by-default for unauthenticated requests. Only exact POST /api/session/authenticate, GET health routes, enabled GET Swagger/OpenAPI routes and CORS preflight are public. The bearer filter runs once within the security chain and is disabled as a separate servlet filter. It resolves a UUID principal, current role and active account on each request, clears request security state, and returns generic 401 errors without echoing tokens. Unknown-account and wrong-password login responses are identical; this is response normalization, not a claim of constant-time authentication.

POST /api/users is guarded at both controller and application-service boundaries by ADMINISTRATOR. Supported partner roles are CITY_HALL, ORGANIZATION and REPRESENTATIVE. Administrator creation is excluded. Passwords are hashed with the existing BCrypt adapter; eight or more characters and fewer than 72 UTF-8 bytes are required. The response contains only id, fullName, email and userRole, and never issues a token for the created account. Existing exact email matching is retained; case normalization requires a separately reviewed collision/backfill decision. Development administrator credentials still come from the ignored local .env and the development-only initializer.

Browser origins are explicitly configured through CORS_ALLOWED_ORIGINS, defaulting to localhost/127.0.0.1 on port 5173. Bearer authorization does not require credentialed cookies. CORS is a browser policy, not a replacement for authorization.

### Attachment API and ownership

POST /api/documents retains the legacy multipart field documento to avoid an unnecessary wire rename. The implementation uses English identifiers. It accepts nonempty PDF, PNG and JPEG signatures, validates display filenames, and applies a ten-MiB file limit; the servlet also limits the complete multipart request to ten MiB. Signature detection does not certify complete format validity or perform malware scanning.

The upload response is AttachmentResponse: id, fileName, contentType and createdAt. It contains no entity, creator, password hash or storage path. Download and deletion resolve both attachment ID and authenticated creator ID. Another administrator receives the same 404 as any other foreign actor. Referenced attachments return 409 on deletion. Existing records need no ownership backfill: they already have creator_id.

The storage adapter generates UUID paths, restricts reads and deletion to its configured directory, rejects symbolic-link targets and bounds reads. Its root is a trusted Docker volume accessible to the application process. This does not defend against an attacker already able to replace the trusted storage root itself. Downloads use safe attachment filenames and nosniff. Open EntityManager in View is disabled; attachment DTO mapping occurs in the service transaction.

### Filesystem and database transactions

Uploads write bytes and register rollback compensation before saving metadata. A database rollback removes the newly written file. A process crash between writing bytes and completing the database transaction, or a failed compensation, can still leave an orphan. Automated orphan reconciliation, quarantine and malware scanning are explicitly deferred; no distributed atomicity claim is made.

V2__attachment_deletion_queue.sql adds attachment_file_deletion with id, unique storage_path, created_at, nonnegative attempts, next_attempt_at and bounded last_error. No existing columns or records are removed. Metadata deletion, FK validation and queue insertion share one transaction. The HTTP endpoint returns 202 after that transaction commits. A rollback leaves both metadata and bytes intact and creates no visible job.

A scheduled worker polls every 60 seconds and claims at most ten due jobs using FOR UPDATE SKIP LOCKED. It deletes bytes idempotently and removes each successful job. Failures increment attempts, record a generic reason, and retry after one minute. If the worker crashes after deleting bytes but before committing, a retry safely observes a missing file. Row locks are held during this bounded filesystem operation; a slow disk can delay a batch. Multiple instances may claim separate jobs, but must share the same underlying storage. The current supported topology is one API container with one attachment volume.

## Contract transition and migration

1. Retain the existing login route and token response. Replace frontend self-registration with an administrator partner form calling POST /api/users.
2. Keep POST /api/session/register as an explicitly deprecated tombstone: authenticated callers receive 410, anonymous callers 401. It never creates an account. Remove it only after frontend migration and a separate documented decision; no unapproved release date is assumed.
3. Update attachment clients to consume safe metadata and expect 202 for deletion, 404 for foreign IDs and 409 for referenced documents. Downloads and the multipart name remain stable. Do not expose compatibility fields containing credentials or internal paths.
4. On an existing fresh-baseline V1 database, restart the rebuilt backend to apply V2 in place. Do not edit V1, drop volumes, run Flyway repair or silently reconcile archived pre-reset histories.
5. Future changes use V3 or later after checking shared migration history. The V3 fixtures in tests are examples only and are not packaged into the runtime image.

## Consequences

The highest-risk account-creation and attachment-access paths now have explicit boundaries. PostgreSQL remains the single durable coordination mechanism, with no new dependencies. Queue maintenance is operational work: inspect aged/failed jobs and fix storage permission or availability issues before retries. Do not manually delete referenced files or remove a job simply to hide a failure.

This is an incremental security slice inside the current repository structure, not a claim that every feature already follows the target module layout. Remaining work includes organization/membership scope, catalog authorization reconciliation, inventory ledger and allocations, collection/sales workflows, password lifecycle and login abuse controls, orphan reconciliation, frontend contract adoption and the outstanding dependency/security upgrade campaign. Historical Portuguese comments in untouched legacy implementation are still subject to the repository-wide English standard during their refactor; all new code and maintained documentation in this slice are English.

## Verification and acceptance

IdentityAttachmentIT exercises administrator provisioning and password hashing; role restrictions; duplicate/UTF-8 validation; retired registration; role changes and deactivation; generic authentication errors; CORS and Swagger; actor authorities; creator-only attachment access; safe DTOs; signature/name validation; deletion and retry; referenced-record protection; upload rollback compensation; and deletion rollback preservation. PostgreSQL migration tests verify V1 to V2 preserves accounts, fresh V1/V2 creates 22 application tables, and test-only V3 success/failure preserves existing data and migration discipline.

See the repository test reports and the implementation completion note for the exact executed result. Tests run against disposable PostgreSQL, generated test keys and isolated file storage; the local development .env is not a test credential source.

## Implementation completion evidence (2026-09-26)

- Maven verify: 44 unit tests and 29 PostgreSQL/HTTP integration tests passed, with zero failures, errors or skips (73 total).
- Docker Compose image rebuilt successfully; API and PostgreSQL containers healthy. Existing fresh-baseline database upgraded from V1 to V2 without deleting its volume.
- Login using the actual ignored development .env succeeded; anonymous protected access returned 401; Swagger UI and OpenAPI returned 200 with 44 documented operations.
- Runtime partner creation/login, safe attachment upload/download, cross-owner administrator rejection (404), retired registration (401/410) and queued deletion (202) passed. The scheduler removed the smoke file and queue job; the temporary smoke partner was explicitly removed.
- English Markdown cross-links and the complete Java source index were validated in both repositories. No dependency vulnerability rescan or completed frontend contract migration is implied by these results.
