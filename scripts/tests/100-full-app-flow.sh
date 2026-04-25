##!/usr/bin/env bash
#set -euo pipefail
#
#BASE_URL="${BASE_URL:-http://localhost:8081}"
#EMAIL="${EMAIL:-full-flow-$(date +%s)@mail.com}"
## Correção 1: Adicionado caractere especial na senha para passar no Bean Validation
#PASSWORD="${PASSWORD:-SenhaForte!123}"
#NAME="${NAME:-Full Flow}"
#PLACA="${PLACA:-ABC1D23}"
#MODELO="${MODELO:-Teste Integrado}"
#NON_ADMIN_TOKEN="${NON_ADMIN_TOKEN:-}"
#
#TMP_BODY=$(mktemp)
#trap 'rm -f "$TMP_BODY"' EXIT
#
#request() {
#  local method="$1"
#  local url="$2"
#  local auth="${3:-}"
#  local data="${4:-}"
#
#  local args=( -s -o "$TMP_BODY" -w "%{http_code}" -X "$method" "$url")
#
#  if [[ -n "$auth" ]]; then
#    args+=(-H "Authorization: Bearer $auth")
#  fi
#
#  if [[ -n "$data" ]]; then
#    args+=(-H 'Content-Type: application/json' -d "$data")
#  fi
#
#  curl "${args[@]}"
#}
#
#assert_status() {
#  local got="$1"
#  local expected="$2"
#  local step="$3"
#
#  if [[ "$got" != "$expected" ]]; then
#    echo "[FAIL] $step -> esperado $expected, recebido $got"
#    echo "BODY: $(cat "$TMP_BODY")"
#    exit 1
#  fi
#
#  echo "[OK] $step -> $got"
#}
#
## Helper seguro para extrair JSON com Python
#extract_json() {
#  local key="$1"
#  python3 -c "import sys,json; data=json.load(sys.stdin); print(str(data.get('$key', '')).lower() if isinstance(data.get('$key'), bool) else data.get('$key', ''))" < "$TMP_BODY" 2>/dev/null || echo ""
#}
#
#echo "====================================================="
#echo "🚀 INICIANDO TESTE E2E (END-TO-END) DA API"
#echo "====================================================="
#
##echo "== 1) Proteção sem Authorization =="
##code=$(request GET "${BASE_URL}/api/motoristas/ok")
##assert_status "$code" "401" "protected without auth"
##
##echo "== 2) Proteção com Bearer inválido =="
##code=$(request GET "${BASE_URL}/api/motoristas/ok" "token-invalido")
##assert_status "$code" "401" "protected with invalid bearer"
#
#echo "== 3) Register usuário =="
#register_data="{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"
## Correção 2: Rota atualizada para o padrão SRP
#code=$(request POST "${BASE_URL}/api/v1/users" "" "$register_data")
#assert_status "$code" "201" "register"
#
#TOKEN=$(extract_json "token")
#
#echo "== 4) Authenticate usuário =="
#auth_data="{\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}"
## Correção 2: Rota atualizada para o padrão SRP
#code=$(request POST "${BASE_URL}/api/v1/auth/login" "" "$auth_data")
#assert_status "$code" "200" "authenticate"
#
#TOKEN=$(extract_json "token")
#
#echo "== 5) Proteção com token válido =="
#code=$(request GET "${BASE_URL}/api/motoristas/ok" "$TOKEN")
#assert_status "$code" "200" "protected with valid token"
#
#echo "== 6) Criar veículo =="
#vehicle_create="{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO}\"}"
#code=$(request POST "${BASE_URL}/api/v1/veiculos" "$TOKEN" "$vehicle_create")
#assert_status "$code" "201" "vehicle create"
#
#VEHICLE_ID=$(extract_json "id")
#
#echo "== 7) Criar veículo duplicado (espera 409) =="
#code=$(request POST "${BASE_URL}/api/veiculos" "$TOKEN" "$vehicle_create")
#assert_status "$code" "409" "vehicle duplicate plate"
#
#echo "== 8) Listar veículos =="
#code=$(request GET "${BASE_URL}/api/v1/veiculos" "$TOKEN")
#assert_status "$code" "200" "vehicle list"
#
#echo "== 9) Buscar veículo por id =="
#code=$(request GET "${BASE_URL}/api/v1/veiculos/${VEHICLE_ID}" "$TOKEN")
#assert_status "$code" "200" "vehicle get by id"
#
#echo "== 10) Atualizar veículo =="
#vehicle_update="{\"placa\":\"${PLACA}\",\"modelo\":\"${MODELO} Atualizado\",\"ativo\":true}"
#code=$(request PUT "${BASE_URL}/api/veiculos/${VEHICLE_ID}" "$TOKEN" "$vehicle_update")
#assert_status "$code" "200" "vehicle update"
#
#if [[ -n "$NON_ADMIN_TOKEN" ]]; then
#  echo "== 11) Desativar veículo com não-admin (espera 403) =="
#  code=$(request DELETE "${BASE_URL}/api/v1/veiculos/${VEHICLE_ID}" "$NON_ADMIN_TOKEN")
#  assert_status "$code" "403" "vehicle delete forbidden for non-admin"
#else
#  echo "== 11) Desativar veículo com não-admin =="
#  echo "[SKIP] Defina NON_ADMIN_TOKEN para validar 403 de autorização"
#fi
#
#echo "== 12) Desativar veículo com admin =="
#code=$(request DELETE "${BASE_URL}/api/v1/veiculos/${VEHICLE_ID}" "$TOKEN")
## Nota: Assumindo que o seu controller retorna 204 No Content para deleção lógica
#assert_status "$code" "204" "vehicle soft delete"
#
#echo "== 13) Confirmar veículo desativado =="
#code=$(request GET "${BASE_URL}/api/v1/veiculos/${VEHICLE_ID}" "$TOKEN")
#assert_status "$code" "200" "vehicle fetch after delete"
#
#ATIVO=$(extract_json "ativo")
#if [[ "$ATIVO" != "false" ]]; then
#  echo "[FAIL] vehicle ativo esperado false, recebido $ATIVO"
#  exit 1
#fi
#
#echo "[OK] vehicle ativo=false após DELETE lógico"
#
#echo
#echo "✅ FLUXO COMPLETO VALIDADO COM SUCESSO!"
#echo "EMAIL GERADO: $EMAIL"
#echo "VEHICLE_ID:   $VEHICLE_ID"
#echo "====================================================="