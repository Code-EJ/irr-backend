# Development operations and database lifecycle

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Configuration

| Variable | Default / requirement | Purpose |
| --- | --- | --- |
| DB_HOST / DB_PORT | localhost / 5435 outside Compose; database / 5432 inside | PostgreSQL endpoint |
| DB_NAME / DB_USER | irr / irr_app | Development database identity |
| DB_PASSWORD | Required; local .env for Compose | Database credential |
| SERVER_PORT | 9191 | Application listener; Compose expects this container port |
| API_PORT | 9191 in Compose | Loopback host binding |
| RSA_PUBLIC_KEY / RSA_PRIVATE_KEY | file:./.local/keys/*.pem; overridden by Compose | External key locations |
| DEV_ADMIN_EMAIL / DEV_ADMIN_PASSWORD | Required when bootstrap enabled | Local administrator login; password hashed before persistence |
| DEV_ADMIN_NAME / DEV_BOOTSTRAP_ENABLED | Development Administrator / true in Compose | Explicit development-only provisioning |
| COMPOSE_PROJECT_NAME | irr-development | Fresh baseline isolation from old volumes |
| API_DOCS_ENABLED | true in development | Enables Swagger UI and OpenAPI; false disables anonymous documentation access |
| STORAGE_DIRECTORY | ./.local/uploads; /data/uploads in Compose | Persistent upload directory |

Spring Boot does not automatically load .env for a host JVM. Export variables before mvnw spring-boot:run. The default Compose database is not published; use Compose for the whole stack or explicitly configure a separate host-development database. Never point integration tests at that database. Tests derive credentials exclusively from their disposable container.

## Startup and persistence

Use the root README sequence. Generate keys using Java 21 once; the script refuses existing files. On Unix, restrict the local key directory to the development account; ensure the container's UID can read the mounted files. Local bind mounts do not provide a production secret manager. Database password changes in .env do not rotate a role password in an already initialized PostgreSQL volume.

Compose starts PostgreSQL, waits for pg_isready, then boots the API. Flyway applies SQL, Hibernate validates mappings, and readiness reports UP only after application/DB health succeeds. Check docker compose logs backend when health does not pass. A missing key file, invalid migration or wrong password is an error to fix, not a reason to disable validation.

The previous Compose volume banco_dados_volume is not the new database_data volume. This gives the refactor a fresh environment without deleting older data. To retain old data, export it deliberately, inspect its Flyway history, restore it to an isolated copy, run preflight and verify migrations there first. Old absolute attachment paths also need deliberate remapping/copying.

## Fresh database and future upgrades

The active Flyway path contains one English V1, documented in ADR-0007. Use an empty database or a new Compose project for this baseline. Keep the earlier volumes intact. Do not point the consolidated V1 at an old V1-V4 history, run repair, or enable automatic baselining to bypass checksum differences.

The archived SQL in docs/database/legacy-migrations is historical evidence, not a runtime migration location. When existing development data matters, export it, reconcile domain/ownership and attachment paths, then design an explicit import into an isolated new schema. A fresh database is not permission to silently discard old records.

Future shared changes use V2+ after the highest applied version. Add new DDL, test a populated-baseline upgrade, verify failures roll back, and document any necessary preflight/backfill. Unique/nonnegative inventory constraints are already part of the baseline.

## Recovery and backup

For valuable development data, stop writes and export PostgreSQL with pg_dump together with a consistent attachment archive and metadata manifest. Keep RSA keys outside the repository and outside published reports. Restore into a separate named project/database, validate Flyway history and attachment references, then run health and representative reads. Do not claim recoverability without a restore exercise. Docker volumes are persistence, not backups.

After a failed migration, inspect logs and transaction outcome before restarting. Transactional PostgreSQL migrations must leave the schema unchanged on failure, as checked by the integration fixture. Reconcile data in a reviewed follow-up; rerun migration on the isolated copy first. Once new application writes rely on new constraints/schema, rollback needs an explicitly tested prior application/schema pair; deleting an applied migration from source does not undo it.

## Verification commands

Use Java 21 and ./mvnw clean verify (Windows: .\mvnw.cmd clean verify). Docker must be running. Testcontainers creates its own random-port PostgreSQL instance and removes test containers after the JVM exits. A missing runtime fails tests. Integration classes end with IT; unit tests run in Surefire, integrations in Failsafe. Never use skipTests as release evidence; the Docker build skips test execution because CI performs verification separately.

For a container smoke test, create a unique Compose project name and an unused API_PORT so existing environments are untouched. Validate configuration with docker compose config --quiet, build, start with --wait, check /actuator/health/readiness, then stop that named project. Preserve volumes unless they were created solely as disposable test fixtures. Keep passwords out of command output and image layers.

## Known operating limits

This is a local single-host topology. Public TLS, backup scheduling, resource sizing, release image digests, least-privilege runtime/migration database roles and external secret management are later release work. No Kubernetes cluster is required by current evidence. The complete authorization/domain refactor remains in the master plan.

## Attachment cleanup operations

V2 is an additive upgrade of the fresh V1 database and must apply without a reset. Rebuild with docker compose up --build -d --wait. The cleanup scheduler runs every 60 seconds; irr.attachments.cleanup-enabled=false is reserved for deterministic tests or deliberate maintenance. A successful API delete returns 202 after committing metadata removal and the queue job, not after physical deletion.

Inspect retry backlog from a database session with:

~~~sql
SELECT id, created_at, attempts, next_attempt_at, last_error
FROM attachment_file_deletion
ORDER BY created_at;
~~~

Repeated failures require checking the attachment volume mount, storage availability and UID 10001 permissions. The worker retries idempotently; do not manually delete queue rows to conceal a storage error. A crash during upload can leave unreferenced bytes. Automated orphan reconciliation is not yet implemented: inspect metadata and backups before any manual cleanup, and never infer that a recently created file is an orphan while uploads are active. Backups must include both committed metadata and attachment content.
