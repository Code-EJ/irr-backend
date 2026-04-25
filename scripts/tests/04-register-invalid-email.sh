#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
# Senha forte para garantir que a falha ocorra APENAS pelo e-mail
PASSWORD="${PASSWORD:-SenhaForte!123}"
NAME="${NAME:-Teste Email Invalido}"
INVALID_EMAIL="email-invalido"

echo "Tentando registrar usuário com e-mail INVÁLIDO: ${INVALID_EMAIL}..."

# Faz a requisição capturando o corpo e o status HTTP (usando a nova rota de usuários)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/users" \
  -H 'Content-Type: application/json' \
  -d "{\"nome\":\"${NAME}\",\"email\":\"${INVALID_EMAIL}\",\"senha\":\"${PASSWORD}\"}")

# Separa o corpo da resposta do status HTTP
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

# Verifica se a validação do DTO bloqueou a requisição (Esperando 400 Bad Request)
if [[ "$HTTP_STATUS" == "400" ]]; then
  echo "[OK] A validação do DTO (Bean Validation) funcionou! O e-mail foi recusado. (Status 400)"
  echo "Resposta da API com o erro mapeado: $HTTP_BODY"
else
  echo "[FALHA CRÍTICA DE VALIDAÇÃO] A API aceitou um e-mail malformado!"
  echo "Status esperado: 400, recebido: $HTTP_STATUS"
  echo "Resposta da API: $HTTP_BODY"
  exit 1
fi