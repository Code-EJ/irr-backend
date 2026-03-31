#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"

curl -s -i -X GET "${BASE_URL}/api/motorista/ok" \
  -H 'Authorization: Bearer token-invalido'

echo
