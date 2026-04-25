#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TARGET_URL="${BASE_URL}/api/motoristas/ok"

echo "Testando rota protegida com Bearer INVÁLIDO..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${TARGET_URL}" \
  -H 'Authorization: Bearer token-invalido-qualquer')

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "401" ]]; then
  echo "[OK] Acesso bloqueado para token corrompido! (Status 401)"
else
  echo "[FALHA] Status esperado: 401, recebido: $HTTP_STATUS"
  exit 1
fi