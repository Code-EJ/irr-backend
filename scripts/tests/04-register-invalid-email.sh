#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"

curl -s -i -X POST "${BASE_URL}/api/session/register" \
  -H 'Content-Type: application/json' \
  -d '{"nome":"Teste","email":"email-invalido","senha":"SenhaForte123"}'

echo
