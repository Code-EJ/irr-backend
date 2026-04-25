#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
ID="${ID:-7}"
PLACA="${PLACA:-XYZ9E87}"
MODELO="${MODELO:-Fiat Fiorino}"
ATIVO="${ATIVO:-true}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para atualizar veículo."
  exit 1
fi

echo "Atualizando veículo ID $ID para placa $PLACA..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X PUT "${BASE_URL}/api/v1/veiculos/${ID}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\",\"ativo\":${ATIVO}}")

HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "200" ]]; then
  echo "[OK] Veículo atualizado! (Status 200)"
  echo "Resposta:"
  echo $HTTP_BODY
else
  echo "[FALHA] Status esperado: 200, recebido: $HTTP_STATUS"
  echo "Erro: $HTTP_BODY"
  exit 1
fi