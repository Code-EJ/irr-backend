#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-}"
# Senha visivelmente incorreta
WRONG_PASSWORD="${WRONG_PASSWORD:-SENHA123!}"

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL para testar a falha de autenticação."
  echo "Exemplo: EMAIL=enzo@codejr.com ./03-authenticate-wrong-password.sh"
  exit 1
fi

echo "Tentando autenticar usuário: ${EMAIL} com senha INCORRETA..."

# Faz a requisição e captura o status HTTP
# Rota atualizada para o padrão da Clean Architecture
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${WRONG_PASSWORD}\"}")

# Extrai o corpo e o status
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

# Verifica se a API bloqueou corretamente (Esperando 401 Unauthorized)
if [[ "$HTTP_STATUS" == "401" || "$HTTP_STATUS" == "403" ]]; then
  echo "[OK] O sistema bloqueou a entrada com sucesso! (Status recebido: $HTTP_STATUS)"
  echo "Resposta da API de erro: $HTTP_BODY"
else
  echo "[FALHA CRÍTICA DE SEGURANÇA] A API não barrou a senha incorreta!"
  echo "Status esperado: 401, recebido: $HTTP_STATUS"
  echo "Resposta da API: $HTTP_BODY"
  exit 1
fi