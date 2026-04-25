#!/usr/bin/env bash
set -euo pipefail

BASE_URL="${BASE_URL:-http://localhost:8081}"
TOKEN="${TOKEN:-eyJhbGciOiJSUzI1NiJ9.eyJzdWIiOiJlbWFpbEBlLmNvbSIsInRpcG8iOiJSRVBSRVNFTlRBTlRFIiwiaXNzIjoic2VsZiIsImlkIjoiYjM3YWU0MGItY2MwMS00NjkyLTk1YjUtNzQzZDhmNjA0MWEyIiwiZXhwIjoxNzc3NDExMzk0LCJpYXQiOjE3NzcxNTIxOTQsImVtYWlsIjoiZW1haWxAZS5jb20ifQ.i_Wu1C_vrrCJk9jQONaGpIwkzhlatKmwngxyEOEo7Up1pMADwiKo4FrD3mcNc2KY9WFZO_vzubqcy6nLDQWoHLJdk8gEHwbU198cbFvoBheTzIRlf1cPp5A1vXw_hD_uAjfJ6jvye5k79jlI80sPs2qqWvZUulIzYvxI13zxplWS0KgpX5MK_6n6apBmywmLKzi_P_Kh5r6ne1zfgftNGYCNJ8u0aIYndwb9qLsqOAuhWW21VI15iuYCwzxAwkY4DPN6MUAyXwb2Af4K0JZm-Jzuy5wJpH949OL4ZT4SUp0s2yOuqPhexhbKhbD8kGleRaYZ7dar9CbPujT1Y4wM_A}"
ID="${ID:-7}"

if [[ -z "${TOKEN}" ]]; then
  echo "Defina TOKEN para desativar veículo."
  exit 1
fi

echo "Desativando veículo ID: $ID..."

RESPONSE=$(curl -s -w "\n%{http_code}" -X DELETE "${BASE_URL}/api/v1/veiculos/${ID}" \
  -H "Authorization: Bearer ${TOKEN}")

HTTP_STATUS=$(echo "$RESPONSE" | tail -n 1)

if [[ "$HTTP_STATUS" == "204" || "$HTTP_STATUS" == "200" ]]; then
  echo "[OK] Veículo desativado com sucesso! (Status $HTTP_STATUS)"
else
  echo "[FALHA] Falha na deleção. Recebido: $HTTP_STATUS"
  exit 1
fi