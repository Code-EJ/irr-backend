# IRR backend

- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

IRR manages waste intake, materials, processing, inventory and sales. This is a pre-production Java 21 / Spring Boot / PostgreSQL application. Docker Compose is the supported local runtime. Swagger UI and its generated OpenAPI document are the official HTTP API documentation. ADRs document architecture and staged implementation.

## 1. Check prerequisites

Install Java 21 and Docker Desktop (Linux containers) or Docker Engine with Compose v2. A separate Maven installation is unnecessary because the Maven wrapper is included. Run all commands below from this repository root.

~~~sh
java -version
docker version
docker compose version
~~~

Java must report version 21. Docker must report a reachable server, not only a client. Start Docker Desktop before integration tests or Compose. On Windows, point JAVA_HOME to the JDK 21 directory and put its bin directory first in PATH; a preinstalled Java 8 executable will not work.

## 2. Prepare the environment file

If .env already exists, keep it. Do not overwrite working credentials with the example. For a new checkout:

~~~powershell
Copy-Item .env.example .env
~~~

On Linux/macOS use `cp .env.example .env`. Edit DB_PASSWORD, DEV_ADMIN_PASSWORD and REDIS_PASSWORD and replace all three placeholders with different random local passwords. DEV_ADMIN_PASSWORD must contain at least 12 and fewer than 72 UTF-8 bytes. Your local .env contains the actual database, Redis and application login credentials; .env.example contains no usable shared credentials. Required/default variables:

| Variable | Meaning |
| --- | --- |
| COMPOSE_PROJECT_NAME | irr-development isolates this fresh baseline from old volumes |
| DEV_ADMIN_NAME / DEV_ADMIN_EMAIL | Display name and login email for the local administrator |
| DEV_ADMIN_PASSWORD | Local application login password, hashed before storage |
| DEV_BOOTSTRAP_ENABLED | true provisions the administrator and explicit development organization association only in the development profile |
| DEV_ORGANIZATION_ID / DEV_ORGANIZATION_NAME | Stable development UUID and display name; bootstrap grants the administrator MANAGER once |
| DB_USER | Database login; example irr_app |
| DB_PASSWORD | Required database password; never commit or publish it |
| REDIS_PASSWORD | Required independent Redis password; internal Docker network only |
| DB_NAME | Database name; example irr |
| API_PORT | Host port; default 9191, bound to 127.0.0.1 |
| API_DOCS_ENABLED | true enables Swagger/OpenAPI; false disables both |

CORS_ALLOWED_ORIGINS is a comma-separated browser origin allowlist; local defaults cover http://localhost:5173 and http://127.0.0.1:5173. Update it if the frontend runs elsewhere, then recreate the backend container. A separate Docker .env is unnecessary: the shared file already supplies Compose and the development administrator.

Compose automatically reads .env in this directory. DB_HOST and DB_PORT from older .env files are not used for container connectivity: the backend always uses database:5432 inside Compose. Older POSTGRES_USER/POSTGRES_PASSWORD/POSTGRES_DB entries are redundant: Compose derives PostgreSQL configuration from DB_USER/DB_PASSWORD/DB_NAME. JWT_SECRET is not used; this backend signs RSA JWTs with the key files generated below. Existing unused entries may be removed manually after confirming no other local scripts need them.

Environment variables already exported in your shell take precedence over .env during Compose interpolation. Avoid conflicting exports. Use `docker compose --env-file .env config --quiet` to validate syntax without printing resolved secrets. Avoid pasting the unrestricted output of docker compose config into issues or chat.

Changing DB_PASSWORD in .env does not change the database role password in an existing initialized volume. If authentication fails after a change, restore the correct value or deliberately rotate that role in PostgreSQL. Do not delete data volumes as a default fix.

## 3. Generate development RSA keys

~~~sh
java scripts/GenerateDevelopmentKeys.java
~~~

