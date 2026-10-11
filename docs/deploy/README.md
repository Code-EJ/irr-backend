# Provider-independent deployment

**Start with the [human operator guide](operator-guide.md).** Choose [Railway](railway/operator-guide.md) or [Hostinger/another VPS](vps-operator-guide.md); follow [backup and recovery](backup-and-recovery.md) before retaining valuable data. These ordered procedures supersede older preparation-only wording as installation instructions. Historical ADRs and execution records retain their dates and evidence.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Authority: [ADR-0027](../adrs/0027-provider-independent-deployment.md). The application is a portable modular monolith using Docker, standard PostgreSQL/Redis, runtime configuration and the final `/api/v1` contract. Provider-specific provisioning stays outside domain/application code.

| Environment | Target | Data | Authorization |
| --- | --- | --- | --- |
| Local development | Existing Docker stacks | Retained local development records | Already running; do not reset |
| Homologation | Railway US East Virginia | Synthetic records only; distinct credentials/volumes | Resumed at owner request and verified; acceptance gates remain |
| Production | Hostinger VPS in Brazil, preferably São Paulo | Approved client data only | Budget/procurement/installation pending; not deployed |

- [Portable Linux VPS installation](portable-vps.md) uses `deploy/compose.yaml` and `deploy/Caddyfile` from this repository.
- [Hostinger production budget and operations plan](hostinger/README.md) adapts the portable installation.
- [Railway homologation runbook](railway/README.md) adapts the same application images/contract to provider-managed services.
- [Release register](../release-readiness.md) separates technical verification, deployment readiness and client acceptance.

The BRL 130/month production planning ceiling is retained. No Hostinger purchase or production data import has occurred. The isolated synthetic Railway deployment and subsequent pause are recorded separately. Keep both providers' secrets separate. Never promote the synthetic homologation database into production. Application images may move; hosting credentials and data do not move implicitly.
