#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
ID="${ID:-2}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para buscar veículo por id."
  echo "Exemplo: TOKEN=<jwt> ID=1 ./scripts/tests/12-vehicle-get-by-id.sh"
  exit 1
fi

curl -i -X GET "${BASE_URL}/api/veiculos/${ID}" \
  -H "Authorization: Bearer ${TOKEN}"

echo
