#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
# Rota protegida de teste (ajuste para /api/v1/drivers se tiver aplicado o SRP aqui também)
TARGET_URL="${BASE_URL}/api/motoristas/ok"

echo "Testando acesso NEGADO em rota protegida: ${TARGET_URL}..."

# Faz a requisição SEM passar nenhum token
RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${TARGET_URL}")

# Extrai o corpo e o status HTTP
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

# Verifica se o Spring Security bloqueou a requisição na porta (Esperando 401)
if [[ "$HTTP_STATUS" == "401" ]]; then
  echo "[OK] Segurança ativada! O acesso sem token foi devidamente bloqueado. (Status 401)"
  echo "Resposta da API: $HTTP_BODY"
else
  echo "[FALHA CRÍTICA DE SEGURANÇA] A API permitiu acesso ou retornou status inesperado!"
  echo "Status esperado: 401, recebido: $HTTP_STATUS"
  echo "Resposta da API: $HTTP_BODY"
  exit 1
fi