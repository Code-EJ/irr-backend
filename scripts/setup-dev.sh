#!/usr/bin/env bash
# Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
set -euo pipefail
cd "$(dirname "$0")/.."
if [ ! -f .env ]; then
    echo 'Copy .env.example to .env and set local credentials before startup.' >&2
    exit 1
fi
if [ -f .local/keys/private.pem ] && [ -f .local/keys/public.pem ]; then
    echo 'Keeping existing development RSA keys.'
else
    java scripts/GenerateDevelopmentKeys.java
fi
docker compose --env-file .env config --quiet
docker compose up --build -d --wait
echo 'IRR is ready. Use API_PORT and login values from your private .env. See README for Swagger and tests.'
