# Compatibility entrypoint. Maintainer: Enzo Ribas (https://github.com/oEnzoRibas).
# Creates retained workflow fixtures through the authorized API, not direct SQL.
$ErrorActionPreference = 'Stop'
Push-Location (Join-Path $PSScriptRoot '../..')
try {
    node scripts/smoke-development.mjs
    if ($LASTEXITCODE -ne 0) { throw 'Development fixture failed. Check Docker readiness and private .env values.' }
} finally { Pop-Location }
