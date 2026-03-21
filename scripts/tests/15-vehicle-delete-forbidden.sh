#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
NON_ADMIN_TOKEN="${NON_ADMIN_TOKEN:-}"
ID="${ID:-1}"

if [[ -z "${NON_ADMIN_TOKEN}" ]]; then
  echo "Defina NON_ADMIN_TOKEN para validar acesso negado no delete de veículo."
  echo "Exemplo: NON_ADMIN_TOKEN=<jwt-nao-admin> ID=1 ./scripts/tests/15-vehicle-delete-forbidden.sh"
  exit 1
fi

STATUS=$(curl -s -o /tmp/vehicle_delete_forbidden_body.json -w "%{http_code}" \
  -X DELETE "${BASE_URL}/api/veiculo/${ID}" \
  -H "Authorization: Bearer ${NON_ADMIN_TOKEN}")

cat /tmp/vehicle_delete_forbidden_body.json
rm -f /tmp/vehicle_delete_forbidden_body.json

echo

echo "HTTP_STATUS=${STATUS}"

if [[ "${STATUS}" != "403" ]]; then
  echo "[FAIL] esperado 403, recebido ${STATUS}"
  exit 1
fi

echo "[OK] acesso negado para não-admin validado"
