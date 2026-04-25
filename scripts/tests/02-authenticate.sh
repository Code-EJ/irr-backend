#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
EMAIL="${EMAIL:-}"
PASSWORD="${PASSWORD:-SenhaForte!123}"

if [[ -z "${EMAIL}" ]]; then
  echo "[ERRO] Defina EMAIL para autenticar."
  echo "Exemplo: EMAIL=enzo@codejr.com ./02-authenticate.sh"
  exit 1
fi

echo "Autenticando usuário: ${EMAIL}..."

# Faz a requisição, silencia a barra de progresso (-s) e captura o status code no final (-w)
# Atenção: Atualizado para a nova rota do AuthController (/api/v1/auth/login)
RESPONSE=$(curl -s -w "\n%{http_code}" -X POST "${BASE_URL}/api/v1/auth/login" \
  -H 'Content-Type: application/json' \
  -d "{\"email\":\"${EMAIL}\",\"senha\":\"${PASSWORD}\"}")

# Separa o corpo da resposta do status HTTP
HTTP_BODY=$(echo "$RESPONSE" | sed '$d')
HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "200" ]]; then
  echo "[OK] Autenticado com sucesso! (Status 200)"

  # Extrai o token usando Python embutido (assim você não precisa instalar bibliotecas extras no Git Bash)
  TOKEN=$(echo "$HTTP_BODY" | python3 -c 'import sys,json; print(json.load(sys.stdin).get("token", ""))' 2>/dev/null || echo "$HTTP_BODY")

  echo "--------------------------------------------------"
  echo "SEU TOKEN (Copie a linha abaixo):"
  echo "$TOKEN"
  echo "--------------------------------------------------"
else
  echo "[FALHA] Erro na autenticação. Status esperado: 200, recebido: $HTTP_STATUS"
  echo "Resposta da API: $HTTP_BODY"
  exit 1
fi