# ADR-0005: Pre-production backend foundation and Docker architecture

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Current implementation reference: [ADR-0013](0013-backend-operational-completion.md) records the completed backend and verified local operations. This ADR retains its decision-time evidence; later records supersede pending implementation statements.


> Database execution update: [ADR-0007](0007-fresh-database-baseline.md) replaces the active historical migration chain with one fresh English V1. Old databases are preserved, not automatically upgraded or deleted.

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Status: Accepted for the development foundation; domain redesign remains staged.
- Date: 2026-09-26.
- Authority: the project owner confirmed that IRR is not in production, authorized backend/database refactoring, requested English standardization and instructed removal of the old PRs.
- Supersedes: production-discovery prerequisites and mandatory release-count waiting periods in ADR-0001/0002 for this pre-production environment. It does not authorize deletion of unknown local data.
- Related: [master plan](0001-master-plan.md), [database](0002-database-schema-redesign.md), [module decisions](0003-architecture-and-dependencies.md), [readiness review](0004-implementation-readiness.md).

## Context

The first audit deliberately did not assume a deployment state. The owner has now resolved that uncertainty: there is no production deployment. The reference project's production constraints are not IRR constraints. The backend may evolve with coordinated breaking changes, while maintaining explicit before/after contracts for the frontend and a migration path for existing development databases.

