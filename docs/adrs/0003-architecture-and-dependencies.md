# ADR-0003: Modular architecture, API contracts and dependency standardization

- Status: Proposed; dependency inventory and baseline checks completed, upgrades not applied.
- Recorded: 2026-09-23.
- Scope: both IRR repositories; backend integration branch `develop`, frontend `main`.
- Related: [master plan](0001-master-plan.md), [schema redesign](0002-database-schema-redesign.md).

## Context

Backend code is divided into `controllers`, `services`, `domain/models`, `domain/ports`, `dto`, `infrastructure/repositories`, security/filter packages and two exception-handling locations. Controllers sometimes call ports, but domain ports depend on HTTP-facing DTOs and Spring Page/Pageable; services depend directly on infrastructure repositories; JPA annotations live in domain models. This is a layered Spring application with partial ports, not strict Clean Architecture. Current annotations already provide useful transaction and authorization boundaries and should not be discarded for package aesthetics.

Frontend code uses page folders, a broad shared entity file, generic CRUD services/forms, two Axios instances, handwritten route definitions, and legacy `services-antigos`. Vite transpiles TypeScript without checking it. ESLint imports `typescript` as if it were `typescript-eslint`, reads nonexistent `configs.recommended`, limits the configured file glob to JS/JSX and uses legacy `extends` shape in a flat config. Fixing only the import is insufficient. All of these problems precede this audit.

## Decision drivers

Make module ownership testable; preserve transactional behavior; establish one wire contract; reduce duplicate transport/state logic; repair quality gates before relying on them; distinguish direct, transitive and runtime dependencies; separate unused-code evidence from framework reflection/autoconfiguration; prioritize exploitable vulnerabilities and compatibility over the latest major release.

## Considered options

| Option | Pros | Cons | Decision |
| --- | --- | --- | --- |
| Strict hexagonal rewrite with separate persistence/domain models everywhere | Framework-independent domain | Large mapping and migration effort for simple CRUD; delays correctness | Use only where business complexity warrants it |
| Feature modules with pragmatic Spring adapters and explicit application use cases | Clear ownership, incremental, retains working framework integration | Requires import rules and some shared infrastructure | Selected |
| Keep layer-by-layer folders and generic entity CRUD | Familiar and low effort | Cross-feature access and accidental entity serialization persist | Transitional only |
| New frontend framework/state/form stack | Opportunity to start fresh | Adds migration risk while contracts already disagree | Rejected for this phase |
| Upgrade every package to latest | Reduces version age quickly | Major/pre-release upgrades, compiler changes and API redesign become inseparable | Rejected |

## Decision

Retain Java 21, Spring MVC/Security/Data JPA, Flyway, PostgreSQL, React, TypeScript, React Router, Axios, Formik and Tailwind for the first compatible baseline. Use one npm lockfile and the Maven wrapper. Pin runtime/toolchain versions after a fresh supported-release check. Do not adopt versions shown as milestone/pre-release by Maven's versions plugin. Do not treat React 19, TypeScript 7, Tailwind 4 or a Spring Boot major upgrade as mandatory security fixes without compatibility analysis.

### Backend target structure and enforcement

```text
org.code.irr
  bootstrap/                 # Application wiring, configuration, health
  identity/{api,application,domain,infrastructure}/
  materials/{api,application,domain,infrastructure}/
  intake/{api,application,domain,infrastructure}/
  processing/{api,application,domain,infrastructure}/
  inventory/{application,domain,infrastructure}/
  sales/{api,application,domain,infrastructure}/
  attachments/{api,application,domain,infrastructure}/
  reporting/{application,domain,infrastructure}/
  shared/{errors,clock,ids}/
```

The package root rename is optional and comes after characterization tests. Moving a Java class is JAVA_ONLY only when reflection, component/entity scanning, tests and serialized class names have been checked. Retain physical `@Table`/`@Column` mappings; moving classes must not rename database objects or endpoints.

Application use cases own transactions and coordinate domain rules through explicit ports. Inventory is the sole writer of balance and movement tables; processing/sales request a posting, never inject its repositories. Attachments own storage; reporting consumes durable invalidation events. Controllers authenticate/validate/map requests, call a use case and map responses. No controller returns JPA entities. API DTOs do not inherit entity or Formik types. Map framework Page to a stable API page envelope.

Keep simple JPA aggregates within each module initially. Extract pure domain calculations for quantity, allocation and pricing; no HTTP or storage calls inside them. Place input/output ports in application where they can use application commands and results without depending on controllers. Tests enforce: domain cannot import api/infrastructure; api cannot import repository; modules communicate through public application contracts; no cross-module repository injection; no dependency cycles. Introduce ArchUnit as a test-only dependency only with these meaningful rules. Consider Spring Modulith only if its runtime features are needed, not merely for folder names.

Security uses an explicit allowlist for authentication/health and authenticated default routes, plus method/object authorization. Current custom `BearerFilter` enforces authentication despite `anyRequest().permitAll()`; therefore this audit does **not** claim every route is anonymous. Its broad session-path bypass, duplicate servlet/security registration risk and controller-specific policies still require integration tests. Prefer one Spring Security resource-server chain and JwtDecoder; migrate custom session semantics with token compatibility and expiry/issuer/audience tests. Keep BCrypt. UI role visibility and `jwt-decode` are not signature validation or authorization.

### Frontend target structure and enforcement

```text
src/
  app/                       # Router, providers, configuration
  features/
    session/{api,model,ui}/
    materials/{api,model,ui}/
    fleet/{api,model,ui}/
    intake/{api,model,ui}/
    processing/{api,model,ui}/
    inventory/{api,model,ui}/
    sales/{api,model,ui}/
  shared/
    api/                     # One HTTP client, generated contract, errors
    ui/                      # Accessible reusable controls
    lib/                     # Small pure helpers
```

Features may import shared code and explicitly exported feature contracts; shared code cannot import features. Pages use typed feature APIs, not `apiClient` directly. Remove service duplicates only after reference checks and route smoke tests. Retain useful Formik forms; replace generic `Partial<Entity>` request bodies with feature-specific create/update schemas. Keep UI state local unless a real shared-state requirement exists. Do not add a second server-state library without a concrete cache/invalidation need.

Use one Axios instance and read the current token at request time. Clear both storage and in-memory headers on logout, avoid `Bearer null`, handle 401 centrally and never log a token. The current router logs it and both clients capture initial headers. First migrate without changing bearer transport; separately decide memory access tokens/secure refresh cookies and CSRF/session policies if requirements justify them. Configure API origin via validated `VITE_API_BASE_URL`; frontend variables are public and must contain no secrets. Prefer a same-origin reverse proxy if infrastructure allows it; otherwise use an explicit CORS origin allowlist and test preflight.

### Contract inventory and migration matrix

The frontend base URL is hardcoded to a Railway legacy service. That endpoint was not called. The following compares source contracts, not verified production behavior.

| Feature | Frontend source contract | Backend develop source contract | Migration |
| --- | --- | --- | --- |
| Login | POST `/auth/login`, `{email,senha}` → `{access_token}` | POST `/api/session/authenticate`, `{email,password}` → `{token}` | Explicit auth adapter and typed result |
| Registration | Legacy user/admin flow and Portuguese role values | POST `/api/session/register`, `{fullName,email,password}`; fixed REPRESENTATIVE | Separate protected partner-management endpoint; agree bootstrap policy |
| Fleet | `/veiculos`, `placa`, `modelo`, generic PATCH | `/api/vehicles`, `licensePlate`, `model`, PUT; batch POST/PUT | Field mapping plus per-feature update method |
| Materials | `/tipologias`, `/subtipologias`; two-level legacy types | `/api/materials/categories`, `/types`, `/subtypes`; three levels | Explicit taxonomy mapping; no inferred one-to-one IDs |
| Donors/donations | `/doadores`, `/doacoes`; flat donation fields | `/api/donors`, `/api/donations`; structured input item list | New nested form schema and decimal normalization |
| Sorting/pressing | `/triagens`, `/prensagens`; flat quantities and legacy enums | `/api/sortings`, `/api/pressings`; nested output lists | Explicit workflow models and source allocations |
| Attachments | `/documentos`, `/medias`, generic JSON CRUD | `/api/documents`; multipart field `documento`, download and DELETE | Versioned upload contract and safe metadata DTO |
| Collection/driver | `/coletas`, `/motoristas` | Models/DTOs exist, controllers absent on audited develop | Gate navigation; implement intake/team contracts first |
| Sale/material movement/user CRUD | Legacy services exist | Sale/buyer/movement/user models do not imply public CRUD | Define use cases; do not expose generic balance writes |
| Pagination | `page,limit` → `{data,total}` | `page,size,sort` → Spring Page with `content,totalElements` | Client adapter, zero-based page and bounded size |
| Updates/deletes | PATCH; DELETE expects response body and status 200 | Most editable controllers use PUT; soft DELETE returns 204 | Per-operation methods and `Promise<void>` for 204 |

New `/api/v1` contract: UUID strings, English camelCase fields, RFC 3339 timestamps with offsets, decimal strings with explicit unit suffixes, `page` zero-based, `size` default 20 and maximum 100, allowlisted sorts, and a stable `{items,page,size,totalItems,totalPages}` envelope. Use create/update DTOs separately. Version update requests carry a concurrency token/ETag when required, with 409/412 documented consistently. Errors use one Problem Details shape with stable machine code, field errors and correlation ID; 401 unauthenticated, 403 unauthorized, 404 inaccessible/missing resource where policy requires concealment, 409 conflict, 422 valid syntax but invalid business transition, 400 malformed/DTO validation. Preserve old error/status semantics only in the adapter.

Add maintained OpenAPI in the backend, validate it in CI, generate frontend types/client deterministically from a pinned schema artifact, and run breaking-change comparison. Generated types do not validate runtime data; contract integration tests must verify real serialization, decimal handling, enum values, authorization and multipart behavior. Do not hand-copy persistence entities into frontend types. Version the schema independently of deployment and link its exact commit/hash in the release manifest.

### English naming and compatibility classification

| Current concept | Target concept | Impact | Compatibility rule |
| --- | --- | --- | --- |
| `org.code.api` root; scattered service packages | Feature modules under the chosen application root | JAVA_ONLY after scanning/reflection review | Preserve wire names and explicit JPA mappings |
| `armazenar`, `deletar`, upload/download method names | `store`, `delete`, `upload`, `download` | JAVA_ONLY for private/internal methods | Update callers; preserve routes until adapter migration |
| `senha`, `nome`, `placa`, `modelo`, `pesagem` | `password`, `fullName`/`name`, `licensePlate`, `model`, `weightKg` | API_IMPACT | Per-DTO mapping, not global text replacement; preserve semantic differences |
| `criadorId` | `creatorId` | API_IMPACT | Actor remains separate from organization ownership |
| Legacy typology/subtype | `materialCategory`/`materialType`/`materialSubtype` | API_IMPACT and possible DATABASE_IMPACT | Three-level taxonomy needs an approved ID/hierarchy map |
| `PF`/`PJ` donor values | `INDIVIDUAL`/`LEGAL_ENTITY` in v1 | API_IMPACT; DATABASE_IMPACT only if persisted enum values change | Translate at DTO boundary first; do not rename stored values in place |
| Legacy Portuguese role values | `ADMINISTRATOR`, `CITY_HALL`, `ORGANIZATION`, `REPRESENTATIVE` | API_IMPACT and token compatibility | Map legacy tokens explicitly; do not infer role from display label |
| `destination_type/destination_id` | Typed allocation links | DATABASE_IMPACT and API_IMPACT | Add links, backfill, observe, then retire legacy fields |
| `API_HOST` | `VITE_API_BASE_URL` | CONFIGURATION_IMPACT | Validate build-time public config; update deployment and rollback artifacts |

All newly authored prose and implementation comments are English. Legacy wire identifiers and persisted enum values above are quoted solely to define their migration boundary. Do not translate physical table/column names merely for style when explicit mappings preserve compatibility.

## Dependency audit method and interpretation

Frontend direct and transitive versions come from committed `package-lock.json`. Source/config import scanning supplies candidate usage evidence, not proof of runtime reachability. `npm audit --json --package-lock-only` queried the live registry and reports **28 affected package entries: 1 critical, 17 high, 6 moderate, 4 low**. Those counts include development/transitive/metavulnerability entries, not 28 distinct exploitable production CVEs. `npm outdated --json` was run before install; use lockfile versions as the authoritative current column, and registry `wanted/latest` as a timestamped snapshot.

Backend Maven tree resolves **89 compile/runtime coordinates**. OSV querybatch matched advisories on **19 coordinates**; counts include optional DevTools and transitive libraries and are not reachability verdicts. Test/plugin dependencies and container OS packages are outside that OSV query. Maven `dependency:analyze` marks reflection/autoconfiguration starters and JDBC/Flyway as apparently unused: they must not be removed on that basis. Some used-but-undeclared artifacts are intentionally provided by starters/BOM; direct Nimbus use should either be declared explicitly or removed behind Spring JWT abstractions.

Critical transitive `form-data` and Axios Node-adapter advisories require distinguishing browser bundle exposure from Node build/runtime usage. React Router framework/SSR-specific advisories do not establish that this Vite SPA runs a vulnerable server mode. Vite dev-server advisories matter because `start` binds all interfaces; they do not imply the static output serves arbitrary filesystem files. Keep a documented affected/not-affected/under-investigation disposition per advisory and recheck after upgrades.

