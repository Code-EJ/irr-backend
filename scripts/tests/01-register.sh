#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-curl-register-$(date +%s)@mail.com}"
PASSWORD="${PASSWORD:-SenhaForte!123}"
NAME="${NAME:-Curl Register}"

echo "Requesting register for: $EMAIL"

curl -i -X POST "${BASE_URL}/api/v1/users" \
  -H 'Content-Type: application/json' \
  -d "{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"

echo