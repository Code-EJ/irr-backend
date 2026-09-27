# Backend architecture

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

IRR is a transactional modular monolith. New modules use feature-owned API/application contracts. Established catalog, identity, attachment and processing adapters remain in controllers/services with shared JPA entities and ports. This is an intentional hybrid, not a claim of framework-free Clean Architecture. [ADR-0014](../adrs/0014-final-preproduction-contract-and-baseline.md) records the verified implementation and limits.

## Runtime topology

~~~mermaid
flowchart LR
    Browser[React client] -->|HTTP and bearer JWT| API[Spring Boot API :9191]
    API -->|JDBC transaction| DB[(PostgreSQL 16)]
    API -->|Authenticated connection| Redis[(Redis 8.2)]
    API --> Uploads[Persistent attachment volume]
    Keys[Read-only RSA keys] --> API
    DB --> DBVolume[Database volume]
    Redis --> AOF[Redis AOF volume]
    Tests[Maven integration suites] --> Ephemeral[Disposable PostgreSQL and Redis]
~~~

Only the API loopback port is published. The API runs non-root with a read-only root filesystem. PostgreSQL owns authorization associations, business truth, receipts, immutable stock movements and cleanup jobs. Redis participates in readiness but is not a correctness dependency for stock or membership decisions. Upload bytes use a separate volume. Kubernetes is deferred until deployment demand justifies it.

## Repository responsibilities

| Path under src/main/java/org/code/api | Responsibility | Boundary |
| --- | --- | --- |
| organizations | Organization metadata, memberships, audit and request scope | Explicit associations; no creator inference |
| parties | Buyers and team staff | Scoped reference CRUD and retained-history guards |
| intake | Collection orchestration and logistics | Validated raw inputs; no saleable stock credit |
| inventory | Ledger, lots, receipts, reversals and queries | Sole stock writer; immutable history |
| sales | Draft/post/reverse use cases | Optimistic drafts, fiscal evidence, transactional lots |
| reporting | Operational summaries and CSV | PostgreSQL repeatable-read snapshot |
| controllers / dto | Established HTTP adapters and safe value contracts | No persistence entities or secrets serialized |
| services | Existing identity, materials, fleet, donation, processing and attachment use cases | Transactions and domain validation |
| domain/models / domain/ports | Shared JPA mappings and application/infrastructure ports | Persistence annotations retained deliberately |
| infrastructure | Security, JPA/JDBC repositories, storage, Swagger, development bootstrap | Framework and external-resource adapters |
| filter | Authentication and request handling | Active account revalidation |

Other repository boundaries: src/main/resources/db/migrations owns immutable versioned SQL; src/test contains disposable integration fixtures; scripts owns key generation, advisory audit, smoke and recovery tools; Dockerfile/Compose own local containers; .github/workflows owns build gates; docs/adrs owns decisions. [Source inventory](../source-inventory.md) indexes every Java file.

## Request and transaction sequence

1. Spring Security verifies the RSA bearer token and reloads the active account.
2. OrganizationScope resolves X-Organization-Id and validates current organization/membership. A write transaction locks that organization and rechecks permission.
3. The application use case resolves every foreign identifier inside the same organization, validates lifecycle/quantities and persists entities.
4. Stock commands flush JPA rows before JDBC inserts. Receipt, operational document, lot allocation, movement and balance projection commit together.
5. Safe DTOs serialize exact decimals as plain strings. The client uses returned versions and stable command keys on retries.

Organization locks serialize writes within one organization. Conditional updates and database checks prevent negative stock even if a higher-level bug bypasses validation. Composite keys prevent cross-organization relationships. First-time balance insertion uses the same serialization. Different organizations remain independent.

## Storage and reporting

Upload bytes cannot join PostgreSQL's transaction. Rollback compensation handles ordinary failures; a durable cleanup queue handles committed deletion; an hourly exclusive advisory-lock scan queues old unreferenced UUID files after crashes. Uploads hold a shared lock until metadata commit. Readers never see filesystem paths or account credentials. Files referenced by business records cannot be deleted.

Reports use live data, inclusive UTC ranges and current POSTED status. Stock is a current snapshot. Historical closing balances and external fiscal validation are not claimed. See [operations](../operations.md) for recovery and [schema](../entities_documentation.md) for constraints.

## Extension rules

New features belong to a feature package with an explicit request/response contract and service transaction. Reuse OrganizationScope and StockLedger; never update balances directly or infer ownership from creator_id. Avoid moving every old class merely for package symmetry. Introduce an ADR when changing ownership, lifecycle, accounting or infrastructure semantics. JavaDoc, tests, Swagger, migrations and maintained documentation are English and credit the maintainer.

## Final contract and schema

Every application controller uses /api/v1. Unversioned mappings and the signup retirement stub are removed; no compatibility adapter remains. The final fresh V1 reproduces the verified schema in new database/attachment volumes. Spotless is enforced by Maven and preserved in the Docker build. ADR-0003 remains the pragmatic modular-monolith direction; this consolidation adds no strict Clean Architecture layer or package-only rewrite.
