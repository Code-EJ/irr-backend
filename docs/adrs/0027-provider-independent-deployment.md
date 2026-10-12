# ADR-0027: Keep deployment portable and separate homologation from production

- Status: Accepted for local artifacts and planning; remote installation pending.
- Date: 2026-10-10.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).
- References: [ADR-0003](0003-architecture-and-dependencies.md), [ADR-0014](0014-final-preproduction-contract-and-baseline.md), [ADR-0025](0025-pre-release-security-and-deployment-preparation.md), [ADR-0026](0026-homologation-storage-bootstrap.md).

## Context and drivers

The owner requests provider-independent hosting and a Hostinger production plan. The subsequent clarification explicitly keeps homologation on Railway, permits US East Virginia only for synthetic homologation data, and requires production in Brazil, preferably São Paulo. No Hostinger account, VPS or domain has been purchased; production procurement is a later budget decision. Preserve the BRL 130/month ceiling and clarify concurrent-environment costs before purchase.

This supersedes ADR-0026's blanket prohibition on foreign regions **only for synthetic Railway homologation**, and its provider-selection blocker. It does not permit client/production data abroad, authorize procurement/publication, waive security/acceptance gates or alter domain behavior.

## Considered options

| Option | Benefits | Costs / risks | Decision |
| --- | --- | --- | --- |
| Couple application code to one hosting platform | Quick initial setup | Difficult relocation, provider-specific secrets/API behavior | Rejected |
| Kubernetes or new cloud-managed domain services | Future scaling options | Unnecessary cost and operational complexity for the MVP | Rejected |
| Linux VPS, Docker Compose, standard images and separate provider runbooks | Portable deployment and explicit operating costs | Owner must operate patching, backup, monitoring and single-host recovery | Selected |

## Decision

Keep Java 21/Spring modular monolith, React/Vite/static Nginx, PostgreSQL 16, Redis 8.2, Brevo HTTPS, filesystem attachments and `/api/v1`. Use immutable image references, runtime variables and read-only key files. No provider SDK, proprietary database feature, new migration, framework rewrite or business API is introduced.

Retain Railway instructions as a homologation adapter. Use provider domains, Virginia for all services, dedicated secrets/volumes and synthetic records only. Never migrate that database to the client as initial data. Confirm storage bootstrap/UID, public contract, mail and actual browser acceptance there.

Add a generic single-host Compose deployment under `deploy/`, without builds on the server, public DB/Redis/application ports, Mailpit or development bootstrap. Only the HTTPS ingress publishes 80/443; operator SSH is restricted by the host firewall. Caddy terminates TLS and forwards requests to maintained frontend/backend containers using two configurable domains, preserving existing API paths and two-origin integration. Logs omit access-request logging by default to avoid recovery-token URL capture. TLS state, PostgreSQL, Redis and uploads persist independently. A private data network isolates DB/Redis; backend also has egress for Brevo. Default Java UID 10001 needs no Railway-specific root bootstrap on a fresh Docker named volume.

Choose Caddy instead of Nginx plus a separate certificate-renewal process to minimize certificate operations. This is one edge infrastructure dependency, not an application framework. Pin and scan its release image before promotion. Do not add a deployment dashboard, Docker socket exposure or Kubernetes. Keep single backend replica; a single VPS has no HA guarantee. The artifact is a deployment candidate until isolated/public verification completes.

Production planning targets Hostinger KVM 2 (2 vCPU, 8 GiB RAM, 100 GB disk) in Brazil, conditional on checkout location/price, workload verification and owner approval. This is an initial sizing proposal, not measured production capacity. Ubuntu LTS plus Docker/Compose is replaceable by any compatible Brazilian Linux VPS. Shared/WordPress hosting is unsuitable for this persistent JVM/database stack.

## Acceptance and verification

Validate Compose without printing resolved secrets; require explicit images/domains/passwords/key directory. Validate Caddy syntax and route preservation locally. Keep application checks, exact OpenAPI, V1–V4 and existing recovery/performance evidence; repeat only checks invalidated by new ingress/runtime settings. An actual VPS acceptance must test TLS renewal, reboot/restart persistence, limits/disk, firewall, mail, roles, attachments, recovery and coordinated off-machine restore. Compose syntax is not remote acceptance.

## Consequences

The same application images/configuration can move between providers; only ingress, secret provisioning and infrastructure operations differ. Hostinger pricing, domains, off-host backup destination, procurement and operational owner remain external decisions. Weekly provider snapshots are supplementary, not proof of database/attachment consistency. Security findings, full browser/accessibility acceptance and production release approval remain open.
