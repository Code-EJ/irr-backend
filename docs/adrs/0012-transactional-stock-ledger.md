# ADR-0012: Transactional stock ledger, traceable lots and idempotent commands

> Final pre-production contract: [ADR-0014](0014-final-preproduction-contract-and-baseline.md) supersedes transitional HTTP aliases and the unreleased migration chain. Earlier evidence remains historical; the supported application API is /api/v1 and the final runtime schema starts at one fresh V1.


> Completion update: [ADR-0013](0013-backend-operational-completion.md) records implemented sales, reversals, scoped CRUDs, reconciliation and local verification. Pending statements below describe the decision-time baseline.


- Status: Accepted; sorting and pressing implemented, sales and reversal consumers in progress.
- Date: 2026-09-27.
- Author: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Related: [database design](0002-database-schema-redesign.md), [organization cutover](0011-organization-business-cutover.md), [Redis decision](0010-containerized-redis.md).

## Context and decision drivers

The earlier sorting implementation credited net output but also recorded rejects as a second negative movement. Pressing logged a positive mass movement despite conserving mass, and clamped insufficient volume to zero. Neither flow prevented repeated allocation of a source or duplicate HTTP commands. These behaviors cannot support sales or trustworthy reports.

The required invariants are conserved mass, nonnegative quantities, explicit provenance, one authoritative posting boundary, atomic retries and organization isolation. Decimal arithmetic must remain exact to the supported four fractional digits.

## Considered options

| Option | Advantages | Disadvantages | Decision |
| --- | --- | --- | --- |
| Patch each service's balance writes | Small immediate diff | Duplicates rules, leaves movement/projection inconsistencies | Rejected |
| Use Redis locks and deduplication | Fast shared coordination | Business data and receipt cannot commit atomically with PostgreSQL | Rejected for stock correctness |
| PostgreSQL ledger, constrained projection and durable receipts | One transaction, auditable history, tested concurrent allocation | Additional schema and reversal workflows | Selected |
| Full event sourcing | Flexible replay of all domain state | Unnecessary complexity for current CRUD and reporting needs | Deferred |

## Decision

V5 introduces command_receipt, stock_operation, stock_movement and stock_lot. The existing inventory_balance remains a projection. StockLedger is the sole posting boundary for new operational stock; the legacy inventory_log remains preserved but receives no new sorting or pressing postings. PostgreSQL rejects UPDATE and DELETE of stock_operation and stock_movement. Corrections require compensating operations, never rewriting history.

Every create-sorting and create-pressing request requires Idempotency-Key (1–128 characters). The receipt stores the command kind, payload hash, response and actor in the same transaction as all business effects. A repeated key and payload returns the original response, preserving timestamp offsets. Reusing a key with different content returns 409. Failed transactions leave no receipt and can be corrected and retried. Keys are scoped by organization and shared among current members. Redis is not the authority for these receipts. Receipt retention is indefinite until a documented retention/replay policy is adopted.

Intake records raw quantities without creating saleable inventory. Sorting requires an input item in the selected organization. Its gross allocation cannot exceed unprocessed input mass or volume; rejects must not exceed gross values. Only net output is posted once and a traceable sorted lot is created. Multiple output materials can result from one mixed intake, but aggregate gross allocation remains bounded by the input.

Pressing requires a sorted item and an available lot of the same material. It consumes source mass and initial volume, creates a pressed lot with the same mass and lower final volume, and posts zero mass plus final-minus-initial volume. Insufficient source quantities cause 409 and roll back the document, its children, lot allocation, movement and receipt. No quantity is clamped. Sorting and pressing destination fields accept only STOCK without a destination ID; subsequent sale/pressing links are created by their own typed commands.

The organization lock serializes source allocations and first-balance insertion. Conditional balance updates require both resulting quantities to remain nonnegative. Composite foreign keys prevent cross-organization sources. JPA changes are explicitly flushed before JDBC ledger statements because both participate in one transaction and refer to the same persisted operational rows.

## Sales and correction contract

Sales use DRAFT, POSTED and REVERSED states. Drafts are editable; posting requires scoped fiscal evidence and lot allocations. Totals are derived on the server. Posting consumes lots and debits the projection in one idempotent command. Reversal restores eligible allocations and posts inverse movements while retaining the original sale. A sorting/pressing reversal must first establish that its outputs have not been consumed downstream. The low-level ledger reversal method alone does not perform those domain eligibility checks and must not be exposed directly.

The schema anticipates this lifecycle, but sales/reversal HTTP workflows are not considered complete until their consumers and concurrency tests pass. Posted operational records must never be exposed through unrestricted generic update/delete CRUD.

## Migration and reconciliation

Do not reinterpret legacy balances or logs as reconciled stock. V5 creates an empty authoritative ledger for organization-owned business flows; unmapped history remains preserved. Any later legacy import requires an explicit opening-balance decision with provenance and reconciliation. New reports must reconcile SUM(stock_movement deltas), inventory_balance and available lot quantities for the same organization/material. A mismatch is an error to investigate, not an opportunity to overwrite the ledger.

## Consequences and validation

The design adds storage and serialized organization writes, but provides replayable HTTP effects and traceable physical stock. ProcessingLedgerIT uses real PostgreSQL to verify net credits, mass-preserving compaction, over-allocation rollback, same-key replay, changed-payload rejection, foreign source denial and database-enforced history immutability. Existing mock tests that asserted the incorrect reject debit or pressing mass credit were replaced by these transactional checks. Remaining gates include simultaneous sales, reversals, reconciliation endpoints and the complete frontend workflow.
