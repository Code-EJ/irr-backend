# ADR-0013: Backend operational contract and completion gates

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

- Status: Implemented and locally verified on 2026-09-27; frontend release pairing remains open.
- Supersedes: pending backend implementation statements in ADR-0001 through ADR-0012. Historical audit evidence remains unchanged.
- Scope: pre-production backend, schema V1–V6 and local Docker operations.

## Context

The original system lacked collection, team and sale boundaries, mixed creator ownership with shared stock and exposed incompatible frontend contracts. ADR-0011 establishes explicit organization ownership; ADR-0012 establishes transactional stock. The remaining backend work must close real workflows, recovery and documentation rather than merely declare CRUD methods complete. The owner has authorized a pre-production overhaul and a fresh baseline, but existing local records still require preservation.

## Decision drivers

1. Authenticate using the actual ignored environment configuration and exercise the deployed container.
2. Keep decimal arithmetic exact across Java, JSON and the future TypeScript client.
3. Preserve fiscal evidence and operational history while supporting safe corrections.
4. Recover both PostgreSQL and attachment bytes, including a crash between file creation and metadata commit.
5. Document implemented package boundaries and operational limits honestly.
6. Complete backend gates without claiming frontend, GitHub board access or public deployment verification.

## Considered options

| Option | Benefits | Costs / risks | Outcome |
| --- | --- | --- | --- |
| Declare completion after controller compilation | Fast | Misses concurrency, authorization, recovery and real configuration failures | Rejected |
| Delete and recreate local volumes for each upgrade | Simple setup | Conceals migration defects and destroys retained evidence | Rejected |
| Docker Compose, incremental SQL and isolated restore rehearsal | Reproducible local operation; exercises real data lifecycle | Requires snapshots and explicit operational scripts | Selected |
| Kubernetes and distributed stock services | Independent scaling | Operational overhead and distributed consistency without demonstrated demand | Deferred |
| Redis report cache or stock locks now | Potential latency reduction | Stale authorization/reports and duplicate consistency authorities | Rejected for this release |

## Decision

### Complete operational surfaces

The backend implements partner administration, organization lifecycle and memberships, three-level material classification, vehicles, donors, buyers, team members, collections, donations and attachments. Editable records support create/read/list/replace/deactivate with reference guards. Organization deactivation retains all history. Administrative organization writes do not grant automatic membership.

Sorting and pressing have create/read/list/reverse transitions. Sales have draft CRUD followed by post/reverse. These are deliberate lifecycle APIs: arbitrary editing or deletion of posted stock effects would violate the ledger. Inventory exposes balances, lots, movements and reconciliation; it has no generic stock-edit endpoint. Reports expose JSON and CSV summaries.

Donors accept a complete optional postal address. Collections accept route description, paired chronological departure/arrival timestamps and nonnegative distance. Driver assignments require DRIVER role; role changes and deactivation are blocked while referenced. V6 adds these fields and expands organization audit actions without rewriting V1–V5.

### Numeric and HTTP contract

Quantities use NUMERIC(15,4), validated before persistence. JSON decimal values are plain strings, including values whose Java representation would otherwise use scientific notation. Clients must use decimal arithmetic. Sale line totals use weight times unit price in BRL rounded HALF_UP to two decimal places; the header sums rounded lines. Price and financial totals are server-controlled.

All business requests require bearer authentication and X-Organization-Id. Membership, active account and active organization are checked server-side. Manager permissions govern reference data; active members may record operations. Idempotency-Key is required for sorting/pressing creation and every posting/reversal command. Platform role and organization role are different permissions.

Swagger UI and /v3/api-docs are the official contract. Unversioned business aliases are deprecated but retained until the frontend integration gate proves replacement. The legacy documento multipart part and PF/PJ donor codes remain explicit compatibility values; English documentation explains them. No removal date is invented before client verification.

### Reporting and attachment recovery

