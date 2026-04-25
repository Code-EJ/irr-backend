#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
TARGET_URL="${BASE_URL}/api/v1/veiculos/health"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para acessar rota protegida."
  exit 1
fi

echo "Acessando rota com token válido..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${TARGET_URL}" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "200" ]]; then
  echo "[OK] Acesso liberado! (Status 200)"
else
  echo "[FALHA] Status esperado: 200, recebido: $HTTP_STATUS"
  echo "Corpo: $HTTP_BODY"
  exit 1
fi