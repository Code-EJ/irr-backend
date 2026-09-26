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

On Linux/macOS use `cp .env.example .env`. Edit DB_PASSWORD and DEV_ADMIN_PASSWORD and replace both placeholders with different random local passwords. DEV_ADMIN_PASSWORD must contain 12-72 UTF-8 bytes. Your local .env contains the actual database and application login credentials; .env.example contains no usable shared credentials. Required/default variables:

| Variable | Meaning |
| --- | --- |
| COMPOSE_PROJECT_NAME | irr-development isolates this fresh baseline from old volumes |
| DEV_ADMIN_NAME / DEV_ADMIN_EMAIL | Display name and login email for the local administrator |
| DEV_ADMIN_PASSWORD | Local application login password, hashed before storage |
| DEV_BOOTSTRAP_ENABLED | true provisions the administrator only in the development profile |
| DB_USER | Database login; example irr_app |
| DB_PASSWORD | Required database password; never commit or publish it |
| DB_NAME | Database name; example irr |
| API_PORT | Host port; default 9191, bound to 127.0.0.1 |
| API_DOCS_ENABLED | true enables Swagger/OpenAPI; false disables both |

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

Use a new empty database for this consolidated baseline. Old Flyway volumes are not compatible with the new V1; keep them for explicit export/import if needed. For an isolated fresh environment, use docker compose -p irr-fresh up --build -d --wait and keep that same -p irr-fresh option on later commands.

The first build downloads dependencies/images and takes longer. Both database and backend should become healthy. PostgreSQL is internal to the Compose network and has no published host port. Default URLs (replace 9191 if API_PORT differs):

- API health: http://localhost:9191/actuator/health
- Readiness including PostgreSQL: http://localhost:9191/actuator/health/readiness
- **Official Swagger UI:** http://localhost:9191/swagger-ui/index.html
- **Official OpenAPI JSON:** http://localhost:9191/v3/api-docs
- OpenAPI YAML: http://localhost:9191/v3/api-docs.yaml

The API runs as UID 10001 with a read-only root filesystem. Database data and uploaded files use separate named volumes. This local stack is not configured for public internet deployment.

## 5. Exercise the API with Swagger

1. Open Swagger UI and expand Sessions. For administrator access, call POST /api/session/authenticate with DEV_ADMIN_EMAIL and DEV_ADMIN_PASSWORD from your local .env. Compose activates the development profile and provisions that account before readiness succeeds.
2. To test the separate representative registration flow in a fresh disposable development database, call POST /api/session/register with fullName, a unique email and a password of 8-72 characters. This currently creates a REPRESENTATIVE and returns a token. Public partner provisioning is still a planned identity change; do not expect a self-registered representative to write every resource.
3. Alternatively, call POST /api/session/authenticate with email/password for an existing development account. Read the token field in the response.
4. Click Authorize, paste the token itself (without adding the Bearer prefix), and confirm. Swagger sends Authorization: Bearer automatically.
5. Try a permitted GET such as /api/vehicles. A missing/invalid token gives 401; insufficient role access can give 403. Page lists use page/size/sort.
6. Log out using Authorize when finished. Swagger does not persist authorization across page reloads.

Example registration body for local testing only:

~~~json
{
  "fullName": "Development User",
  "email": "developer@example.test",
  "password": "local-test-password"
}
~~~

Swagger is generated from running controllers/DTOs and documents bearer authentication and current method role rules. It does not certify unresolved ownership, lifecycle or inventory behavior. The [source API inventory](docs/api_documentation.md) is supporting audit material; it is not a second official specification.

The development initializer updates the configured administrator password on restart if DEV_ADMIN_PASSWORD changes. It refuses to elevate an existing non-administrator with the same email. Set DEV_BOOTSTRAP_ENABLED=false to disable this initializer; it is never active outside the development profile. This is local test access, not the final partner-provisioning workflow.

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

The first command runs unit tests with generated ephemeral RSA keys and no database. The second packages the application and adds PostgreSQL integration tests through Testcontainers. Docker must be running, but docker compose up is not required. Integration tests create their own database and keys and never use your .env database credentials. They fail explicitly if Docker is unavailable.

Reports are in target/surefire-reports (unit) and target/failsafe-reports (integration). Tests cover authentication, token signing, storage containment, migration integrity, fresh baseline, incremental upgrade/rollback, competing balance insertion, stale updates, health and Swagger access/configuration. Full workflow acceptance is still being implemented.

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

Stopping preserves named volumes. The new database_data volume is separate from the old banco_dados_volume; old data is neither imported nor deleted automatically. Do not add --volumes unless you explicitly intend to discard a known disposable database and its uploads. See [operations](docs/operations.md) for migration preflight and recovery.

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
- [Fresh baseline and future migrations](docs/adrs/0007-fresh-database-baseline.md)
- [Database/entity inventory](docs/entities_documentation.md)
- [Dependencies and ownership](docs/dependencies.md)
- [Enum vocabulary](docs/enums_documentation.md)
- [Java source index](docs/source-inventory.md)
- [Contribution and JavaDoc rules](docs/feature_creation_workflow.md)
- [Execution evidence](docs/adrs/0006-backend-foundation-execution.md)

The foundation is implemented; the module/domain rewrite remains staged. Identity/provisioning, safe attachment DTOs and authorization, organization scoping, stock ledger/allocation and frontend contract migration remain explicit work. New/modified code and all maintained documentation use English, with Enzo Ribas attribution for this refactor. Preserve previous contributor credits.