Reports query PostgreSQL in one repeatable-read snapshot. Inclusive UTC date ranges are limited to 366 days. Processing and sales count only currently POSTED records; reversal removes their contribution even from an older date range. Stock fields are the current snapshot, not historical closing stock. These semantics avoid misleading historical accounting claims. Backdated input is visible immediately because no report cache exists.

Upload transactions take a shared PostgreSQL advisory lock. An hourly orphan scan takes the corresponding exclusive lock before examining unreferenced, UUID-named regular files older than 24 hours. Recent files, symbolic links, unrelated names and all referenced files are preserved. Up to 500 candidates are processed per invocation; a process-local cursor progresses through filenames. Directory sorting is O(n), so large installations should move to paged object-storage reconciliation. Suspected orphans enter the existing durable cleanup queue; the scanner never directly deletes them. Database failures fail closed.

### Dependencies and deployment

Retain Java 21 and Spring Boot 3.5.16 for this compatibility baseline, with explicitly audited patch-family overrides listed in the dependency register. Remove unused devtools and obsolete report stubs. Do not combine a Boot 4 migration with the domain cutover. OSV results describe known coordinate matches at scan time, not a guarantee of security or a vendor support contract.

Docker Compose runs the non-root API, PostgreSQL and authenticated Redis with independent volumes. Only the loopback API port is published. Redis is provisioned and health-tested but does not hold stock, memberships or command receipts. Kubernetes is unnecessary for this development topology.

Development bootstrap creates an administrator and one explicit manager association only when absent. It never silently restores revoked access or deactivated organizations. The ignored .env holds database, Redis and application credentials plus development organization identity; keys remain outside source control. A separate Docker environment file is unnecessary.

## Evidence and acceptance gates

| Gate | Evidence | Result |
| --- | --- | --- |
| Unit and integration verification | Maven verify: 40 unit tests and 59 PostgreSQL/Redis integration tests, no failures or skips | Passed |
| Fresh schema and incremental upgrade | Flyway integration fixtures, failed-upgrade rollback, runtime V3 to V6 upgrade | Passed |
| Stock correctness | Concurrent sale winner/loser, conservation, idempotent replay, immutable ledger, reversal dependency order | Passed |
| Membership and reference integrity | Cross-organization denial, revoked access, manager writes, retained references | Passed |
| Attachments | Metadata privacy, durable cleanup, old orphan reconciliation and upload/scan lock exclusion | Passed |
| Container workflow | Actual .env login, donation -> sorting -> pressing -> sale -> reverse all, JSON/CSV reports, zero reconciliation differences | Passed |
| Restore rehearsal | Network-isolated PostgreSQL; six identical Flyway version/checksum pairs, 30 public tables including Flyway history, two attachment files, zero stock differences | Passed |
| Dependency coordinates | 156 resolved Maven dependencies; zero OSV matches on 2026-09-27 | Passed at scan time |
| Frontend compatibility | Client still needs migration and browser E2E | Pending frontend work |
| GitHub Projects reconciliation | Issue audit available; board access/URL not supplied | External follow-up |
| Public production deployment | No production environment or operational SLO supplied | Outside this local completion claim |

Local raw evidence is ignored under .local/release, .local/audit and .local/recovery. Backups contain secrets and must not be uploaded as CI artifacts. Automated tests use disposable databases, never the development volume.

## Consequences

The backend supports the documented pre-production workflow and is ready for frontend integration. Organization-level write serialization trades throughput for simple correctness; measure contention before finer-grained locking. Local disk storage limits horizontal API scaling. Fiscal evidence is an attachment requirement, not a tax-authority integration or malware-scanning service. Reports are operational summaries, not certified accounting statements. These are explicit scope boundaries, not hidden incomplete CRUDs.

Release pairing, browser tests and retirement of compatibility aliases remain frontend/release milestones. Preserve the old frontend until its replacement is verified. Restore scripts create and remove only labeled disposable containers and never remove the application's named volumes.
