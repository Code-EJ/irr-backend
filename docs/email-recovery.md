# Password recovery operations

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Contract and security

[ADR-0018](adrs/0018-password-recovery-and-transactional-email.md) owns the storage/provider decision. The executable contract is Swagger. Public POST `/api/v1/session/password-recovery/request` accepts `{ "email": "account@example.test" }` and returns an empty 202 for existing, absent, inactive and throttled accounts. Mail disabled or Redis unavailable returns an account-independent 503. The bounded worker performs lookup/delivery outside the public request thread; queue overflow is suppressed neutrally and logged without personal data. There is no delivery guarantee across a process crash; retry after the cooldown.

POST `/api/v1/session/password-recovery/confirm` accepts a token and newPassword, returns 204 once, and revokes previous bearer sessions through credential generation. Invalid/expired/used/revoked links return the same 400. Password validation failures also return 400. Neither response exposes a token, hash, password or account metadata. The frontend token lives only in memory, is read from a URL fragment and removed from browser history; reloading that page requires reopening the email link.

V3 stores only SHA-256 of 32 random bytes, account ID, issued email/generation, timestamps and consumption state. Account locking and token consumption share the password transaction. Email changes, deactivation and any credential replacement invalidate prior authorization. The hourly cleanup removes at most 1,000 records expired for over one day. Back up the table with PostgreSQL; do not copy raw email links into logs or support tickets.

Redis limits: one request per normalized email per minute, ten requests per directly connected client address per minute, 300 aggregate accepted requests per rolling 24-hour window, and twenty confirmations per client address per minute. Keys obscure addresses/emails with SHA-256; this is not encryption. The aggregate limit includes nonexistent addresses, preserving account-independent behavior. Forwarded headers are deliberately ignored. Behind a shared proxy, the address limit can aggregate users: verify actual proxy behavior during homologation before increasing limits or adding explicitly trusted-proxy handling. Never trust arbitrary client forwarding headers.

## Development, step by step

1. Copy `.env.example` to the ignored private `.env`. Set database, Redis and application development credentials. Existing startup never rewrites an account password; use authenticated change or recovery.
2. Set `MAIL_PROVIDER=mailpit`, `MAIL_FROM=development@irr.local`, `MAILPIT_URL=http://mailpit:8025`, and `PASSWORD_RESET_BASE_URL=http://localhost:5173/reset-password`. Replace the frontend port if your actual frontend uses another port. Keep `PASSWORD_RESET_TTL_MINUTES=30` unless testing a reviewed alternative (5–60 minutes).
3. Follow the root README to generate keys, then run `docker compose --env-file .env up --build -d`. Compose runs Mailpit, PostgreSQL, Redis and the backend. Mailpit is a local catcher; its UI is loopback-only at `http://localhost:8025` (or MAILPIT_PORT). SMTP is not published and no relay is configured. Messages are development artifacts and may disappear when the catcher is recreated.
4. Run the frontend with its explicit `/api/v1` URL. Open Login → **Esqueci minha senha**, supply your seeded account email and check Mailpit. Verify the neutral response also for an absent email.
5. Open the captured link, enter/confirm a different password and return to login. The old password and old bearer token must fail, the new password must work, and reusing the link must fail. Do not paste its token into command output.
6. Restart the backend using the same volumes. Verify login still uses your changed password. Change bootstrap input only in a local rehearsal to prove startup is create-only; the existing account must remain unchanged. Never use `down -v`, Flyway clean/repair or a database reset for this check.

Tests use disposable Testcontainers infrastructure, captured test mail and local HTTP stubs. `./mvnw clean verify` includes expiry, replay, concurrent consumption, inactive/email/generation invalidation, delivery failure, Redis failure and throttling. No third-party email traffic is required.

## Brevo homologation setup

The selected provider's [free plan](https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan) advertises 300 emails/day as checked on 2026-10-04. API limits and account quotas are separate; recheck the plan and account restrictions before deployment. Mailpit has no external quota.

1. Create a Brevo account and complete provider verification. Enable transactional email and review account approval restrictions.
2. Add your sender and authenticate the sending domain using the DNS records shown by the provider. Verify domain/sender status before sending. DNS ownership and provider approval are external operator dependencies.
3. Create a dedicated API key, store it as backend `MAIL_API_KEY` in Railway Variables/Secrets, and never use a VITE variable for it.
4. Set `MAIL_PROVIDER=brevo`, verified `MAIL_FROM`, `MAIL_FROM_NAME=IRR`, `PASSWORD_RESET_BASE_URL=https://your-frontend-public-domain/reset-password`, and `PASSWORD_RESET_TTL_MINUTES=30`. HTTPS delivery uses the fixed official Brevo endpoint; no SMTP port or provider SDK is added.
5. Deploy a compatible frontend/backend pair through Git/PR/main and Railway. Verify health/readiness, CORS, enabled recovery, external inbox arrival, spam placement, expiry and replay. A green readiness probe alone does not verify email delivery.
6. Monitor aggregate delivery failure/queue-full logs and provider quotas. The adapter intentionally drops provider response bodies and exception causes because they can contain recipients or secrets. Inspect provider diagnostics through the provider's secure console. There is no automatic fallback, durable outbox or provider retry that could unexpectedly duplicate/reset delivery.

The default application setting is `MAIL_PROVIDER=disabled`, preserving ordinary login in existing environments without inventing secrets. This is **not** an acceptable completed recovery homologation gate: it returns 503 explicitly.

## Rollout and rollback

No reset is required. Preserve V1/V2 checksums; Flyway applies V3 automatically. An existing database, Redis, RSA keys and upload volume remain in place. Railway needs the listed mail settings plus sender/domain verification and HTTPS egress; no new database or Redis service is required. The local implementation does not change remote settings or secrets.

Deploy the backend first, then the frontend pinned to its generated OpenAPI. Keep both manifests and migration checksums. A pre-recovery binary can ignore the additive V3 table, but a rollback disables recovery and needs the compatible UI; retain the table and data. Do not roll back before credential-generation enforcement, because doing so can revive revoked sessions. Do not delete migrations or token rows as a substitute for a reviewed security rollback. Infrastructure/key rotation remains an explicit operator action.
