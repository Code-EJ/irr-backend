# ADR-0007: Fresh pre-production database baseline and incremental evolution

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Current implementation reference: [ADR-0013](0013-backend-operational-completion.md) records the completed backend and verified local operations. This ADR retains its decision-time evidence; later records supersede pending implementation statements.


> Subsequent implementation: [ADR-0008](0008-identity-and-attachment-boundaries.md) adds runtime V2 for durable attachment cleanup. The initial V1 remains unchanged; the active schema now has 22 application tables. Incremental example tests now use V3. The baseline decision below records its original state.

- Status: Accepted for the current backend foundation.
- Date: 2026-09-26.
- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: the owner explicitly requested a fresh database instead of accumulating pre-production migrations, while retaining a step-by-step migration process.
- Supersedes: ADR-0005's decision to keep V1-V4 plus V5 in the active runtime path, and ADR-0002's production-oriented migration prerequisites for this environment.

## Context

IRR has no production deployment. Multiple small historical migrations describe intermediate development states rather than a released compatibility contract. Keeping these as the only way to understand a fresh schema adds unnecessary indirection. Existing development volumes can still contain valuable data and must not be dropped as a side effect of choosing a cleaner baseline.

## Decision drivers

1. A new developer can read one English SQL file to understand the current implemented schema.
2. Fresh startup and JPA mappings remain verifiable on PostgreSQL.
3. Subsequent changes retain a real migration mechanism, immutable shared versions and data-preserving tests.
4. Historical schema evidence remains available without executing it on fresh databases.
5. A current implementation baseline must not be mislabeled as the completed organization/ledger redesign.

## Considered options

| Option | Advantages | Disadvantages | Decision |
| --- | --- | --- | --- |
| Retain every intermediate migration as the active startup path | Easy compatibility with old local histories | Harder to understand an unreleased initial schema | Replaced at the owner's request |
| One fresh V1, archive history, use V2+ for later changes | Clear current schema and disciplined future evolution | Existing local histories need an explicit new database or export/import decision | Selected |
| Let Hibernate recreate/update tables | Fewer SQL files | Loses reviewed DDL, constraints and upgrade verification | Rejected |
| Repeatedly edit V1 after collaborators apply it | Superficially simple | Checksum drift and unreproducible databases | Rejected |

## Decision

The active Flyway directory contains only V1__initial_schema.sql. It creates the 21 current operational tables with material/balance versions, destination columns, unique material balances and nonnegative stock checks directly in CREATE TABLE declarations. All active SQL comments are English. The previous V1-V4 and the unshipped foundation V5 are archived byte-for-byte under docs/database/legacy-migrations with SHA-256 evidence. The archive is not on the runtime classpath.

The baseline supports the currently implemented domain. Organization scope, stock allocation/ledger and complete sales lifecycle remain the target design in ADR-0002. They are not silently fabricated merely to call this the final database. The next business slices must implement and test those decisions in sequence.

Use a new empty database/Compose project for this baseline. Do not use Flyway baseline-on-migrate, repair or clean to make an old history appear compatible. Keep old volumes untouched. If their data is needed, export it, reconcile it and design an explicit import into an isolated new database; absolute attachment paths also need deliberate remapping. There is no automatic in-place upgrade from the archived history to this reset baseline.

## Step-by-step evolution process

1. Agree the next domain invariant and record its schema/API effect in an ADR. Produce the exact DDL and required data mapping before implementation.
2. Allocate the next unused version after the highest shared migration. Once this V1 is shared/applied, do not edit it. Future changes are V2, V3 and so on with English descriptive filenames.
3. For a new capability, create tables/nullable columns and keys first; add approved backfill/import scripts if retained data requires them. Never infer organization identity from creator_id alone.
4. Validate/backfill existing rows before enforcing stricter NOT NULL, uniqueness or cross-scope constraints. Refuse ambiguous data rather than choosing arbitrary winners.
5. Run both fresh creation and populated-baseline upgrade tests on disposable PostgreSQL. Verify conservation, concurrent writers and rollback for affected business invariants.
6. Switch the corresponding use case and update Swagger/consumers together. Document pre-production wire renames explicitly; no production release-count waiting period applies.
7. Remove obsolete structures only in a later reviewed migration after consumers are moved and retained data has an approved destination.

The immediate domain sequence remains identity/attachments, organization/catalog scope, ledger/allocation, then collection/processing/sales workflows. Exact version numbers for those changes are assigned when their code/DDL is ready, not reserved speculatively in documentation.

## Verification

Integration tests create the full fresh baseline and validate 21 domain tables. Test-only V2 fixtures prove a successful additive index migration retains existing records and is not rerun, and a failing DDL migration rolls back its marker table while retaining records and V1 history. These examples live exclusively in src/test/resources and are not packaged into the application. Inventory tests separately enforce uniqueness, competing first inserts, nonnegative balances and stale-update rejection.

## Consequences

Positive: one readable current schema, honest separation from the target ERD, and a tested future migration process. Historical contributors' SQL remains recoverable in the archive.

Negative: old local volumes cannot be reused merely by pointing the new app at them. Data import is explicit work if needed. This resets unreleased history once; it does not justify resetting shared migrations repeatedly or dropping unknown data.
