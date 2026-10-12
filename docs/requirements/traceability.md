# Requirements traceability

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Reviewed 2026-10-04.

## Sources and authority

S1: Documento Visão_Requisitos IRR, 24 pages, supplied by the owner. Text requirements are on pages 1–9; identity diagrams on pages 10–11 were visually inspected. S2: Plataforma IRR de Gestão da Coleta Seletiva, 3 pages, supplied by the owner. S3: owner's consolidated Identity & Access direction dated 2026-10-04. Source titles retain their original spelling for identification; requirements below are interpreted, not copied as technical contracts. The PDFs remain owner-supplied reference material and are not republished in the repository.

Status vocabulary: IMPLEMENTED, IN_PROGRESS, PLANNED_CURRENT_MILESTONE, PLANNED_POST_DEPLOY, PLANNED_AFTER_ROUTES, ARCHITECTURAL_DIRECTION, OPERATIONAL_CONFIGURATION_PENDING, DEFERRED, REINTERPRETED, PRODUCT_DECISION_REQUIRED, SUPERSEDED. PRODUCT DEFINITION REQUIRED is a product-decision subtype for metric semantics. A historical request does not independently authorize new implementation.

| ID / source | Current architectural interpretation | Status | Implementation / decision / roadmap |
| --- | --- | --- | --- |
| R01 — S1 FF-1, pp.1–2; S2 §1 p.2; S3 | Global IRR administration and tenant-local operations | IMPLEMENTED | Backend ADR-0017, AccountLifecycleService, OrganizationService, RequestOrganizationScope; frontend identity and organization controls; NOW |
| R02 — S1 FF-1.a, p.1 | Organization login becomes Organization + User + explicit Membership; no credentials on tenant | REINTERPRETED | ADR-0009/0017; organization onboarding UX; NOW |
| R03 — S1 FF-1.b and diagrams pp.10–11 | Creator-only access replaced by explicit tenant scope; global ADMINISTRATOR oversight | REINTERPRETED | ADR-0017; MANAGER and MEMBER only, no role inference from CITY_HALL |
| R04 — S1 FF-2.a p.2 | Unified login/logout, protected navigation, current identity | IMPLEMENTED | SessionController, bearer filter, frontend Login/SessionProvider/Router; regression gate remains current |
| R05 — S1 FF-2.b p.2; S2 §1 p.2 | Expiring one-use recovery link and confirmed new password | IMPLEMENTED | ADR-0018, PostgreSQL recovery authorization and mail port; NOW |
| R06 — S3 | Profile, password changes, administrative reset and token revocation | IMPLEMENTED | ADR-0016 core, UserController and identity forms; global target, revocation and concurrency tests passed |
| R07 — S3 | Global admin creates/administers all account roles; last active global admin protected | IMPLEMENTED | ADR-0017, transaction guard and concurrent PostgreSQL test; NOW |
| R08 — S3 | First administrator created once via BOOTSTRAP_ADMIN_*; no SQL or redeploy reset | IMPLEMENTED | Existing initializer adapted to final configuration names; NOW |
| R09 — S1 FF-3 pp.2–4; S2 §2 p.2 | Scoped collections/donations, team/vehicle links, attachments and material intake | IMPLEMENTED | Existing backend intake/fleet/document modules and integrated frontend; not a claim that every historical field is implemented |
| R10 — S1 FF-5 pp.5–6; S2 §5 p.3 | Tenant material hierarchy with protected deletion/references | IMPLEMENTED | Existing catalog; current global authorization applies; historical physical-delete language does not override invariants |
| R11 — S1 FF-6/7/8 pp.6–9; S2 §§5–7 p.3 | Transactional processing, ledger and commercial records | IMPLEMENTED / LOCAL FLOW ACCEPTED | Backend ledger/sales and frontend processing/stock/sales are integrated; owner accepted the local processing/reversal journey. Broader client acceptance remains separate |
| R12 — S1 FF-4 p.5; S2 §3 p.2; S3 | Internal routes, execution/history, coverage, tenant ownership and municipality context | IMPLEMENTED | Additive V4, scoped plans/executions/coverage/history and linked collection UI/API; locally verified, remote homologation pending |
| R13 — S1 FF-4.b p.5; S2 integrations p.2; S3 | Provider-neutral tracking/GPS boundary | PLANNED_POST_DEPLOY | After initial Routes; no provider selection or integration now |
| R14 — S1 FF-9 p.9; S2 §8 p.3; S3 | Native monthly reports and filters, backend aggregates, frontend presentation | IN_PROGRESS | First approved operations-1 report locally verified; remaining reporting families require definitions; no mandatory BI |
| R15 — S2 purpose/differentials pp.1,3; S3 | Multi-municipality separation and consolidated IRR comparison | IMPLEMENTED / PLANNED_AFTER_ROUTES | Operational separation uses organization-owned Route and shared Municipality/Neighborhood with immutable execution territory; consolidated comparison/analytics remains planned |
| R16 — S2 dashboard §4 p.2; S3 | Native route/vehicle/period/municipality indicators | IN_PROGRESS | Approved execution count, reported km, elapsed seconds and collection kg implemented; other formulas remain PRODUCT DEFINITION REQUIRED |
| R17 — S2 §8 p.3 | Embedded external BI as optional extension | DEFERRED | Main reporting remains native; no external BI dependency |
| R18 — old technical policies | No admin creation, mandatory membership for global users, bootstrap password overwrite, legacy client adapters | SUPERSEDED | ADR-0017 and current v1-only policy; supersession markers retain history |

