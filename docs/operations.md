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

Compose starts PostgreSQL and authenticated Redis, waits for both health checks, then boots the API. Flyway applies SQL, Hibernate validates mappings, and readiness reports UP only after application/DB health succeeds. Check docker compose logs backend when health does not pass. A missing key file, invalid migration or wrong password is an error to fix, not a reason to disable validation.

ADR-0014 uses new database_final_v1 and attachment_final_v1 logical volumes, prefixed by the Compose project. Earlier database_data and attachment_data volumes remain intact. Redis retains its existing independent volume. Do not reconnect the final V1 image to an earlier migration history. Restore the matching previous application/database snapshot for rollback, or prepare a deliberate data import into the new baseline.

## Fresh database and future upgrades

The active Flyway path contains only the final V1__initial_schema.sql established by ADR-0014. It creates the verified final schema, including all organization, ledger, logistics and recovery constraints. Use a fresh database. Never run Flyway repair or baseline-on-migrate to conceal a mismatch with the unreleased predecessor.

Earlier SQL is recoverable from Git history at commit 3ca0cab. Duplicate SQL archives were removed from the maintained tree. The private pre-consolidation backup and old volumes preserve actual local data. Existing creator identities are insufficient evidence for organization mapping.

Future shared changes use V2+ after the highest applied version. Add new DDL, test a populated-baseline upgrade, verify failures roll back, and document any necessary preflight/backfill. Unique/nonnegative inventory constraints are already part of the baseline.

## Recovery and backup

For valuable development data, stop writes and export PostgreSQL with pg_dump together with a consistent attachment archive and metadata manifest. Keep RSA keys outside the repository and outside published reports. Restore into a separate named project/database, validate Flyway history and attachment references, then run health and representative reads. Do not claim recoverability without a restore exercise. Docker volumes are persistence, not backups.

After a failed migration, inspect logs and transaction outcome before restarting. Transactional PostgreSQL migrations must leave the schema unchanged on failure, as checked by the integration fixture. Reconcile data in a reviewed follow-up; rerun migration on the isolated copy first. Once new application writes rely on new constraints/schema, rollback needs an explicitly tested prior application/schema pair; deleting an applied migration from source does not undo it.

## Verification commands

Use Java 21 and ./mvnw clean verify (Windows: .\mvnw.cmd clean verify). Docker must be running. Testcontainers creates its own random-port PostgreSQL instance and removes test containers after the JVM exits. A missing runtime fails tests. Integration classes end with IT; unit tests run in Surefire, integrations in Failsafe. Never use skipTests as release evidence; the Docker build skips test execution because CI performs verification separately.

For a container smoke test, create a unique Compose project name and an unused API_PORT so existing environments are untouched. Validate configuration with docker compose config --quiet, build, start with --wait, check /actuator/health/readiness, then stop that named project. Preserve volumes unless they were created solely as disposable test fixtures. Keep passwords out of command output and image layers.

## Known operating limits

This is a local single-host topology. Public TLS, backup scheduling, resource sizing, release image digests, least-privilege runtime/migration database roles and external secret management are later release work. No Kubernetes cluster is required by current evidence. The local authorization/domain workflow is verified; public deployment and frontend release pairing remain separate gates.

## Attachment cleanup operations

The final V1 includes the durable deletion queue. Future changes use additive V2+ migrations. Rebuild with docker compose up --build -d --wait. The cleanup scheduler runs every 60 seconds; irr.attachments.cleanup-enabled=false is reserved for deterministic tests or deliberate maintenance. A successful API delete returns 202 after committing metadata removal and the queue job, not after physical deletion.

Inspect retry backlog from a database session with:

~~~sql
SELECT id, created_at, attempts, next_attempt_at, last_error
FROM attachment_file_deletion
ORDER BY created_at;
~~~

Repeated failures require checking the attachment volume mount, storage availability and UID 10001 permissions. The worker retries idempotently; do not manually delete queue rows to conceal a storage error. A crash during upload can leave unreferenced bytes. The hourly orphan reconciler uses a 24-hour grace period and an exclusive advisory lock against upload transactions; inspect metadata and backups before any manual cleanup. Backups must include both committed metadata and attachment content.

## Organizations and Redis execution update

