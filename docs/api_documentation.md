# HTTP contract map

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

The official documentation is the running [Swagger UI](http://localhost:9191/swagger-ui/index.html) and [OpenAPI JSON](http://localhost:9191/v3/api-docs). This supporting map was generated from the verified container on 2026-10-04 at backend commit 18f4891825821dae931ab7f3d44a2d0cd48c311f: 95 supported operations, all under /api/v1. Request schemas and response examples must be read from Swagger, not copied from historical issue descriptions.

## Contract rules

- POST /api/v1/session/authenticate accepts email/password and returns token. Current account metadata is GET /api/v1/users/me. Global administrators manage all four account roles, organizations and organization associations. Concurrent demotion/deactivation cannot remove the last active global administrator.
- Business routes require Authorization: Bearer and X-Organization-Id. Global administrators may select any active organization without membership; all other users need an active membership. GET /api/v1/organizations lists effective visibility; GET /api/v1/organizations/{id}/membership returns the selected role.
- All quantities and monetary amounts are exact JSON decimal strings. UUID identities, English fields, PUT replacements and Spring Page content/totalElements replace old client assumptions. Default pages contain 20 items, with a maximum of 200.
- Reference-data writes require global ADMINISTRATOR or organization MANAGER. Active MEMBER and MANAGER associations may operate intake, processing and sales. Attachments can be read by members; uploader or manager may change unreferenced metadata.
- Missing/invalid authentication returns 401; insufficient role returns 403; absent/foreign resources return 404; missing organization and invalid input return 400; stale versions, depleted stock and retained references return 409. Payload limits return 413. Read the structured error response; do not infer field validation from an HTTP status alone.
- Idempotency-Key is mandatory for processing creation and post/reverse transitions. Retry the same command and payload with the same key. Editing the command requires a new key. Draft replacement uses the last returned version.
- POST /api/v1/documents accepts multipart documento, retaining that legacy wire name. Maximum size is 10 MiB. PDF/PNG/JPEG signatures are checked; this is not malware scanning.
- Every application route uses /api/v1, including session, account and organization operations. Unversioned aliases and the self-registration stub have been removed. Framework Swagger and actuator URLs are unchanged.


Public recovery uses POST `/api/v1/session/password-recovery/request` (neutral 202) and POST `/api/v1/session/password-recovery/confirm` (single-use 204). Both exclude bearer credentials and organization headers. Disabled recovery/Redis failure returns 503 independently of account existence; confirmation abuse returns 429. Profile and password changes remain authenticated. See [email recovery operations](email-recovery.md) and [verified identity delivery](identity-verification.md).

## Operation inventory

| Method | Path | Resource |
| --- | --- | --- |
| GET | `/api/v1/buyers` | Buyers |
| POST | `/api/v1/buyers` | Buyers |
| GET | `/api/v1/buyers/{id}` | Buyers |
| PUT | `/api/v1/buyers/{id}` | Buyers |
| DELETE | `/api/v1/buyers/{id}` | Buyers |
| GET | `/api/v1/collections` | Collections |
| POST | `/api/v1/collections` | Collections |
| GET | `/api/v1/collections/{id}` | Collections |
| PUT | `/api/v1/collections/{id}` | Collections |
| DELETE | `/api/v1/collections/{id}` | Collections |
| GET | `/api/v1/documents` | Attachments |
| POST | `/api/v1/documents` | Attachments |
| GET | `/api/v1/documents/{id}` | Attachments |
| PUT | `/api/v1/documents/{id}` | Attachments |
| DELETE | `/api/v1/documents/{id}` | Attachments |
| GET | `/api/v1/documents/{id}/download` | Attachments |
| GET | `/api/v1/donations` | Donations |
| POST | `/api/v1/donations` | Donations |
| GET | `/api/v1/donations/{id}` | Donations |
| PUT | `/api/v1/donations/{id}` | Donations |
| DELETE | `/api/v1/donations/{id}` | Donations |
| GET | `/api/v1/donors` | Donors |
| POST | `/api/v1/donors` | Donors |
| GET | `/api/v1/donors/{id}` | Donors |
| PUT | `/api/v1/donors/{id}` | Donors |
| DELETE | `/api/v1/donors/{id}` | Donors |
| GET | `/api/v1/inventory/balances` | Inventory |
| GET | `/api/v1/inventory/lots` | Inventory |
| GET | `/api/v1/inventory/movements` | Inventory |
| GET | `/api/v1/inventory/reconciliation` | Inventory |
| GET | `/api/v1/materials/categories` | Material categories |
| POST | `/api/v1/materials/categories` | Material categories |
| GET | `/api/v1/materials/categories/{id}` | Material categories |
| PUT | `/api/v1/materials/categories/{id}` | Material categories |
| DELETE | `/api/v1/materials/categories/{id}` | Material categories |
| GET | `/api/v1/materials/subtypes` | Material subtypes |
| POST | `/api/v1/materials/subtypes` | Material subtypes |
| GET | `/api/v1/materials/subtypes/{id}` | Material subtypes |
| PUT | `/api/v1/materials/subtypes/{id}` | Material subtypes |
| DELETE | `/api/v1/materials/subtypes/{id}` | Material subtypes |
| GET | `/api/v1/materials/types` | Material types |
| POST | `/api/v1/materials/types` | Material types |
| GET | `/api/v1/materials/types/{id}` | Material types |
| PUT | `/api/v1/materials/types/{id}` | Material types |
| DELETE | `/api/v1/materials/types/{id}` | Material types |
| GET | `/api/v1/organizations` | Organizations |
| POST | `/api/v1/organizations` | Organizations |
| GET | `/api/v1/organizations/{id}` | Organizations |
| PUT | `/api/v1/organizations/{id}` | Organizations |
| DELETE | `/api/v1/organizations/{id}` | Organizations |
| GET | `/api/v1/organizations/{id}/members/{userId}` | Organizations |
| PUT | `/api/v1/organizations/{id}/members/{userId}` | Organizations |
| DELETE | `/api/v1/organizations/{id}/members/{userId}` | Organizations |
| GET | `/api/v1/organizations/{id}/membership` | Organizations |
| GET | `/api/v1/pressings` | Pressing |
| POST | `/api/v1/pressings` | Pressing |
| GET | `/api/v1/pressings/{id}` | Pressing |
| POST | `/api/v1/pressings/{id}/reverse` | Processing reversals |
| GET | `/api/v1/reports/summary` | Reports |
| GET | `/api/v1/reports/summary.csv` | Reports |
| GET | `/api/v1/sales` | Sales |
| POST | `/api/v1/sales` | Sales |
| GET | `/api/v1/sales/{id}` | Sales |
| PUT | `/api/v1/sales/{id}` | Sales |
| DELETE | `/api/v1/sales/{id}` | Sales |
| POST | `/api/v1/sales/{id}/post` | Sales |
| POST | `/api/v1/sales/{id}/reverse` | Sales |
| POST | `/api/v1/session/authenticate` | Sessions |
| POST | `/api/v1/session/password-recovery/confirm` | Password recovery |
| POST | `/api/v1/session/password-recovery/request` | Password recovery |
| GET | `/api/v1/sortings` | Sorting |
| POST | `/api/v1/sortings` | Sorting |
| GET | `/api/v1/sortings/{id}` | Sorting |
| POST | `/api/v1/sortings/{id}/reverse` | Processing reversals |
| GET | `/api/v1/team-members` | Team members |
| POST | `/api/v1/team-members` | Team members |
| GET | `/api/v1/team-members/{id}` | Team members |
| PUT | `/api/v1/team-members/{id}` | Team members |
| DELETE | `/api/v1/team-members/{id}` | Team members |
| GET | `/api/v1/users` | Accounts |
| POST | `/api/v1/users` | Accounts |
| GET | `/api/v1/users/me` | Accounts |
| PUT | `/api/v1/users/me` | Accounts |
| PUT | `/api/v1/users/me/password` | Accounts |
| GET | `/api/v1/users/{id}` | Accounts |
| PUT | `/api/v1/users/{id}` | Accounts |
| DELETE | `/api/v1/users/{id}` | Accounts |
| PUT | `/api/v1/users/{id}/password` | Accounts |
| GET | `/api/v1/vehicles` | Vehicles |
| POST | `/api/v1/vehicles` | Vehicles |
| PUT | `/api/v1/vehicles/batch` | Vehicles |
| POST | `/api/v1/vehicles/batch` | Vehicles |
| GET | `/api/v1/vehicles/{id}` | Vehicles |
| PUT | `/api/v1/vehicles/{id}` | Vehicles |
| DELETE | `/api/v1/vehicles/{id}` | Vehicles |
