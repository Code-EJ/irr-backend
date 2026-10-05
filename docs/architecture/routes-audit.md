# Routes baseline audit

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Audited 2026-10-04 before route code/schema changes.

| Repository/worktree | Starting branch | Base / current HEAD | Status |
| --- | --- | --- | --- |
| irr-backend / existing irr-backend-identity worktree | feat/route-planning-and-execution | d43f481 (Identity closure); main ancestor 4c50f171b03f7d97487e08aadd4d80257b31bc03 | Clean before Routes documentation |
| irr-frontend / existing checkout | feat/route-planning-and-execution | f9e7054 (Identity/dialog closure); main ancestor 01a95436859b747241fab573e8761b4dc73d0ade | Clean before Routes documentation |

Existing original backend add-railway_deploy-docs worktree and its unrelated staged/unstaged changes are preserved. Existing repositories were not moved; new functional branches reuse the same implementation checkouts.

Audit evidence: Collection model/DTO/service/repository/controller and collection_team; VehicleService/ReferenceLifecycleGuard; TeamMember model/service/roles; Organization model/store/scope/write locking; donor PostalAddress; V1/V2/V3 constraints and indexes; frontend generated intake contracts/forms/pickers/router; historical FF-4/platform geographic/GPS requirements recorded in requirements traceability. No reusable Route or stable Municipality/Neighborhood exists. Existing routeDescription, collection timing/km, fleet/staff and exact material input facts are reused rather than replaced.

Identity regression closure: clean Maven verify 48 unit + 75 integration tests, zero failures/errors/skips; frontend lint/typecheck/build, 59 unit/component and 18 real-backend tests; current Swagger 95 v1 operations; six read-only pair checks. Source audit confirms EmailSenderPort/HttpEmailSender and local Mailpit work without external traffic. Provider deployment remains explicitly external; the email runbook contains configuration/DNS/secrets/test/troubleshooting/replacement steps.

Accepted decisions: [Route planning/execution](../adrs/0019-route-planning-execution-and-geographic-boundaries.md) and [Analytics & Data](../adrs/0020-analytics-reporting-and-data-architecture.md). Planned schema V4 adds normalized territory/planning/execution/snapshot/audit records and optional Collection association; no reset. Runtime/API/permission/collection-history and frontend acceptance remain pending until implementation and real verification are recorded. No dataset import, GPS, map or analytics feature is claimed.

## Delivered implementation

The pending status above is the pre-implementation audit. Routes is now IMPLEMENTED / READY FOR HOMOLOGATION; [the verification record](../routes-verification.md) documents V4, actual source/contract checks and release limitations.
