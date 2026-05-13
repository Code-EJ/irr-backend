$ErrorActionPreference = "Stop"

# Navega para o diretório raiz do projeto para garantir que os caminhos relativos funcionem
Set-Location -Path "$PSScriptRoot\.."

Write-Host "========================================="
Write-Host "⚙️  Configuração do Ambiente de Dev - IRR "
Write-Host "========================================="

# 1. Gerar Chaves RSA (OpenSSL)
Write-Host "[1/6] Verificando chaves RSA..."
$resourcesPath = "src\main\resources"
if (-not (Test-Path -Path $resourcesPath)) {
    New-Item -ItemType Directory -Force -Path $resourcesPath | Out-Null
}

if (-not (Test-Path -Path "$resourcesPath\private.key") -or -not (Test-Path -Path "$resourcesPath\public.key")) {
    Write-Host "Gerando novas chaves RSA..."
    # Necessita do OpenSSL instalado e presente no PATH do Windows
    openssl genrsa -out "$resourcesPath\private.key" 2048
    openssl rsa -in "$resourcesPath\private.key" -pubout -out "$resourcesPath\public.key"
    Write-Host "Chaves geradas com sucesso!"
} else {
    Write-Host "Chaves RSA já existem. Pulando..."
}

# 2. Subir Containers Docker (banco + backend)
Write-Host "[2/6] Verificando banco de dados (Docker)..."
$dockerStatus = docker compose ps
if ($dockerStatus -match "Up") {
    Write-Host "Container do banco de dados já está rodando. Pulando inicialização."
} else {
    Write-Host "Subindo banco de dados (Docker)..."
    docker compose up -d
    Write-Host "Aguardando o PostgreSQL ficar pronto..."
    Start-Sleep -Seconds 5
}

# Credenciais do banco (alinhadas com docker-compose.yml)
$DB_USER = "irr_admin"
$DB_PASS = "root"
$DB_NAME = "irr"
$env:PGPASSWORD = $DB_PASS

# 3. Instalar Dependências via container Docker
Write-Host "[3/6] Instalando dependências (Maven)..."
docker compose run --rm backend ./mvnw clean install -DskipTests

# 4 e 5. Inserir Admin e popular o banco de dados.
Write-Host "[4/6] Verificando se o banco já está populado..."

# Cria o arquivo SQL do admin (hash correto para a senha '123456')
$adminSqlPath = "scripts\getReadyDevelop\00-admin-user.sql"
$adminSqlContent = @'
-- Insere um administrador padrao com a senha '123456' ($2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq', 'Administrador Root', 'ADMINISTRATOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
'@
Set-Content -Path $adminSqlPath -Value $adminSqlContent -Encoding UTF8

# Checa via COUNT(*) se o admin já existe (robusto: não falha se a tabela não existir ainda)
$SKIP_MOCK = $false
try {
    $userCount = psql -h localhost -p 5435 -U $DB_USER -d $DB_NAME -t -A -c "SELECT COUNT(*) FROM users WHERE email = 'admin@irr.com';" 2>$null
    if ([int]$userCount -gt 0) {
        Write-Host "Banco de dados já configurado e populado. Pulando preparação de mock."
        $SKIP_MOCK = $true
    }
} catch {
    $SKIP_MOCK = $false
}

if (-not $SKIP_MOCK) {
    Write-Host "Preparando dados Fictícios (Mock)..."
    Write-Host "Criando as tabelas necessárias com base nos arquivos de migração (V1 e V2)..."

    Write-Host "Inicializando o projeto Java via container para o Flyway versionar as tabelas..."
    docker rm -f irr_flyway_init 2>$null
    docker compose run --rm -d --name irr_flyway_init backend ./mvnw spring-boot:run "-Dspring-boot.run.jvmArguments=-Dspring.main.web-application-type=none"

    Write-Host "Aguardando as tabelas serem versionadas pelo Flyway..."
    $flywayDone = $false
    while (-not $flywayDone) {
        $logs = docker logs irr_flyway_init 2>&1
        if ($logs -match "Successfully applied|Started IrrApplication|Tomcat started on port") {
            $flywayDone = $true
        } else {
            $running = docker ps -q -f "name=irr_flyway_init"
            if (-not $running) { break }
            Start-Sleep -Seconds 2
        }
    }

    Write-Host "Encerrando o container do projeto Java..."
    docker stop irr_flyway_init 2>$null | Out-Null

    Write-Host "Populando o banco de dados e inserindo o usuário Admin..."
    & ".\scripts\getReadyDevelop\populate_db.ps1"
}

# 6. Executar a aplicação Java via Docker Compose
Write-Host "[6/6] Iniciando o servidor no modo Debug..."
Write-Host "========================================="
Write-Host "✅ Ambiente quase pronto!"

if ($SKIP_MOCK) {
    Write-Host "O banco de dados já estava populado. Nenhuma ação extra foi necessária."
}

Write-Host "========================================="
Write-Host "Iniciando Spring Boot via Docker Compose..."

# Rotina em background: abre uma nova janela PowerShell com as credenciais após o Spring Boot iniciar
$projectRoot = (Get-Location).Path
$backgroundScript = {
    param($projectRoot)
    Set-Location $projectRoot
    Start-Sleep -Seconds 3
    $ready = $false
    while (-not $ready) {
        $logs = docker logs irr_backend 2>&1
        if ($logs -match "Started IrrApplication") {
            $ready = $true
        } else {
            $running = docker ps -q -f "name=irr_backend"
            if (-not $running) { break }
            Start-Sleep -Seconds 2
        }
    }

    # Cria script temporário com a mensagem de boas-vindas
    $tmpScript = [System.IO.Path]::GetTempFileName() -replace '\.tmp$', '.ps1'
    @'
Clear-Host
Write-Host ""
Write-Host "  +=======================================+" -ForegroundColor Cyan
Write-Host "  |   OK  IRR - Aplicação Pronta!        |" -ForegroundColor Green
Write-Host "  +=======================================+" -ForegroundColor Cyan
Write-Host "  |                                       |" -ForegroundColor Cyan
Write-Host "  |  Credenciais do Administrador:        |" -ForegroundColor White
Write-Host "  |     E-mail:  admin@irr.com            |" -ForegroundColor Yellow
Write-Host "  |     Senha:   123456                   |" -ForegroundColor Yellow
Write-Host "  |                                       |" -ForegroundColor Cyan
Write-Host "  |  API:   http://localhost:8081          |" -ForegroundColor White
Write-Host "  |  Debug: porta 5005                    |" -ForegroundColor White
Write-Host "  |                                       |" -ForegroundColor Cyan
Write-Host "  +=======================================+" -ForegroundColor Cyan
Write-Host ""
Read-Host "  Pressione Enter para fechar esta janela"
'@ | Set-Content -Path $tmpScript -Encoding UTF8

    Start-Process powershell -ArgumentList "-NoExit", "-File", $tmpScript `
        -WindowStyle Normal
}

$job = Start-Job -ScriptBlock $backgroundScript -ArgumentList $projectRoot

docker compose up backend

# Aguarda o job terminar ao encerrar o script
$job | Stop-Job -PassThru | Remove-Job