### Dependency actions

| Action | Rationale | Verification gate |
| --- | --- | --- |
| Remove frontend `front: file:` self-dependency | Self-reference adds no application capability and complicates lockfile topology | Regenerate lockfile, clean install, route build |
| Remove `dotenv` if no Node config consumer is introduced | Current Vite client config does not import it; public config uses Vite env | Source/config search and production base-URL test |
| Remove vanilla `lucide`, retain `lucide-react` | Duplicate icon ecosystems; React imports use the React package | Scan imports and icon-render smoke test |
| Retain `jwt-decode` and `zustand` | `Profile/UserDetails.tsx` decodes claims; `utils/FilterState.ts` uses Zustand | Decode only for display; authorization stays on the server; consolidate state deliberately |
| Move `typescript` to devDependencies | Compiler/tooling rather than browser runtime | Build pipeline installs development dependencies before producing static assets |
| Introduce `typescript-eslint` correctly | Current ESLint configuration uses the wrong package and excludes TS/TSX | Flat config loads; TS/TSX lint actually executes |
| Upgrade Axios/Router/Vite and affected transitives in compatible batches | Registry advisory matches on locked versions | Audit after lock regeneration, build/type/lint, auth/navigation/upload tests |
| Align Flyway module with core via Boot dependency management | 11.7.2 core vs explicit 10.17.0 PostgreSQL module | Effective POM/tree convergence and real database migration rehearsal |
| Upgrade supported Spring Boot patch baseline as a unit | Spring/Tomcat/Jackson/security advisories are BOM-managed | Package/unit/integration/security/upgrade tests; no arbitrary individual pins |
| Keep `spring-security-oauth2-jose` while code directly uses JWT classes | Direct declaration documents real usage despite transitive inclusion | Remove only after code migration and dependency verification |
| Keep JPA, validation, security, web, JDBC driver and Flyway | Runtime discovery defeats simple unused scans | Application startup and representative requests/migrations |
| Keep optional DevTools development-only; exclude from release artifact | Developer convenience is not production functionality | Inspect packaged JAR and release classpath |
| Retain Lombok optional and excluded from runtime JAR | Compilation annotations/processor are in use | Java 21 compile and test; no unrelated Lombok removal rewrite |
| Add PostgreSQL integration fixtures, architecture/contract tooling in test/dev scope | Existing tests do not prove clean startup or first-write correctness | Each new package supports a concrete gate; no speculative infrastructure |

No `npm audit fix --force`, blanket major upgrade, lockfile deletion or applied-migration rewrite belongs to this plan. Resolve high-risk compatible updates first, then deliberate major migrations in separate decisions. Pin npm/Node and Java/Maven in CI and deployment; current Docker `node:latest`, `npm install` and Vite preview are not a reproducible production serving strategy. Use a build stage with `npm ci` and a pinned static HTTP runtime that supports SPA fallback; validate actual port/listen address.

## Operational configuration inventory

| Setting | Current evidence | Target |
| --- | --- | --- |
| DB_HOST/PORT/NAME/USER/PASSWORD | Property fallbacks and Compose development values; no secrets reproduced | Required production env, separate migration/runtime privileges |
| `spring.jpa.hibernate.ddl-auto` | `validate` on develop | Retain validate; never update/create in production |
| `spring.flyway.locations` | `classpath:db/migrations` | Retain explicit path and immutable checksums |
| `spring.flyway.baseline-on-migrate` | true | Explicit controlled baseline only after catalog validation |
| `rsa.publickey/privatekey` | Classpath resources, absent in clean clone | External production key material; ephemeral generated test keys |
| `server.port` / Compose | 9191 vs published 8081 | One env-driven port and health check |
| Mappings/debug/SQL logging | Enabled in shared configuration | Development profile only; redact tokens and personal data |
| CORS | Wildcard origin patterns with credentials | Explicit environment allowlist and preflight tests |
| API_HOST | Hardcoded Railway origin | Validated public Vite env or same-origin proxy |
| Attachment storage | Mock directory under OS user home | Configured development directory and durable production storage |

## Validation, consequences and follow-up

Baseline commands: `npm ci --ignore-scripts --no-audit --no-fund`, `npm run build`, `npm run lint`, `tsc --noEmit -p tsconfig.app.json`; `mvn -B -DskipTests package`, selected unit tests, `dependency:tree`, `dependency:list`, `dependency:analyze`, `versions:display-dependency-updates`; npm registry audit and OSV querybatch. JDK 21.0.10 was explicitly selected. Logs and scan results were kept in audit scratch space; the durable findings and version inventories are below. No real production credentials or database were used.

Positive: modules and generated contracts constrain drift, tools become release gates, and upgrade decisions are grounded in resolved versions. Negative: repairing 519 existing compiler diagnostics and generic form assumptions takes real effort; declaring strict gates before repair may initially block feature merges. Introduce a short tracked repair milestone rather than disabling strictness or hiding diagnostics. Do not normalize failure as a permanent allowed CI result.

Backend required gates: isolated unit/security tests; disposable PostgreSQL startup, migration and concurrency tests; OpenAPI compatibility; architecture/import rules; resolved-dependency and image scans. Frontend required gates: clean install, TS/TSX lint, strict type check, production bundle, component/form tests, real API contract tests and representative browser E2E. No CI workflow files are present in either audited tree. Creating branch names alone is not CI implementation.

## References

