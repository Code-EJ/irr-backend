# IRR architecture diagrams

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Reviewed 2026-10-10 against the current repositories.

These diagrams describe the implemented modular monolith and the proposed Railway installation separately. They continue ADR-0003/0014, organization boundaries, the stock ledger and approved reporting ADRs. They do not introduce microservices, a strict hexagonal rewrite or Kubernetes. See [responsibilities and transaction rules](readme.md) and [release readiness](../release-readiness.md).

## Implemented runtime

```mermaid
flowchart LR
    Operator["Operator browser"] -->|"Static HTML, CSS and JavaScript"| Frontend["Frontend container: unprivileged Nginx 8080"]
    Operator -->|"Bearer JWT and organization header: /api/v1"| Backend["Backend container: Java 21 / Spring Boot 9191"]
    Backend -->|"Transactions and live report queries"| Postgres[("PostgreSQL 16")]
    Backend -->|"Infrastructure and recovery throttling"| Redis[("Redis 8.2")]
    Backend -->|"Attachment bytes"| Uploads[("Persistent upload volume")]
    Keys["Private RSA configuration"] --> Backend
    Backend -->|"Development mail delivery"| Mailpit["Local Mailpit"]
    Backend -.->|"Configured HTTPS mail adapter"| Brevo["Brevo: external setup pending"]
    Postgres --> DatabaseVolume[("Database volume")]
```

The browser calls the API directly; Nginx serves static assets and SPA fallback, not application business logic. Local frontend port 5173 maps to container 8080. The active backend worktree uses host 9292; the standard development default is 9191. Neither is a production hostname. PostgreSQL owns durable business truth and authorization associations; Redis is not the stock ledger. Attachment bytes and database metadata require coordinated recovery. Mailpit is local-only; external Brevo delivery is not yet accepted.

## Backend module boundaries

```mermaid
flowchart TB
    HTTP["Controllers and feature API adapters"] --> Security["Authentication and current organization authorization"]
    Security --> Identity["Identity and organizations"]
    Security --> Catalog["Materials, fleet and parties"]
    Security --> Intake["Collections and donations"]
    Security --> Routes["Route plans, executions and section coverage"]
    Security --> Processing["Sorting and pressing"]
    Security --> Sales["Sale draft, posting and reversal"]
    Security --> Reports["Operational reports and Analytics"]
    Processing --> Ledger["Inventory: lots, allocations and immutable movements"]
    Sales --> Ledger
    Intake --> Persistence["Shared JPA models and repository adapters"]
    Routes --> Persistence
    Catalog --> Persistence
    Identity --> Persistence
    Ledger --> Persistence
    Reports --> Queries["Scoped JDBC aggregates and backend formulas"]
    Persistence --> DB[("One PostgreSQL database")]
    Queries --> DB
```

Modules share one JVM and relational database. Feature-owned packages coexist with established controllers/services and shared JPA models; this is the pragmatic hybrid documented by ADR-0003. The diagram shows responsibilities, not a claim that every dependency is enforced by a module framework. Processing/sales coordinate ledger writes transactionally; intake alone does not credit saleable inventory. Analytics reads live scoped facts without a warehouse or report cache.

## Transactional organization write

```mermaid
sequenceDiagram
    participant UI as Browser
    participant API as API and Security
    participant Scope as Organization scope
    participant UseCase as Business service
    participant DB as PostgreSQL
    UI->>API: Versioned request, bearer token, organization ID
    API->>DB: Reload active account and credential state
    API->>Scope: Resolve organization and current permission
    Scope->>DB: Lock organization and recheck write access
    Scope->>UseCase: Authorized scoped command
    UseCase->>DB: Validate referenced records and lifecycle
    UseCase->>DB: Persist operation and applicable ledger changes
    alt Any validation or persistence failure
        DB-->>UseCase: Roll back transaction
        API-->>UI: Safe error response
    else Successful transaction
        DB-->>UseCase: Commit together
        API-->>UI: Safe DTO with exact decimal strings
    end
```

Global administrator authority differs from organization MANAGER/MEMBER associations. Scope is checked server-side; browser guards are usability controls. Referenced records must belong to the same organization. Reports preserve missing values and compute ratios in the backend. Filesystem bytes cannot join the database transaction; compensation and durable cleanup are documented separately in the architecture guide.

## Proposed Railway homologation topology — synthetic data only, not deployed

```mermaid
flowchart LR
    Browser["Browser"] -->|"Public HTTPS"| FE["Frontend service: Nginx 8080"]
    Browser -->|"Public HTTPS /api/v1"| BE["Backend service: Spring Boot 9191"]
    subgraph Environment["One approved Railway environment"]
        FE
        BE
        PG[("Private PostgreSQL")]
        RD[("Private authenticated Redis")]
        Disk[("Persistent uploads: /data/uploads")]
        BE -->|"Private network"| PG
        BE -->|"Private network"| RD
        BE --> Disk
    end
    BE -->|"HTTPS recovery email"| Mail["Verified Brevo sender"]
    PG -.->|"Coordinated backup procedure"| Backup["Encrypted off-machine recovery set"]
    Disk -.->|"Same consistency boundary"| Backup
```

This is a review target, not evidence of cloud resources. Use one backend replica while uploads use one filesystem volume. Verify UID 10001 write/read permissions on the mounted volume and account for deployment downtime. Approve domains, region/budget, secret management, real email, monitoring, backup retention/RPO/RTO and client data before installation. Frontend build-time API origin must be public; private Railway DNS is only for server-to-server connections. [Preparation details](../deploy/railway/preparation.md) record platform limitations and approval gates.

## Portable production topology — Hostinger Brazil planning target

```mermaid
flowchart LR
    Browser["Operator browser"] -->|"HTTPS 443"| TLS["Caddy ingress: certificates and reverse proxy"]
    subgraph VPS["Brazilian Linux VPS: Docker Compose"]
        TLS -->|"Frontend hostname"| FE["Static Nginx: UID 101, port 8080"]
        TLS -->|"API hostname; preserve /api/v1"| BE["Java: UID 10001, port 9191"]
        BE --> PG[("Private PostgreSQL 16")]
        BE --> RD[("Private authenticated Redis 8.2")]
        BE --> Uploads[("Persistent uploads")]
        PG --> DBVolume[("Persistent database")]
        TLS --> Certs[("Persistent certificate state")]
    end
    BE -->|"HTTPS"| Brevo["Approved Brevo sender"]
    DBVolume -.->|"Coordinated recovery boundary"| Backup["Encrypted off-machine backup"]
    Uploads -.-> Backup
```

Only ingress publishes 80/443; SSH is restricted to operators. PostgreSQL/Redis have a private internal Docker network and no host ports. No Docker socket, development profile, Mailpit or application build is exposed on the host. Single-host/volume deployment has no HA guarantee. [ADR-0027](../adrs/0027-provider-independent-deployment.md), [portable installation](../deploy/portable-vps.md) and [Hostinger budget plan](../deploy/hostinger/README.md) define the operational boundary. Railway Virginia is synthetic homologation only. This is a candidate topology, not a purchased or accepted environment.
