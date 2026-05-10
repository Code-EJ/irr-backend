$ErrorActionPreference = "Stop"

# Navega para a raiz do projeto para garantir que os caminhos relativos funcionem
Set-Location -Path "$PSScriptRoot\..\.."

# Define a senha temporariamente na sessão do PowerShell para não ser pedida no prompt interactivo
$env:PGPASSWORD = "root"

Write-Host "Iniciando a inserção de dados no PostgreSQL..."

# Executa todos os scripts usando psql contra o banco local
psql -h localhost -p 5435 -U irr_admin -d irr -f "scripts/getReadyDevelop/00-admin-user.sql"
psql -h localhost -p 5435 -U irr_admin -d irr -f "scripts/getReadyDevelop/01-mock-materials.sql"
psql -h localhost -p 5435 -U irr_admin -d irr -f "scripts/getReadyDevelop/02-mock-donors-vehicles.sql"

Write-Host "Banco de Dados preenchido com sucesso!"

Write-Host "========================================="
Write-Host "✅ Populamento Concluído!"
Write-Host "-----------------------------------------"
Write-Host "🧑‍💻 Credenciais do Usuário Administrador:"
Write-Host "    E-mail: admin@irr.com"
Write-Host "    Senha:  123456"
Write-Host "========================================="