This creates .local/keys/private.pem and public.pem. They are ignored by Git and excluded from the Docker build context. If the script says keys already exist, keep them and continue; it refuses implicit rotation. Do not generate or commit shared real credentials. Keys are mounted read-only into the API container.

## 4. Build and start

~~~sh
docker compose --env-file .env config --quiet
docker compose up --build -d --wait
docker compose ps
~~~

ADR-0014 establishes the final fresh V1. Compose now uses database_final_v1 and attachment_final_v1 volumes, preserving earlier development volumes. Existing V1–V6 databases are not upgraded in place or repaired; retain their backups for deliberate import if needed. For an isolated fresh environment, use docker compose -p irr-fresh up --build -d --wait and keep that same -p irr-fresh option on later commands.

The first build downloads dependencies/images and takes longer. All three services should become healthy. PostgreSQL is internal to the Compose network and has no published host port. Default URLs (replace 9191 if API_PORT differs):

- API health: http://localhost:9191/actuator/health
- Readiness including PostgreSQL and Redis: http://localhost:9191/actuator/health/readiness
- **Official Swagger UI:** http://localhost:9191/swagger-ui/index.html
- **Official OpenAPI JSON:** http://localhost:9191/v3/api-docs
- OpenAPI YAML: http://localhost:9191/v3/api-docs.yaml

The API runs as UID 10001 with a read-only root filesystem. PostgreSQL, Redis AOF data and uploaded files use separate named volumes. Redis uses internal port 6379 with no host publication. This local stack is not configured for public internet deployment.

## 5. Exercise the API with Swagger

1. Open Swagger UI and expand Sessions. Call POST /api/v1/session/authenticate with DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD from your ignored local .env. Compose activates the development profile and provisions that administrator before readiness succeeds.
2. Copy the token field. Click Authorize, paste the token itself (without the Bearer prefix), and confirm. Swagger sends Authorization: Bearer automatically.
3. Expand Partners and call POST /api/v1/users with the example below. Only an administrator may create CITY_HALL, ORGANIZATION or REPRESENTATIVE partners. Passwords require at least eight characters and fewer than 72 UTF-8 bytes. The response contains safe metadata and does not log you in as the new partner.
4. To test partner access, authenticate separately using that partner's email/password and replace the Swagger authorization token. Missing/invalid authentication returns 401; insufficient roles return 403.
5. Call GET /api/v1/organizations and select the development organization from .env. Supply its UUID as X-Organization-Id on business operations, for example GET /api/v1/vehicles. New partners need an explicit association through PUT /api/v1/organizations/{id}/members/{userId}. Page lists use page/size/sort and return content/totalElements.
6. For attachments, upload a small PDF, PNG or JPEG with POST /api/v1/documents using the documento multipart field. Record the returned id. Active organization members can download it; its uploader or an organization manager can rename or delete unreferenced metadata. Platform administrator status does not bypass membership. Unsupported signatures/names return 400; foreign IDs return 404; referenced documents return 409. Deletion returns 202 after metadata removal and queues physical cleanup, normally processed within the next minute.
7. Log out using Authorize when finished. Swagger does not persist authorization across page reloads.

Example partner body for local testing only:

~~~json
{
  "fullName": "Development Partner",
  "email": "partner@example.test",
  "password": "local-test-password",
  "userRole": "REPRESENTATIVE"
}
~~~

Public self-registration and all unversioned application routes have been removed under ADR-0014. Create partners only through the administrator operation POST /api/v1/users. No redirect or retirement stub is supported.

Swagger is generated from running controllers/DTOs and documents bearer authentication and current method role rules. Runtime authorization and business invariants are independently verified by integration tests. The [source API inventory](docs/api_documentation.md) is supporting audit material; it is not a second official specification.

The development initializer updates the configured administrator password on restart if DEV_ADMIN_PASSWORD changes. It refuses to elevate an existing non-administrator with the same email. Set DEV_BOOTSTRAP_ENABLED=false to disable this initializer; it is never active outside the development profile. This initializer supplies local administrator access; partner creation uses the administrator-only endpoint.

