#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
PLACA="${PLACA:-ABC1D23}"
MODELO="${MODELO:-Fiat Palio}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para criar veículo."
  exit 1
fi

echo "Criando veículo: $MODELO - $PLACA..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/veiculos" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\"}")

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "201" ]]; then
  echo "[OK] Veículo criado com sucesso! (Status 201)"
  VEHICLE_ID=$(echo "$HTTP_BODY" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("id", ""))' 2>/dev/null)
  echo "ID DO VEÍCULO CRIADO: $VEHICLE_ID"
else
  echo "[FALHA] Status esperado: 201, recebido: $HTTP_STATUS"
  echo "Erro: $HTTP_BODY"
  exit 1
fi