- [Spring security advisories](https://spring.io/security/) for matching resolved Spring versions and exposure conditions.
- [OSV querybatch API](https://google.github.io/osv.dev/api/#tag/api/operation/OSV_QueryAffectedBatch) for coordinate/version matching used in this audit.
- [npm audit](https://docs.npmjs.com/cli/v11/commands/npm-audit) for audit scope and advisory interpretation.
- [Vite TypeScript](https://vite.dev/guide/features.html#typescript) for the distinction between transpilation and type checking.
- [Spring Boot security](https://docs.spring.io/spring-boot/3.5/reference/web/spring-security.html) for framework security integration.

## Appendix A: Direct frontend dependency inventory

Locked means committed lockfile resolution. Wanted/latest values are registry observations, not upgrade approvals. Import counts below inspect tracked application/config text and may include type-only references; tooling has configuration/CLI use even when no source import appears.

| Package | Scope | Declared | Locked | Wanted | Latest observed | Reference files | Disposition |
| --- | --- | --- | --- | --- | --- | --- | --- |
| axios | runtime | ^1.7.9 | 1.7.9 | 1.20.0 | 1.20.0 | src/config/security/ApiClient.ts, src/config/security/Client.ts, src/pages/ScreeningTuple.jsx | Retain one HTTP client; security update |
| dotenv | runtime | ^16.4.7 | 16.4.7 | 16.6.1 | 18.0.3 | CLI/config/transitive use must be reviewed | Remove candidate: no import/config usage |
| formik | runtime | ^2.4.6 | 2.4.6 | 2.4.9 | 2.4.9 | src/components/InputList.tsx, src/components/InputText.tsx, src/components/SimpleForm.tsx, src/components/generics/CreateForm.tsx, src/components/generics/InputField.tsx, src/components/generics/UpdateForm.tsx, src/models/Entities.ts, src/pages/Profile/UserEdit.tsx | Retain; feature-specific form models |
| front | runtime | file: | local link | same/not reported | same/not reported | CLI/config/transitive use must be reviewed | Remove self-reference |
| jwt-decode | runtime | ^4.0.0 | 4.0.0 | same/not reported | same/not reported | src/pages/Profile/UserDetails.tsx | Retain; display claims only |
| lucide | runtime | ^0.476.0 | 0.476.0 | 0.476.0 | 1.47.0 | CLI/config/transitive use must be reviewed | Remove duplicate vanilla icons |
| lucide-react | runtime | ^0.476.0 | 0.476.0 | 0.476.0 | 1.47.0 | src/components/FilterDropdown.tsx, src/components/Sidebar.tsx, src/components/TableComponent.tsx, src/pages/FilterTest.tsx | Retain React icons |
| react | runtime | ^18.3.1 | 18.3.1 | 18.3.1 | 19.3.0 | eslint.config.js, src/components/DeleteEntityModal.tsx, src/components/FilterDropdown.tsx, src/components/Header.tsx, src/components/InputFile.tsx, src/components/MenuIcon.tsx, src/components/Page.tsx, src/components/Sidebar.tsx, src/components/generics/List.tsx, src/components/generics/UpdateForm.tsx, src/components/generics/ViewForm.tsx, src/pages/CollectionPages/CollectionCreate.tsx, src/pages/CollectionPages/CollectionList.tsx, src/pages/CollectionPages/CollectionUpdate.tsx, src/pages/DocumentPages/DocumentCreate.tsx, src/pages/DocumentPages/DocumentList.tsx, src/pages/DocumentPages/DocumentUpdate.tsx, src/pages/DonationPages/DonationCreate.tsx, src/pages/DonationPages/DonationList.tsx, src/pages/DonationPages/DonationUpdate.tsx, src/pages/DonatorPages/DonatorCreate.tsx, src/pages/DonatorPages/DonatorList.tsx, src/pages/DonatorPages/DonatorUpdate.tsx, src/pages/DriverPages/DriverCreate.tsx, src/pages/DriverPages/DriverList.tsx, src/pages/DriverPages/DriverUpdate.tsx, src/pages/FilterTest.tsx, src/pages/InputMaterialPages/InputMaterialCreate.tsx, src/pages/InputMaterialPages/InputMaterialList.tsx, src/pages/InputMaterialPages/InputMaterialUpdate.tsx, src/pages/OutputMaterialPages/OutputMaterialCreate.tsx, src/pages/OutputMaterialPages/OutputMaterialList.tsx, src/pages/OutputMaterialPages/OutputMaterialUpdate.tsx, src/pages/PressingPages/PressingCreate.tsx, src/pages/PressingPages/PressingList.tsx, src/pages/PressingPages/PressingUpdate.tsx, src/pages/Profile/UserDetails.tsx, src/pages/Profile/UserEdit.tsx, src/pages/SalesPages/SalesCreate.tsx, src/pages/SalesPages/SalesList.tsx, src/pages/SalesPages/SalesUpdate.tsx, src/pages/ScreeningTuple.jsx, src/pages/SubTypologyPages/SubTypologyCreate.tsx, src/pages/SubTypologyPages/SubTypologyList.tsx, src/pages/SubTypologyPages/SubtypologyUpdate.tsx, src/pages/TriagePages/TriageCreate.tsx, src/pages/TriagePages/TriageList.tsx, src/pages/TriagePages/TriageUpdate.tsx, src/pages/TypologyPages/TypologyCreate.tsx, src/pages/TypologyPages/TypologyList.tsx, src/pages/TypologyPages/TypologyUpdate.tsx, src/pages/UsersPages/ManageUsers.tsx, src/pages/VehiclePages/VehicleCreate.tsx, src/pages/VehiclePages/VehicleList.tsx, src/pages/VehiclePages/VehicleUpdate.tsx, src/route/Router.tsx | Retain; separate major migration |
| react-dom | runtime | ^18.3.1 | 18.3.1 | 18.3.1 | 19.3.0 | src/main.jsx | Keep aligned with React |
| react-router-dom | runtime | ^7.0.2 | 7.1.5 | 7.18.4 | 7.18.4 | src/components/BackButton.tsx, src/components/TableComponent.tsx, src/pages/CollectionPages/CollectionCreate.tsx, src/pages/CollectionPages/CollectionList.tsx, src/pages/CollectionPages/CollectionUpdate.tsx, src/pages/DocumentPages/DocumentCreate.tsx, src/pages/DocumentPages/DocumentList.tsx, src/pages/DocumentPages/DocumentUpdate.tsx, src/pages/DonationPages/DonationCreate.tsx, src/pages/DonationPages/DonationList.tsx, src/pages/DonationPages/DonationUpdate.tsx, src/pages/DonatorPages/DonatorCreate.tsx, src/pages/DonatorPages/DonatorList.tsx, src/pages/DonatorPages/DonatorUpdate.tsx, src/pages/DriverPages/DriverCreate.tsx, src/pages/DriverPages/DriverList.tsx, src/pages/DriverPages/DriverUpdate.tsx, src/pages/InputMaterialPages/InputMaterialCreate.tsx, src/pages/InputMaterialPages/InputMaterialList.tsx, src/pages/InputMaterialPages/InputMaterialUpdate.tsx, src/pages/OutputMaterialPages/OutputMaterialCreate.tsx, src/pages/OutputMaterialPages/OutputMaterialList.tsx, src/pages/OutputMaterialPages/OutputMaterialUpdate.tsx, src/pages/PressingPages/PressingCreate.tsx, src/pages/PressingPages/PressingList.tsx, src/pages/PressingPages/PressingUpdate.tsx, src/pages/Profile/UserDetails.tsx, src/pages/Profile/UserEdit.tsx, src/pages/SalesPages/SalesCreate.tsx, src/pages/SalesPages/SalesList.tsx, src/pages/SalesPages/SalesUpdate.tsx, src/pages/SubTypologyPages/SubTypologyCreate.tsx, src/pages/SubTypologyPages/SubTypologyList.tsx, src/pages/SubTypologyPages/SubtypologyUpdate.tsx, src/pages/TriagePages/TriageCreate.tsx, src/pages/TriagePages/TriageList.tsx, src/pages/TriagePages/TriageUpdate.tsx, src/pages/TypologyPages/TypologyCreate.tsx, src/pages/TypologyPages/TypologyList.tsx, src/pages/TypologyPages/TypologyUpdate.tsx, src/pages/UsersPages/RegisterUserAdmin.tsx, src/pages/VehiclePages/VehicleCreate.tsx, src/pages/VehiclePages/VehicleList.tsx, src/pages/VehiclePages/VehicleUpdate.tsx, src/route/Router.tsx, src/route/RoutesList.tsx | Retain; compatible advisory remediation |
| react-toastify | runtime | ^11.0.1 | 11.0.3 | 11.1.0 | 11.1.0 | src/App.tsx, src/components/TableComponent.tsx, src/components/generics/List.tsx, src/pages/CollectionPages/CollectionCreate.tsx, src/pages/CollectionPages/CollectionUpdate.tsx, src/pages/DocumentPages/DocumentCreate.tsx, src/pages/DocumentPages/DocumentUpdate.tsx, src/pages/DonationPages/DonationCreate.tsx, src/pages/DonationPages/DonationUpdate.tsx, src/pages/DonatorPages/DonatorCreate.tsx, src/pages/DonatorPages/DonatorUpdate.tsx, src/pages/DriverPages/DriverCreate.tsx, src/pages/DriverPages/DriverUpdate.tsx, src/pages/InputMaterialPages/InputMaterialCreate.tsx, src/pages/InputMaterialPages/InputMaterialUpdate.tsx, src/pages/Login.tsx, src/pages/OutputMaterialPages/OutputMaterialCreate.tsx, src/pages/OutputMaterialPages/OutputMaterialUpdate.tsx, src/pages/PressingPages/PressingCreate.tsx, src/pages/PressingPages/PressingUpdate.tsx, src/pages/SalesPages/SalesCreate.tsx, src/pages/SalesPages/SalesUpdate.tsx, src/pages/SubTypologyPages/SubTypologyCreate.tsx, src/pages/SubTypologyPages/SubtypologyUpdate.tsx, src/pages/TriagePages/TriageCreate.tsx, src/pages/TriagePages/TriageUpdate.tsx, src/pages/TypologyPages/TypologyCreate.tsx, src/pages/TypologyPages/TypologyUpdate.tsx, src/pages/UsersPages/ManageUsers.tsx, src/pages/UsersPages/RegisterUserAdmin.tsx, src/pages/VehiclePages/VehicleCreate.tsx, src/pages/VehiclePages/VehicleUpdate.tsx, src/route/Router.tsx, src/utils/ExportService.ts | Retain user feedback |
| typescript | runtime | ^5.8.2 | 5.8.2 | 5.9.3 | 7.0.2 | eslint.config.js | Move to devDependencies; repair checks |
| zustand | runtime | ^5.0.3 | 5.0.3 | 5.0.15 | 5.0.15 | src/utils/FilterState.ts | Retain FilterState; constrain shared state |
| @eslint/js | dev | ^9.15.0 | 9.19.0 | 9.39.5 | 10.0.1 | eslint.config.js | Retain build/lint/style tooling; update compatibly |
| @types/react | dev | ^18.3.12 | 18.3.18 | 18.3.31 | 19.3.0 | CLI/config/transitive use must be reviewed | Retain build/lint/style tooling; update compatibly |
| @types/react-dom | dev | ^18.3.1 | 18.3.5 | 18.3.7 | 19.3.0 | CLI/config/transitive use must be reviewed | Retain build/lint/style tooling; update compatibly |
| @vitejs/plugin-react | dev | ^4.3.4 | 4.3.4 | 4.7.0 | 6.1.1 | vite.config.js | Retain build/lint/style tooling; update compatibly |
| autoprefixer | dev | ^10.4.20 | 10.4.20 | 10.6.1 | 10.6.1 | CLI/config/transitive use must be reviewed | Retain build/lint/style tooling; update compatibly |
| eslint | dev | ^9.15.0 | 9.19.0 | 9.39.5 | 10.11.0 | CLI/config/transitive use must be reviewed | Retain build/lint/style tooling; update compatibly |
| eslint-plugin-react | dev | ^7.37.2 | 7.37.4 | 7.37.5 | 7.37.5 | eslint.config.js | Retain build/lint/style tooling; update compatibly |
| eslint-plugin-react-hooks | dev | ^5.0.0 | 5.1.0 | 5.2.0 | 7.1.1 | eslint.config.js | Retain build/lint/style tooling; update compatibly |
| eslint-plugin-react-refresh | dev | ^0.4.14 | 0.4.18 | 0.4.26 | 0.5.7 | eslint.config.js | Retain build/lint/style tooling; update compatibly |
| globals | dev | ^15.12.0 | 15.14.0 | 15.15.0 | 17.12.0 | eslint.config.js | Retain build/lint/style tooling; update compatibly |
| postcss | dev | ^8.4.49 | 8.5.1 | 8.5.28 | 8.5.28 | CLI/config/transitive use must be reviewed | Retain build/lint/style tooling; update compatibly |
| tailwindcss | dev | ^3.4.17 | 3.4.17 | 3.4.19 | 4.3.3 | tailwind.config.js | Retain build/lint/style tooling; update compatibly |
| vite | dev | ^6.0.1 | 6.0.11 | 6.4.3 | 8.3.0 | src/vite-env.d.ts, vite.config.js | Retain build/lint/style tooling; update compatibly |

## Appendix B: npm advisory inventory

One row per affected package entry. Linked advisories are the direct advisory IDs reported by npm; transitive chains are listed by name. Fix availability is a registry suggestion, not proof that an upgrade is compatible.

| Package | Locked | Severity | Direct | Advisories or affected dependencies |
| --- | --- | --- | --- | --- |
| @babel/core | 7.26.7 | low | no | [GHSA-4x5r-pxfx-6jf8](https://github.com/advisories/GHSA-4x5r-pxfx-6jf8) |
| @babel/helpers | 7.26.7 | moderate | no | [GHSA-968p-4wvh-cqc8](https://github.com/advisories/GHSA-968p-4wvh-cqc8) |
| @eslint/plugin-kit | 0.2.5 | low | no | [GHSA-xffm-g5w8-qvg7](https://github.com/advisories/GHSA-xffm-g5w8-qvg7) |
| @humanfs/node | 0.16.6 | moderate | no | [GHSA-p498-v437-472g](https://github.com/advisories/GHSA-p498-v437-472g) |
| ajv | 6.12.6 | moderate | no | [GHSA-2g4f-4pwh-qvx6](https://github.com/advisories/GHSA-2g4f-4pwh-qvx6) |
| axios | 1.7.9 | high | yes | [GHSA-jr5f-v2jv-69x6](https://github.com/advisories/GHSA-jr5f-v2jv-69x6); [GHSA-4hjh-wcwx-xvwj](https://github.com/advisories/GHSA-4hjh-wcwx-xvwj); [GHSA-3p68-rc4w-qgx5](https://github.com/advisories/GHSA-3p68-rc4w-qgx5); [GHSA-w9j2-pvgh-6h63](https://github.com/advisories/GHSA-w9j2-pvgh-6h63); [GHSA-pmwg-cvhr-8vh7](https://github.com/advisories/GHSA-pmwg-cvhr-8vh7); [GHSA-3w6x-2g7m-8v23](https://github.com/advisories/GHSA-3w6x-2g7m-8v23); [GHSA-xhjh-pmcv-23jw](https://github.com/advisories/GHSA-xhjh-pmcv-23jw); [GHSA-445q-vr5w-6q77](https://github.com/advisories/GHSA-445q-vr5w-6q77); [GHSA-m7pr-hjqh-92cm](https://github.com/advisories/GHSA-m7pr-hjqh-92cm); [GHSA-5c9x-8gcm-mpgx](https://github.com/advisories/GHSA-5c9x-8gcm-mpgx); [GHSA-vf2m-468p-8v99](https://github.com/advisories/GHSA-vf2m-468p-8v99); [GHSA-pf86-5x62-jrwf](https://github.com/advisories/GHSA-pf86-5x62-jrwf); [GHSA-6chq-wfr3-2hj9](https://github.com/advisories/GHSA-6chq-wfr3-2hj9); [GHSA-xx6v-rp6x-q39c](https://github.com/advisories/GHSA-xx6v-rp6x-q39c); [GHSA-43fc-jf86-j433](https://github.com/advisories/GHSA-43fc-jf86-j433); [GHSA-q8qp-cvcw-x6jj](https://github.com/advisories/GHSA-q8qp-cvcw-x6jj); [GHSA-fvcv-3m26-pcqx](https://github.com/advisories/GHSA-fvcv-3m26-pcqx); [GHSA-62hf-57xw-28j9](https://github.com/advisories/GHSA-62hf-57xw-28j9); [GHSA-hfxv-24rg-xrqf](https://github.com/advisories/GHSA-hfxv-24rg-xrqf); [GHSA-777c-7fjr-54vf](https://github.com/advisories/GHSA-777c-7fjr-54vf); [GHSA-p92q-9vqr-4j8v](https://github.com/advisories/GHSA-p92q-9vqr-4j8v); [GHSA-j5f8-grm9-p9fc](https://github.com/advisories/GHSA-j5f8-grm9-p9fc); [GHSA-3g43-6gmg-66jw](https://github.com/advisories/GHSA-3g43-6gmg-66jw); [GHSA-35jp-ww65-95wh](https://github.com/advisories/GHSA-35jp-ww65-95wh); [GHSA-898c-q2cr-xwhg](https://github.com/advisories/GHSA-898c-q2cr-xwhg); [GHSA-mmx7-hfxf-jppx](https://github.com/advisories/GHSA-mmx7-hfxf-jppx); [GHSA-pmv8-rq9r-6j72](https://github.com/advisories/GHSA-pmv8-rq9r-6j72); [GHSA-7q8q-rj6j-mhjq](https://github.com/advisories/GHSA-7q8q-rj6j-mhjq); [GHSA-jqh4-m9w3-8hp9](https://github.com/advisories/GHSA-jqh4-m9w3-8hp9); [GHSA-42h9-826w-cgv3](https://github.com/advisories/GHSA-42h9-826w-cgv3) |
| brace-expansion | 1.1.11 | high | no | [GHSA-v6h2-p8h4-qcjw](https://github.com/advisories/GHSA-v6h2-p8h4-qcjw); [GHSA-v6h2-p8h4-qcjw](https://github.com/advisories/GHSA-v6h2-p8h4-qcjw); [GHSA-f886-m6hf-6m8v](https://github.com/advisories/GHSA-f886-m6hf-6m8v); [GHSA-f886-m6hf-6m8v](https://github.com/advisories/GHSA-f886-m6hf-6m8v); [GHSA-3jxr-9vmj-r5cp](https://github.com/advisories/GHSA-3jxr-9vmj-r5cp); [GHSA-3jxr-9vmj-r5cp](https://github.com/advisories/GHSA-3jxr-9vmj-r5cp); [GHSA-mh99-v99m-4gvg](https://github.com/advisories/GHSA-mh99-v99m-4gvg); [GHSA-mh99-v99m-4gvg](https://github.com/advisories/GHSA-mh99-v99m-4gvg); [GHSA-rgw5-rvv9-x895](https://github.com/advisories/GHSA-rgw5-rvv9-x895); [GHSA-rgw5-rvv9-x895](https://github.com/advisories/GHSA-rgw5-rvv9-x895) |
| browserslist | 4.24.4 | high | no | [GHSA-c83g-rgw3-j3cx](https://github.com/advisories/GHSA-c83g-rgw3-j3cx); [GHSA-73wf-gq98-2v4g](https://github.com/advisories/GHSA-73wf-gq98-2v4g) |
| esbuild | 0.24.2 | moderate | no | [GHSA-67mh-4wv8-2f99](https://github.com/advisories/GHSA-67mh-4wv8-2f99) |
| eslint | 9.19.0 | low | yes | @eslint/plugin-kit |
| flatted | 3.3.2 | high | no | [GHSA-25h7-pfq9-p65f](https://github.com/advisories/GHSA-25h7-pfq9-p65f); [GHSA-rf6f-7fwh-wjgh](https://github.com/advisories/GHSA-rf6f-7fwh-wjgh) |
| follow-redirects | 1.15.9 | moderate | no | [GHSA-r4q5-vmmm-2653](https://github.com/advisories/GHSA-r4q5-vmmm-2653) |
| form-data | 4.0.1 | critical | no | [GHSA-fjxv-7rqg-78g4](https://github.com/advisories/GHSA-fjxv-7rqg-78g4); [GHSA-hmw2-7cc7-3qxx](https://github.com/advisories/GHSA-hmw2-7cc7-3qxx) |
| glob | 10.4.5 | high | no | [GHSA-5j98-mcp5-4vw2](https://github.com/advisories/GHSA-5j98-mcp5-4vw2) |
| js-yaml | 4.1.0 | high | no | [GHSA-mh29-5h37-fv8m](https://github.com/advisories/GHSA-mh29-5h37-fv8m); [GHSA-h67p-54hq-rp68](https://github.com/advisories/GHSA-h67p-54hq-rp68); [GHSA-52cp-r559-cp3m](https://github.com/advisories/GHSA-52cp-r559-cp3m); [GHSA-5p4m-2wfm-xmqj](https://github.com/advisories/GHSA-5p4m-2wfm-xmqj); [GHSA-2883-xcg3-v3hh](https://github.com/advisories/GHSA-2883-xcg3-v3hh) |
| lodash | 4.17.21 | high | no | [GHSA-r5fr-rjxr-66jc](https://github.com/advisories/GHSA-r5fr-rjxr-66jc); [GHSA-f23m-r3pf-42rh](https://github.com/advisories/GHSA-f23m-r3pf-42rh); [GHSA-xxjr-mmjv-4gpg](https://github.com/advisories/GHSA-xxjr-mmjv-4gpg) |
| lodash-es | 4.17.21 | high | no | [GHSA-r5fr-rjxr-66jc](https://github.com/advisories/GHSA-r5fr-rjxr-66jc); [GHSA-f23m-r3pf-42rh](https://github.com/advisories/GHSA-f23m-r3pf-42rh); [GHSA-xxjr-mmjv-4gpg](https://github.com/advisories/GHSA-xxjr-mmjv-4gpg) |
| minimatch | 3.1.2 | high | no | [GHSA-3ppc-4f35-3m26](https://github.com/advisories/GHSA-3ppc-4f35-3m26); [GHSA-3ppc-4f35-3m26](https://github.com/advisories/GHSA-3ppc-4f35-3m26); [GHSA-7r86-cg39-jmmj](https://github.com/advisories/GHSA-7r86-cg39-jmmj); [GHSA-7r86-cg39-jmmj](https://github.com/advisories/GHSA-7r86-cg39-jmmj); [GHSA-23c5-xmqv-rm74](https://github.com/advisories/GHSA-23c5-xmqv-rm74); [GHSA-23c5-xmqv-rm74](https://github.com/advisories/GHSA-23c5-xmqv-rm74) |
| nanoid | 3.3.8 | high | no | [GHSA-28wg-ghj8-5hjv](https://github.com/advisories/GHSA-28wg-ghj8-5hjv); [GHSA-2v37-7h3g-55p8](https://github.com/advisories/GHSA-2v37-7h3g-55p8); [GHSA-xwg4-73v4-xw9w](https://github.com/advisories/GHSA-xwg4-73v4-xw9w) |
| picomatch | 2.3.1 | high | no | [GHSA-3v7f-55p6-f55p](https://github.com/advisories/GHSA-3v7f-55p6-f55p); [GHSA-c2c7-rcm5-vvqj](https://github.com/advisories/GHSA-c2c7-rcm5-vvqj) |
| postcss | 8.5.1 | high | yes | [GHSA-qx2v-qp2m-jg93](https://github.com/advisories/GHSA-qx2v-qp2m-jg93); [GHSA-6g55-p6wh-862q](https://github.com/advisories/GHSA-6g55-p6wh-862q); [GHSA-fxqj-rqcc-2cmp](https://github.com/advisories/GHSA-fxqj-rqcc-2cmp); [GHSA-r28c-9q8g-f849](https://github.com/advisories/GHSA-r28c-9q8g-f849) |
| postcss-selector-parser | 6.1.2 | low | no | [GHSA-w9m9-85wc-3x92](https://github.com/advisories/GHSA-w9m9-85wc-3x92) |
| react-router | 7.1.5 | high | no | [GHSA-2w69-qvjg-hvjx](https://github.com/advisories/GHSA-2w69-qvjg-hvjx); [GHSA-9jcx-v3wj-wh4m](https://github.com/advisories/GHSA-9jcx-v3wj-wh4m); [GHSA-cpj6-fhp6-mr6j](https://github.com/advisories/GHSA-cpj6-fhp6-mr6j); [GHSA-49rj-9fvp-4h2h](https://github.com/advisories/GHSA-49rj-9fvp-4h2h); [GHSA-8x6r-g9mw-2r78](https://github.com/advisories/GHSA-8x6r-g9mw-2r78); [GHSA-rxv8-25v2-qmq8](https://github.com/advisories/GHSA-rxv8-25v2-qmq8); [GHSA-h5cw-625j-3rxh](https://github.com/advisories/GHSA-h5cw-625j-3rxh); [GHSA-wrjc-x8rr-h8h6](https://github.com/advisories/GHSA-wrjc-x8rr-h8h6); [GHSA-337j-9hxr-rhxg](https://github.com/advisories/GHSA-337j-9hxr-rhxg); [GHSA-chx6-hx7r-mcp5](https://github.com/advisories/GHSA-chx6-hx7r-mcp5); [GHSA-2j2x-hqr9-3h42](https://github.com/advisories/GHSA-2j2x-hqr9-3h42); [GHSA-8v8x-cx79-35w7](https://github.com/advisories/GHSA-8v8x-cx79-35w7); [GHSA-3cgp-3xvw-98x8](https://github.com/advisories/GHSA-3cgp-3xvw-98x8); turbo-stream |
| react-router-dom | 7.1.5 | high | yes | react-router |
| rollup | 4.34.2 | high | no | [GHSA-mw96-cpmx-2vgc](https://github.com/advisories/GHSA-mw96-cpmx-2vgc) |
| turbo-stream | 2.4.0 | high | no | [GHSA-rxv8-25v2-qmq8](https://github.com/advisories/GHSA-rxv8-25v2-qmq8) |
| vite | 6.0.11 | high | yes | [GHSA-x574-m823-4x7w](https://github.com/advisories/GHSA-x574-m823-4x7w); [GHSA-356w-63v5-8wf4](https://github.com/advisories/GHSA-356w-63v5-8wf4); [GHSA-859w-5945-r5v3](https://github.com/advisories/GHSA-859w-5945-r5v3); [GHSA-xcj6-pq6g-qj4x](https://github.com/advisories/GHSA-xcj6-pq6g-qj4x); [GHSA-g4jq-h2w9-997c](https://github.com/advisories/GHSA-g4jq-h2w9-997c); [GHSA-jqfw-vq24-v9c3](https://github.com/advisories/GHSA-jqfw-vq24-v9c3); [GHSA-93m4-6634-74q7](https://github.com/advisories/GHSA-93m4-6634-74q7); [GHSA-4r4m-qw57-chr8](https://github.com/advisories/GHSA-4r4m-qw57-chr8); [GHSA-4w7w-66w2-5vf9](https://github.com/advisories/GHSA-4w7w-66w2-5vf9); [GHSA-p9ff-h696-f583](https://github.com/advisories/GHSA-p9ff-h696-f583); [GHSA-v6wh-96g9-6wx3](https://github.com/advisories/GHSA-v6wh-96g9-6wx3); [GHSA-fx2h-pf6j-xcff](https://github.com/advisories/GHSA-fx2h-pf6j-xcff); esbuild |
| yaml | 2.7.0 | moderate | no | [GHSA-48c2-rrv3-qjmp](https://github.com/advisories/GHSA-48c2-rrv3-qjmp) |

## Appendix C: Direct backend dependency inventory

Parent: `org.springframework.boot:spring-boot-starter-parent:3.5.6`; Java release 21. Test versions follow the same BOM.

| Coordinate | Declared version source | Resolved | Scope | Optional | Decision |
| --- | --- | --- | --- | --- | --- |
| org.springframework.boot:spring-boot-starter-validation | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.springframework.boot:spring-boot-starter-actuator | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.springframework.boot:spring-boot-starter-data-jpa | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.springframework.boot:spring-boot-starter-security | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.springframework.security:spring-security-oauth2-jose | Boot BOM | 6.5.5 | compile | no | Keep direct JWT dependency while used |
| org.springframework.boot:spring-boot-starter-web | Boot BOM | 3.5.6 | compile | no | Retain; BOM-compatible security remediation |
| org.flywaydb:flyway-core | Boot BOM | 11.7.2 | compile | no | Retain; BOM-compatible security remediation |
| org.flywaydb:flyway-database-postgresql | 10.17.0 | 10.17.0 | compile | no | Remove explicit version override; align module/core |
| org.springframework.boot:spring-boot-devtools | Boot BOM | 3.5.6 | runtime | yes | Development only; verify production exclusion |
| org.postgresql:postgresql | Boot BOM | 42.7.7 | runtime | no | Retain; BOM-compatible security remediation |
| org.projectlombok:lombok | Boot BOM | 1.18.40 | compile | yes | Retain; BOM-compatible security remediation |
| org.springframework.boot:spring-boot-starter-test | Boot BOM | 3.5.6 | test | no | Retain; BOM-compatible security remediation |
| org.springframework.security:spring-security-test | Boot BOM | 6.5.5 | test | no | Retain; BOM-compatible security remediation |

## Appendix D: Complete resolved backend runtime/compile inventory and OSV matches

OSV query timestamp: `2026-09-23T02:34:00.801Z`. No match means no coordinate/version match in this query, not a security guarantee. DevTools is listed by Maven but normally excluded by Boot packaging; inspect the actual release artifact. Test dependencies/plugins and image packages require separate scans.

| Coordinate | Resolved version | Scope | OSV advisory matches |
| --- | --- | --- | --- |
| org.springframework.boot:spring-boot-starter-validation | 3.5.6 | compile | No match returned |
| org.springframework.boot:spring-boot-starter | 3.5.6 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-logging | 3.5.6 | compile | No match returned |
| ch.qos.logback:logback-classic | 1.5.18 | compile | No match returned |
| ch.qos.logback:logback-core | 1.5.18 | compile | [GHSA-25qh-j22f-pwp8](https://osv.dev/vulnerability/GHSA-25qh-j22f-pwp8); [GHSA-jhq6-gfmj-v8fx](https://osv.dev/vulnerability/GHSA-jhq6-gfmj-v8fx); [GHSA-p47f-322f-whfh](https://osv.dev/vulnerability/GHSA-p47f-322f-whfh); [GHSA-qqpg-mvqg-649v](https://osv.dev/vulnerability/GHSA-qqpg-mvqg-649v) |
| org.apache.logging.log4j:log4j-to-slf4j | 2.24.3 | compile | No match returned |
| org.apache.logging.log4j:log4j-api | 2.24.3 | compile | [GHSA-qv9r-c865-cp47](https://osv.dev/vulnerability/GHSA-qv9r-c865-cp47) |
| org.slf4j:jul-to-slf4j | 2.0.17 | compile | No match returned |
| jakarta.annotation:jakarta.annotation-api | 2.1.1 | compile | No match returned |
| org.yaml:snakeyaml | 2.4 | compile | No match returned |
| org.apache.tomcat.embed:tomcat-embed-el | 10.1.46 | compile | No match returned |
| org.hibernate.validator:hibernate-validator | 8.0.3.Final | compile | No match returned |
| jakarta.validation:jakarta.validation-api | 3.0.2 | compile | No match returned |
| org.jboss.logging:jboss-logging | 3.6.1.Final | compile | No match returned |
| com.fasterxml:classmate | 1.7.0 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-actuator | 3.5.6 | compile | [GHSA-8hfc-fq58-r658](https://osv.dev/vulnerability/GHSA-8hfc-fq58-r658); [GHSA-mgvc-8q2h-5pgc](https://osv.dev/vulnerability/GHSA-mgvc-8q2h-5pgc) |
| org.springframework.boot:spring-boot-actuator-autoconfigure | 3.5.6 | compile | No match returned |
| org.springframework.boot:spring-boot-actuator | 3.5.6 | compile | No match returned |
| com.fasterxml.jackson.core:jackson-databind | 2.19.2 | compile | [GHSA-3pjw-73gf-8qr5](https://osv.dev/vulnerability/GHSA-3pjw-73gf-8qr5); [GHSA-5jmj-h7xm-6q6v](https://osv.dev/vulnerability/GHSA-5jmj-h7xm-6q6v); [GHSA-hgj6-7826-r7m5](https://osv.dev/vulnerability/GHSA-hgj6-7826-r7m5); [GHSA-j3rv-43j4-c7qm](https://osv.dev/vulnerability/GHSA-j3rv-43j4-c7qm); [GHSA-rmj7-2vxq-3g9f](https://osv.dev/vulnerability/GHSA-rmj7-2vxq-3g9f) |
| io.micrometer:micrometer-observation | 1.15.4 | compile | No match returned |
| io.micrometer:micrometer-commons | 1.15.4 | compile | No match returned |
| io.micrometer:micrometer-jakarta9 | 1.15.4 | compile | No match returned |
| io.micrometer:micrometer-core | 1.15.4 | compile | [GHSA-g3pr-3p32-fp23](https://osv.dev/vulnerability/GHSA-g3pr-3p32-fp23); [GHSA-w737-wx49-qj23](https://osv.dev/vulnerability/GHSA-w737-wx49-qj23) |
| org.hdrhistogram:HdrHistogram | 2.2.2 | runtime | No match returned |
| org.latencyutils:LatencyUtils | 2.0.3 | runtime | No match returned |
| org.springframework.boot:spring-boot-starter-data-jpa | 3.5.6 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-jdbc | 3.5.6 | compile | No match returned |
| com.zaxxer:HikariCP | 6.3.3 | compile | No match returned |
| org.springframework:spring-jdbc | 6.2.11 | compile | No match returned |
| org.hibernate.orm:hibernate-core | 6.6.29.Final | compile | No match returned |
| jakarta.persistence:jakarta.persistence-api | 3.1.0 | compile | No match returned |
| jakarta.transaction:jakarta.transaction-api | 2.0.1 | compile | No match returned |
| org.hibernate.common:hibernate-commons-annotations | 7.0.3.Final | runtime | No match returned |
| io.smallrye:jandex | 3.2.0 | runtime | No match returned |
| net.bytebuddy:byte-buddy | 1.17.7 | runtime | No match returned |
| org.glassfish.jaxb:jaxb-runtime | 4.0.5 | runtime | No match returned |
| org.glassfish.jaxb:jaxb-core | 4.0.5 | runtime | No match returned |
| org.eclipse.angus:angus-activation | 2.0.2 | runtime | No match returned |
| org.glassfish.jaxb:txw2 | 4.0.5 | runtime | No match returned |
| com.sun.istack:istack-commons-runtime | 4.1.2 | runtime | No match returned |
| jakarta.inject:jakarta.inject-api | 2.0.1 | runtime | No match returned |
| org.antlr:antlr4-runtime | 4.13.0 | compile | No match returned |
| org.springframework.data:spring-data-jpa | 3.5.4 | compile | No match returned |
| org.springframework.data:spring-data-commons | 3.5.4 | compile | [GHSA-5m4m-73w9-8433](https://osv.dev/vulnerability/GHSA-5m4m-73w9-8433); [GHSA-5vpf-xvv7-c8vh](https://osv.dev/vulnerability/GHSA-5vpf-xvv7-c8vh); [GHSA-88fw-v6x4-3f58](https://osv.dev/vulnerability/GHSA-88fw-v6x4-3f58); [GHSA-9fw2-h3hf-293r](https://osv.dev/vulnerability/GHSA-9fw2-h3hf-293r) |
| org.springframework:spring-orm | 6.2.11 | compile | No match returned |
| org.springframework:spring-context | 6.2.11 | compile | No match returned |
| org.springframework:spring-tx | 6.2.11 | compile | No match returned |
| org.springframework:spring-beans | 6.2.11 | compile | No match returned |
| org.slf4j:slf4j-api | 2.0.17 | compile | No match returned |
| org.springframework:spring-aspects | 6.2.11 | compile | No match returned |
| org.aspectj:aspectjweaver | 1.9.24 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-security | 3.5.6 | compile | No match returned |
| org.springframework:spring-aop | 6.2.11 | compile | No match returned |
| org.springframework.security:spring-security-config | 6.5.5 | compile | No match returned |
| org.springframework.security:spring-security-web | 6.5.5 | compile | [GHSA-293q-567p-wmwq](https://osv.dev/vulnerability/GHSA-293q-567p-wmwq); [GHSA-mf92-479x-3373](https://osv.dev/vulnerability/GHSA-mf92-479x-3373); [GHSA-x2r2-rvhq-2mqv](https://osv.dev/vulnerability/GHSA-x2r2-rvhq-2mqv) |
| org.springframework:spring-expression | 6.2.11 | compile | [GHSA-9f52-rjqv-25qv](https://osv.dev/vulnerability/GHSA-9f52-rjqv-25qv); [GHSA-r5w3-xv2f-j59q](https://osv.dev/vulnerability/GHSA-r5w3-xv2f-j59q); [GHSA-wxpp-56q6-5pcg](https://osv.dev/vulnerability/GHSA-wxpp-56q6-5pcg) |
| org.springframework.boot:spring-boot-starter-oauth2-resource-server | 3.5.6 | compile | No match returned |
| org.springframework.security:spring-security-core | 6.5.5 | compile | [GHSA-vxf7-qj7q-83fh](https://osv.dev/vulnerability/GHSA-vxf7-qj7q-83fh); [GHSA-x2wq-9x2f-fhj7](https://osv.dev/vulnerability/GHSA-x2wq-9x2f-fhj7) |
| org.springframework.security:spring-security-crypto | 6.5.5 | compile | No match returned |
| org.springframework.security:spring-security-oauth2-resource-server | 6.5.5 | compile | No match returned |
| org.springframework.security:spring-security-oauth2-jose | 6.5.5 | compile | [GHSA-cvc6-q2cp-2xhw](https://osv.dev/vulnerability/GHSA-cvc6-q2cp-2xhw) |
| org.springframework.security:spring-security-oauth2-core | 6.5.5 | compile | No match returned |
| org.springframework:spring-core | 6.2.11 | compile | [GHSA-659m-px2c-25wj](https://osv.dev/vulnerability/GHSA-659m-px2c-25wj) |
| org.springframework:spring-jcl | 6.2.11 | compile | No match returned |
| com.nimbusds:nimbus-jose-jwt | 9.37.4 | compile | No match returned |
| com.github.stephenc.jcip:jcip-annotations | 1.0-1 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-web | 3.5.6 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-json | 3.5.6 | compile | No match returned |
| com.fasterxml.jackson.datatype:jackson-datatype-jdk8 | 2.19.2 | compile | No match returned |
| com.fasterxml.jackson.module:jackson-module-parameter-names | 2.19.2 | compile | No match returned |
| org.springframework.boot:spring-boot-starter-tomcat | 3.5.6 | compile | No match returned |
| org.apache.tomcat.embed:tomcat-embed-core | 10.1.46 | compile | [GHSA-563x-q5rq-57qp](https://osv.dev/vulnerability/GHSA-563x-q5rq-57qp); [GHSA-5m62-pw8w-7w9f](https://osv.dev/vulnerability/GHSA-5m62-pw8w-7w9f); [GHSA-5mp6-jrq3-r938](https://osv.dev/vulnerability/GHSA-5mp6-jrq3-r938); [GHSA-9m3c-qcxr-9x87](https://osv.dev/vulnerability/GHSA-9m3c-qcxr-9x87); [GHSA-9m89-8frq-c98c](https://osv.dev/vulnerability/GHSA-9m89-8frq-c98c); [GHSA-9xv2-5v5q-p794](https://osv.dev/vulnerability/GHSA-9xv2-5v5q-p794); [GHSA-fpj8-gq4v-p354](https://osv.dev/vulnerability/GHSA-fpj8-gq4v-p354); [GHSA-fv25-8xcx-gqjc](https://osv.dev/vulnerability/GHSA-fv25-8xcx-gqjc); [GHSA-gcx9-497g-6cp6](https://osv.dev/vulnerability/GHSA-gcx9-497g-6cp6); [GHSA-gx5v-xp9w-j4cg](https://osv.dev/vulnerability/GHSA-gx5v-xp9w-j4cg); [GHSA-h3x4-894j-xpx5](https://osv.dev/vulnerability/GHSA-h3x4-894j-xpx5); [GHSA-h6fc-48rj-7qqh](https://osv.dev/vulnerability/GHSA-h6fc-48rj-7qqh); [GHSA-hgrr-935x-pq79](https://osv.dev/vulnerability/GHSA-hgrr-935x-pq79); [GHSA-mgp5-rv84-w37q](https://osv.dev/vulnerability/GHSA-mgp5-rv84-w37q); [GHSA-r29c-68gh-xp6x](https://osv.dev/vulnerability/GHSA-r29c-68gh-xp6x); [GHSA-rv64-5gf8-9qq8](https://osv.dev/vulnerability/GHSA-rv64-5gf8-9qq8); [GHSA-x4m4-345f-5h5g](https://osv.dev/vulnerability/GHSA-x4m4-345f-5h5g) |
| org.apache.tomcat.embed:tomcat-embed-websocket | 10.1.46 | compile | No match returned |
| org.springframework:spring-web | 6.2.11 | compile | [GHSA-7m2p-62gw-p8qq](https://osv.dev/vulnerability/GHSA-7m2p-62gw-p8qq) |
| org.springframework:spring-webmvc | 6.2.11 | compile | [GHSA-3chg-m5w7-qfv5](https://osv.dev/vulnerability/GHSA-3chg-m5w7-qfv5); [GHSA-4773-3jfm-qmx3](https://osv.dev/vulnerability/GHSA-4773-3jfm-qmx3); [GHSA-6hcq-hmm3-jj3c](https://osv.dev/vulnerability/GHSA-6hcq-hmm3-jj3c); [GHSA-6p4f-wcwh-5vvm](https://osv.dev/vulnerability/GHSA-6p4f-wcwh-5vvm); [GHSA-72pg-x5f8-j25j](https://osv.dev/vulnerability/GHSA-72pg-x5f8-j25j); [GHSA-957g-f97v-vppc](https://osv.dev/vulnerability/GHSA-957g-f97v-vppc); [GHSA-cjpg-rgq5-fr37](https://osv.dev/vulnerability/GHSA-cjpg-rgq5-fr37); [GHSA-h3qp-gqrc-q736](https://osv.dev/vulnerability/GHSA-h3qp-gqrc-q736); [GHSA-mq64-j8f9-9gcj](https://osv.dev/vulnerability/GHSA-mq64-j8f9-9gcj); [GHSA-wg35-8jpf-2xv3](https://osv.dev/vulnerability/GHSA-wg35-8jpf-2xv3); [GHSA-x23c-287f-qqv5](https://osv.dev/vulnerability/GHSA-x23c-287f-qqv5) |
| org.flywaydb:flyway-core | 11.7.2 | compile | No match returned |
| com.fasterxml.jackson.dataformat:jackson-dataformat-toml | 2.19.2 | compile | No match returned |
| com.fasterxml.jackson.core:jackson-core | 2.19.2 | compile | [GHSA-72hv-8253-57qq](https://osv.dev/vulnerability/GHSA-72hv-8253-57qq); [GHSA-r7wm-3cxj-wff9](https://osv.dev/vulnerability/GHSA-r7wm-3cxj-wff9) |
| com.fasterxml.jackson.datatype:jackson-datatype-jsr310 | 2.19.2 | compile | No match returned |
| com.fasterxml.jackson.core:jackson-annotations | 2.19.2 | compile | No match returned |
| org.flywaydb:flyway-database-postgresql | 10.17.0 | compile | No match returned |
| org.springframework.boot:spring-boot-devtools | 3.5.6 | runtime | [GHSA-56v8-86gj-66jp](https://osv.dev/vulnerability/GHSA-56v8-86gj-66jp) |
| org.springframework.boot:spring-boot | 3.5.6 | compile | [GHSA-wwpq-f5c3-7hvx](https://osv.dev/vulnerability/GHSA-wwpq-f5c3-7hvx) |
| org.springframework.boot:spring-boot-autoconfigure | 3.5.6 | compile | [GHSA-ggg2-9786-hwc8](https://osv.dev/vulnerability/GHSA-ggg2-9786-hwc8) |
| org.postgresql:postgresql | 42.7.7 | runtime | [GHSA-98qh-xjc8-98pq](https://osv.dev/vulnerability/GHSA-98qh-xjc8-98pq); [GHSA-j92g-9f8w-j867](https://osv.dev/vulnerability/GHSA-j92g-9f8w-j867) |
| org.checkerframework:checker-qual | 3.49.3 | runtime | No match returned |
| org.projectlombok:lombok | 1.18.40 | compile | No match returned |
| jakarta.xml.bind:jakarta.xml.bind-api | 4.0.2 | runtime | No match returned |
| jakarta.activation:jakarta.activation-api | 2.1.4 | runtime | No match returned |

## Appendix E: Complete controller endpoint inventory

Static mapping inventory for the audited develop commit. No claim is made that these endpoints are deployed. Session routes bypass BearerFilter; other paths require its token validation. Method authorization does not replace object ownership checks.

| Method | Path | Method authorization | Controller |
| --- | --- | --- | --- |
| POST | /api/documents | No method annotation; inspect BearerFilter and service ownership | DocumentController.java |
| GET | /api/documents/{id}/download | No method annotation; inspect BearerFilter and service ownership | DocumentController.java |
| DELETE | /api/documents/{id} | No method annotation; inspect BearerFilter and service ownership | DocumentController.java |
| POST | /api/donations | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonationController.java |
| GET | /api/donations | isAuthenticated() | DonationController.java |
| GET | /api/donations/{id} | isAuthenticated() | DonationController.java |
| PUT | /api/donations/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonationController.java |
| DELETE | /api/donations/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonationController.java |
| POST | /api/donors | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonorController.java |
| GET | /api/donors | isAuthenticated() | DonorController.java |
| GET | /api/donors/{id} | isAuthenticated() | DonorController.java |
| PUT | /api/donors/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonorController.java |
| DELETE | /api/donors/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | DonorController.java |
| POST | /api/materials/categories | hasRole('ADMINISTRATOR') | MaterialCategoryController.java |
| GET | /api/materials/categories | isAuthenticated() | MaterialCategoryController.java |
| GET | /api/materials/categories/{id} | isAuthenticated() | MaterialCategoryController.java |
| PUT | /api/materials/categories/{id} | hasRole('ADMINISTRATOR') | MaterialCategoryController.java |
| DELETE | /api/materials/categories/{id} | hasRole('ADMINISTRATOR') | MaterialCategoryController.java |
| POST | /api/materials/subtypes | hasRole('ADMINISTRATOR') | MaterialSubtypeController.java |
| GET | /api/materials/subtypes | isAuthenticated() | MaterialSubtypeController.java |
| GET | /api/materials/subtypes/{id} | isAuthenticated() | MaterialSubtypeController.java |
| PUT | /api/materials/subtypes/{id} | hasRole('ADMINISTRATOR') | MaterialSubtypeController.java |
| DELETE | /api/materials/subtypes/{id} | hasRole('ADMINISTRATOR') | MaterialSubtypeController.java |
| POST | /api/materials/types | hasRole('ADMINISTRATOR') | MaterialTypeController.java |
| GET | /api/materials/types | isAuthenticated() | MaterialTypeController.java |
| GET | /api/materials/types/{id} | isAuthenticated() | MaterialTypeController.java |
| PUT | /api/materials/types/{id} | hasRole('ADMINISTRATOR') | MaterialTypeController.java |
| DELETE | /api/materials/types/{id} | hasRole('ADMINISTRATOR') | MaterialTypeController.java |
| POST | /api/pressings | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | PressingController.java |
| GET | /api/pressings | isAuthenticated() | PressingController.java |
| GET | /api/pressings/{id} | isAuthenticated() | PressingController.java |
| POST | /api/session/register | No method annotation; inspect BearerFilter and service ownership | SessionController.java |
| POST | /api/session/authenticate | No method annotation; inspect BearerFilter and service ownership | SessionController.java |
| POST | /api/sortings | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | SortingController.java |
| GET | /api/sortings | isAuthenticated() | SortingController.java |
| GET | /api/sortings/{id} | isAuthenticated() | SortingController.java |
| POST | /api/vehicles | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | VehicleController.java |
| POST | /api/vehicles/batch | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | VehicleController.java |
| GET | /api/vehicles | isAuthenticated() | VehicleController.java |
| GET | /api/vehicles/{id} | isAuthenticated() | VehicleController.java |
| PUT | /api/vehicles/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | VehicleController.java |
| PUT | /api/vehicles/batch | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | VehicleController.java |
| DELETE | /api/vehicles/{id} | hasAnyRole('ADMINISTRATOR', 'ORGANIZATION', 'CITY_HALL') | VehicleController.java |

## Appendix F: Frontend service routes

| Service | Legacy path |
| --- | --- |
| src/services/CollectionService.ts | /coletas |
| src/services/DocumentService.ts | /documentos |
| src/services/DonationService.ts | /doacoes |
| src/services/DonatorService.ts | /doadores |
| src/services/DriverService.ts | /motoristas |
| src/services/InputMaterialService.ts | /materialEntrada |
| src/services/MediaService.ts | /documentos |
| src/services/OutputMaterialService.ts | /materialSaida |
| src/services/PressingService.ts | /prensagem |
| src/services/SaleService.ts | /vendas |
| src/services/SubTypologyService.ts | /subtipologias |
| src/services/TriageService.ts | /triagem |
| src/services/TypologyService.ts | /tipologias |
| src/services/UserService.ts | /usuarios |
| src/services/VehicleService.ts | /veiculos |

## Appendix G: Backend request/response DTO inventory

Record field types are taken from source after removing validation annotations and comments. Validation rules remain authoritative in the linked source files; this field inventory does not replace OpenAPI or bean validation.

| DTO | Fields | Source |
| --- | --- | --- |
| AttachmentCreateRequestDTO | String fileName, String fileType, String storageUrl | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/attachment/request/AttachmentCreateRequestDTO.java) |
| AttachmentResponseDTO | UUID id, String fileName, String fileType, String storageUrl, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/attachment/response/AttachmentResponseDTO.java) |
| CollectionCreateRequestDTO | OffsetDateTime realizationDate, BigDecimal totalWeightKg, UUID vehicleId, UUID driverId, UUID mtrGeneratorId, UUID mtrDestinatorId, UUID collectionDiaryId, Set<UUID> teamMemberIds, List<InputItemRequestDTO> inputItems | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/collection/request/CollectionCreateRequestDTO.java) |
| InputItemRequestDTO | UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3 | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/collection/request/InputItemRequestDTO.java) |
| CollectionResponseDTO | UUID id, OffsetDateTime realizationDate, BigDecimal totalWeightKg, UUID vehicleId, UUID driverId, UUID mtrGeneratorId, UUID mtrDestinatorId, UUID collectionDiaryId, Boolean isActive, Set<UUID> teamMemberIds, List<InputItemResponseDTO> inputItems, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/collection/response/CollectionResponseDTO.java) |
| InputItemResponseDTO | UUID id, UUID collectionId, UUID donationId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/collection/response/InputItemResponseDTO.java) |
| DonationCreateRequestDTO | OffsetDateTime donationDate, BigDecimal totalWeightKg, UUID donorId, UUID proofAttachmentId, List<InputItemRequestDTO> inputItems | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donation/request/DonationCreateRequestDTO.java) |
| DonationUpdateRequestDTO | OffsetDateTime donationDate, BigDecimal totalWeightKg, UUID proofAttachmentId, List<InputItemRequestDTO> inputItems | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donation/request/DonationUpdateRequestDTO.java) |
| DonationResponseDTO | UUID id, OffsetDateTime donationDate, BigDecimal totalWeightKg, UUID donorId, UUID proofAttachmentId, Boolean isActive, List<InputItemResponseDTO> inputItems, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donation/response/DonationResponseDTO.java) |
| DonorCreateRequestDTO | String name, String document, DonorType donorType | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donor/request/DonorCreateRequestDTO.java) |
| DonorUpdateRequestDTO | String name, String document | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donor/request/DonorUpdateRequestDTO.java) |
| DonorResponseDTO | UUID id, String name, String document, DonorType donorType, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/donor/response/DonorResponseDTO.java) |
| InventoryBalanceResponseDTO | UUID id, UUID materialSubtypeId, BigDecimal currentWeightKg, BigDecimal currentVolumeM3, OffsetDateTime lastUpdatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/inventory/response/InventoryBalanceResponseDTO.java) |
| InventoryLogResponseDTO | UUID id, UUID materialSubtypeId, BigDecimal quantityKg, BigDecimal quantityM3, OperationType operationType, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/inventory/response/InventoryLogResponseDTO.java) |
| VehicleBulkCreateRequestDTO | List<VehicleCreateRequestDTO> vehicles | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkCreateRequestDTO.java) |
| VehicleBulkUpdateItemDTO | UUID id, String licensePlate, String model, Boolean isActive | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateItemDTO.java) |
| VehicleBulkUpdateRequestDTO | List<VehicleBulkUpdateItemDTO> vehicles | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleBulkUpdateRequestDTO.java) |
| VehicleCreateRequestDTO | String licensePlate, String model | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleCreateRequestDTO.java) |
| VehicleUpdateRequestDTO | String licensePlate, String model, Boolean isActive | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/request/VehicleUpdateRequestDTO.java) |
| VehicleResponseDTO | UUID id, String licensePlate, String model, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, String creatorId | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/logistic/vehicle/response/VehicleResponseDTO.java) |
| MaterialCategoryCreateRequestDTO | String name | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialCategoryCreateRequestDTO.java) |
| MaterialCategoryUpdateRequestDTO | String name, Long version | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialCategoryUpdateRequestDTO.java) |
| MaterialSubtypeCreateRequestDTO | UUID typeId, String name | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialSubtypeCreateRequestDTO.java) |
| MaterialSubtypeUpdateRequestDTO | String name, Long version | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialSubtypeUpdateRequestDTO.java) |
| MaterialTypeCreateRequestDTO | UUID categoryId, String name | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialTypeCreateRequestDTO.java) |
| MaterialTypeUpdateRequestDTO | String name, Long version | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/request/MaterialTypeUpdateRequestDTO.java) |
| MaterialCategoryResponseDTO | UUID id, String name, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/response/MaterialCategoryResponseDTO.java) |
| MaterialSubtypeResponseDTO | UUID id, UUID typeId, String name, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/response/MaterialSubtypeResponseDTO.java) |
| MaterialTypeResponseDTO | UUID id, UUID categoryId, String name, Boolean isActive, Long version, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/material/response/MaterialTypeResponseDTO.java) |
| PressedBaleRequestDTO | UUID sortedItemId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal initialVolumeM3, BigDecimal finalVolumeM3, DestinationType destinationType, UUID destinationId | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/pressing/request/PressedBaleRequestDTO.java) |
| PressingCreateRequestDTO | OffsetDateTime pressingDate, List<PressedBaleRequestDTO> pressedBales | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/pressing/request/PressingCreateRequestDTO.java) |
| PressedBaleResponseDTO | UUID id, UUID pressingId, UUID sortedItemId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal initialVolumeM3, BigDecimal finalVolumeM3, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, DestinationType destinationType, UUID destinationId | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/pressing/response/PressedBaleResponseDTO.java) |
| PressingResponseDTO | UUID id, OffsetDateTime pressingDate, Boolean isActive, List<PressedBaleResponseDTO> pressedBales, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/pressing/response/PressingResponseDTO.java) |
| BuyerCreateRequestDTO | String name, String document | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/request/BuyerCreateRequestDTO.java) |
| BuyerUpdateRequestDTO | String name, String document | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/request/BuyerUpdateRequestDTO.java) |
| SaleCreateRequestDTO | OffsetDateTime saleDate, UUID buyerId, UUID nfeAttachmentId, UUID mtrAttachmentId, UUID cdfAttachmentId, BigDecimal totalValue, List<SaleItemRequestDTO> saleItems | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/request/SaleCreateRequestDTO.java) |
| SaleItemRequestDTO | UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3, BigDecimal unitPrice | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/request/SaleItemRequestDTO.java) |
| BuyerResponseDTO | UUID id, String name, String document, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/response/BuyerResponseDTO.java) |
| SaleItemResponseDTO | UUID id, UUID saleId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3, BigDecimal unitPrice, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/response/SaleItemResponseDTO.java) |
| SaleResponseDTO | UUID id, OffsetDateTime saleDate, UUID buyerId, UUID nfeAttachmentId, UUID mtrAttachmentId, UUID cdfAttachmentId, BigDecimal totalValue, Boolean isActive, List<SaleItemResponseDTO> saleItems, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sale/response/SaleResponseDTO.java) |
| LoginRequestDTO | String email, String password | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/session/request/LoginRequestDTO.java) |
| RegisterRequestDTO | String fullName, String email, String password | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/session/request/RegisterRequestDTO.java) |
| LoginResponseDTO | String token | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/session/response/LoginResponseDTO.java) |
| RegisterResponseDTO | String token | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/session/response/RegisterResponseDTO.java) |
| SortedItemRequestDTO | UUID inputItemId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3, BigDecimal rejectWeightKg, BigDecimal rejectVolumeM3, DestinationType destinationType, UUID destinationId | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sorting/request/SortedItemRequestDTO.java) |
| SortingCreateRequestDTO | OffsetDateTime sortingDate, SortingType sortingType, List<SortedItemRequestDTO> sortedItems | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sorting/request/SortingCreateRequestDTO.java) |
| SortedItemResponseDTO | UUID id, UUID sortingId, UUID inputItemId, UUID materialSubtypeId, BigDecimal weightKg, BigDecimal volumeM3, BigDecimal rejectWeightKg, BigDecimal rejectVolumeM3, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt, DestinationType destinationType, UUID destinationId | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sorting/response/SortedItemResponseDTO.java) |
| SortingResponseDTO | UUID id, OffsetDateTime sortingDate, SortingType sortingType, Boolean isActive, List<SortedItemResponseDTO> sortedItems, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/sorting/response/SortingResponseDTO.java) |
| TeamMemberCreateRequestDTO | String name, String role | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/team/request/TeamMemberCreateRequestDTO.java) |
| TeamMemberUpdateRequestDTO | String name, String role | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/team/request/TeamMemberUpdateRequestDTO.java) |
| TeamMemberResponseDTO | UUID id, String name, String role, Boolean isActive, OffsetDateTime createdAt, OffsetDateTime updatedAt | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/dto/team/response/TeamMemberResponseDTO.java) |