## 6. Run automated tests

Windows PowerShell:

~~~powershell
.\mvnw.cmd test
.\mvnw.cmd clean verify
~~~

Linux/macOS:

~~~sh
./mvnw test
./mvnw clean verify
~~~

Spotless 3.10.3 runs in the Maven validate phase and must pass for both commands. After editing Java, run .\mvnw.cmd spotless:apply (or ./mvnw spotless:apply on Unix), review the diff, then verify. Do not replace the configured formatter with an unrelated tool.

The first command runs unit tests with generated ephemeral RSA keys and no database. The second packages the application and adds PostgreSQL and Redis integration tests through Testcontainers. Docker must be running, but docker compose up is not required. Integration tests create their own PostgreSQL/Redis services and keys and never use your .env credentials. They fail explicitly if Docker is unavailable.

Reports are in target/surefire-reports (unit) and target/failsafe-reports (integration). Tests cover authentication, token signing, storage containment, migration integrity, fresh baseline, incremental upgrade/rollback, competing balance insertion, stale updates, health and Swagger access/configuration. The complete organization-scoped intake, processing, sale, reversal and reconciliation workflow is covered. Final consolidation also checks all application routes and 560 independently captured schema definitions.

To build just the runtime image:

~~~sh
docker build -t irr-backend:local .
~~~

The image build skips test execution; it does not replace clean verify. CI runs verification before the image build.

## 7. Logs, stop and restart

~~~sh
docker compose logs --tail=100 backend
docker compose logs --tail=100 database
docker compose down
docker compose up -d --wait
~~~

Stopping preserves named volumes. The final database_final_v1 and attachment_final_v1 volumes are separate from earlier database_data, attachment_data and banco_dados_volume volumes; old data is neither imported nor deleted automatically. Do not add --volumes unless you explicitly intend to discard a known disposable database and its uploads. See [operations](docs/operations.md) for migration preflight and recovery.

## Troubleshooting

| Symptom | Action |
| --- | --- |
| Docker named pipe/socket missing | Start/restart Docker Desktop, then confirm docker version shows the server |
| Invalid source release 21 / old Java | Fix JAVA_HOME and PATH to use JDK 21 |
| Missing RSA key | Run the key generator in the repository root; keep both files in .local/keys |
| DB_PASSWORD interpolation error | Set a nonempty value in .env and rerun config --quiet |
| Database authentication fails | Check the credential used when that volume was initialized; .env edits do not rotate existing roles |
| Port already allocated | Change API_PORT to an unused host port, then recreate the backend |
| Swagger returns 401/404 | Set API_DOCS_ENABLED=true and run docker compose up -d --force-recreate backend |
| Flyway checksum/history mismatch | Use a new empty database for the consolidated baseline; retain the old volume for explicit export/import |
| Testcontainers cannot reach Docker | Check daemon/server availability; never redirect integration tests to a personal database |

## Architecture and implementation status

- [Master plan](docs/adrs/0001-master-plan.md)
- [Accepted Docker/pre-production architecture](docs/adrs/0005-preproduction-docker-foundation.md)
- [Repository architecture and module map](docs/architecture/readme.md)
- [Final supported API and fresh baseline](docs/adrs/0014-final-preproduction-contract-and-baseline.md)
- [Database/entity inventory](docs/entities_documentation.md)
- [Dependencies and ownership](docs/dependencies.md)
- [Enum vocabulary](docs/enums_documentation.md)
- [Java source index](docs/source-inventory.md)
- [Contribution and JavaDoc rules](docs/feature_creation_workflow.md)
- [Execution evidence](docs/adrs/0006-backend-foundation-execution.md)

The backend supports the documented pre-production workflow. ADR-0014 freezes the single /api/v1 application contract and the final fresh baseline. The pragmatic modular monolith remains; frontend integration is the next milestone. Maintained JavaDoc, documentation and commits are English and credit Enzo Ribas while preserving contributor attribution.

## Organization membership workflow

