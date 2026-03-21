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

## Uso rápido

Defina variáveis quando necessário:

- `BASE_URL` (opcional)
- `EMAIL` / `PASSWORD` / `NAME` (quando o script aceitar)
- `TOKEN` (para `07-protected-valid-bearer.sh`)

Exemplo:

- `BASE_URL=http://localhost:8081 ./curl/01-register.sh`
- `TOKEN=<jwt> ./curl/07-protected-valid-bearer.sh`
- `./curl/99-auth-flow-e2e.sh`
