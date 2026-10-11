# Client synthetic homologation session

Maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

This is the owner-authorized Railway Virginia environment from ADR-0028. Client participation is for functional evaluation with synthetic records only. Production release, real client imports and final security acceptance are separate. Current runtime verification and exact deployment identifiers are in [the execution record](execution-2026-10-11.md).

## Access

1. Open [the frontend login](https://frontend-homologation-ffac.up.railway.app/login).
2. Retrieve the operator credential from the ignored backend file `.local/railway-homologation/.env-homologation`. It is private, generated for this environment and must never be committed, pasted into issues or included in public documentation.
3. Sign in with `OPERATOR_EMAIL` / `OPERATOR_PASSWORD` and select **Homologação IRR — Dados Sintéticos**. The account has organization-scoped MANAGER membership and does not have global platform administrator permissions.
4. Evaluate catalog, fleet, intake, routes/executions, processing, inventory, sales and reports using invented identifiers and records. Reports initially have no real business activity; unavailable metrics must remain unavailable instead of being treated as zero.
5. Check the clearly named synthetic PDF in attachments. Its download bytes were compared to the uploaded PDF and its persistence is verified across the backend restart in the execution record.
6. Record the screen, action, expected behavior and observed behavior for any defect. Do not include passwords, tokens or real personal/client information in feedback.

The owner can use the separate ADMIN credential from that private file to evaluate platform users and membership administration. Do not distribute the global administrator credential for routine client operational testing. The initial bootstrap credential was rotated; bootstrap inputs are cleared and startup creation is disabled before handoff.

## Scope and limitations

The UI is in Brazilian Portuguese; technical documentation and code remain English. Public Swagger is the [executable API reference](https://backend-homologation-7600.up.railway.app/swagger-ui/index.html). HTTP clients must use `/api/v1`; no deprecated aliases exist.

Mail remains disabled until the approved provider sender/key and actual inbox verification are configured. Password recovery delivery is not accepted and must be excluded from this initial session. The environment uses temporary Full Trial credits shared with the account's preserved project; availability is not guaranteed beyond the trial. Existing image/build findings and broader operator/accessibility acceptance remain documented gates. Successful healthchecks or API checks do not establish client acceptance or production readiness.
