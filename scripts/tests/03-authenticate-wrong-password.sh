#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-}"
WRONG_PASSWORD="${WRONG_PASSWORD:-senha-errada}"

if [[ -z "${EMAIL}" ]]; then
  echo "Defina EMAIL para autenticar com senha inválida."
  echo "Exemplo: EMAIL=seu@email.com ./curl/03-authenticate-wrong-password.sh"
  exit 1
fi

curl -i -X POST "${BASE_URL}/api/session/authenticate" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${WRONG_PASSWORD}\"}"

echo