The final V1 creates 29 application tables. Organization membership and access audit are part of that baseline, and no ownership is inferred from the creator. Administrative effects and audit records commit together. Test-only incremental V2 migrations remain outside the runtime image.

Compose now starts PostgreSQL, authenticated Redis and the API. Redis uses the internal network, a separate persistent volume and no host port. Both data services must pass health checks before API startup; readiness includes both. Redis credentials are required in the ignored .env and should be rotated together on Redis/backend recreation. The [README Redis workflow](../README.md#redis-start-check-and-test) provides commands. Redis AOF is neither a PostgreSQL backup nor a stock/authorization authority.

## Current verified recovery procedure

Use Node.js 22.6+ and run node scripts/backup-development.mjs from the checkout. It briefly stops the API, creates a custom-format pg_dump and attachment tarball, copies .env and RSA keys, and writes SHA-256 hashes plus the current Git commit and Flyway checksums. Its finally block restarts the API. Keep this directory private; filesystem permissions on Windows must be managed using the account ACL. If the command fails, inspect the partial snapshot and do not treat it as complete without manifest.json.

Run node scripts/verify-recovery.mjs followed by the printed backup directory. The tool checks every manifest hash, creates a labeled PostgreSQL container with no network and no host ports, restores SQL with exit-on-error, compares migration checksums, extracts attachments and checks stock against both ledger and lots when those tables exist. It removes only its labeled disposable container and anonymous volume. The live stack and named volumes are untouched. This proves backup readability and database consistency; it does not automatically roll back the live application.

Historical ADR-0013 verification on 2026-09-27: the pre-upgrade V1–V3 snapshot restored; the post-upgrade V1–V6 snapshot restored with 30 public tables (including Flyway history), two attachment fixture files and zero stock differences. Checksums are recorded in local manifests. Backup retention and off-machine encrypted storage remain the operator responsibility before any valuable deployment.

## Organization bootstrap and legacy records

DEV_ORGANIZATION_ID and DEV_ORGANIZATION_NAME identify one development organization. Bootstrap creates its explicit administrator MANAGER association only on first creation. It does not restore revoked access or inactive organizations. Keep the UUID stable across restarts. Production has no development bootstrap.

The preserved predecessor database may contain old business rows with null organization_id. They are deliberately invisible through scoped APIs; creator_id is not a valid organization mapping. A legacy import requires reviewed ownership, reference closure and opening-stock provenance. Do not guess ownership, alter Flyway checksums or expose null-scoped data.

## Monitoring and cleanup

Check readiness for PostgreSQL and Redis, liveness for the process, and Docker health. A healthy process does not prove inventory consistency: use the authenticated organization reconciliation endpoint. Investigate any nonzero difference before further operational writes; never overwrite immutable movements to make a report match.

Attachment deletion queues persist in PostgreSQL. The existing worker retries failed physical deletion. The hourly orphan reconciler scans at most 500 eligible candidates per invocation after a 24-hour grace period, with a cursor and an exclusive database advisory lock that excludes upload transactions. Local directory sorting still traverses the directory; monitor volume growth and move to paged storage reconciliation if scale requires it. Do not manually delete referenced bytes.

Reports are uncached UTC operational summaries; reversals change the current-status interpretation of older periods. Redis holds no recoverable business truth today. Readiness depends on Redis by explicit ADR-0010 policy; stop/start Redis only during a controlled local resilience test.

## Final pre-production consolidation

ADR-0014 supersedes the active V1–V6 chain and compatibility API. All application requests now use /api/v1, including session authentication, account administration and organizations. Documentation and actuator infrastructure retain their framework URLs. Current restore evidence must contain one successful baseline version, not the historical six-row sequence.

The independent schema fixture records 560 column/default/nullability, constraint, index, trigger and domain-function definitions from the verified predecessor. FinalBaselineSchemaIT compares the fresh schema exactly after removing schema qualification and whitespace. Test-only V2 examples prove future upgrades and transactional failure rollback. Production migration discipline begins with this final shared baseline.

Final ADR-0014 rehearsal on 2026-09-27 restored V1 checksum 583196656, 30 public tables including Flyway history, one uploaded evidence file and zero stock differences. The script also verifies that every restored attachment metadata reference resolves to a canonical file in the archive. Earlier six-migration results above are historical only.
