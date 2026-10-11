# Local release consolidation register

## Supported open-source platform — 2026-10-11

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). [ADR-0029](adrs/0029-open-source-spring-security-upgrade.md) records the owner's no-Enterprise decision and the bounded Boot 4.0.8 / Framework 7.0.9 migration. The resolved graph now has 185 coordinates and zero OSV matches; earlier Spring 6.2.19 findings below are historical and are superseded for this candidate only. This does not close container findings, public deployment, mail or operator acceptance gates. Java 21, the modular monolith, versioned API and V1–V4 migrations remain. Final verification evidence is recorded in ADR-0029.

## Current execution checkpoint — 2026-10-11

[ADR-0028](adrs/0028-isolated-trial-homologation.md) now authorizes isolated synthetic Railway execution. [The provisioning record](deploy/railway/execution-2026-10-11.md) records a new project, four services, three volumes, limits, fresh private secrets and reserved domains. The previous project is preserved. No new workload has deployed; effective Virginia volume locality, final image security review, real public workflows, mail and merge CI remain open. References below to preparation-only authorization, missing cloud access or a blanket foreign-region prohibition are historical and superseded for this synthetic environment only.

Current hosting authority is [ADR-0027](adrs/0027-provider-independent-deployment.md): Railway Virginia is explicitly permitted only for synthetic homologation; production requires a Brazilian Hostinger VPS and later budget/procurement approval. The earlier region blocker below is superseded for homologation. [Portable deployment](deploy/README.md) includes verified Compose boundaries and validated Caddy syntax, not remote acceptance. Caddy's 49 raw image findings require review before promotion; no added infrastructure finding is waived.

## Latest homologation preparation increment

[ADR-0026](adrs/0026-homologation-storage-bootstrap.md) and [the fresh installation checklist](deploy/railway/homologation-installation.md) incorporate the owner's choice of a new homologation environment, Railway domains and economical deployment. The owner subsequently required Brazil/Sao Paulo and BRL 130/month; Railway currently lists no Brazilian compute region, so no foreign region is authorized. Backend `89e9945` implements bounded directory ownership initialization followed by privilege drop; seven isolated container scenarios, actual local Java UID/GID 10001, health, six read-only pair checks and 116 operations pass. Real Railway volume permissions remain unverified. No API, migration or domain formula changed.

[The new candidate scan](security/backend-storage-bootstrap-grype-2026-10-10.json) retains 290 raw findings; all 13 critical binary CPE entries now have individual vendor evidence. Two conditional Spring mitigations and other high/infrastructure/build findings remain release review, not waived risks. [Current image provenance](deploy/railway/local-storage-candidate-2026-10-10.json) supersedes only the running backend image in the historical checkpoint. Remaining owner inputs: Brazil-capable provider choice/cloud access, initial-data/admin scope, Brevo sender/inbox and backup policy. Operator/accessibility/PDF acceptance and actual public environment verification remain open. Deployment is not claimed complete.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Updated 2026-10-10. Continues existing Master Plans; [ADR-0025](adrs/0025-pre-release-security-and-deployment-preparation.md) authorizes preparation, not deployment.

## Outcome gates

| Outcome | Status | Blocking condition |
| --- | --- | --- |
| A — Technical homologation | IN PROGRESS | Broader browser/role/error/accessibility acceptance and residual security review |
| B — Deployment readiness | NOT READY; preparation underway | A, reviewed final images, storage permissions, domains/secrets/mail/backup policy and approved release pair |
| C — Client delivery | NOT DELIVERED | B, actual installation, approved client inputs/import and operator acceptance |

Successful tests or one accepted processing journey do not close all three outcomes. No public installation or client import occurred.

## Requirement → implementation → verification → acceptance

