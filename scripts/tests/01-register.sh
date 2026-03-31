#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-curl-register-$(date +%s)@mail.com}"
PASSWORD="${PASSWORD:-SenhaForte123}"
NAME="${NAME:-Curl Register}"

curl -s -i -X POST "${BASE_URL}/api/session/register" \
  -H 'Content-Type: application/json' \
  -d "{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"

echo
