# Client initial-data preparation

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Status: foundation validation implemented; actual client import NOT authorized or performed. No client source file has been supplied. Synthetic benchmark fixtures must never become client operational data.

## Collect and approve the source

The client must provide reviewed organization names/types, account names/emails/global roles, organization memberships and membership roles. Record a stable external key for each organization/account and an immutable batch identifier. Confirm municipalities/neighborhoods, material categories/subcategories/types, fleet/drivers, route sections and any historical operations separately against the current `/api/v1` OpenAPI. Do not infer missing mandatory fields, identity relationships, weights, dates, distances or opening balances.

Opening stock needs an explicit decision: `not-required` or `pending`. A pending balance requires source evidence, units/material identities, cut-off time, ledger provenance and a separately approved posting workflow. This validator intentionally rejects quantities. Do not insert balances directly or generate fictional purchases, donations or pressing operations to manufacture inventory.

## Foundation preflight

Create a private UTF-8 JSON manifest outside tracked files. Version 1 accepts:

| Field | Required contents |
| --- | --- |
| `version`, `sourceBatchId` | `1`; stable string identifier, maximum 80 characters |
| `organizations` | Array of `{key, name, organizationType, existingId?}`; type is the reviewed string required by the create-organization contract, maximum 50 characters |
| `users` | Array of `{key, fullName, email, userRole, existingId?}`; supported global role, no password/token |
| `memberships` | Array of `{organizationKey, userKey, role}`; role `MEMBER` or `MANAGER` |
| `openingStock` | `{decision: "not-required"}` or `{decision: "pending"}` |

Run `node scripts/validate-initial-data.mjs <private-manifest.json>` and `node --test scripts/tests/initial-data.test.mjs`. The tool performs zero network/database writes, returns counts and a canonical SHA-256, rejects duplicate external keys/emails/destination identities, invalid roles, unresolved references and credentials. Exit 1 means invalid input. A valid result establishes shape/reference consistency only: destination UUID existence, authorization, client approval and real email delivery are separate checks. Keep inputs and reports private; error locations can include source field names.

## Future approved execution and retries

Before an actual import, pin the release pair and executable OpenAPI; back up PostgreSQL and attachments together; obtain client approval of a redacted preview. Resolve existing identities with authorized API reads before creating anything. Store a private checkpoint mapping each external key to destination UUID, batch hash, completed action and timestamp. Refuse resume when the batch hash changes. A timeout is an unknown outcome: reconcile by identity before retry, rather than blindly repeating POST. Existing records must not be overwritten or promoted automatically.

Use supported credential provisioning and mandatory password change; deliver temporary credentials through an approved private channel. No passwords belong in the manifest, repository, screenshots or logs. Explicitly review global administrator roles. Memberships follow successful organization/account reconciliation. Catalog/fleet/routes follow their actual dependency order; operational history and opening stock remain separate approved batches.

No write-capable importer/checkpoint executor is provided by this foundation preflight. Its implementation depends on the approved client dataset and import scope. After execution, reconcile source/destination counts, memberships, foreign-key references and stock ledger totals; record rejected rows without sensitive contents. A partial batch is not a complete delivery. Rehearse against an isolated restored database before client installation.