PRs [#50](https://github.com/Code-EJ/irr-backend/pull/50), [#66](https://github.com/Code-EJ/irr-backend/pull/66) and [#73](https://github.com/Code-EJ/irr-backend/pull/73) were closed without merging on September 26 at the owner's request. Their branches and history remain intact. Underlying user stories remain relevant. Closing a PR does not complete its linked issue.

## Decision drivers

1. One reproducible local environment and CI path, independent of machine keys or development database contents.
2. A small operating footprint appropriate to one API and one PostgreSQL instance.
3. Honest documentation of current code versus target module boundaries.
4. Database-enforced inventory integrity and migrations exercised against actual PostgreSQL.
5. English code/documentation for each changed slice, with a tracked path for remaining legacy names.

## Considered options

| Option | Advantages | Disadvantages | Decision |
| --- | --- | --- | --- |
| Host-only Java and PostgreSQL | Minimal tooling | Machine-specific databases, keys and port drift | Supported only as a developer alternative |
| Docker Compose, API image and PostgreSQL | Reproducible dependencies; persistent named volumes; few moving parts | Single-host lifecycle and backups remain operator responsibilities | Selected |
| Kubernetes now | Scheduling and multi-node operations | No demonstrated requirement; adds cluster, networking, storage and secret-management overhead | Deferred until measured availability/scale requirements justify it |
| Delete migrations and create a new V1 immediately | Short apparent reset path | Hides defects and destroys the upgrade path for collaborators | Rejected for the foundation; an explicit disposable-only baseline may be considered later |

## Decision

### Runtime and trust boundaries

Use Java 21, the existing Spring Boot 3.5 baseline, PostgreSQL 16 and Maven. Build one executable JAR in a multi-stage Docker image; runtime uses a JRE and UID 10001, a read-only root filesystem, dropped capabilities, writable temporary space and a dedicated attachment volume. The source checkout and Maven cache are not mounted into the runtime. No debugger port is exposed.

Compose binds the API to loopback port 9191. PostgreSQL is reachable only on the Compose network, not published on the host. Database readiness gates backend startup. Backend readiness includes database health; liveness is separate. Health exposes status only. Other actuator endpoints are not exposed. The current authentication filter still needs the identity refactor described in ADR-0001.

RSA keys are generated locally and mounted read-only outside the image. Passwords come from an ignored local .env file. The example is a placeholder, not a shared usable credential. The container build context excludes keys, .env and local uploads. Production-grade secret delivery, TLS ingress and separate database migration/runtime roles must be revisited before any public deployment; this development stack is not a production security certification.

Named volumes retain database and attachment data across container replacement. The new database volume is distinct from the previous banco_dados_volume; old data is not copied or deleted automatically. Key rotation invalidates existing tokens and must be explicit. Upload paths are generated UUIDs, never supplied filenames. Stored legacy attachment paths require an explicit file-copy and metadata-remapping step before reusing old records in this container layout.

### Repository architecture and documentation contract

Keep a modular monolith. Target feature modules are identity, organizations, materials, logistics, intake, processing, inventory, sales, attachments and reporting. Within a migrated module use api (HTTP DTO/controller), application (use cases and transaction boundaries), domain (rules and ports), and infrastructure (JPA/storage adapters). Shared code is limited to IDs, time, error envelopes and cross-cutting configuration. No feature accesses another feature's repositories; use application ports and immutable IDs/results. Inventory owns posting and balances. Reporting consumes projections and never mutates operational data.

The current repository is still predominantly layered (controllers/services/domain/infrastructure/dto). Do not claim that moving one class has completed modularization. Migrate a vertical slice only after its behavior tests and contract inventory exist. Do not simultaneously invent a new workflow, move every package and change the persistence model.

Every directory is covered by the repository map, every Java source by the source inventory, every controller operation by the API map, every DTO by its field contract, and every current SQL table by the schema inventory. These are linked from README and the architecture index. API changes update the map and consumer migration entry in the same commit. New or changed code identifiers, comments, error messages, tests, documentation and commit messages use English. Existing Portuguese source comments and document endpoint names are tracked legacy debt, not declared fully translated by this foundation.

### Database entry slice

Preserve V1-V4 bytes. V5 adds a unique constraint on inventory_balance.material_subtype_id and a check against negative weight or volume. This closes database acceptance of duplicate first balances; service-level conflict retry/idempotency is still required in the inventory slice. If old data violates the constraints, migration fails transactionally instead of selecting a winner or clamping quantities. A preflight query and recovery procedure are in the operations guide.

Align Attachment.fileName with its existing SQL limit of 255 characters. Hibernate validates schema; it does not create/update it. Disable automatic Flyway baselining to avoid blessing an unknown nonempty database. Align Flyway core and PostgreSQL modules through the Spring Boot BOM. Testcontainers 1.21.4 is a deliberate test-only BOM override for Docker 29 compatibility, supported by the upstream [Docker API issue](https://github.com/testcontainers/testcontainers-java/issues/11211); remove the override when the chosen Boot baseline manages a suitable version.

The target organization and stock-ledger topology remains in ADR-0002. Do not make creator_id into an organization ID by renaming it. No production discovery is needed to start, but catalog sharing, organization membership and multi-stage sorting semantics are still domain decisions. For the future ledger, balance equals SUM(all committed deltas, including the OPENING movement); never count opening twice.

### Official API documentation and developer environment

The owner explicitly selected Swagger as the official API documentation. Add springdoc v2 for the Spring Boot 3 baseline, generate OpenAPI from the controllers/DTOs, describe bearer authentication and public session exceptions, and verify UI/spec availability in integration tests. Repository Markdown API inventories are supporting audit evidence only. API_DOCS_ENABLED controls documentation availability; do not bypass authorization for arbitrary API paths.

Compose loads the existing local .env; preserve existing credentials and append only missing nonsecret defaults. DB_USER/DB_PASSWORD/DB_NAME are the single source for container PostgreSQL settings. Legacy POSTGRES_* duplicates and JWT_SECRET are not consumed by this runtime. Keys remain external RSA files. Integration tests never consume .env credentials. Validate the actual .env against an isolated Compose project without printing resolved secrets. README provides the supported step-by-step path.

### Development access credentials

The owner requested complete local credentials in .env. Compose activates the development profile and passes DEV_ADMIN_NAME, DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD to a profile-gated initializer, also controlled by DEV_BOOTSTRAP_ENABLED. It creates the local administrator using the password hashing port, preserves an unchanged hash, synchronizes explicit development password changes and refuses to promote an existing non-administrator. Outside the development profile it is absent. The actual .env remains ignored; the example has placeholders. This is local access setup, not the final partner-provisioning policy.

### Verification and delivery

Unit tests generate ephemeral RSA keys and do not start Spring or contact PostgreSQL. Maven test runs these tests. Maven verify additionally runs *IT classes against a disposable PostgreSQL container and test-only keys; no Docker means an explicit failure, not a silent skip or fallback to a developer database. Fixtures create their own users/material hierarchy and independent transactions establish stale-update behavior.

CI runs Java 21 verification and a Docker image build. A locally successful run is not evidence that the remote workflow has run. Container image tags remain updateable; record the digest of validated release artifacts when introducing deployment automation. No Kubernetes resources are added.

### Ordered execution plan

| Slice | Scope | Exit criteria |
| --- | --- | --- |
| BE0 foundation | Isolated tests, Docker image/Compose, dependency convergence, V5 integrity, repository documentation | Unit/integration tests pass; image builds; fresh container boots healthy; V4 upgrade and invalid-data refusal verified |
| BE1 identity and attachments | Protected administrator provisioning, bootstrap, principal consistency, role matrix, safe DTOs, object authorization and durable deletion | Unauthorized/cross-owner cases fail; passwords/paths not serialized; rollback never removes referenced files |
| BE2 organizations and inventory | Confirm scope/stages, detailed DDL, ledger/allocation/idempotency model, one posting service | Conservation/reconciliation; no double release; two concurrent sales cannot oversell; deterministic reversal |
| BE3 workflows | Collection/team, donations, pressing, sales/invoices and reports | Issue acceptance criteria mapped to tests and English contracts |
| FE integration | Consume the agreed backend contract and repair build/lint/types | Matched end-to-end workflows; no release of an incompatible frontend/backend pair |

Breaking pre-production API changes need a documented old-to-new mapping, updated consumer and contract tests, not an artificial two-release/30-day waiting period. Existing development data is retained by default; a developer may deliberately choose a new empty volume. No automatic volume deletion or schema clean is introduced.

## Consequences

Positive: backend work can proceed immediately; the environment is reproducible; schema failures and race-related invariants become observable; documentation describes both implemented and planned architecture.

Negative: Compose is a single-host topology, not a high-availability solution. Persistent local volumes still need backups if their data matters. The domain rewrite and full English cleanup are multiple slices; the foundation does not complete the application or remediate every historical advisory. Identity/attachment authorization and ledger accounting remain explicit next-stage work.

## Validation

See the dated [execution record](0006-backend-foundation-execution.md) for actual commands/results and limitations. A design decision is not itself a passing test.

## Sources

- [Compose service health and dependency semantics](https://docs.docker.com/reference/compose-file/services/).
- [Testcontainers shared lifecycle](https://java.testcontainers.org/test_framework_integration/manual_lifecycle_control/).
- [Docker Engine API compatibility](https://docs.docker.com/reference/api/engine/).
