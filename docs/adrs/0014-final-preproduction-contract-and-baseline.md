# ADR-0014: Final pre-production API contract and database baseline

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Status: Accepted, implemented and locally verified on 2026-09-27.
- Scope: Final backend consolidation before frontend integration.
- Architecture: The pragmatic modular monolith in ADR-0003 remains in force.

## Context

ADR-0013 established a locally verified backend with retained compatibility routes and a six-step development migration history. The owner confirms that there is no production deployment and no supported external consumer of those routes. Keeping transitional contracts would unnecessarily teach the new frontend an API already scheduled for removal. The owner therefore authorizes removal before freezing the frontend contract and a final fresh database baseline.

Recent Maven/Spotless, formatting and Docker changes are intentional owner work. They must be reviewed and verified, not replaced by another formatting tool or architectural rewrite. Existing working and staged changes must be preserved. Local databases, attachments, environment credentials and signing keys still require backups; pre-production status does not make silent deletion appropriate.

## Decision drivers

1. Publish one supported versioned API without duplicate routes or deprecated registration stubs.
2. Preserve authorization, organization ownership, fiscal evidence, exact quantities, stock conservation, command replay and reversal semantics.
3. Establish a readable fresh schema without replaying unreleased development corrections.
4. Keep future migration discipline demonstrable through incremental-upgrade and rollback tests.
5. Respect intentional owner tooling changes and avoid unnecessary package/framework churn.
6. Preserve a recoverable pre-consolidation snapshot and prove the new baseline locally.

## Considered options

| Option | Benefits | Costs and risks | Decision |
| --- | --- | --- | --- |
| Keep aliases until frontend integration | Existing local clients continue working | Duplicate unsupported contracts enter the frontend and documentation | Rejected by owner |
| Redirect old routes to versioned routes | Superficial compatibility | Still retains transitional behavior, with method/authentication ambiguity | Rejected |
| Remove aliases and version every application endpoint now | One contract; small explicit frontend target | Existing local scripts/tests must change together | Selected |
| Keep all six unreleased migrations forever | Preserves development chronology | Retains historical restructuring in every fresh setup | Rejected for this final pre-production reset |
| One final schema baseline, followed by future additive migrations | Fast reproducible setup; clear schema and evolution policy | Existing development volumes cannot be upgraded in place by checksum replacement | Selected with preserved snapshots and fresh volumes |
| Strict Clean Architecture or separate microservices | Stronger formal isolation | High churn without new business value | Rejected; ADR-0003 remains authoritative |

## Decision

### One supported application API

All application controller routes use `/api/v1`, including `/api/v1/session/authenticate`, `/api/v1/users` and `/api/v1/organizations`. Business controllers expose only their versioned route. Remove unversioned mappings, alias-only Swagger logic and the deprecated self-registration endpoint. Do not introduce redirects, forwarding adapters or another compatibility layer.

Swagger resources (`/swagger-ui/**`, `/v3/api-docs`) and infrastructure health (`/actuator/health/**`) keep their framework paths; these are documentation/operations endpoints, not alternate application APIs. The only anonymous application operation is the exact versioned login route. Old paths have no controller mapping; authenticated calls must return 404, while anonymous requests may be rejected by the authentication boundary first. Tests must assert mapping absence as well as runtime denial.

Preserve bearer authentication, explicit X-Organization-Id, current membership checks, role separation, exact decimal strings, optimistic versions and idempotency keys. This decision changes route support, not domain authorization. Retained wire field names and enum codes such as `documento` and `PF`/`PJ` are not HTTP aliases and are not renamed incidentally.

### Final fresh baseline

Replace the active unreleased V1–V6 chain with one schema-only `V1__initial_schema.sql` describing the verified final tables, indexes, foreign keys, checks, functions and triggers. Preserve the established domain semantics; do not remove audit/history structures merely because their names predate this consolidation. Verify schema equivalence against the pre-consolidation database, excluding Flyway history and schema qualification.

Do not concatenate migration archaeology, run Flyway repair, enable automatic baselining or overwrite the history of an existing database. Preserve the old chain in Git history and the old database/attachment volumes plus private backups. Start the final baseline in new explicitly named development volumes. No live records are implicitly assigned to an organization or copied into the fresh database. Any later data import requires an explicit, validated relationship and ownership mapping.

After this consolidation, new shared schema changes start at V2 and never edit an applied baseline. Test-only V2 examples prove populated-baseline upgrade, repeated execution and transactional rollback. They remain outside the runtime migration location. Remove obsolete migration-archive copies from the maintained runtime documentation surface after their Git history and recovery evidence are secured.

