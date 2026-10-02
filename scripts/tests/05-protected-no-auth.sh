#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"

curl -i -X GET "${BASE_URL}/api/motoristas/ok"

echo
