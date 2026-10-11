# ADR-0026: Prepare economical homologation with a bounded storage bootstrap

- Status: Accepted for local implementation; cloud provisioning and verification pending.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- References: [ADR-0003](0003-architecture-and-dependencies.md), [ADR-0025](0025-pre-release-security-and-deployment-preparation.md).

## Context and decision drivers

The owner selected a new homologation environment, Railway-generated domains, economical operation and the region closest to Belo Horizonte. The current compute-region list makes Virginia (`us-east4-eqdc4a`) the closest available option; the Brazilian CDN location is not a compute region. All four services should share that region. A numeric spending ceiling remains undecided. No production environment is needed yet.

Railway mounts volumes at runtime with root ownership. The existing backend runs as UID/GID 10001 and image-layer ownership alone cannot initialize a fresh mounted upload directory. Preserve that runtime identity, the modular monolith, `/api/v1`, V1–V4 and the single-replica filesystem storage boundary.

## Considered options

| Option | Benefits | Costs and risks | Outcome |
| --- | --- | --- | --- |
| Run the JVM as root | Simple volume access | Permanent excessive privileges | Rejected |
| Replace filesystem storage | Different scaling boundary | Unnecessary scope and invalidated workflows | Deferred |
| Provision ownership manually for every volume | No root application bootstrap | Requires platform access to a fresh mounted filesystem | Alternative if platform capabilities prevent bootstrap |
| Explicit directory initialization followed by irreversible privilege drop | Repeatable fresh-volume startup; unprivileged application | Brief root entrypoint requires bounded permissions and testing | Selected |

## Decision

Keep Docker's default user 10001. A root invocation must explicitly opt in with `IRR_INIT_STORAGE_PERMISSIONS=true`. Initialization is restricted to the canonical `/data/uploads` directory, rejects a symlink or different location, changes only the directory ownership/mode and never recursively modifies files. Drop to UID/GID 10001, clear capability bounds, set no-new-privileges and then execute the application as PID 1. Refuse unexpected users and unwritable storage. Existing files with incompatible ownership require an independently reviewed offline recovery operation.

For Railway only, review `RAILWAY_RUN_UID=0` together with the opt-in; this starts the bounded entrypoint as root, not the JVM. If required capabilities are unavailable, fail startup rather than retain root. Do not enable this mode in the ordinary local Compose configuration.

Create only backend, static frontend, PostgreSQL 16 and authenticated Redis 8.2. Keep databases private, automatic deployments disabled, no Mailpit, no extra replicas or Kubernetes. Set spending alerts and an owner-approved hard limit before provisioning. The limit can stop all workloads; it is not a billing estimate or availability guarantee. Do not enable application sleeping without validating recovery and token/Redis behavior.

## Acceptance and verification

Retain automated container checks for default identity, refusal of implicit root, opt-in initialization of a deliberately root-owned isolated volume, retained file access, invalid paths and symlinks. Confirm PID 1 has UID 10001 and no capabilities after dropping privileges. Rebuild and verify local health before promotion. Real Railway upload/download/restart and permission verification remain a separate gate. Refresh image/security provenance after changing the entrypoint.

## Consequences and remaining dependencies

This resolves a local implementation prerequisite without changing APIs or migrations. Cloud access, spending ceiling, real mail, initial-data scope, backup policy, residual security dispositions and operator acceptance remain explicit release gates. No remote resources are created by this ADR.

Sources checked on 2026-10-10: [regions](https://docs.railway.com/deployments/regions), [volumes](https://docs.railway.com/volumes), [cost control](https://docs.railway.com/pricing/cost-control).
