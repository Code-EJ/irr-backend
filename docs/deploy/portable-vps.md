# Portable Linux VPS installation candidate

For an ordered installation with copyable commands, follow [the VPS operator manual](vps-operator-guide.md) and [backup/recovery procedure](backup-and-recovery.md). This page retains architectural constraints and earlier candidate verification; it is not the primary installation sequence.

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Use after reviewing [ADR-0027](../adrs/0027-provider-independent-deployment.md), [release gates](../release-readiness.md) and the provider's budget/region approval. This procedure is not evidence of an installed public environment.

## Host and access

Provision a supported Ubuntu LTS host with Docker Engine and Compose v2 from [official Docker instructions](https://docs.docker.com/engine/install/ubuntu/). Use a named operator account, SSH keys and restricted source-IP SSH ingress. Test a second SSH session and console recovery before disabling password/root SSH access. Docker-group membership is root-equivalent; do not grant it to application users or expose the Docker socket.

Allow only TCP 80/443 publicly and restricted SSH through the provider firewall. Docker-published ports can bypass simplistic UFW rules: verify actual external reachability and the Docker firewall chain policy. Do not publish 5432, 6379, 9191 or 8080. Configure OS/security updates, disk/inode alerts and remote uptime monitoring with a named incident owner. A single host has downtime and no automatic HA.

## Immutable images and secrets

Build the backend/frontend in the controlled build environment and scan all final images. Build the frontend with its final `https://API-DOMAIN/api/v1` build argument; verify its exact bundle with `npm run verify:production`. Push only approved immutable digests to an owner-approved registry; authenticate on the host without putting registry passwords in scripts. Do not build application sources on the production VPS.

Copy only the reviewed `deploy/compose.yaml` and `deploy/Caddyfile` to `/srv/irr/deploy`. Copy `deploy/hosted.env.example` to a protected file outside Git, for example `/srv/irr/secrets/hosted.env`, with mode 0600. Replace **all** example domains/digests/passwords; placeholders are not production configuration. Explicitly select reviewed Caddy 2, PostgreSQL 16 and Redis 8.2 images and the matched application pair. No mutable latest tags or silent major changes.

Generate a new matching environment-specific RSA keypair, never reuse development/homologation keys. Store PKCS#8 private PEM and X.509 public PEM in `/srv/irr/keys`; ownership UID/GID 10001, directory mode 0700, private mode 0400, public mode 0444. Set `IRR_KEYS_DIRECTORY` to that existing absolute path. Do not print keys or resolved environment configuration. Keep protected off-host key recovery separate from data backups.

Configure two DNS names pointing to the VPS IP, frontend and backend. Set `FRONTEND_DOMAIN`, `BACKEND_DOMAIN`, ACME contact and build the matching frontend origin. Caddy preserves `/api/v1` and provides automatic HTTPS; it does not strip API prefixes or implement business authorization. Certificates require working DNS and ingress 80/443. TLS state persists in its volumes. Do not add access logging of recovery URLs. Edge runs with the image's default identity but drops capabilities except bind-to-low-port, has a read-only root filesystem and no-new-privileges; Java and frontend retain UID 10001/101.

Set Brevo sender/key through backend-only configuration. `MAIL_PROVIDER=disabled` is only an explicitly incomplete interim setup, not passed recovery acceptance. Set optional first-admin bootstrap only for the approved empty destination and remove the temporary password/disable bootstrap after mandatory change. Do not enable a development profile, development bootstrap, Mailpit or synthetic/client import silently.

## Validate and install

Repository-only verification: `node scripts/verify-hosted-compose.mjs` validates network/port/development/image boundaries using synthetic configuration and starts no services. The dated local Caddy check validated version 2.11.7 syntax; its image inventory has pending findings and is not an approved production digest. Neither check verifies public TLS, actual resource limits or host persistence.

On the approved Linux host:

```sh
cd /srv/irr/deploy
docker compose --env-file /srv/irr/secrets/hosted.env config --quiet
docker compose --env-file /srv/irr/secrets/hosted.env pull
docker compose --env-file /srv/irr/secrets/hosted.env up -d --wait --wait-timeout 180
docker compose --env-file /srv/irr/secrets/hosted.env ps
```

Do not omit `--env-file`, print `config` without `--quiet`, or use `down -v`. A fresh Docker named upload volume receives the image's UID 10001 ownership; Railway's opt-in root initializer is **not enabled** in this Compose file. Existing/restored permissions must be checked offline before startup; no recursive automatic chown is performed. Existing V1–V4 migrations run during backend startup, with Hibernate validation and no baseline/repair/reset.

The candidate limits total roughly 4.5 GiB across services, leaving headroom on proposed 8 GiB VPS; limits are not measured capacity guarantees. Confirm JVM startup/RSS, normal/peak report requests, DB memory, Redis no-eviction behavior and disk growth before release. Do not perform large source builds on the production server.

## Acceptance, backups and rollback

Run existing frontend read-only `verify:homologation` against the real HTTPS pair with exact source SHA and no local mode; verify Swagger contract independently. Confirm DNS/TLS, CORS, nested SPA routes, ordinary/manager/admin permissions, mandatory password change, real recovery mail, valid PDF viewing, stock/reversals and report definitions. Test upload/download after application restart and an authorized host reboot. Check external ports and certificate renewal state, not only internal health.

Approve RPO/RTO/retention and an encrypted off-machine backup destination. Quiesce business/file writes at the documented recovery boundary, capture database plus attachments and reconcile the restore in isolation. Local/provider snapshots do not establish that boundary or off-host recovery. Reuse existing recovery tooling; never run rehearsal against live volumes. Keep startup secrets/key recovery separate.

Record SHA/digests, OpenAPI/migration hashes, provider region and acceptance evidence. Keep previous compatible image digests for application rollback; rollback does not undo data or migrations. Never restore old data as a normal deploy. No production purchase/install is authorized by syntax checks or this runbook.
