#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-full-flow-$(date +%s)@mail.com}"
PASSWORD="${PASSWORD:-SenhaForte123}"
NAME="${NAME:-Full Flow}"
PLACA="${PLACA:-ZZZ9Z99}"
MODELO="${MODELO:-Teste Integrado}"
NON_ADMIN_TOKEN="${NON_ADMIN_TOKEN:-}"
TMP_BODY=$(mktemp)
trap 'rm -f "$TMP_BODY"' EXIT

request() {
  local method="$1"
  local url="$2"
  local auth="${3:-}"
  local data="${4:-}"

  local args=(-s -o "$TMP_BODY" -w "%{http_code}" -X "$method" "$url")

  if [[ -n "$auth" ]]; then
    args+=(-H "Authorization: Bearer $auth")
  fi

  if [[ -n "$data" ]]; then
    args+=(-H 'Content-Type: application/json' -d "$data")
  fi

  curl "${args[@]}"
}

assert_status() {
  local got="$1"
  local expected="$2"
  local step="$3"

  if [[ "$got" != "$expected" ]]; then
    echo "[FAIL] $step -> esperado $expected, recebido $got"
    echo "BODY: $(cat "$TMP_BODY")"
    exit 1
  fi

  echo "[OK] $step -> $got"
}

echo "== 1) Proteção sem Authorization =="
code=$(request GET "${BASE_URL}/api/motorista/ok")
assert_status "$code" "401" "protected without auth"

echo "== 2) Proteção com Bearer inválido =="
code=$(request GET "${BASE_URL}/api/motorista/ok" "token-invalido")
assert_status "$code" "401" "protected with invalid bearer"

echo "== 3) Register usuário =="
register_data="{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"
code=$(request POST "${BASE_URL}/api/session/register" "" "$register_data")
assert_status "$code" "201" "register"

TOKEN=$(python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])' < "$TMP_BODY")

echo "== 4) Authenticate usuário =="
auth_data="{\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"
code=$(request POST "${BASE_URL}/api/session/authenticate" "" "$auth_data")
assert_status "$code" "200" "authenticate"

TOKEN=$(python3 -c 'import sys,json; print(json.load(sys.stdin)["token"])' < "$TMP_BODY")

echo "== 5) Proteção com token válido =="
code=$(request GET "${BASE_URL}/api/motorista/ok" "$TOKEN")
assert_status "$code" "200" "protected with valid token"

echo "== 6) Criar veículo =="
vehicle_create="{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\"}"
code=$(request POST "${BASE_URL}/api/veiculo" "$TOKEN" "$vehicle_create")
assert_status "$code" "201" "vehicle create"

VEHICLE_ID=$(python3 -c 'import sys,json; print(json.load(sys.stdin)["id"])' < "$TMP_BODY")

echo "== 7) Criar veículo duplicado (espera 409) =="
code=$(request POST "${BASE_URL}/api/veiculo" "$TOKEN" "$vehicle_create")
assert_status "$code" "409" "vehicle duplicate plate"

echo "== 8) Listar veículos =="
code=$(request GET "${BASE_URL}/api/veiculo" "$TOKEN")
assert_status "$code" "200" "vehicle list"

echo "== 9) Buscar veículo por id =="
code=$(request GET "${BASE_URL}/api/veiculo/${VEHICLE_ID}" "$TOKEN")
assert_status "$code" "200" "vehicle get by id"

echo "== 10) Atualizar veículo =="
vehicle_update="{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO} Atualizado\",\"ativo\":true}"
code=$(request PUT "${BASE_URL}/api/veiculo/${VEHICLE_ID}" "$TOKEN" "$vehicle_update")
assert_status "$code" "200" "vehicle update"

if [[ -n "$NON_ADMIN_TOKEN" ]]; then
  echo "== 11) Desativar veículo com não-admin (espera 403) =="
  code=$(request DELETE "${BASE_URL}/api/veiculo/${VEHICLE_ID}" "$NON_ADMIN_TOKEN")
  assert_status "$code" "403" "vehicle delete forbidden for non-admin"
else
  echo "== 11) Desativar veículo com não-admin =="
  echo "[SKIP] Defina NON_ADMIN_TOKEN para validar 403 de autorização"
fi

echo "== 12) Desativar veículo com admin =="
code=$(request DELETE "${BASE_URL}/api/veiculo/${VEHICLE_ID}" "$TOKEN")
assert_status "$code" "204" "vehicle soft delete"

echo "== 13) Confirmar veículo desativado =="
code=$(request GET "${BASE_URL}/api/veiculo/${VEHICLE_ID}" "$TOKEN")
assert_status "$code" "200" "vehicle fetch after delete"

ATIVO=$(python3 -c 'import sys,json; print(str(json.load(sys.stdin)["ativo"]).lower())' < "$TMP_BODY")
if [[ "$ATIVO" != "false" ]]; then
  echo "[FAIL] vehicle ativo esperado false, recebido $ATIVO"
  exit 1
fi

echo "[OK] vehicle ativo=false após DELETE lógico"

echo

echo "Fluxo completo validado com sucesso."
echo "EMAIL: $EMAIL"
echo "VEHICLE_ID: $VEHICLE_ID"
