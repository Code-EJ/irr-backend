# Docker-host backup and recovery procedure

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Reviewed 2026-10-11.

This procedure applies to the selected Docker Compose host. It does not remotely back up Railway, and it must never select a development stack as a substitute for the deployed stack. See [Railway backup boundaries](railway/operator-guide.md#7-pause-update-rollback-and-backups). The retained tooling and local restore evidence are described in [recovery verification](../recovery-verification.md).

## 1. Prepare before collecting valuable data

Agree on maximum tolerable data loss (RPO), restoration time (RTO), schedule, retention, encrypted off-machine destination and incident owner. A backup left only on the VPS is not protection against losing the VPS. Provider snapshots supplement, rather than replace, coordinated application backup and restoration tests.

Install Node.js 24 and Git on the operator host and keep a protected checkout of the exact reviewed backend operations scripts at `/srv/irr/ops/irr-backend`. This checkout is for operations, not production source builds. Record its commit with `git rev-parse HEAD`. The scripts invoke Docker directly: run them under the trusted operator with Docker access or an explicitly controlled sudo environment. Protect the checkout's ignored `.local` directory, which will contain backup secrets. Never publish or send those files to support.

For a fresh operations directory, as the trusted server operator with repository access:

```bash
sudo install -d -m 0700 -o "$USER" -g "$(id -gn)" /srv/irr/ops
git clone https://github.com/Code-EJ/irr-backend.git /srv/irr/ops/irr-backend
cd /srv/irr/ops/irr-backend
git checkout --detach REPLACE_WITH_FULL_REVIEWED_BACKEND_SHA
git rev-parse HEAD
node --version
docker version
```

Expected: the reviewed commit, Node 24 and a reachable Docker daemon. On an existing checkout, inspect changes before changing refs. Do not clone over an existing operations directory.

The backup requires one healthy backend writer and one healthy database. Disable independent SQL/file writers and additional API replicas for the window. Its script verifies project labels and actual running environment against selected Compose configuration, stops only backend writes/cleanup, dumps PostgreSQL and archives/uploads, then restarts the backend in a finally block. Coordinate the short outage with users.

## 2. Take and verify a backup on the VPS

Run from the operations checkout on the **server**:

```bash
cd /srv/irr/ops/irr-backend
umask 077
node scripts/backup-development.mjs \
  --compose-file /srv/irr/deploy/compose.yaml \
  --project irr-hosted \
  --env-file /srv/irr/secrets/hosted.env
```

Despite its historical filename, the script supports the explicit selected Compose file/project. Expected output identifies `irr-hosted`, prints a new backup directory and confirms the backend restarted healthy. A partial directory or restart failure is not successful completion. Inspect the selected stack privately if it fails; never change secrets to bypass a selection mismatch.

Set SNAPSHOT to the exact printed directory, then run:

```bash
SNAPSHOT=/srv/irr/ops/irr-backend/.local/recovery/REPLACE_WITH_PRINTED_DIRECTORY
node scripts/verify-recovery.mjs "$SNAPSHOT"
```

Expected: isolated restore passes hashes, migration history, row counts, attachment references/bytes and stock reconciliation. The script uses an archived immutable PostgreSQL image ID with `--pull=never`; retain that image or load its saved archive on another recovery host before the rehearsal. It creates a labeled temporary database with no network or published ports and removes only its own disposable resources. It does not restore the live application.

The backup contains database.dump, attachments.tar.gz, manifest.json, resolved Compose config, RSA keys and the supplied environment file. **It contains secrets.** Successful rehearsal adds private restore evidence. Redis AOF and Caddy certificate state are not included by this script: preserve their volumes during normal updates. In a total-host recovery a fresh Redis loses transient counters; Caddy can reissue certificates subject to DNS and provider limits. These are explicit differences from full-volume recovery, not silently verified behavior.

## 3. Preserve images and an encrypted off-machine copy

Keep the exact reviewed release files, image digests, scripts and database image available outside the failed-host boundary. For a controlled local archive, use `docker image save` with the exact database/backend/frontend references recorded in the release. This also preserves the local image ID required by the rehearsal. Package only the intended private backup directory, encrypt with your approved backup tool and copy to the approved off-machine destination. For example, with GnuPG installed:

```bash
umask 077
tar -czf /srv/irr/secrets/recovery-bundle.tar.gz -C "$(dirname "$SNAPSHOT")" "$(basename "$SNAPSHOT")"
gpg --symmetric --cipher-algo AES256 --output /srv/irr/secrets/recovery-bundle.tar.gz.gpg /srv/irr/secrets/recovery-bundle.tar.gz
sha256sum /srv/irr/secrets/recovery-bundle.tar.gz.gpg
scp /srv/irr/secrets/recovery-bundle.tar.gz.gpg BACKUP_OPERATOR@BACKUP_HOST:APPROVED_PRIVATE_DIRECTORY/
```

Choose new dated archive names for each backup; do not overwrite a retained recovery point. GPG prompts for the passphrase: keep it in a separate owner-controlled secret store, never command arguments. Confirm the remote encrypted-file checksum matches, prove decryption on a recovery machine, and apply the approved retention policy. The unencrypted local bundle remains sensitive and must be handled under the host's protected retention policy. The example does not configure scheduling or a remote account for you. Keep decryption credentials and SSH/key recovery independently accessible.

On the isolated recovery host, copy the encrypted bundle through the approved backup channel, verify its recorded SHA-256, and decrypt into a fresh protected directory:

```bash
umask 077
RECOVERY_DIR=$(mktemp -d)
gpg --output "$RECOVERY_DIR/bundle.tar.gz" --decrypt /absolute/path/to/recovery-bundle.tar.gz.gpg
tar -tzf "$RECOVERY_DIR/bundle.tar.gz"
```

Inspect the listing: it must contain only the expected generated snapshot directory and its files, with no absolute or parent paths. Extract that trusted outer archive with `tar -xzf "$RECOVERY_DIR/bundle.tar.gz" -C "$RECOVERY_DIR"`, set SNAPSHOT to the extracted snapshot directory and run the retained verifier before using the inner database/attachment artifacts. A mismatched checksum, unexpected archive listing or decryption failure stops recovery.

## 4. Recover onto a new isolated host

Use a new host/project with empty volumes; preserve the failed host and backup until the owner accepts recovery. Restoring replaces a point in time and can lose newer writes. Do not point these commands at the running production project. No public ingress should reach the recovery host until validation is complete.

1. Follow VPS steps 1–3 for a new host, but keep ingress closed and **do not start backend/edge yet**. Restore the reviewed Compose/Caddy files and protected environment from the backup/release. Select the archived compatible image digests, not current latest images. Restore the backed-up PEM pair with UID/GID 10001 and the permissions from the VPS guide; do not generate a different pair accidentally. Keep bootstrap disabled.
2. Securely transfer/decrypt the snapshot, retain the exact database image (`docker image load` if archived), and rerun `verify-recovery.mjs` on this host. Stop if any hash, history, file or reconciliation check fails.
3. Define `dc` as in the VPS guide. Confirm its explicit host, project and empty volumes. Run `dc pull`, then `dc up -d --wait database redis`. The database must contain **zero application tables** before import.

On that **new isolated server only**, after setting SNAPSHOT:

```bash
dc exec -T database sh -ec 'psql -U "$POSTGRES_USER" -d "$POSTGRES_DB" -Atc "SELECT count(*) FROM information_schema.tables WHERE table_schema = '\''public'\''"'
```

Expected result: `0`. If nonzero, stop; do not drop the schema. Copy and restore the verified custom dump:

```bash
dc cp "$SNAPSHOT/database.dump" database:/tmp/irr-recovery.dump
dc exec -T database sh -ec 'pg_restore --exit-on-error --no-owner --no-privileges -U "$POSTGRES_USER" -d "$POSTGRES_DB" /tmp/irr-recovery.dump'
dc exec -T database rm /tmp/irr-recovery.dump
dc create --no-recreate backend
```

Create the backend container without starting it so its fresh uploads volume exists. Confirm it is not running. Restore only to that empty volume, using the already verified archive and the selected PostgreSQL image as a shell/tar helper:

```bash
ApiContainer=$(dc ps -a -q backend)
DatabaseContainer=$(dc ps -q database)
test -n "$ApiContainer" && test -n "$DatabaseContainer"
test "$(sudo docker inspect --format '{{.State.Running}}' "$ApiContainer")" = false
DatabaseImage=$(sudo docker inspect --format '{{.Image}}' "$DatabaseContainer")
sudo docker run --rm --network none --volumes-from "$ApiContainer" \
  --mount "type=bind,source=$SNAPSHOT,target=/backup,readonly" \
  "$DatabaseImage" sh -ec '
    test -z "$(ls -A /data/uploads)"
    tar -xzf /backup/attachments.tar.gz -C /data/uploads
    chown 10001:10001 /data/uploads
    find /data/uploads -type f -exec chown 10001:10001 {} +
  '
```

This extraction is allowed only after the retained verifier accepted the archive's paths and rejected links. Never extract an unverified tar into a live volume. Start `dc up -d --wait --wait-timeout 240 backend frontend`, inspect migration validation and readiness, and verify the recovered account/organization/PDF/inventory/report totals against the snapshot. Compare Flyway history and counts using the private manifest; do not rewrite history to make them match. The image pair should match the backup's schema before any new migration is introduced.

4. Test the recovered application through a controlled hostname and exact rebuilt frontend origin if needed. Keep emails disabled during isolated rehearsal unless an approved test inbox is used. Recheck the retained PDF and role behavior; rerun the common acceptance checks.
5. Only after owner acceptance, plan DNS/ingress cutover, enable the approved mail configuration, start edge, verify TLS/CORS and repeat external acceptance. Record actual elapsed recovery time and data cutoff. Preserve the old host until retention/disposal is explicitly decided.

This full-host procedure must be rehearsed in the chosen environment before production delivery. The previously verified local script proves its isolated database/files checks; it does not certify Hostinger DNS cutover, email restoration or operational RTO.
