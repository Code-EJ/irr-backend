#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
ID="${ID:-1}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para desativar veículo."
  echo "Exemplo: TOKEN=<jwt> ID=1 ./scripts/tests/14-vehicle-delete.sh"
  exit 1
fi

curl -s -i -X DELETE "${BASE_URL}/api/veiculo/${ID}" \
  -H "Authorization: Bearer ${TOKEN}"

echo
