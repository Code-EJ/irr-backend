# Pull request CI diagnosis — 2026-10-12

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

Backend [PR #79](https://github.com/Code-EJ/irr-backend/pull/79) head `02c473b` failed [workflow 38183432022](https://github.com/Code-EJ/irr-backend/actions/runs/38183432022) only in Container Verification → Verify stateful authentication and restart persistence. Java Verification, Dependency Security and Documentation Integrity passed. The backend image and patched infrastructure images also built successfully. The failing stage was PostgreSQL TCP authentication, not Java compilation or a schema migration.

`verify-stateful-images.mjs` previously waited on `pg_isready` over its default Unix socket. PostgreSQL's upstream entrypoint starts a temporary socket-only server for initialization. That server can accept the readiness probe before the final TCP listener starts, so the immediately following authenticated TCP query fails depending on timing.

The fix waits for `SELECT 1` over the same authenticated TCP connection used by the assertions. A disposable initialization SQL file sleeps for three seconds to exercise the temporary-server window on every run. Wrong-password rejection, PostgreSQL 16, restart row persistence, Redis authentication/AOF persistence and non-root process checks remain mandatory. The script still uses uniquely labeled disposable resources, no published ports/network and no development credentials; no verification step was skipped or weakened.

Local regression evidence:

- The pre-fix script with the controlled slow initializer reproduces the same TCP-authentication failure.
- The corrected script passes PostgreSQL authenticated startup and retained row after restart, plus Redis authentication/AOF/non-root checks.
- The Railway Redis regression passes mount permissions, authentication, non-root PID 1, secret-safe arguments/environment, AOF restart and noeviction.
- Documentation inventory and links are validated separately; fresh GitHub checks on the new head remain the source of truth for merge approval.

Frontend [PR #67](https://github.com/Code-EJ/irr-frontend/pull/67) at `56b4788` passed [workflow 38183435126](https://github.com/Code-EJ/irr-frontend/actions/runs/38183435126). Older red/cancelled runs do not override that head's successful result. Follow each PR's latest Checks after new commits.

The owner will review/approve the PRs. Preparing review and draft release notes does not merge either branch or establish production security/client acceptance. Candidate release notes are in [v0.1.0-rc.1](releases/v0.1.0-rc.1.md).
