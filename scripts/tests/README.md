# Curl auth flow

Arquivos de teste para o fluxo de autenticação da API.

Base URL padrão: `http://localhost:8081`

## Arquivos

- `01-register.sh`: registra usuário e retorna token
- `02-authenticate.sh`: autentica usuário existente e retorna token
- `03-authenticate-wrong-password.sh`: autentica com senha errada (esperado: 401)
- `04-register-invalid-email.sh`: registra com email inválido (esperado: 400)
- `05-protected-no-auth.sh`: acessa rota protegida sem header Authorization (esperado: 401)
- `06-protected-invalid-bearer.sh`: acessa rota protegida com header Bearer inválido (esperado: 401)
- `07-protected-valid-bearer.sh`: acessa rota protegida com token válido (esperado: 200)
- `99-auth-flow-e2e.sh`: fluxo completo (register -> authenticate -> protected)
- `10-vehicle-create.sh`: cria veículo (esperado: 201)
- `11-vehicle-list.sh`: lista veículos (esperado: 200)
- `12-vehicle-get-by-id.sh`: busca veículo por id (esperado: 200)
- `13-vehicle-update.sh`: atualiza veículo (esperado: 200)
- `14-vehicle-delete.sh`: remove logicamente veículo (esperado: 204)
- `15-vehicle-delete-forbidden.sh`: valida bloqueio de delete para não-admin (esperado: 403)
- `100-full-app-flow.sh`: valida fluxo completo da aplicação (auth + proteção + vehicle CRUD)

## Uso rápido

Defina variáveis quando necessário:

- `BASE_URL` (opcional)
- `EMAIL` / `PASSWORD` / `NAME` (quando o script aceitar)
- `TOKEN` (para `07-protected-valid-bearer.sh`)
- `NON_ADMIN_TOKEN` (para validar bloqueio de autorização no delete de vehicle)
- `PLACA` / `MODELO` / `ID` / `ATIVO` (scripts de vehicle)

Exemplo:

- `BASE_URL=http://localhost:8081 ./scripts/tests/01-register.sh`
- `TOKEN=<jwt> ./scripts/tests/07-protected-valid-bearer.sh`
- `./scripts/tests/99-auth-flow-e2e.sh`
- `TOKEN=<jwt> ./scripts/tests/10-vehicle-create.sh`
- `NON_ADMIN_TOKEN=<jwt-nao-admin> ID=1 ./scripts/tests/15-vehicle-delete-forbidden.sh`
- `./scripts/tests/100-full-app-flow.sh`

### Observação sobre `100-full-app-flow.sh`

- Sem `NON_ADMIN_TOKEN`, o script executa todo o fluxo e **pula** a validação de `403` para não-admin.
- Com `NON_ADMIN_TOKEN`, o script também valida o cenário de autorização no `DELETE /api/veiculo/{id}`.
