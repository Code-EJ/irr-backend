#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
PLACA="${PLACA:-ABC1D23}"
MODELO="${MODELO:-Fiorino}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para criar veículo."
  echo "Exemplo: TOKEN=<jwt> ./scripts/tests/10-vehicle-create.sh"
  exit 1
fi

curl -s -i -X POST "${BASE_URL}/api/veiculo" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\"}"

echo