## Product decisions required — not implementation authorization

| Requirement / origin | Suggested interpretation | Impact / dependencies | Suggested priority |
| --- | --- | --- | --- |
| Delegated account creation/inactivation by non-IRR users — S1 diagram p.10 | PRODUCT_DECISION_REQUIRED: current S3 allows only global POST /users; local MANAGER governs operations. Future tenant invitation could avoid mutating shared global accounts | Account ownership, shared memberships, invitation/security policy | After current Identity; ask owner before promoting |
| Monthly report recalculation after edits — S1 FF-3 pp.3–4 | ACCEPTED for operations-1: live recalculation | Owner-approved operational semantics; published fiscal closure remains outside this slice | Implemented first reporting slice |
| Persistent completion toast for route streets — S1 FF-4 p.5 | PRODUCT_DECISION_REQUIRED: prefer route completeness status; do not impose perpetual toast without validation | Route field requirements and field-user workflow | During Routes discovery |
| IBGE neighborhood cartographic base — S2 §4 p.2 | PRODUCT_DECISION_REQUIRED: confirm authoritative dataset, licensing, identifiers and update process | Municipality mapping and map/provider boundary | After initial Routes unless owner reprioritizes |
| Atypical-variation/delay alerts, targets and benchmarks — S2 pp.1–2 | PRODUCT_DEFINITION_REQUIRED: formulas, thresholds, denominator/units and missing-data handling | Reliable route/time/distance/weight facts | Before indicator implementation |
| Bulk market price tree editor — S1 FF-8.e p.9 | PRODUCT_DECISION_REQUIRED: reconcile tenant-local pricing and global administration | Currency, effective dates, concurrency and audit policy | Further module evolution |

Do not create issues, required milestones or functional implementations from these rows until the Product Owner approves them. The authoritative current order is the [Master Plan](../adrs/0001-master-plan.md). Evidence and statuses must be updated at each delivered slice rather than declaring completion from compilation alone.

## Current delivery evidence — 2026-10-04

R01/R05/R06/R07/R08 are implemented and locally verified, including real PostgreSQL/Redis/Mailpit integration. R02 is realized by the explicit Organization → User → Membership onboarding with safe partial retries. IMPLEMENTED does not assert external email delivery or deployed homologation. See [verification and release gates](../identity-verification.md). R11 and later module priorities remain unchanged.

## Historical owner-authorized Routes phase — 2026-10-05

Identity is IMPLEMENTED / READY FOR HOMOLOGATION. Email provider configuration is OPERATIONAL CONFIGURATION_PENDING / EXTERNAL CONFIGURATION REQUIRED; it no longer blocks Routes development. Routes is IMPLEMENTED / READY FOR HOMOLOGATION under the verified architecture decision; see [the actual delivery evidence](../routes-verification.md). Analytics/reports/filters/indicators/dashboards are PLANNED_AFTER_ROUTES. Operational multi-municipality separation is IMPLEMENTED; consolidated analytics comparison remains PLANNED_AFTER_ROUTES. See [the data dictionary](../data/README.md); every unapproved metric formula is PRODUCT DEFINITION REQUIRED. GPS remains PLANNED_POST_DEPLOY.

## Current pre-release traceability — 2026-10-10

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). The Routes checkpoint above is historical. R11 processing/stock/sales is integrated and the local workflow was explicitly accepted by the owner. R14/R16 operations-1 and owner-approved analytics-1 are implemented and locally verified; unapproved exports, cross-organization comparisons and other formulas remain product decisions. R15 operational separation is implemented, while consolidated comparison remains proposed. No declaration below supersedes source-document requirements silently.

The release-readiness register maps identity, intake/catalog/fleet, processing/sales, Routes and reports to their actual backend/frontend test suites and dated evidence, and separates implementation from browser/operator/client acceptance. Backend docs/release-readiness.md is the coordinated security/data/deployment closure reference; frontend docs/release-readiness.md records the frontend counterpart. GPS/maps and optional BI remain deferred. Planning Railway artifacts is now authorized; provisioning, secrets and publication are not.
