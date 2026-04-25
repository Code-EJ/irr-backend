#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-curl-register-$(date +%s)@mail.com}"
# 1. Correção: Inserindo uma senha forte como fallback para passar no Validator do Spring
PASSWORD="${PASSWORD:-SenhaForte!123}"
NAME="${NAME:-Curl Register}"

echo "Registrando novo usuário: $EMAIL..."

# 2. Faz a requisição capturando o corpo e o status HTTP na última linha
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/users" \
  -H 'Content-Type: application/json' \
  -d "{\"nome\":\"${NAME}\",\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}")

# 3. Separa o corpo da resposta do status HTTP
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

# 4. Verifica se o registro foi um sucesso (Criado = 201)
if [[ "$HTTP_STATUS" == "201" ]]; then
  echo "[OK] Usuário registrado com sucesso! (Status 201)"

  # Extrai o token gerado no registro
  TOKEN=$(echo "$HTTP_BODY" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token", ""))' 2>/dev/null || echo "$HTTP_BODY")

  echo "--------------------------------------------------"
  echo "CREDENCIAIS CRIADAS:"
  echo "E-mail: $EMAIL"
  echo "Senha:  $PASSWORD"
  echo "--------------------------------------------------"
  echo "SEU TOKEN DE ACESSO:"
  echo "$TOKEN"
  echo "--------------------------------------------------"
else
  echo "[FALHA] Erro no registro. Status esperado: 201, recebido: $HTTP_STATUS"
  echo "Resposta da API: $HTTP_BODY"
  exit 1
fi