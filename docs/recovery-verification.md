# Selected-stack local recovery verification

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas). Verified 2026-10-10. Continues ADR-0013 recovery gates, ADR-0014 baseline discipline and the retained V1-V4 Identity/Routes schema. This is an implementation correction within the existing architecture, not a new deployment decision.

## Selection and consistency

The original backup script always used the default Compose selection and copied default .env/key paths. The active identity worktree uses a private Compose file and inline PEM keys, so that procedure could select the wrong environment or archive unrelated credentials. The script now accepts repeatable --compose-file, --project and optional --env-file arguments. With no arguments it retains Docker Compose's default selection. It verifies the selected project/service labels, current health and resolved database/backend environment against the running containers before stopping writes.

Only the selected API stops. PostgreSQL remains running; pg_dump produces a custom-format snapshot while the sole application writer and attachment cleanup jobs are stopped. No external database writers or additional API replicas may write during this single-host procedure. The attachment volume is read-only in network-isolated helper containers. Flat UUID files are hashed individually. Unrecognized directories, links or file names fail closed for explicit review.

Format 2 archives database.dump, attachments.tar.gz, compose.resolved.json and the actual RSA keys (inline or copied from their configured absolute container locations). An explicitly supplied environment file is copied as environment.env; the resolved configuration already contains interpolated values. These files contain secrets and stay in ignored .local/recovery with host-account ACL protection. No secrets enter the public evidence. Keep the backup private and move an encrypted copy off-machine before valuable deployment; scheduling, retention and recovery objectives remain operator work.

The manifest records immutable database/backend image IDs, checkout source reference, Flyway checksums, every public table's row count, per-file hashes and artifact hashes. The checkout reference can include later documentation changes; the backend image ID identifies what was actually running. A finally block attempts API restart even on snapshot failure and waits for health. A partial directory without a valid complete manifest is not an accepted backup. If restart fails, the command fails and reports the operating issue; inspect the selected stack rather than resetting volumes.

## Reproduce on the retained local stack

Run from this backend repository with Node.js 22.6+ and Docker available:

~~~powershell
node --test scripts/tests/recovery.test.mjs
node scripts/backup-development.mjs --compose-file .local/identity/compose.json --project irr-identity-verification
node scripts/verify-recovery.mjs .local/recovery/<directory-printed-by-backup>
docker compose -f .local/identity/compose.json -p irr-identity-verification ps
~~~

For the standard root Compose setup, omit selection arguments. For another local stack, pass its actual file/project; use --env-file only for the interpolation environment that belongs to it. Never change credentials to make a selection mismatch pass.

Restore checks all required artifact hashes before Docker starts. Legacy manifests are still supported, with their narrower evidence explicitly identified. A new uniquely labeled PostgreSQL container uses the archived local immutable image, --pull=never, no network and no published ports. pg_restore runs with exit-on-error. The rehearsal checks identical Flyway history and row counts, rejects unsafe tar entries before extraction, checks every database attachment reference, verifies each restored file's bytes and compares stock movements, lots and balance projections. Only its labeled disposable container and anonymous volume are removed afterward. The script cannot target the live database for restoration.

## Executed evidence

The retained local snapshot restored successfully:

| Check | Result |
| --- | --- |
| Public tables, including Flyway | 39; every table count matched |
| Applied versions/checksums | V1 583196656; V2 -1853521204; V3 -686690239; V4 -1708031960 |
| Attachment files and database references | 20 files; 20 references resolved; all 20 byte hashes matched |
| Inventory reconciliation | Zero ledger/balance and lot/balance differences |
| Isolation and cleanup | No network or ports; labeled rehearsal container removed |
| Live service recovery | Backend restarted healthy; authenticated Documents page remained available |
| Guard regression tests | Six passed: current/legacy manifests, missing entries, corruption, traversal/image identity, archive links and canonical references |

Private raw evidence is in the snapshot's manifest.json and restore-evidence.json. The latter includes a manifest digest to identify the exact snapshot it verified. The Docker database image was sha256:20edbde7749f822887a1a022ad526fde0a47d6b2be9a8364433605cf65099416. The running API image was sha256:66555e66612eb06a20350316d9796b9d33602083f76df1c54efab66081bfae81 (Analytics implementation 996e36f). Scripts and documentation changed; application code, dependencies and applied migrations did not.

The frontend browser uploaded and downloaded the valid synthetic PDF before this snapshot; its stored bytes are included. The actual downloaded file matched SHA-256 1716a8a315fe809bb0e4595dfcf0fd6c59358bcd75fbec3a455ac5145b058410, passed strict one-page parsing and rendered correctly. Opening file:// inside the in-app browser was policy-blocked; this is not claimed as viewer acceptance. See the frontend document verification guide for actual UI and render evidence.

This proves local snapshot readability, reference completeness, stock consistency and restart. It does not prove an external backup location, a full application cutover, retention policy, performance at client volume or disaster recovery time objectives.
