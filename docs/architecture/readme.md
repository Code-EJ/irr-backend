# Backend architecture

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

The accepted direction is a modular monolith in Docker Compose. [ADR-0005](../adrs/0005-preproduction-docker-foundation.md) defines decisions and tradeoffs; this document maps them to the repository. The implemented code is still predominantly layered. The future module tree below is a target, not a claim that all packages have already moved.

## Container topology

~~~mermaid
flowchart LR
    Browser[Frontend browser] -->|HTTP 127.0.0.1:9191| API[Spring Boot API / non-root JRE]
    API -->|JDBC / internal network| DB[(PostgreSQL 16)]
    DB --> Data[(Database volume)]
    API -->|Authenticated internal connection| Redis[(Redis 8.2)]
    Redis --> RedisData[(Redis AOF volume)]
    API --> Uploads[(Attachment volume)]
    Keys[Local RSA keys / read-only mount] --> API
    CI[CI / Maven verify] --> Tests[Disposable PostgreSQL and Redis / test keys]
~~~

The browser never talks to PostgreSQL or the file volume directly. The API owns transactions and authorization. PostgreSQL owns constraints; Hibernate validates, Flyway migrates. File writes are outside DB transactions. ADR-0008 implements upload rollback compensation and a PostgreSQL-backed post-commit deletion queue; crash-orphan reconciliation remains open. Container readiness includes PostgreSQL and Redis connectivity and does not certify business correctness.

## Repository map

| Path | Responsibility | Documentation / verification |
| --- | --- | --- |
| src/main/java/org/code/api/organizations | First feature module: api/application/domain/infrastructure, explicit audited membership | [ADR-0009](../adrs/0009-organization-scope-and-catalog-integrity.md) |
| src/main/java/org/code/api/controllers | Existing HTTP entrypoints | [Operation map](../api_documentation.md) |
| src/main/java/org/code/api/dto | Request/response records and validation | Same contract inventory |
| src/main/java/org/code/api/services | Current use cases and transaction boundaries | [Source inventory](../source-inventory.md), unit tests |
| src/main/java/org/code/api/domain | Current entities, enums, exceptions and ports; entities still use JPA | [Entities](../entities_documentation.md), [enums](../enums_documentation.md) |
| src/main/java/org/code/api/infrastructure | JPA repositories, security and technical adapters | Source inventory; PostgreSQL and JWT tests |
| src/main/java/org/code/api/filter | Current request authentication and logging | ADR-0004 identity findings; HTTP integration smoke tests |
| src/main/resources | Environment configuration and Flyway SQL | [Operations](../operations.md) |
| src/test/java | Unit tests and *IT integration suites | Maven Surefire/Failsafe |
| scripts | Development key generation | README commands; no real credentials tracked |
| Dockerfile / docker-compose.yml / .dockerignore | Build, local runtime, persistence and build-context exclusions | Image build and readiness smoke test |
| pom.xml / mvnw / .mvn | Dependencies, Java toolchain and Maven wrapper | clean verify and dependency tree |
| .github | Review templates and CI | Backend verification workflow |
| docs/adrs | Strategic plan, alternatives, consequences and execution evidence | Update with every architectural change |
| docs | Repository-wide architecture and contract references | Cross-link and inventory checks |

## Target feature boundaries

| Module | Owns | Collaborates through |
| --- | --- | --- |
| identity | Users, credentials, sessions and provisioning | Principal/authorization ports |
| organizations | Organizations, memberships and scope | Membership lookup; no creator-to-org guessing |
| materials | Category/type/subtype catalog | Scoped material lookup |
| logistics | Vehicles, team members and assignments | Validated logistics references |
| intake | Collections, donations and input allocations | Logistics/material/attachment ports |
| processing | Sorting stages and pressing transformations | Inventory posting commands |
| inventory | Ledger, balances, reservations, idempotency and reversals | Single transactional posting interface |
| sales | Buyer, invoice-backed sale and item allocation | Inventory/attachment application ports |
| attachments | Metadata, storage lifecycle and object authorization | Opaque attachment references |
| reporting | Read projections and reconciliation | Read-only queries/events |

Within each migrated module: api -> application -> domain. Infrastructure implements domain/application ports. Domain rules avoid servlet/security context and repository calls. A shared module contains only stable primitives/configuration; it must not become a cross-feature service repository. Application transactions encompass all DB writes for one business operation; external file effects require lifecycle/outbox handling. Cross-module repository access is prohibited in migrated slices and must be replaced by ports as each old slice moves.

## Request and transaction flow

~~~mermaid
sequenceDiagram
    participant Client
    participant HTTP as Controller / filter
    participant UseCase as Application use case
    participant Domain as Domain rules
    participant Store as PostgreSQL / adapters
    Client->>HTTP: Request with bearer token
    HTTP->>UseCase: Validated DTO and authenticated principal
    UseCase->>Domain: Authorize scope and check invariants
    Domain-->>UseCase: Allowed command
    UseCase->>Store: Transactional reads and writes
    Store-->>UseCase: Constraint-checked result
    UseCase-->>HTTP: Response DTO
    HTTP-->>Client: HTTP status and English contract
~~~

This is the target module flow. ADR-0008 removes attachment entity serialization and the session-prefix bypass while retaining current package locations. Conflicting catalog rules and broader module extraction remain open. See ADR-0004 for historical evidence and ADR-0008 for the current identity/attachment implementation.

## Dependency ownership

See [dependency decisions](../dependencies.md) for current purposes, convergence and test-only overrides; ADR-0003 retains the complete historical inventory.

## Quality and evolution

Unit tests cover domain/application behavior; integration tests use real PostgreSQL, independent transactions and generated keys. Every new invariant needs a meaningful failure case. Keep historical migrations immutable. Document wire renames and frontend migration together. Prefer a small tested vertical slice over package-wide renaming. Add enforceable package dependency rules when the first complete feature module moves; no dependency rule currently proves the entire legacy tree modular.
