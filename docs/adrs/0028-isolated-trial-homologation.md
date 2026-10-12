# ADR-0028: Authorize an isolated, credit-limited synthetic homologation

- Status: Accepted for isolated homologation execution; merge and production release gates remain independent.
- Date: 2026-10-11.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- References: [ADR-0025](0025-pre-release-security-and-deployment-preparation.md), [ADR-0027](0027-provider-independent-deployment.md).

## Context and authority

The owner now requests execution of remaining homologation steps and preparation for merges, identifies a Full Trial account with USD 5 credit, authenticates the official Railway CLI and explicitly chooses a separate environment while preserving the existing `lovely-wonder` project. This supersedes the earlier preparation-only remote restriction solely for the new synthetic homologation. It does not authorize paid upgrades, purchases, modification/deletion of the existing project, client data abroad, unreviewed main-branch commits or production release.

## Decision drivers and options

Reusing existing services would reduce resources but the owner rejected it. Duplicating production/client records would invalidate the synthetic-only boundary and is rejected. Select a separate project/environment with dedicated keys, passwords and volumes, four services and one replica each, no Mailpit or extra ingress service. Railway supplies public HTTPS; the portable Caddy candidate is not part of this deployment.

## Decision

Use Railway US East Virginia and provider domains only for synthetic homologation. Create project `irr-homologation` and environment `homologation`, verifying exact IDs before every mutation. Preserve `lovely-wonder` and its default environment untouched. Keep PostgreSQL 16, authenticated Redis 8.2, existing application images/Dockerfiles and `/api/v1`. No migrations, stock rules or API alias changes are required.

Use Full Trial constraints: at most five services, three 0.5 GB volumes and up to 1 GB RAM per service. The four-service deployment needs database, Redis and upload volumes; a Free account's single volume is insufficient for the approved topology. Keep synthetic fixtures small and export necessary evidence before trial expiry. USD 5 is a one-time temporary allowance, not a monthly hosting promise. Low traffic still consumes resident memory. Do not upgrade billing, add payment details or enable an agent. Inspect actual credit/usage and stop provisioning when constraints prevent a correct deployment rather than remove persistence or merge gates silently.

Credentials stay in private local files and provider secret configuration; never chat, logs, Git, images or frontend bundles. Brevo real-inbox verification depends on approved sender/key and operator inbox. A disabled mail adapter is a documented incomplete gate, not email success. Operator/accessibility acceptance is not fabricated from tests or from this authorization.

## Verification and merge gates

Backend current full Maven verification passes 51 unit and 98 integration tests; script guards and isolated storage checks are retained. Run current frontend lint/types/tests/build and real contract checks before promotion. CI must include initial-data privacy, storage privilege drop and portable deployment boundaries. Existing unresolved dependency findings remain reported; do not suppress them or convert a failed audit into a pass merely to merge.

Record public HTTPS/CORS/health/contract, real upload persistence, mail and scoped browser/operator results separately. Feature branches/PRs are the review path. Preparing merges does not mean main is already safe: failed checks, unresolved risk and acceptance gates remain visible. No automatic main merge occurs solely because the trial deploy starts.

## Consequences

The existing project remains intact. The new trial consumes shared account credits and can stop when allowance expires; its stateful data retention is temporary. Production remains separately budgeted Hostinger Brazil planning. Cloud access removes one blocker but does not establish security clearance or client delivery.

Primary references checked 2026-10-11: [Trial](https://docs.railway.com/pricing/free-trial), [plan resources](https://docs.railway.com/pricing/plans), [volume limits](https://docs.railway.com/volumes/reference).
