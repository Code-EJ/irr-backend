# ADR-0006: Backend foundation execution and acceptance evidence

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Current implementation reference: [ADR-0013](0013-backend-operational-completion.md) records the completed backend and verified local operations. This ADR retains its decision-time evidence; later records supersede pending implementation statements.


- Status: Implemented foundation; broader domain/module overhaul remains in progress.
- Date: 2026-09-26.
- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Related: [master plan](0001-master-plan.md), [Docker foundation](0005-preproduction-docker-foundation.md), [fresh database baseline](0007-fresh-database-baseline.md).
- Branches: backend general-backend-refactor; frontend refactor-froned-general. No main/master commit was made by this work.

## Context

The owner authorized backend/database implementation, confirmed the absence of production, selected Docker and Swagger, requested a clean database baseline and required English documentation with Enzo Ribas attribution. The foundation turns those decisions into runnable code and a reproducible local environment. It is not the completion of every master-plan domain phase.

## Decision drivers and considered options

Deliver a tested vertical infrastructure slice rather than call an untested schema/package rewrite complete. Keeping the legacy setup would preserve missing key fixtures, implicit personal-database tests, mismatched ports and unverified schema startup. A wholesale domain rewrite would combine unresolved catalog/sorting semantics with deployment changes. The selected foundation makes those later changes independently testable and documents their remaining gates.

## Implemented decisions

1. Replaced file-based JWT test keys with ephemeral in-process keys and added real authentication tests.
2. Separated unit and integration lifecycles with Surefire/Failsafe. PostgreSQL integration tests own their database, credentials and fixtures.
3. Consolidated the current 21-table schema into one English V1 with inventory uniqueness/nonnegative constraints and mapping alignment. Historical SQL is archived outside the runtime path. Test-only V2 examples verify incremental evolution and rollback.
4. Aligned Flyway through the Boot BOM; added test-only Testcontainers 1.21.4 for Docker 29 and springdoc 2.9.1 for the official API documentation.
5. Added a multi-stage, non-root Docker runtime with read-only root filesystem, health gating, persistent database/attachment volumes and external RSA keys. Maven dependency downloads use a BuildKit cache.
6. Standardized the actual ignored .env for Compose database configuration, port, documentation switch and a development administrator. Existing database credentials were preserved. Obsolete duplicate POSTGRES_* and JWT_SECRET entries were removed. A separate Docker .env is unnecessary for this topology.
7. Added an explicitly development-only administrator initializer, gated by both profile and bootstrap flag. It hashes passwords, synchronizes a changed configured development password on restart and refuses to elevate a conflicting non-administrator. No credentials are committed or printed in this record.
8. Published Swagger UI and generated OpenAPI with Enzo Ribas contact metadata, bearer security, public session exceptions and current operation summaries. Markdown API inventories are supporting audit references, not a second official specification.
9. Added backend verification CI and English README/architecture/API/schema/dependency/contribution documentation. Frontend README and shared ADRs now describe current integration limitations in English. Existing contributor credits and user edits were preserved.

## Validation evidence

| Check | Observed result |
| --- | --- |
| Java 21 / Maven clean verify | Passed: 46 unit tests and 13 integration tests; zero failures/errors/skips |
| Fresh PostgreSQL baseline | One applied V1; 21 domain tables; Hibernate schema validation passed |
| Incremental migration examples | Additive V2 retains records and does not rerun; failing V2 rolls back DDL and preserves V1 records/history |
| Inventory integrity | Duplicate and negative balances rejected; two competing first inserts commit one row; stale independent transaction cannot overwrite a committed balance |
| Development administrator unit checks | Hashing, stable-password restart, role conflict and placeholder rejection passed |
| Docker build | Multi-stage image built from source; no source/key bind mount in runtime |
| Actual .env / Compose | Configuration validated without displaying values; fresh irr-development project started healthy on localhost:9191 |
| Recreated containers | Both services returned healthy; attachment volume marker persisted; real administrator login and protected access passed again |
| Container runtime | UID 10001; attachment directory writable; application root read-only |
| Health / unauthenticated API | Readiness UP / HTTP 200; unauthenticated vehicle request HTTP 401 |
| Official documentation | Swagger UI and OpenAPI return HTTP 200; generated document includes 43 current operations and bearer authentication |
| Real development login | DEV_ADMIN_EMAIL/DEV_ADMIN_PASSWORD from .env authenticated successfully; resulting bearer token accessed the protected vehicle endpoint with HTTP 200 |
| Documentation disabled | Integration test verifies anonymous Swagger/OpenAPI requests no longer bypass authentication |

The initial Docker daemon was unavailable and later crashed during verification. It was restarted; the successful checks above were obtained afterward. An earlier Docker build stalled while resolving dependencies; the cached rebuild completed. These environmental failures are not hidden by skipped integration tests.

The remote GitHub Actions workflow has been authored, not claimed to have run. Frontend runtime/type/lint checks were not repeated for documentation-only changes. The prior vulnerability snapshot remains dated; this foundation does not claim that all advisories were remediated.

## GitHub and backlog disposition

All 51 backend and five frontend repository issues were included in the readiness inventory. At that review point 25 backend issues were open, 26 closed, and all five frontend issues were closed. GitHub Projects-only/archived draft cards were not accessible and are not represented as inspected.

At the owner's request, backend PRs #50, #66 and #73 were closed without merging. Branches and contribution history were retained. Their linked issues were not automatically marked complete. Functional requirements still belong to the shared backlog and must be satisfied by the new compatible implementation.

## Consequences and next slices

The backend now has a reproducible development environment, official Swagger contracts, usable local administrator access, an explicit fresh database and enforceable database checks. The next architecture slice can proceed without depending on local secrets or an unknown database.

This is not a completed modular/domain rewrite. Identity policy beyond local bootstrap, attachment ownership/safe DTOs/durable deletion, organization/catalog scope, sorting-stage allocation, ledger reconciliation, full collection/sales workflows and frontend adaptation remain open. Current attachment entity responses and legacy inventory behavior must not be mistaken for approved target architecture.

The frontend still targets an old configured API and incompatible contracts; its UI is not certified to log into this local backend merely because the .env administrator works in Swagger. Shared schema changes after this V1 use incremental migrations; unknown old volumes are not deleted or silently imported.
