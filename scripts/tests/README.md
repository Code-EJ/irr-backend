# Legacy manual HTTP scripts

- Documentation maintainer: [Enzo Ribas (@oEnzoRibas)](https://github.com/oEnzoRibas).

These historical scripts can still target old routes, fields, numeric IDs, ports and role assumptions. They are not the official specification or the acceptance gate for the refactored backend. Use Swagger at http://localhost:9191/swagger-ui/index.html and the root README for current login/testing instructions.

| Scripts | Historical purpose |
| --- | --- |
| 01-register.sh / 02-authenticate.sh | Registration and authentication |
| 03-authenticate-wrong-password.sh | Credential rejection |
| 04-register-invalid-email.sh | Input validation |
| 05-protected-no-auth.sh / 06-protected-invalid-bearer.sh | Bearer rejection |
| 07-protected-valid-bearer.sh | Authenticated request |
| 99-auth-flow-e2e.sh | Combined authentication flow |
| 10-vehicle-create.sh through 15-vehicle-delete-forbidden.sh | Legacy vehicle CRUD and role checks |
| 100-full-app-flow.sh | Legacy combined application flow |

Historical inputs include BASE_URL, EMAIL, PASSWORD, NAME, TOKEN, NON_ADMIN_TOKEN, PLACA, MODELO, ID and ATIVO. Names here are literal script interfaces. Set BASE_URL=http://localhost:9191 when trying a compatible script; internal defaults may still use port 8081. Compare each script with Swagger before executing writes. Missing NON_ADMIN_TOKEN can skip a forbidden-access check; skipping does not establish authorization correctness. Use Maven clean verify for isolated automated acceptance.
