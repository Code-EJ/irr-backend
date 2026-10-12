# Hostinger or another VPS: manual Docker deployment

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Reviewed 2026-10-11.

This procedure uses ordinary Linux, Docker Compose, PostgreSQL, Redis and Caddy. It requires no Hostinger plugin, Kubernetes or AI. Read [operator prerequisites](operator-guide.md) and [Hostinger procurement](hostinger/README.md). These commands are a runbook; no production VPS installation is claimed. Replace every uppercase example before running it. **Workstation** commands use Bash (Linux or WSL); **server** commands use Bash over SSH. Do not paste Bash blocks into Windows PowerShell.

## 1. Prepare the server and DNS

1. Purchase only after confirming Brazil/São Paulo location, total contract price, renewal, backup/domain costs and the approved budget. Use a plain supported Ubuntu LTS VPS, not shared/WordPress hosting. The current candidate expects an 8 GB host; the smaller trial limits are not production sizing.
2. In Hostinger hPanel open the selected VPS management screen, record its public IPv4 and enable your SSH public key. Other providers have equivalent controls. Store console recovery access privately.
3. Create a named sudo operator, test key-based SSH in a second terminal, then restrict root/password login according to your host policy. Keep console access until confirmed. On the provider firewall allow TCP 22 only from operator IPs and TCP 80/443 publicly. Do not expose 5432, 6379, 9191 or 8080. Docker-published ports can bypass simple UFW rules; verify from outside the VPS.
4. Install Docker Engine and Compose v2 using [Docker's Ubuntu repository instructions](https://docs.docker.com/engine/install/ubuntu/). Use sudo for Docker or deliberately grant the trusted operator Docker access (equivalent to root). Run `sudo docker version` and `sudo docker compose version`; both client and server must respond.
5. In the authoritative DNS provider create A records for `irr.YOUR-DOMAIN` and `api.irr.YOUR-DOMAIN` pointing to this VPS. Add AAAA only if IPv6 routing/firewall is actually configured. Remove conflicting records. From the workstation run `nslookup irr.YOUR-DOMAIN` and `nslookup api.irr.YOUR-DOMAIN` and wait for the intended address.
6. Record an ACME contact email and keep ports 80/443 free. Caddy will issue and renew HTTPS certificates. Do not add another proxy on the same ports without adapting the reviewed deployment.

## 2. Build and publish images on the workstation

Use clean archives of the reviewed commits. Set paths, SHAs, final backend name and an owner-controlled registry namespace (for example a GitHub Container Registry organization where you have package write permission). Authenticate with `docker login REGISTRY` using the registry's credential flow; do not store a token in this guide. For private GHCR packages the server later needs package-read access.

```bash
set -euo pipefail
BACKEND_REPO=/absolute/path/to/irr-backend
FRONTEND_REPO=/absolute/path/to/irr-frontend
BACKEND_SHA=REPLACE_WITH_FULL_REVIEWED_BACKEND_SHA
FRONTEND_SHA=REPLACE_WITH_FULL_REVIEWED_FRONTEND_SHA
REGISTRY=ghcr.io/YOUR-LOWERCASE-OWNER
API_DOMAIN=api.YOUR-DOMAIN
RELEASE=REPLACE_WITH_UNIQUE_RELEASE_LABEL
STAGE=$(mktemp -d)
mkdir "$STAGE/backend" "$STAGE/frontend"
git -C "$BACKEND_REPO" archive "$BACKEND_SHA" | tar -x -C "$STAGE/backend"
git -C "$FRONTEND_REPO" archive "$FRONTEND_SHA" | tar -x -C "$STAGE/frontend"

docker buildx build --platform linux/amd64 --load -t "$REGISTRY/irr-backend:$RELEASE" "$STAGE/backend"
docker buildx build --platform linux/amd64 --load --build-arg "VITE_API_BASE_URL=https://$API_DOMAIN/api/v1" -t "$REGISTRY/irr-frontend:$RELEASE" "$STAGE/frontend"
docker buildx build --platform linux/amd64 --load -t "$REGISTRY/irr-postgres:$RELEASE" "$STAGE/backend/deploy/postgres"
docker buildx build --platform linux/amd64 --load -t "$REGISTRY/irr-redis:$RELEASE" "$STAGE/backend/deploy/redis"
```

These images target x86-64. If the selected host is ARM64, build/review that architecture instead, including all dependency images. The ordinary VPS Redis Dockerfile is intentional; Compose supplies its configuration. The backend's literal Maven cache namespace can stay unchanged for a local Docker build. It is Railway that requires a target-service namespace.

Run the repository's release checks and image review before publishing. The backend Dockerfile skips tests during packaging, so a successful build alone is insufficient. Existing commands include Java 21 `./mvnw clean verify`, backend `node scripts/verify-hosted-compose.mjs`, and frontend `npm ci`, `npm run lint`, `npm test`, `npm run build`, `npm run api:check`. Run checks from their corresponding reviewed checkout. Follow [the security register](../security/README.md) for remaining image findings; do not interpret this runbook as clearance.

```bash
docker push "$REGISTRY/irr-backend:$RELEASE"
docker push "$REGISTRY/irr-frontend:$RELEASE"
docker push "$REGISTRY/irr-postgres:$RELEASE"
docker push "$REGISTRY/irr-redis:$RELEASE"
docker buildx imagetools inspect "$REGISTRY/irr-backend:$RELEASE"
docker buildx imagetools inspect "$REGISTRY/irr-frontend:$RELEASE"
docker buildx imagetools inspect "$REGISTRY/irr-postgres:$RELEASE"
docker buildx imagetools inspect "$REGISTRY/irr-redis:$RELEASE"
```

Record the registry **Digest** for each image and use `registry/name@sha256:...` in configuration, not a local image ID or mutable release tag. Select and review a Caddy 2 image in the same way; the example env contains placeholders, not approved digests. Preserve the previous release's digests for rollback. Build sources on the workstation/CI, not on the production VPS.

## 3. Copy deployment files and configure secrets

On the server, create directories (commands assume the signed-in account is the intended operator):

```bash
sudo install -d -m 0750 -o "$USER" -g "$(id -gn)" /srv/irr/deploy
sudo install -d -m 0700 -o "$USER" -g "$(id -gn)" /srv/irr/secrets
sudo install -d -m 0700 /srv/irr/keys
```

On the workstation copy the files from the **reviewed backend archive**, replacing operator and IP:

```bash
scp "$STAGE/backend/deploy/compose.yaml" "$STAGE/backend/deploy/Caddyfile" OPERATOR@VPS_IP:/srv/irr/deploy/
scp "$STAGE/backend/deploy/hosted.env.example" OPERATOR@VPS_IP:/srv/irr/secrets/hosted.env
```

The env copy is for a fresh installation only: never overwrite an existing working secret file with the example. On the server:

```bash
chmod 600 /srv/irr/secrets/hosted.env
nano /srv/irr/secrets/hosted.env
```

Replace every placeholder. Set all five `IRR_*_IMAGE` values to reviewed registry digests; use bare DNS hostnames for `FRONTEND_DOMAIN` and `BACKEND_DOMAIN`, not URLs. Set ACME_EMAIL, DB_NAME/DB_USER and different random database/Redis passwords. Set `IRR_KEYS_DIRECTORY=/srv/irr/keys`. A password manager can generate base64url passwords without `.env` quoting/interpolation pitfalls. Do not use `source hosted.env`; Compose parses it directly. Preserve the environment file privately off-machine.

For a fresh destination generate keys **on the server**, then protect them for Java UID 10001. Skip this block if keys already exist:

```bash
sudo sh -ec '
  umask 077
  test ! -e /srv/irr/keys/private.pem
  test ! -e /srv/irr/keys/public.pem
  openssl genpkey -algorithm RSA -pkeyopt rsa_keygen_bits:3072 -out /srv/irr/keys/private.pem
  openssl pkey -in /srv/irr/keys/private.pem -pubout -out /srv/irr/keys/public.pem
  chown 10001:10001 /srv/irr/keys /srv/irr/keys/private.pem /srv/irr/keys/public.pem
  chmod 700 /srv/irr/keys
  chmod 400 /srv/irr/keys/private.pem
  chmod 444 /srv/irr/keys/public.pem
'
```

Expected: matching PKCS#8 private/public PEM files readable by the backend. Keep a protected recovery copy. Never copy local/homologation keys into production. Configure approved Brevo variables following [email setup](../email-recovery.md). Disabled mail permits a deliberately incomplete test but does not pass production acceptance. Set `API_DOCS_ENABLED=true` for controlled initial API verification, then choose the approved public-docs policy.

## 4. Validate and start

On the server authenticate to private registries with `sudo docker login REGISTRY` (the same Docker context that will pull). Define this helper in **each new SSH shell** to avoid selecting another project:

```bash
dc() { sudo docker compose -f /srv/irr/deploy/compose.yaml --env-file /srv/irr/secrets/hosted.env "$@"; }
dc config --quiet
dc pull
dc up -d --wait --wait-timeout 240 database redis
dc up -d --wait --wait-timeout 240 backend frontend
dc up -d --wait --wait-timeout 240 edge
dc ps
```

Stop on a nonzero exit; inspect `dc logs --tail 100 SERVICE` privately before continuing. Expected: database/Redis/backend/frontend healthy and edge running. Never run unrestricted `dc config` in a shared terminal: it prints secrets. Compose uses `irr-hosted` volumes; keep the same project and files on subsequent deployments. It does not expose database/Redis directly. Fresh database migrations run automatically; there is no manual schema creation or repair step.

From outside the VPS:

```bash
curl --fail --show-error https://irr.YOUR-DOMAIN/health
curl --fail --show-error https://api.YOUR-DOMAIN/actuator/health/readiness
curl --fail --show-error --output /dev/null https://irr.YOUR-DOMAIN/login
```

Expected HTTP 200 and backend UP. Check the browser certificate and direct routes. Run [common public acceptance](operator-guide.md#4-common-acceptance-checklist), including the current `/api/v1` contract. Confirm the externally reachable ports match step 1, and verify persistence after restarting backend/frontend. Do not reboot the server during an unannounced client session.

## 5. First administrator and client data

On a fresh database, edit the protected env to set bootstrap true and explicit name/email/unique temporary password. Run `dc up -d --wait backend`. Log in through the frontend and complete mandatory password change. Edit bootstrap back to false, clear its name/email/password and run the same command again. Confirm the new password works. Existing administrators are not reset by bootstrap; do not delete users to force it.

Create the organization and ordinary operational accounts using the interface/Swagger, retaining global administrator access with the owner. Production imports follow [initial-data validation](../data/initial-load.md). Do not transfer synthetic Railway records or development data into production.

## 6. Backups and routine maintenance

Before valuable data or a schema-changing update, complete [the backup/recovery procedure](backup-and-recovery.md), define retention and off-machine encryption, and prove an isolated restore. Record who responds to alerts. At minimum monitor external HTTPS/readiness, disk space/inodes (`df -h`, `df -i`), container health/restarts (`dc ps`), resource pressure (`sudo docker stats --no-stream`) and backup age. Configure actual alerts in an owner-controlled monitoring service; this guide does not imply they already exist.

Inspect usage daily during initial acceptance and after workload changes. Patch OS and reviewed images in a scheduled window; immutable digests do not update themselves. Validate certificate renewal state and retain Caddy volumes. Do not use system-wide `docker system prune --volumes` on this host.

## 7. Update, pause and rollback

For an application update: take the coordinated backup, record current digests, build/scan/push the new reviewed image pair on the workstation, then change only the intended digest entries in protected hosted.env. If Compose/Caddy changed, copy their reviewed versions as well. Run:

```bash
dc config --quiet
dc pull backend frontend
dc up -d --wait --wait-timeout 240 backend frontend
dc up -d --wait --wait-timeout 240 edge
dc ps
```

Repeat public acceptance. Changes to env or image need `up`, not just `restart`. Domain/API origin changes also need DNS, frontend rebuild, CORS/reset URL and TLS checks. Upgrade database/Redis/Caddy in their own reviewed maintenance window, never incidentally with an app rollback.

To pause applications while preserving dependencies use `dc stop edge frontend backend`. Resume with `dc up -d --wait --wait-timeout 240`. To stop the entire stack for planned maintenance use `dc stop`; do not add `-v` or delete volumes. A VPS subscription normally continues charging even while containers or the VM are stopped; confirm your provider contract.

For an application rollback, restore only the previous **compatible** backend/frontend image references, pull and run the update commands. Database history and uploaded files remain. If new migrations/data are incompatible with the old binary, stop and follow the isolated recovery procedure; restoring a backup can lose later writes and is not an ordinary release rollback. Never revert credential-generation enforcement or manipulate Flyway history.

## 8. Diagnose without resetting data

| Symptom | Command/check and next action |
| --- | --- |
| Pull denied | `sudo docker login` with package-read permission; verify exact registry digest |
| Bind/key error | Confirm IRR_KEYS_DIRECTORY, existing PEM files and UID 10001 permissions |
| TLS/certificate failure | DNS A/AAAA, ports 80/443, `dc logs --tail 100 edge`; confirm no competing web server |
| API 502/503 | `dc ps`, then backend/database/Redis logs; confirm readiness, credentials and memory |
| Database password failure after edit | Restore previous matching secret or deliberately rotate the PostgreSQL role; env edits do not rotate it |
| Upload failure | Check persistent uploads mount, free disk/inodes and backend UID; preserve existing file ownership |
| Browser CORS or localhost API | Rebuild frontend with final public /api/v1 URL and correct exact CORS origin |
| Recovery 503 | Configure approved mail provider and check Redis; use the email runbook |
| Migration mismatch | Preserve evidence and stop; compare release with schema history, do not clean/repair |

When asking a human maintainer for help, supply release SHA, container/deployment status and a redacted error. Never share hosted.env, resolved Compose output, private PEM or recovery links.
