#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
PLACA="${PLACA:-ZYXW1}"
MODELO="${MODELO:-PALIO1}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para criar veículo."
  echo "Exemplo: TOKEN=<jwt> ./scripts/tests/10-vehicle-create.sh"
  exit 1
fi

curl -i -X POST "${BASE_URL}/api/veiculos" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\"}"

echo
