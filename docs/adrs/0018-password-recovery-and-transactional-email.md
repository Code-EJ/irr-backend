# ADR-0018: Password recovery and transactional email

- Status: Accepted; implemented and locally verified. External homologation pending.
- Date: 2026-10-04.
- Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

## Context and drivers

The consolidated owner direction approves email recovery in the current Identity milestone. Historical FF-2.b requires an expiring link but specifies no duration. ADR-0016's authenticated change and administrative reset remain; its prior exclusion of public recovery is superseded. Preserve the current Java/Spring modular monolith, BCrypt policy, credential generation and single frontend transport. Avoid account enumeration, reusable bearer recovery secrets and paid local email traffic.

## Storage options

Redis offers native TTL and fast atomic consumption but cannot atomically commit a password change with PostgreSQL. Consuming first can lose the user's link on database failure; consuming later can permit races without added coordination. PostgreSQL permits token consumption, account row locking, password replacement and generation increment in one transaction and survives service restart. Choose PostgreSQL with indexed expiry and bounded cleanup. This adds one small V3 table rather than a distributed transaction.

## Decision and threat model

Use POST `/api/v1/session/password-recovery/request` and POST `/api/v1/session/password-recovery/confirm`. Requests return the same 202 neutral response for existing, absent, inactive and throttled addresses. A bounded asynchronous worker performs lookup and sending so request latency does not disclose provider/account existence. No email or token enters application logs. Queue overflow has the same neutral response; operators monitor aggregate failure counters/log messages without PII. Requests can be retried after the cooldown; queued messages are not guaranteed across a process crash, so this is not a durable email-delivery service.

Generate 32 secure random bytes, URL-safe encoded; store only SHA-256 digest, user ID, issued credential generation, expiry and usage time. Default PASSWORD_RESET_TTL_MINUTES=30, configurable within a bounded safe range. This is a technical risk/exposure decision, not a historical product requirement. User password changes, administrative resets, deactivation and profile email changes invalidate outstanding recovery authorization through generation/address checks. Completion locks the account before consuming its token; a valid token can change credentials once, revoke previous bearer tokens and clear mandatory-change state atomically. Invalid, expired and used tokens share a safe rejection; the UI explains requesting another link.

Apply a shared Redis request throttle keyed by hashed normalized email and client address, plus a global provider-budget ceiling. Do not trust client-supplied forwarding headers. A Redis failure must not bypass limits; return an account-independent service error. This use of Redis for abuse control does not make it the source of recovery authorization. Random-token guessing is computationally infeasible; still bound input size and apply a coarse confirmation throttle before BCrypt work.

Recovery URLs are built only from configured PASSWORD_RESET_BASE_URL, never Host headers or request input. Use a frontend URL fragment for the token to avoid web-server URL/referrer logging; frontend clears it from browser history after reading into memory. Password fields and tokens are never stored in localStorage, analytics, logs or API responses. Production link origin must be public HTTPS; local HTTP is allowed only for loopback development. No automatic fallback sends secrets to a different provider.

## Provider comparison (official sources checked 2026-10-04)

| Provider | Free allocation / limits | API and domain | Pros / cons | Decision |
| --- | --- | --- | --- | --- |
| Brevo | 300 sends/day; general transactional send API limits documented separately (1,000 requests/second and 3,600,000/hour); account quota still applies | HTTPS API / SMTP; authenticated sender/domain | Higher daily free allowance; account/sender approval and deliverability operations required | Selected HTTPS adapter |
| Resend | Free plan advertises 100/day; monthly allocation must be confirmed at signup; current pricing lists three custom domains | HTTPS API / SMTP | Simple API; tighter daily ceiling | Viable future adapter |

Sources: [Brevo free plan](https://help.brevo.com/hc/en-us/articles/208580669-FAQs-What-are-the-limits-of-the-Free-plan), [Brevo rate limits](https://developers.brevo.com/docs/api-limits), [transactional API](https://developers.brevo.com/docs/send-a-transactional-email), [Resend pricing](https://resend.com/pricing). Limits are point-in-time evidence, not contractual guarantees; recheck before enabling delivery.

EmailSenderPort separates recovery policy from the Brevo HTTPS adapter. Use existing HTTP/Jackson facilities rather than a provider SDK. A local Mailpit adapter captures email inside Docker without consuming external quota. MAIL_PROVIDER defaults to disabled for existing environments; disabled recovery returns an explicit service-unavailable result uniformly, never pretends to send. Local Compose enables Mailpit. Configuration includes MAIL_API_KEY, MAIL_FROM, MAIL_FROM_NAME, PASSWORD_RESET_BASE_URL and PASSWORD_RESET_TTL_MINUTES. No secrets belong in Git or VITE_*.

## Deployment and consequences

V3 is additive, no reset required. Existing login and ordinary account operations keep working before mail is configured. The Identity homologation gate requires recovery enabled and verified. Operators must create/verify a provider account, authenticate sender/domain DNS, add mail Variables/Secret and set the public frontend link URL. Those are explicit Railway infrastructure changes, unlike the earlier identity slice's NONE statement. Local code and contract completion cannot prove external inbox delivery without that configuration.

Local tests must cover request neutrality, captured email, expiry, single-use/races, generation/email invalidation, inactive users, malformed tokens, throttling, provider failure and actual changed login. Test the local catcher end to end; use an HTTP stub for the provider adapter, with no actual third-party email. Switching providers requires a new adapter and reviewed configuration, not rewriting recovery policy. Reporting email may reuse the port later under a separate domain workflow.

## Verification closure — 2026-10-04

The locally executed checks and remaining release gates are recorded in [the identity verification record](../identity-verification.md). Backend executable baseline: 18f4891825821dae931ab7f3d44a2d0cd48c311f. No production reset, remote secret change or external delivery was performed.
