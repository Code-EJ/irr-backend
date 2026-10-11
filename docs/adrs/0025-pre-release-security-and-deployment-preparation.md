# ADR-0025: Consolidate local release evidence and prepare client installation

- Status: Accepted for local execution and Railway preparation only; deployment and client acceptance pending.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- Authority: owner-supplied pre-release consolidation request; continues [ADR-0001](0001-master-plan.md), [ADR-0014](0014-final-preproduction-contract-and-baseline.md), [ADR-0022](0022-approved-mvp-analytics.md) and [ADR-0024](0024-analytics-execution-fact-aggregation.md).

## Context

The local domain implementation, preserved V1–V4 migrations, isolated recovery and synthetic 50,000-execution performance baseline already have dated evidence. They do not establish browser accessibility, image security, published environment behavior or client initial-data acceptance. The owner now authorizes Railway planning/artifacts before local closure, while expressly withholding production resources, real secrets and publication. This supersedes only the earlier prohibition on Railway preparation, not the deployment gate or domain decisions.

## Decision drivers

Preserve approved behavior and owner-authored Docker/Maven work. Close actual defects rather than introduce a new architecture. Keep evidence reproducible and distinguish scans, applicability analysis and human acceptance. Avoid forcing incompatible dependency majors merely to clear audit counters. Protect client identities, original attachments and stock provenance. Keep release authority explicit.

## Considered options

| Option | Benefits | Costs / risks | Decision |
| --- | --- | --- | --- |
| Declare readiness from successful compilation and historical checks | Fast | Omits browser, security and client/environment gates | Rejected |
| Rewrite frameworks, storage and hosting | Potential future flexibility | Unrequested regressions and invalidated evidence | Rejected |
| Suppress conditional advisories or skip images when Scout needs login | Lower reported count | Hides residual risk | Rejected |
| Bounded security patches, loaded-context tests, alternative scanner, explicit acceptance matrix | Reviewable changes; preserves existing direction | Unfixed upstream and external gates remain visible | Selected |

## Decision

Use three independent outcomes: A technical homologation; B deployment readiness; C client delivery. None is inferred from another. Preserve existing tests/evidence unless a change invalidates them. New browser evidence must be genuine; an automation access block remains a blocker, not grounds to substitute screenshots or certify WCAG.

Individually classify the seven current npm build entries and two Spring MVC findings. Preserve scanner severity and vendor prerequisite evidence separately. Test the loaded MVC context and handlers, not only source text. Do not upgrade Boot/Spring majors without compatibility work. Use Grype against actual images and retain sanitized per-finding inventories without credentials or ignored vulnerabilities. Compatible OS/runtime patches must be built, scanned and checked against the real `/api/v1` contract before promotion.

Keep the unprivileged Nginx 1.28 line and Alpine 3.23; remove dynamic modules unused by the maintained configuration to permit the vendor patch and reduce image surface. Restore UID 101 after build-only privileged package maintenance. Keep Java 21/Jammy, existing build cache and backend UID 10001. System repositories are mutable: freeze the resulting approved image digest/inventory rather than assuming a later rebuild is identical.

Prepare Railway runbooks and a provider-neutral review artifact; these do not apply infrastructure. Do not introduce deprecated `railway.toml` configuration for a new service. Existing Dockerfiles, service ports, Flyway startup and Brevo adapter remain. Verify non-root volume permissions before installation; do not silently run the JVM as root. Retain a single backend replica while attachments use a single filesystem volume. Budget, domains, regions, RPO/RTO, secrets, backups and real mail delivery need owner/operator configuration and verification.

Prepare foundation manifests with stable external keys and a no-write validator. Client approval, destination reconciliation, checkpointed retries and supported credential provisioning precede any real import. Opening stock/history need their own approved provenance; synthetic fixtures must never become client data. Exports/comparisons remain product proposals; GPS/maps/advanced BI remain deferred.

## Acceptance and verification

The [release register](../release-readiness.md) ties each area to implementation/tests/evidence and remaining action. [Security review](../security/README.md) preserves pending findings. [Client data preparation](../data/initial-load.md) documents zero-write validation. [Railway preparation](../deploy/railway/preparation.md) defines configuration and approval gates. Both Master Plan registers reference this phase without replacing their historical decisions.

## Consequences

Compatible patches can proceed locally. Residual scanner findings, blocked browser journeys and absent client/cloud inputs prevent a clean-release assertion. Package maintenance introduces dated image resolution, requiring immutable artifact promotion and re-scanning. No new API, migration, domain metric, external resource or production secret is authorized by this ADR.
