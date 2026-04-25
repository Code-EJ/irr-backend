##!/usr/bin/env bash
#set -euo pipefail
#
#BASE_URL="${BASE_URL:-http://localhost:8081}"
#EMAIL="${EMAIL:-curl-e2e-$(date +%s)@mail.com}"
#PASSWORD="${PASSWORD:-SenhaForte123}"
#NAME="${NAME:-Curl E2E}"
#
#echo "=== REGISTER ==="
#REGISTER_RESPONSE=$(curl -X POST "${BASE_URL}/api/session/register" \
#  -H 'Content-Type: application/json' \
#  -d "{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}")
#
#echo "${REGISTER_RESPONSE}"
#
#echo "=== AUTHENTICATE ==="
#AUTH_RESPONSE=$(curl -X POST "${BASE_URL}/api/session/authenticate" \
#  -H 'Content-Type: application/json' \
#  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}")
#
#echo "${AUTH_RESPONSE}"
#
#TOKEN=$(python3 -c 'import sys,json; print(json.loads(sys.stdin.read())["token"])' <<< "${AUTH_RESPONSE}")
#
#echo "=== PROTECTED /api/motoristas/ok ==="
#curl -s -i -X GET "${BASE_URL}/api/motoristas/ok" \
#  -H "Authorization: Bearer ${TOKEN}"
#
#echo
#
#echo "EMAIL usado: ${EMAIL}"
