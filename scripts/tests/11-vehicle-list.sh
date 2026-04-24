#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para listar veículos."
  echo "Exemplo: TOKEN=<jwt> ./scripts/tests/11-vehicle-list.sh"
  exit 1
fi

curl -i -X GET "${BASE_URL}/api/veiculos" \
  -H "Authorization: Bearer ${TOKEN}"

echo
