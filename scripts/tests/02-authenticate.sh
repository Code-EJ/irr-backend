#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-}"
PASSWORD="${PASSWORD:-SenhaForte!123}"

if [[ -z "${EMAIL}" ]]; then
  echo "Defina EMAIL para autenticar."
  echo "Exemplo: EMAIL=seu@email.com ./curl/02-authenticate.sh"
  exit 1
fi

curl -i -X POST "${BASE_URL}/api/session/authenticate" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"

echo