## Appendix H: Locked frontend transitive inventory

The table includes optional platform packages and the local self-link; installed package count can differ by platform. The committed lockfile is authoritative for integrity hashes and dependency edges. No source code or lockfile was changed.

<details>
<summary>All non-root lockfile package entries</summary>

| Lockfile package path | Version | Scope | Optional |
| --- | --- | --- | --- |
| node_modules/@alloc/quick-lru | 5.2.0 | development | no |
| node_modules/@ampproject/remapping | 2.3.0 | development | no |
| node_modules/@babel/code-frame | 7.26.2 | development | no |
| node_modules/@babel/compat-data | 7.26.5 | development | no |
| node_modules/@babel/core | 7.26.7 | development | no |
| node_modules/@babel/generator | 7.26.5 | development | no |
| node_modules/@babel/helper-compilation-targets | 7.26.5 | development | no |
| node_modules/@babel/helper-module-imports | 7.25.9 | development | no |
| node_modules/@babel/helper-module-transforms | 7.26.0 | development | no |
| node_modules/@babel/helper-plugin-utils | 7.26.5 | development | no |
| node_modules/@babel/helper-string-parser | 7.25.9 | development | no |
| node_modules/@babel/helper-validator-identifier | 7.25.9 | development | no |
| node_modules/@babel/helper-validator-option | 7.25.9 | development | no |
| node_modules/@babel/helpers | 7.26.7 | development | no |
| node_modules/@babel/parser | 7.26.7 | development | no |
| node_modules/@babel/plugin-transform-react-jsx-self | 7.25.9 | development | no |
| node_modules/@babel/plugin-transform-react-jsx-source | 7.25.9 | development | no |
| node_modules/@babel/template | 7.25.9 | development | no |
| node_modules/@babel/traverse | 7.26.7 | development | no |
| node_modules/@babel/traverse/node_modules/globals | 11.12.0 | development | no |
| node_modules/@babel/types | 7.26.7 | development | no |
| node_modules/@esbuild/aix-ppc64 | 0.24.2 | development | yes |
| node_modules/@esbuild/android-arm | 0.24.2 | development | yes |
| node_modules/@esbuild/android-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/android-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/darwin-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/darwin-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/freebsd-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/freebsd-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-arm | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-ia32 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-loong64 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-mips64el | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-ppc64 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-riscv64 | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-s390x | 0.24.2 | development | yes |
| node_modules/@esbuild/linux-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/netbsd-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/netbsd-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/openbsd-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/openbsd-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/sunos-x64 | 0.24.2 | development | yes |
| node_modules/@esbuild/win32-arm64 | 0.24.2 | development | yes |
| node_modules/@esbuild/win32-ia32 | 0.24.2 | development | yes |
| node_modules/@esbuild/win32-x64 | 0.24.2 | development | yes |
| node_modules/@eslint-community/eslint-utils | 4.4.1 | development | no |
| node_modules/@eslint-community/eslint-utils/node_modules/eslint-visitor-keys | 3.4.3 | development | no |
| node_modules/@eslint-community/regexpp | 4.12.1 | development | no |
| node_modules/@eslint/config-array | 0.19.2 | development | no |
| node_modules/@eslint/core | 0.10.0 | development | no |
| node_modules/@eslint/eslintrc | 3.2.0 | development | no |
| node_modules/@eslint/eslintrc/node_modules/globals | 14.0.0 | development | no |
| node_modules/@eslint/js | 9.19.0 | development | no |
| node_modules/@eslint/object-schema | 2.1.6 | development | no |
| node_modules/@eslint/plugin-kit | 0.2.5 | development | no |
| node_modules/@humanfs/core | 0.19.1 | development | no |
| node_modules/@humanfs/node | 0.16.6 | development | no |
| node_modules/@humanfs/node/node_modules/@humanwhocodes/retry | 0.3.1 | development | no |
| node_modules/@humanwhocodes/module-importer | 1.0.1 | development | no |
| node_modules/@humanwhocodes/retry | 0.4.1 | development | no |
| node_modules/@isaacs/cliui | 8.0.2 | development | no |
| node_modules/@jridgewell/gen-mapping | 0.3.8 | development | no |
| node_modules/@jridgewell/resolve-uri | 3.1.2 | development | no |
| node_modules/@jridgewell/set-array | 1.2.1 | development | no |
| node_modules/@jridgewell/sourcemap-codec | 1.5.0 | development | no |
| node_modules/@jridgewell/trace-mapping | 0.3.25 | development | no |
| node_modules/@nodelib/fs.scandir | 2.1.5 | development | no |
| node_modules/@nodelib/fs.stat | 2.0.5 | development | no |
| node_modules/@nodelib/fs.walk | 1.2.8 | development | no |
| node_modules/@pkgjs/parseargs | 0.11.0 | development | yes |
| node_modules/@rollup/rollup-android-arm-eabi | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-android-arm64 | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-darwin-arm64 | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-darwin-x64 | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-freebsd-arm64 | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-freebsd-x64 | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-arm-gnueabihf | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-arm-musleabihf | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-arm64-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-arm64-musl | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-loongarch64-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-powerpc64le-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-riscv64-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-s390x-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-x64-gnu | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-linux-x64-musl | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-win32-arm64-msvc | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-win32-ia32-msvc | 4.34.2 | development | yes |
| node_modules/@rollup/rollup-win32-x64-msvc | 4.34.2 | development | yes |
| node_modules/@types/babel__core | 7.20.5 | development | no |
| node_modules/@types/babel__generator | 7.6.8 | development | no |
| node_modules/@types/babel__template | 7.4.4 | development | no |
| node_modules/@types/babel__traverse | 7.20.6 | development | no |
| node_modules/@types/cookie | 0.6.0 | production/transitive | no |
| node_modules/@types/estree | 1.0.6 | development | no |
| node_modules/@types/hoist-non-react-statics | 3.3.6 | production/transitive | no |
| node_modules/@types/json-schema | 7.0.15 | development | no |
| node_modules/@types/prop-types | 15.7.14 | production/transitive | no |
| node_modules/@types/react | 18.3.18 | production/transitive | no |
| node_modules/@types/react-dom | 18.3.5 | development | no |
| node_modules/@vitejs/plugin-react | 4.3.4 | development | no |
| node_modules/acorn | 8.14.0 | development | no |
| node_modules/acorn-jsx | 5.3.2 | development | no |
| node_modules/ajv | 6.12.6 | development | no |
| node_modules/ansi-regex | 6.1.0 | development | no |
| node_modules/ansi-styles | 4.3.0 | development | no |
| node_modules/any-promise | 1.3.0 | development | no |
| node_modules/anymatch | 3.1.3 | development | no |
| node_modules/arg | 5.0.2 | development | no |
| node_modules/argparse | 2.0.1 | development | no |
| node_modules/array-buffer-byte-length | 1.0.2 | development | no |
| node_modules/array-includes | 3.1.8 | development | no |
| node_modules/array.prototype.findlast | 1.2.5 | development | no |
| node_modules/array.prototype.flat | 1.3.3 | development | no |
| node_modules/array.prototype.flatmap | 1.3.3 | development | no |
| node_modules/array.prototype.tosorted | 1.1.4 | development | no |
| node_modules/arraybuffer.prototype.slice | 1.0.4 | development | no |
| node_modules/async-function | 1.0.0 | development | no |
| node_modules/asynckit | 0.4.0 | production/transitive | no |
| node_modules/autoprefixer | 10.4.20 | development | no |
| node_modules/available-typed-arrays | 1.0.7 | development | no |
| node_modules/axios | 1.7.9 | production/transitive | no |
| node_modules/balanced-match | 1.0.2 | development | no |
| node_modules/binary-extensions | 2.3.0 | development | no |
| node_modules/brace-expansion | 1.1.11 | development | no |
| node_modules/braces | 3.0.3 | development | no |
| node_modules/browserslist | 4.24.4 | development | no |
| node_modules/call-bind | 1.0.8 | development | no |
| node_modules/call-bind-apply-helpers | 1.0.1 | development | no |
| node_modules/call-bound | 1.0.3 | development | no |
| node_modules/callsites | 3.1.0 | development | no |
| node_modules/camelcase-css | 2.0.1 | development | no |
| node_modules/caniuse-lite | 1.0.30001697 | development | no |
| node_modules/chalk | 4.1.2 | development | no |
| node_modules/chokidar | 3.6.0 | development | no |
| node_modules/chokidar/node_modules/glob-parent | 5.1.2 | development | no |
| node_modules/clsx | 2.1.1 | production/transitive | no |
| node_modules/color-convert | 2.0.1 | development | no |
| node_modules/color-name | 1.1.4 | development | no |
| node_modules/combined-stream | 1.0.8 | production/transitive | no |
| node_modules/commander | 4.1.1 | development | no |
| node_modules/concat-map | 0.0.1 | development | no |
| node_modules/convert-source-map | 2.0.0 | development | no |
| node_modules/cookie | 1.0.2 | production/transitive | no |
| node_modules/cross-spawn | 7.0.6 | development | no |
| node_modules/cssesc | 3.0.0 | development | no |
| node_modules/csstype | 3.1.3 | production/transitive | no |
| node_modules/data-view-buffer | 1.0.2 | development | no |
| node_modules/data-view-byte-length | 1.0.2 | development | no |
| node_modules/data-view-byte-offset | 1.0.1 | development | no |
| node_modules/debug | 4.4.0 | development | no |
| node_modules/deep-is | 0.1.4 | development | no |
| node_modules/deepmerge | 2.2.1 | production/transitive | no |
| node_modules/define-data-property | 1.1.4 | development | no |
| node_modules/define-properties | 1.2.1 | development | no |
| node_modules/delayed-stream | 1.0.0 | production/transitive | no |
| node_modules/didyoumean | 1.2.2 | development | no |
| node_modules/dlv | 1.1.3 | development | no |
| node_modules/doctrine | 2.1.0 | development | no |
| node_modules/dotenv | 16.4.7 | production/transitive | no |
| node_modules/dunder-proto | 1.0.1 | development | no |
| node_modules/eastasianwidth | 0.2.0 | development | no |
| node_modules/electron-to-chromium | 1.5.92 | development | no |
| node_modules/emoji-regex | 9.2.2 | development | no |
| node_modules/es-abstract | 1.23.9 | development | no |
| node_modules/es-define-property | 1.0.1 | development | no |
| node_modules/es-errors | 1.3.0 | development | no |
| node_modules/es-iterator-helpers | 1.2.1 | development | no |
| node_modules/es-object-atoms | 1.1.1 | development | no |
| node_modules/es-set-tostringtag | 2.1.0 | development | no |
| node_modules/es-shim-unscopables | 1.0.2 | development | no |
| node_modules/es-to-primitive | 1.3.0 | development | no |
| node_modules/esbuild | 0.24.2 | development | no |
| node_modules/escalade | 3.2.0 | development | no |
| node_modules/escape-string-regexp | 4.0.0 | development | no |
| node_modules/eslint | 9.19.0 | development | no |
| node_modules/eslint-plugin-react | 7.37.4 | development | no |
| node_modules/eslint-plugin-react-hooks | 5.1.0 | development | no |
| node_modules/eslint-plugin-react-refresh | 0.4.18 | development | no |
| node_modules/eslint-scope | 8.2.0 | development | no |
| node_modules/eslint-visitor-keys | 4.2.0 | development | no |
| node_modules/espree | 10.3.0 | development | no |
| node_modules/esquery | 1.6.0 | development | no |
| node_modules/esrecurse | 4.3.0 | development | no |
| node_modules/estraverse | 5.3.0 | development | no |
| node_modules/esutils | 2.0.3 | development | no |
| node_modules/fast-deep-equal | 3.1.3 | development | no |
| node_modules/fast-glob | 3.3.3 | development | no |
| node_modules/fast-glob/node_modules/glob-parent | 5.1.2 | development | no |
| node_modules/fast-json-stable-stringify | 2.1.0 | development | no |
| node_modules/fast-levenshtein | 2.0.6 | development | no |
| node_modules/fastq | 1.19.0 | development | no |
| node_modules/file-entry-cache | 8.0.0 | development | no |
| node_modules/fill-range | 7.1.1 | development | no |
| node_modules/find-up | 5.0.0 | development | no |
| node_modules/flat-cache | 4.0.1 | development | no |
| node_modules/flatted | 3.3.2 | development | no |
| node_modules/follow-redirects | 1.15.9 | production/transitive | no |
| node_modules/for-each | 0.3.4 | development | no |
| node_modules/foreground-child | 3.3.0 | development | no |
| node_modules/form-data | 4.0.1 | production/transitive | no |
| node_modules/formik | 2.4.6 | production/transitive | no |
| node_modules/fraction.js | 4.3.7 | development | no |
| node_modules/front | local self-link | production/transitive | no |
| node_modules/fsevents | 2.3.3 | development | yes |
| node_modules/function-bind | 1.1.2 | development | no |
| node_modules/function.prototype.name | 1.1.8 | development | no |
| node_modules/functions-have-names | 1.2.3 | development | no |
| node_modules/gensync | 1.0.0-beta.2 | development | no |
| node_modules/get-intrinsic | 1.2.7 | development | no |
| node_modules/get-proto | 1.0.1 | development | no |
| node_modules/get-symbol-description | 1.1.0 | development | no |
| node_modules/glob | 10.4.5 | development | no |
| node_modules/glob-parent | 6.0.2 | development | no |
| node_modules/glob/node_modules/brace-expansion | 2.0.1 | development | no |
| node_modules/glob/node_modules/minimatch | 9.0.5 | development | no |
| node_modules/globals | 15.14.0 | development | no |
| node_modules/globalthis | 1.0.4 | development | no |
| node_modules/gopd | 1.2.0 | development | no |
| node_modules/has-bigints | 1.1.0 | development | no |
| node_modules/has-flag | 4.0.0 | development | no |
| node_modules/has-property-descriptors | 1.0.2 | development | no |
| node_modules/has-proto | 1.2.0 | development | no |
| node_modules/has-symbols | 1.1.0 | development | no |
| node_modules/has-tostringtag | 1.0.2 | development | no |
| node_modules/hasown | 2.0.2 | development | no |
| node_modules/hoist-non-react-statics | 3.3.2 | production/transitive | no |
| node_modules/ignore | 5.3.2 | development | no |
| node_modules/import-fresh | 3.3.1 | development | no |
| node_modules/imurmurhash | 0.1.4 | development | no |
| node_modules/internal-slot | 1.1.0 | development | no |
| node_modules/is-array-buffer | 3.0.5 | development | no |
| node_modules/is-async-function | 2.1.1 | development | no |
| node_modules/is-bigint | 1.1.0 | development | no |
| node_modules/is-binary-path | 2.1.0 | development | no |
| node_modules/is-boolean-object | 1.2.2 | development | no |
| node_modules/is-callable | 1.2.7 | development | no |
| node_modules/is-core-module | 2.16.1 | development | no |
| node_modules/is-data-view | 1.0.2 | development | no |
| node_modules/is-date-object | 1.1.0 | development | no |
| node_modules/is-extglob | 2.1.1 | development | no |
| node_modules/is-finalizationregistry | 1.1.1 | development | no |
| node_modules/is-fullwidth-code-point | 3.0.0 | development | no |
| node_modules/is-generator-function | 1.1.0 | development | no |
| node_modules/is-glob | 4.0.3 | development | no |
| node_modules/is-map | 2.0.3 | development | no |
| node_modules/is-number | 7.0.0 | development | no |
| node_modules/is-number-object | 1.1.1 | development | no |
| node_modules/is-regex | 1.2.1 | development | no |
| node_modules/is-set | 2.0.3 | development | no |
| node_modules/is-shared-array-buffer | 1.0.4 | development | no |
| node_modules/is-string | 1.1.1 | development | no |
| node_modules/is-symbol | 1.1.1 | development | no |
| node_modules/is-typed-array | 1.1.15 | development | no |
| node_modules/is-weakmap | 2.0.2 | development | no |
| node_modules/is-weakref | 1.1.1 | development | no |
| node_modules/is-weakset | 2.0.4 | development | no |
| node_modules/isarray | 2.0.5 | development | no |
| node_modules/isexe | 2.0.0 | development | no |
| node_modules/iterator.prototype | 1.1.5 | development | no |
| node_modules/jackspeak | 3.4.3 | development | no |
| node_modules/jiti | 1.21.7 | development | no |
| node_modules/js-tokens | 4.0.0 | production/transitive | no |
| node_modules/js-yaml | 4.1.0 | development | no |
| node_modules/jsesc | 3.1.0 | development | no |
| node_modules/json-buffer | 3.0.1 | development | no |
| node_modules/json-schema-traverse | 0.4.1 | development | no |
| node_modules/json-stable-stringify-without-jsonify | 1.0.1 | development | no |
| node_modules/json5 | 2.2.3 | development | no |
| node_modules/jsx-ast-utils | 3.3.5 | development | no |
| node_modules/jwt-decode | 4.0.0 | production/transitive | no |
| node_modules/keyv | 4.5.4 | development | no |
| node_modules/levn | 0.4.1 | development | no |
| node_modules/lilconfig | 3.1.3 | development | no |
| node_modules/lines-and-columns | 1.2.4 | development | no |
| node_modules/locate-path | 6.0.0 | development | no |
| node_modules/lodash | 4.17.21 | production/transitive | no |
| node_modules/lodash-es | 4.17.21 | production/transitive | no |
| node_modules/lodash.merge | 4.6.2 | development | no |
| node_modules/loose-envify | 1.4.0 | production/transitive | no |
| node_modules/lru-cache | 5.1.1 | development | no |
| node_modules/lucide | 0.476.0 | production/transitive | no |
| node_modules/lucide-react | 0.476.0 | production/transitive | no |
| node_modules/math-intrinsics | 1.1.0 | development | no |
| node_modules/merge2 | 1.4.1 | development | no |
| node_modules/micromatch | 4.0.8 | development | no |
| node_modules/mime-db | 1.52.0 | production/transitive | no |
| node_modules/mime-types | 2.1.35 | production/transitive | no |
| node_modules/minimatch | 3.1.2 | development | no |
| node_modules/minipass | 7.1.2 | development | no |
| node_modules/ms | 2.1.3 | development | no |
| node_modules/mz | 2.7.0 | development | no |
| node_modules/nanoid | 3.3.8 | development | no |
| node_modules/natural-compare | 1.4.0 | development | no |
| node_modules/node-releases | 2.0.19 | development | no |
| node_modules/normalize-path | 3.0.0 | development | no |
| node_modules/normalize-range | 0.1.2 | development | no |
| node_modules/object-assign | 4.1.1 | development | no |
| node_modules/object-hash | 3.0.0 | development | no |
| node_modules/object-inspect | 1.13.4 | development | no |
| node_modules/object-keys | 1.1.1 | development | no |
| node_modules/object.assign | 4.1.7 | development | no |
| node_modules/object.entries | 1.1.8 | development | no |
| node_modules/object.fromentries | 2.0.8 | development | no |
| node_modules/object.values | 1.2.1 | development | no |
| node_modules/optionator | 0.9.4 | development | no |
| node_modules/own-keys | 1.0.1 | development | no |
| node_modules/p-limit | 3.1.0 | development | no |
| node_modules/p-locate | 5.0.0 | development | no |
| node_modules/package-json-from-dist | 1.0.1 | development | no |
| node_modules/parent-module | 1.0.1 | development | no |
| node_modules/path-exists | 4.0.0 | development | no |
| node_modules/path-key | 3.1.1 | development | no |
| node_modules/path-parse | 1.0.7 | development | no |
| node_modules/path-scurry | 1.11.1 | development | no |
| node_modules/path-scurry/node_modules/lru-cache | 10.4.3 | development | no |
| node_modules/picocolors | 1.1.1 | development | no |
| node_modules/picomatch | 2.3.1 | development | no |
| node_modules/pify | 2.3.0 | development | no |
| node_modules/pirates | 4.0.6 | development | no |
| node_modules/possible-typed-array-names | 1.0.0 | development | no |
| node_modules/postcss | 8.5.1 | development | no |
| node_modules/postcss-import | 15.1.0 | development | no |
| node_modules/postcss-import/node_modules/resolve | 1.22.10 | development | no |
| node_modules/postcss-js | 4.0.1 | development | no |
| node_modules/postcss-load-config | 4.0.2 | development | no |
| node_modules/postcss-nested | 6.2.0 | development | no |
| node_modules/postcss-selector-parser | 6.1.2 | development | no |
| node_modules/postcss-value-parser | 4.2.0 | development | no |
| node_modules/prelude-ls | 1.2.1 | development | no |
| node_modules/prop-types | 15.8.1 | development | no |
| node_modules/proxy-from-env | 1.1.0 | production/transitive | no |
| node_modules/punycode | 2.3.1 | development | no |
| node_modules/queue-microtask | 1.2.3 | development | no |
| node_modules/react | 18.3.1 | production/transitive | no |
| node_modules/react-dom | 18.3.1 | production/transitive | no |
| node_modules/react-fast-compare | 2.0.4 | production/transitive | no |
| node_modules/react-is | 16.13.1 | production/transitive | no |
| node_modules/react-refresh | 0.14.2 | development | no |
| node_modules/react-router | 7.1.5 | production/transitive | no |
| node_modules/react-router-dom | 7.1.5 | production/transitive | no |
| node_modules/react-toastify | 11.0.3 | production/transitive | no |
| node_modules/read-cache | 1.0.0 | development | no |
| node_modules/readdirp | 3.6.0 | development | no |
| node_modules/reflect.getprototypeof | 1.0.10 | development | no |
| node_modules/regexp.prototype.flags | 1.5.4 | development | no |
| node_modules/resolve | 2.0.0-next.5 | development | no |
| node_modules/resolve-from | 4.0.0 | development | no |
| node_modules/reusify | 1.0.4 | development | no |
| node_modules/rollup | 4.34.2 | development | no |
| node_modules/run-parallel | 1.2.0 | development | no |
| node_modules/safe-array-concat | 1.1.3 | development | no |
| node_modules/safe-push-apply | 1.0.0 | development | no |
| node_modules/safe-regex-test | 1.1.0 | development | no |
| node_modules/scheduler | 0.23.2 | production/transitive | no |
| node_modules/semver | 6.3.1 | development | no |
| node_modules/set-cookie-parser | 2.7.1 | production/transitive | no |
| node_modules/set-function-length | 1.2.2 | development | no |
| node_modules/set-function-name | 2.0.2 | development | no |
| node_modules/set-proto | 1.0.0 | development | no |
| node_modules/shebang-command | 2.0.0 | development | no |
| node_modules/shebang-regex | 3.0.0 | development | no |
| node_modules/side-channel | 1.1.0 | development | no |
| node_modules/side-channel-list | 1.0.0 | development | no |
| node_modules/side-channel-map | 1.0.1 | development | no |
| node_modules/side-channel-weakmap | 1.0.2 | development | no |
| node_modules/signal-exit | 4.1.0 | development | no |
| node_modules/source-map-js | 1.2.1 | development | no |
| node_modules/string-width | 5.1.2 | development | no |
| node_modules/string-width-cjs | 4.2.3 | development | no |
| node_modules/string-width-cjs/node_modules/ansi-regex | 5.0.1 | development | no |
| node_modules/string-width-cjs/node_modules/emoji-regex | 8.0.0 | development | no |
| node_modules/string-width-cjs/node_modules/strip-ansi | 6.0.1 | development | no |
| node_modules/string.prototype.matchall | 4.0.12 | development | no |
| node_modules/string.prototype.repeat | 1.0.0 | development | no |
| node_modules/string.prototype.trim | 1.2.10 | development | no |
| node_modules/string.prototype.trimend | 1.0.9 | development | no |
| node_modules/string.prototype.trimstart | 1.0.8 | development | no |
| node_modules/strip-ansi | 7.1.0 | development | no |
| node_modules/strip-ansi-cjs | 6.0.1 | development | no |
| node_modules/strip-ansi-cjs/node_modules/ansi-regex | 5.0.1 | development | no |
| node_modules/strip-json-comments | 3.1.1 | development | no |
| node_modules/sucrase | 3.35.0 | development | no |
| node_modules/supports-color | 7.2.0 | development | no |
| node_modules/supports-preserve-symlinks-flag | 1.0.0 | development | no |
| node_modules/tailwindcss | 3.4.17 | development | no |
| node_modules/tailwindcss/node_modules/resolve | 1.22.10 | development | no |
| node_modules/thenify | 3.3.1 | development | no |
| node_modules/thenify-all | 1.6.0 | development | no |
| node_modules/tiny-warning | 1.0.3 | production/transitive | no |
| node_modules/to-regex-range | 5.0.1 | development | no |
| node_modules/ts-interface-checker | 0.1.13 | development | no |
| node_modules/tslib | 2.8.1 | production/transitive | no |
| node_modules/turbo-stream | 2.4.0 | production/transitive | no |
| node_modules/type-check | 0.4.0 | development | no |
| node_modules/typed-array-buffer | 1.0.3 | development | no |
| node_modules/typed-array-byte-length | 1.0.3 | development | no |
| node_modules/typed-array-byte-offset | 1.0.4 | development | no |
| node_modules/typed-array-length | 1.0.7 | development | no |
| node_modules/typescript | 5.8.2 | production/transitive | no |
| node_modules/unbox-primitive | 1.1.0 | development | no |
| node_modules/update-browserslist-db | 1.1.2 | development | no |
| node_modules/uri-js | 4.4.1 | development | no |
| node_modules/util-deprecate | 1.0.2 | development | no |
| node_modules/vite | 6.0.11 | development | no |
| node_modules/which | 2.0.2 | development | no |
| node_modules/which-boxed-primitive | 1.1.1 | development | no |
| node_modules/which-builtin-type | 1.2.1 | development | no |
| node_modules/which-collection | 1.0.2 | development | no |
| node_modules/which-typed-array | 1.1.18 | development | no |
| node_modules/word-wrap | 1.2.5 | development | no |
| node_modules/wrap-ansi | 8.1.0 | development | no |
| node_modules/wrap-ansi-cjs | 7.0.0 | development | no |
| node_modules/wrap-ansi-cjs/node_modules/ansi-regex | 5.0.1 | development | no |
| node_modules/wrap-ansi-cjs/node_modules/emoji-regex | 8.0.0 | development | no |
| node_modules/wrap-ansi-cjs/node_modules/string-width | 4.2.3 | development | no |
| node_modules/wrap-ansi-cjs/node_modules/strip-ansi | 6.0.1 | development | no |
| node_modules/wrap-ansi/node_modules/ansi-styles | 6.2.1 | development | no |
| node_modules/yallist | 3.1.1 | development | no |
| node_modules/yaml | 2.7.0 | development | no |
| node_modules/yocto-queue | 0.1.0 | development | no |
| node_modules/zustand | 5.0.3 | production/transitive | no |