1. Authenticate as the development administrator through Swagger.
2. Under Organizations, POST /api/v1/organizations with name and organizationType, for example an association name and the descriptive value ASSOCIATION. Record the returned id.
3. Under Partners, create an account if needed and record its id.
4. PUT /api/v1/organizations/{id}/members/{userId} with {"role":"MEMBER"} or {"role":"MANAGER"}. Both are local membership roles; management of associations still requires a platform administrator.
5. Authenticate as that member. GET /api/v1/organizations lists only active associations; GET /api/v1/organizations/{id} returns the selected organization. The creating administrator also needs explicit membership for these read routes.
6. As administrator, DELETE the membership. The existing member token immediately loses access to organization reads. Missing/foreign/inactive scopes return 404.

Catalog, intake, processing, sales and attachments require the explicit organization header and current membership. Creator identity is audit metadata, not ownership. Revocation takes effect immediately; no platform administrator bypass exists.

## Redis: start, check and test

The existing docker compose up --build -d --wait command starts all three services. The ignored .env contains REDIS_PASSWORD; .env.example contains only a placeholder. The backend uses redis:6379 internally. Do not publish 6379 to use local development tools; execute redis-cli inside its container:

~~~powershell
docker compose ps
docker compose exec redis sh -c 'REDISCLI_AUTH="$REDIS_PASSWORD" redis-cli PING'
~~~

The authenticated command must print PONG. An unauthenticated docker compose exec redis redis-cli PING returns NOAUTH. To inspect persistence, use the same authenticated command prefix with INFO persistence; appendonly must be enabled. Do not print resolved Compose configuration or credentials in bug reports. Redis uses append-only persistence in redis_data, with one-second fsync policy, a 128-MiB data limit and noeviction. Readiness includes Redis; a stopped/unreachable Redis makes /actuator/health/readiness report unavailable. Liveness is independent.

For routine restarts, run docker compose restart redis, then check PING and readiness. For a password change, update .env and run docker compose up -d --force-recreate redis backend so both services receive the new credential. Preserve volumes; do not use down -v. No separate .env.docker is required.

Run .\mvnw.cmd clean verify (PowerShell) or ./mvnw clean verify (Linux/macOS). Docker must be running: integration tests launch isolated PostgreSQL and authenticated Redis with random ports and fixture credentials. RedisInfrastructureIT checks connection, authentication and TTL expiry; it never connects to your development Redis. The backend currently configures Redis infrastructure and health, not an automatic business cache. [ADR-0010](docs/adrs/0010-containerized-redis.md) defines its limits and future key policy.

## Complete workflow and recovery verification

Install Node.js 22.6+ for the optional operational scripts (the Java application does not require Node). After Compose reports healthy, run:

~~~sh
node scripts/smoke-development.mjs
node scripts/backup-development.mjs
node scripts/verify-recovery.mjs .local/recovery/<timestamp-printed-by-backup>
~~~

The smoke script reads login and organization values from .env, creates retained development fixtures, tests intake, sorting, pressing, fiscal sale posting, reversals, reports, exact decimals and isolation. It saves sanitized evidence under .local/release. Existing exported environment variables take precedence over .env. Never run this fixture script against production.

The backup script briefly stops API writes, snapshots PostgreSQL and attachments, includes .env and RSA keys, then restarts the API. Backups contain secrets: keep .local/recovery private and do not commit/upload it. Restore rehearsal verifies checksums in an isolated container and removes only that labeled disposable container. It does not overwrite the live database or its volumes. See [operations](docs/operations.md).

Posted sorting/pressing/sales use explicit reversal commands, not destructive CRUD. Each posting command needs an Idempotency-Key; retries retain that same key and payload. Quantities and money are JSON strings. Swagger is the official field-level reference. [ADR-0014](docs/adrs/0014-final-preproduction-contract-and-baseline.md) records the final consolidation and supersedes the transitional portions of ADR-0013; frontend integration and a compatible release pair remain separate gates.
