#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
ID="${ID:-2}"
PLACA="${PLACA:-000000}"
MODELO="${MODELO:-Fiorino}"
ATIVO="${ATIVO:-true}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para atualizar veículo."
  echo "Exemplo: TOKEN=<jwt> ID=1 ./scripts/tests/13-vehicle-update.sh"
  exit 1
fi

curl -i -X PUT "${BASE_URL}/api/veiculos/${ID}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\",\"ativo\":${ATIVO}}"

echo