</details>

## Appendix I: Maven outdated observations

The versions plugin also enumerates BOM-managed libraries that are not installed; those are not project dependencies. Selected direct observations below are newer candidates, not compatibility recommendations.

| Coordinate family | Current | Newer observed | Disposition |
| --- | --- | --- | --- |
| Spring Boot | 3.5.6 | 4.2.0-M1 | Do not adopt milestone; select supported stable patch with fresh release/advisory verification |
| Spring Security JOSE/test | 6.5.5 | 7.2.0-M1 | Do not adopt milestone or upgrade independently of Boot BOM |
| Flyway core / PostgreSQL module | 11.7.2 / 10.17.0 | 13.7.0 | First align current module/core; major migration needs separate compatibility testing |
| PostgreSQL JDBC | 42.7.7 | 42.7.13 | Evaluate compatible remediation through supported BOM |
| Lombok | 1.18.40 | 1.18.48 | Validate Java 21 annotation processing; not a runtime requirement |

## Appendix J: Source evidence anchors

| Finding | Immutable source |
| --- | --- |
| Contract transport | [source](https://github.com/Code-EJ/irr-frontend/blob/96f2d3908e51c808257780cca55c6035405e4217/src/bases/BaseService.ts) |
| Legacy login | [source](https://github.com/Code-EJ/irr-frontend/blob/96f2d3908e51c808257780cca55c6035405e4217/src/services/AuthService.ts) |
| Lint configuration | [source](https://github.com/Code-EJ/irr-frontend/blob/96f2d3908e51c808257780cca55c6035405e4217/eslint.config.js) |
| services/SortingService.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/services/SortingService.java) |
| services/PressingService.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/services/PressingService.java) |
| services/DocumentService.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/services/DocumentService.java) |
| services/AuthService.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/services/AuthService.java) |
| infrastructure/security/SecurityConfig.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/infrastructure/security/SecurityConfig.java) |
| filter/BearerFilter.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/filter/BearerFilter.java) |
| domain/ports/SortingPort.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/domain/ports/SortingPort.java) |
| domain/models/base/Attachment.java | [source](https://github.com/Code-EJ/irr-backend/blob/0e675044018f1cfb7c52f7c722aafc49a499d53a/src/main/java/org/code/api/domain/models/base/Attachment.java) |
