#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para listar veículos."
  exit 1
fi

echo "Listando frota..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X GET "${BASE_URL}/api/v1/veiculos" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "200" ]]; then
  echo "[OK] Listagem efetuada com sucesso! (Status 200)"
  echo "Resposta:"
  echo "$HTTP_BODY"
else
  echo "[FALHA] Status esperado: 200, recebido: $HTTP_STATUS"
  exit 1
fi