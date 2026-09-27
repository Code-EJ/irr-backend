# Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '..')
try {
    if (-not (Test-Path -LiteralPath '.env')) { throw 'Copy .env.example to .env and set local credentials before startup.' }
    if ((Test-Path -LiteralPath '.local/keys/private.pem') -and (Test-Path -LiteralPath '.local/keys/public.pem')) {
        Write-Host 'Keeping existing development RSA keys.'
    } else {
        java scripts/GenerateDevelopmentKeys.java
        if ($LASTEXITCODE -ne 0) { throw 'Key preparation failed. Check Java 21 and incomplete key pairs.' }
    }
    docker compose --env-file .env config --quiet
    if ($LASTEXITCODE -ne 0) { throw 'Compose configuration is invalid.' }
    docker compose up --build -d --wait
    if ($LASTEXITCODE -ne 0) { throw 'Container startup failed. Inspect docker compose logs.' }
    Write-Host 'IRR is ready. Use API_PORT and login values from your private .env. See README for Swagger and tests.'
} finally { Pop-Location }