| Area / requirements | Implementation | Dated tests/evidence | Status / next action |
| --- | --- | --- | --- |
| Identity/onboarding R01–R08 | Backend identity/lifecycle/membership/recovery; frontend `features/identity` | AccountLifecycleIT, GlobalAdministratorIT, OrganizationMembershipIT, PasswordRecoveryIT; frontend identity/administration/Router tests; [identity register](identity-verification.md) | IMPLEMENTED, local evidence; real mail and complete operator/role browser acceptance pending |
| Fleet/catalog/intake R09–R10 | Scoped vehicles/materials/parties/intake/documents and matching feature services | MaterialCatalogIT, IdentityAttachmentIT; frontend fleet/materials/intake tests and actual API suite | IMPLEMENTED; broader mobile/error/accessibility operator review pending |
| Processing/stock/sales R11 | Transactional sorting/pressing/lots/ledger/sales and enabled frontend features | ProcessingLedgerIT, DecimalContractTest; frontend processing/sales tests; owner explicitly accepted local processing/reversal/stock workflow | LOCALLY ACCEPTED for that journey; no generalized client acceptance |
| Routes R12/R15 separation | Planning/execution/section coverage, completed collection linkage, immutable territory | RoutesIT, RoutesMigrationIT; frontend routes tests; [Routes evidence](routes-verification.md) | IMPLEMENTED / local verification; GPS/maps remain R13 deferred |
| Native reports/Analytics R14/R16 | `reporting`, operations-1/analytics-1; frontend reporting pages/definitions | OperationalReportIT, AnalyticsReportIT, AnalyticsMathTest; frontend reporting/analytics/duration tests; [query correction](analytics-query-planning.md) | Approved MVP IMPLEMENTED; exports/global comparisons/other indicators remain separate product decisions |
| Data schema/integrity | Applied V1–V4, constraints and exact ledger semantics | FinalBaselineSchemaIT, InventoryBalanceSchemaIT, RoutesMigrationIT; [data dictionary](data/README.md) | VERIFIED for dated local scope; no migration reset/rewrite |
| Backup/restore | Explicit-stack backup and network-isolated restore scripts | [39 table counts / 20 file hashes and reconciliation](recovery-verification.md) | Local rehearsal VERIFIED; off-machine schedule/remote drill/RPO/RTO pending |
| Performance | Deterministic isolated synthetic generator and independent oracle | [benchmark baseline](reporting-benchmarks.md); ADR-0024: 960 exact responses at 1k/10k/50k | VERIFIED synthetic baseline; not production performance certification |
| Frontend build chain | Retained stack; patched source-map-js; trusted CSS/globs boundary | Frontend 511f461: lint/types/build, 136 tests; [seven individual entries](security/README.md) | MITIGATED residual findings; compatible upstream/toolchain remediation pending |
| Spring MVC | Current Boot 3.5/Spring 6.2, REST-only loaded handlers | 436b75b: Spotless/documentation + MvcSecuritySurfaceIT two integration passes | Conditional exposure MITIGATED; framework remains affected, supported patch decision pending |
| Containers | Actual five local images scanned by Grype; frontend 232d60b security patch | [sanitized inventories](security/README.md); candidate Nginx config/build and six actual pair checks passed | Frontend high/critical scan matches corrected; other images/build stages and individual residuals pending |
| Initial client data | No-write foundation preflight; private batch fingerprints/reference checks | 7f3c069: seven tests; [initial-load procedure](data/initial-load.md) | Preparation IMPLEMENTED; source approval/write executor/import remain pending |
| Railway | Existing Dockerfiles and variable/runbook topology | [review-only plan](deploy/railway/preparation.md), no cloud apply | PREPARED in part; resources/secrets/public URLs not configured or authorized |
| Exports/comparisons R14/R15 | [bounded proposal](reporting-extension-scope.md) | No fabricated export or comparison evidence | PRODUCT APPROVAL REQUIRED |

## Browser and WCAG 2.2 AA review boundary

Existing actual Analytics empty/populated/filtered screenshots and attachment upload/download evidence remain in the frontend documentation. Strict downloaded PDF byte comparison/parse/render passed; a PDF render is labelled as such and is not a browser-viewer screenshot. Original invoice attachments are retained. Owner processing acceptance does not silently accept every PDF or accessibility criterion.

Current browser automation access was policy-blocked; no alternate automation/browser workaround or fabricated new screenshots was used. Continue independent HTTP, integration and source/configuration checks. Human/operator browser evaluation remains required for: administrator/member/manager access; login/recovery/mandatory-password; account/membership dialogs; empty/loading/error/retry states; fleet/intake/routes; processing/reversal; Analytics filters and metric explanations; actual invoice open/download.

