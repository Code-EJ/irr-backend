#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-}"
ID="${ID:-1}"
PLACA="${PLACA:-ABC1D23}"
MODELO="${MODELO:-Fiorino Atualizada}"
ATIVO="${ATIVO:-true}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para atualizar veículo."
  echo "Exemplo: TOKEN=<jwt> ID=1 ./scripts/tests/13-vehicle-update.sh"
  exit 1
fi

curl -s -i -X PUT "${BASE_URL}/api/veiculo/${ID}" \
  -H 'Content-Type: application/json' \
  -H "Authorization: Bearer ${TOKEN}" \
  -d "{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\",\"ativo\":${ATIVO}}"

echo
