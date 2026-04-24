#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para acessar rota protegida."
  echo "Exemplo: TOKEN=<jwt> ./curl/07-protected-valid-bearer.sh"
  exit 1
fi

curl -i -X GET "${BASE_URL}/api/motorista/ok" \
  -H "Authorization: Bearer ${TOKEN}"

echo