### Tooling and architecture continuity

Retain and validate the owner's Spotless plugin and its `validate` lifecycle check, Maven dependency versions, line-ending policy and Docker build/runtime design. Use the configured Spotless workflow for subsequent Java changes. Review Docker build behavior with the formatting gate enabled, preserve non-root/read-only execution and private volumes, and fix only demonstrated defects.

Keep feature modules plus the established shared JPA/application adapters. Apply KISS, SOLID, DRY and YAGNI to concrete duplication and boundary problems. No framework-free domain rewrite, blanket port extraction, Kubernetes adoption or package-only reorganization is authorized by this ADR.

## Superseded decisions

| Earlier record | Superseded portion | Replacement |
| --- | --- | --- |
| ADR-0011, HTTP transition and deprecation | Retain unversioned aliases until frontend cutover | Remove all transitional mappings before frontend integration |
| ADR-0011, migration/recovery | Preserve the unreleased V1–V5 chain as the active upgrade path | Preserve its data/history externally; use the final fresh V1 in new volumes |
| ADR-0013, numeric and HTTP contract | Retain deprecated business aliases and unversioned identity/organization routes | One `/api/v1` application contract |
| ADR-0013, deployment/evidence | V1–V6 is the current runtime baseline and 134 operations includes aliases | Historical evidence only; regenerate operation/schema/recovery evidence for final V1 |
| ADR-0013, frontend handoff | Frontend may start against the locally verified transitional contract | Freeze the consolidated contract only after the checks below pass |

The ownership, transaction, ledger, reporting, attachment recovery and modular-monolith decisions in ADR-0011/0013 remain in force. Historical local validation results remain factual but are not substitutes for this consolidation's verification.

## Execution and validation gates

1. Record this ADR before removing routes or replacing SQL. Preserve an exact working-tree/index snapshot of intentional owner changes and a consistent database/attachment backup.
2. Review and run the owner's formatting/build configuration. Commit coherent changes without sweeping unrelated staged work into the ADR commit.
3. Update controllers, security, Location headers, Swagger, tests, smoke/recovery scripts and maintained guides to the single API contract. Assert no application mapping outside `/api/v1` and no retired signup route.
4. Compare the final fresh schema against the verified predecessor. Run fresh-install, future-V2 upgrade and rollback tests with disposable PostgreSQL; retain every domain/integrity/concurrency test.
5. Start Docker against fresh named database and attachment volumes, authenticate from the private `.env`, and repeat the full processing/sale/reversal/report workflow.
6. Back up and restore the final baseline in an isolated container; compare migration history, archive integrity and ledger/lot/projection reconciliation.
7. Refresh ADR-0001/0013 pointers, schema/API inventories, README and release evidence. Record actual test counts, operation count and outstanding external/frontend gates. Only then hand the contract to frontend integration.

## Consequences

The new frontend has one stable route family and one fresh schema. Existing local scripts using old paths must be updated; this is an intentional pre-production breaking change with no supported-consumer deprecation window. Old development data remains recoverable but is not automatically imported. Future migrations regain immutability after this explicitly authorized reset. Docker Compose remains sufficient, and the consolidation does not claim production deployment or completed frontend integration.

## Final execution evidence

- Decision recorded first in commit 3ca0cab; owner Maven/Spotless and line-ending policy preserved in dd355e5. Runtime contract/schema consolidation committed in 6f449e4.
- Maven Spotless apply/check and clean verify passed: 40 unit tests plus 62 integration tests, zero failures/errors/skips.
- FinalBaselineSchemaIT matched all 560 independently captured predecessor definitions. Future V2 upgrade, idempotent migration, failed-DDL rollback and changed-baseline rejection passed.
- Application handler and Swagger tests reject unversioned routes, deprecated operations and the signup stub. The running specification contains 89 supported operations.
- Docker rebuilt successfully with the configured Maven formatting gate. API, PostgreSQL and Redis are healthy. Development login used the real ignored .env.
- The full donation/sorting/pressing/sale/reversal/report workflow passed with stable replay, exact decimal strings, foreign-organization denial and zero stock differences.
- Final backup restored in a network-isolated container: one V1 (Flyway checksum 583196656), 30 public tables including history, one evidence attachment and zero ledger/lot/projection differences.
- Old database_data and attachment_data volumes remain present beside database_final_v1 and attachment_final_v1; Redis persistence was retained. The pre-consolidation backup and exact working/index snapshot remain private under .local.
- Frontend integration is the next major milestone. No supported external consumer or production migration is claimed. GitHub Projects reconciliation remains an external access follow-up, not a backend-runtime defect.
