# Hostinger production plan — not purchased or deployed

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Production target under [ADR-0027](../../adrs/0027-provider-independent-deployment.md): a Brazilian Hostinger KVM VPS, preferably São Paulo. Homologation remains Railway Virginia with synthetic data only. Application/deployment artifacts remain provider-independent; follow [the portable VPS procedure](../portable-vps.md). No Hostinger SDK, application patch, Kubernetes or hosting control panel is required.

## Budget and initial sizing

Official [VPS offer](https://www.hostinger.com/br/servidor-vps), checked 2026-10-10:

| Candidate | Resources | Advertised monthly equivalent | Advertised renewal equivalent | Assessment |
| --- | --- | --- | --- | --- |
| KVM 1 | 1 vCPU, 4 GB RAM, 50 GB disk | BRL 29.99 | BRL 59.99 | Tight memory/CPU margin for all services; not selected without measured validation |
| KVM 2 | 2 vCPU, 8 GB RAM, 100 GB disk | BRL 43.99 | BRL 77.99 | Proposed starting point with resource headroom; capacity verification pending |
| KVM 4 | 4 vCPU, 16 GB RAM, 200 GB disk | BRL 59.99 | BRL 149.99 | Renewal exceeds BRL 130 ceiling before other expenses; not selected |

These are time-limited published offers, **not a quote or monthly-payable promise**. The provider says plans are paid in full and the displayed rate averages the selected contract term; renewal shown is for two years. Check the actual checkout's term, total payment/installments, taxes, renewal, location and optional add-ons before approval. Do not purchase merely because the promotional equivalent fits.

At the published KVM 2 renewal equivalent, BRL 52.01/month remains within the BRL 130 ceiling for domain renewal, off-machine backup storage, required mail and other hosting expenses. This subtraction is a planning allowance, not verified pricing for those services. Simultaneous Railway homologation costs require a separate allocation or must fit the same approved total; do not silently budget each environment BRL 130.

## Region and operating model

[Hostinger's current location table](https://www.hostinger.com/support/1583267-where-are-hostinger-servers-located/) lists Brazil for VPS. Availability at setup is authoritative; confirm Brazil and the desired São Paulo facility in the order/dashboard before completing setup. Do not treat Brazil shared-hosting availability as proof of a specific VPS order. VPS location is fixed after setup; relocating requires a reviewed backup/reinstallation process.

Select a supported Ubuntu LTS template with plain Docker/Compose. Avoid cPanel/WordPress/shared hosting for the JVM/database stack and avoid unnecessary deployment dashboards or agents. Restrict firewall ingress, configure SSH keys, operator console recovery, scheduled OS/image patches and external alerts. The owner operates these responsibilities; a dashboard/AI agent is not managed application reliability.

The advertised weekly VPS backups are supplementary. They do not establish application-consistent PostgreSQL/attachment recovery, approved RPO/RTO, encrypted independent retention or a tested off-machine restore. Hostinger-specific backup/snapshot controls require separate evidence once a real VPS exists.

## Procurement and release gates

1. Owner approves checkout total/term/renewal/location and complete BRL 130 budget.
2. Owner supplies account-controlled VPS/domain access without sharing secrets in documentation/chat.
3. Reviewed images, environment-specific secrets, TLS/DNS, Brevo, monitoring and coordinated backup destination are configured using the portable runbook.
4. An isolated fresh installation and retained-volume restart pass; actual roles/workflows, HTTPS, PDF, real mail, browser/accessibility and off-machine restore are accepted.
5. Approved client batch is rehearsed and imported through the existing initial-data gates; synthetic Railway records are never copied.

Purchase, VPS creation, domain changes, live import and production deployment remain unperformed. Remaining security findings and full acceptance still block a ready-for-client claim.