Review keyboard-only tab order, skip link/page focus, dialog focus entry/trap/return/Escape, labels/errors/announcements, visible focus, 200% zoom/reflow, text/non-text contrast in light/dark/high-contrast modes, enlarged fonts, reduced motion and narrow viewports. Record each criterion/result with viewport/theme/role and a real screenshot where appropriate. Nine actual-router fixture tests cover guard/focus/session boundaries; passing jsdom tests does not establish native dialog behavior, contrast or WCAG conformance. No complete WCAG claim is made.

## Changes and environmental failures in this consolidation

New tests passed: two loaded MVC integration cases and seven initial-data preflight cases. Frontend image build includes strict TypeScript/Vite; Nginx syntax and six read-only backend pair checks passed for the patched candidate, plus the 116-operation contract check. Previously approved full suites, migrations, recovery, Analytics totals and benchmarks were not needlessly rerun.

Docker Desktop repeatedly stopped during local work. A backend Jammy package-update build failed with Docker RPC EOF before completion; its unverified Dockerfile edit was removed, preserving the existing owner baseline. Backend image remediation remains pending. Frontend candidate build/scan/checks completed before the host interruption; restarting a container is not proof of a stable development host. No volume was deleted or reset. Stabilize Docker resources/runtime before another backend build or sustained client demonstration.

## Ordered remaining actions

1. Stabilize local Docker and finish compatible backend/infrastructure/build-base scans and vendor-specific disposition; repeat only checks invalidated by each patch.
2. Complete remaining genuine browser/WCAG/operator journeys and PDF viewer acceptance when authorized automation becomes available or through operator evidence.
3. Close A with evidence, then approve the immutable release pair and B's domain/storage/email/backup/secrets inputs.
4. Rehearse the reviewed initial-data batch in isolation; install only after explicit deployment authorization; validate public pair and real mail; obtain client acceptance C.
5. Decide the bounded export/comparison proposal separately. GPS/maps/advanced BI remain deferred.

## Latest closure checkpoint — 2026-10-10

The backend package-update failure above is historical: the bounded Maven build subsequently succeeded in 8cadda5. Image 549937278a93 runs healthy on retained local volumes and passes six read-only pair checks plus the 116-operation contract check. Runtime scan 298 -> 290 and nine specific critical vendor-backport/not-affected dispositions are documented in the security register. Remaining findings are not waived. Node/Maven build-base scans have completed; their remediation remains open. The final operator/WCAG worksheet exists in the frontend repository and the initial-data preflight now passes eight tests. See the [local candidate record](deploy/railway/local-candidate-2026-10-10.json); it is not an approved deployment.

| Area | Status | Evidence | Blocker | Next action |
| --- | --- | --- | --- | --- |
| Functional homologation | PARTIAL | Existing workflow/API tests and scoped processing owner acceptance | Broader operator journeys | Execute final frontend worksheet |
| Accessibility | PENDING complete review | Shared UI/router tests and prior scoped browser check | Browser/assistive-technology evidence | Keyboard, contrast, reflow and announcement review |
| PDF viewing | PARTIAL | Valid downloaded bytes, strict parse and render | Operator viewer acceptance | Open downloaded valid PDF and confirm usability |
| Dependencies | MITIGATED / PENDING | Seven npm entries and two Spring dispositions | Compatible upstream/support decision | Resolve residual risks without forced architecture migration |
| Containers | PARTIAL | Seven runtime/build scans; frontend/backend patches and actual pair checks | Remaining vendor findings and infrastructure image corrections | Review and patch each compatible image, then verify persisted services |
| Traceability | UPDATED | Current requirement/test/evidence matrix and both registers | Open acceptance evidence | Maintain state per delivered/accepted scope |
| Initial load | PREPARATION VERIFIED | Eight zero-write validation tests and documented batch gates | Actual client inputs and approval | Review dataset, reconcile and rehearse approved import |
| Railway infrastructure | PREPARATION ONLY | Topology, variables, review artifact and local image manifest | Account/domains/budget/region/permissions | Configure only after explicit authorization |
| Production security | PENDING | Non-root images, scoped API and secret boundaries | Final image risk acceptance, real secrets/monitoring/storage | Verify environment controls before public installation |
| Backup/recovery | LOCAL VERIFIED | Prior isolated restore/reconciliation | External schedule/retention/RPO/RTO/remote drill | Approve policy and rehearse off-machine recovery |
| Client acceptance | PENDING | Owner accepted one local workflow | Installed environment and complete client review | Formal acceptance after authorized installation/import |
