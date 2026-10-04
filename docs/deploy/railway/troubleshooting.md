# Railway troubleshooting

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

| Symptom | Check and safe action |
| --- | --- |
| Datasource connection fails | Verify DB_* reference names, especially DB_PORT. Do not print passwords or credential-bearing URLs. |
| Readiness DOWN | Verify PostgreSQL and Redis connectivity/authentication. Liveness alone does not prove dependencies are ready. |
| RSA binding failure | Supply a matching PEM pair with preserved line breaks or readable mounted resources. Never place private keys in VITE_* variables. |
| Flyway checksum mismatch | Confirm deployed commit and database history. Do not rewrite V1, erase history, use clean or automatically repair checksums. Investigate the artifact/database mismatch. |
| V2 lock wait | Identify active database transactions; schedule deployment appropriately. Do not manually recreate users. |
| First administrator not created | Bootstrap is disabled by default and inactive in development. An existing administrator, including inactive history, closes bootstrap. Verify optional inputs without logging their values. |
| First-account email already exists | Bootstrap deliberately refuses promotion. Use a unique intended administrator address or an explicitly reviewed recovery procedure. |
| Temporary login cannot access business data | Expected password_change_required restriction. Deploy the identity-capable frontend, change password, then sign in again. |
| Old password or bearer token rejected | Expected after change/reset/deactivation. Authenticate with current credentials; do not disable generation checks. |
| Administrator cannot see an organization's business data | Platform role does not grant membership. Use explicit organization association administration. |
| Frontend cannot connect | Check its build-time public HTTPS /api/v1 URL and exact backend CORS origin. Rebuild after URL changes. |
| Nested frontend route returns 404 | Verify the existing Nginx SPA fallback and target port 8080. |
| Uploaded document disappears after release | Check persistent volume and STORAGE_DIRECTORY; image filesystems are not persistent storage. |

Normal releases use Git → Docker build → startup → Flyway → health. Never patch source inside a running container. See [rollout](rollout.md) before rollback: old credential-unaware binaries are unsafe after password changes.
