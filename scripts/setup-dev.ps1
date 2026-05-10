$ErrorActionPreference = "Stop"

# Navega para o diretório raiz do projeto para garantir que o .\mvnw.cmd e as pastas sejam encontradas
Set-Location -Path "$PSScriptRoot\.."

Write-Host "========================================="
Write-Host "⚙️  Configuração do Ambiente de Dev - IRR "
Write-Host "========================================="

# 1. Gerar Chaves RSA (OpenSSL)
Write-Host "[1/6] Verificando chaves RSA..."
$resourcesPath = "src/main/resources"
if (-not (Test-Path -Path $resourcesPath)) {
    New-Item -ItemType Directory -Force -Path $resourcesPath | Out-Null
}

if (-not (Test-Path -Path "$resourcesPath/private.key") -or -not (Test-Path -Path "$resourcesPath/public.key")) {
    Write-Host "Gerando novas chaves RSA..."
    # Necessita do OpenSSL instalado e presente no PATH do Windows
    openssl genrsa -out "$resourcesPath/private.key" 2048
    openssl rsa -in "$resourcesPath/private.key" -pubout -out "$resourcesPath/public.key"
    Write-Host "Chaves geradas com sucesso!"
} else {
    Write-Host "Chaves RSA já existem. Pulando..."
}

# 2. Subir Container Docker
Write-Host "[2/6] Verificando banco de dados (Docker)..."
$dockerStatus = docker-compose ps
if ($dockerStatus -match "Up") {
    Write-Host "Container do banco de dados já está rodando. Pulando inicialização."
} else {
    Write-Host "Subindo banco de dados (Docker)..."
    docker-compose up -d
    Write-Host "Aguardando o PostgreSQL ficar pronto..."
    Start-Sleep -Seconds 5
}

# Extraindo credenciais do arquivo application.properties (ou padrão do Docker)
$DB_USER = "postgres"
$DB_PASS = "postgres"
$DB_NAME = "irrv2"

# 3. Instalar Dependências
Write-Host "[3/6] Instalando dependências (Maven)..."
.\mvnw.cmd clean install -DskipTests

# 4 e 5. Inserir Admin e popular o banco de dados.
Write-Host "[4/6] Verificando se o banco já está populado..."

# Cria o mock file de qualquer forma para não quebrar o populate_db se rodado manualmente
$adminSqlPath = "scripts/getReadyDevelop/00-admin-user.sql"
$adminSqlContent = @'
-- Insere um administrador padrao com a senha '123456' ($2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK', 'Administrador Root', 'ADMINISTRATOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
'@
Set-Content -Path $adminSqlPath -Value $adminSqlContent -Encoding UTF8

$env:PGPASSWORD = "root"
$dbCheck = psql -h localhost -p 5435 -U irr_admin -d irr -t -c "SELECT 1 FROM users WHERE email = 'admin@irr.com';" 2>$null
$SKIP_MOCK = $false

if ($dbCheck -match "1") {
    Write-Host "Banco de dados já configurado e populado. Pulando preparação de mock."
    $SKIP_MOCK = $true
}

if (-not $SKIP_MOCK) {
    Write-Host "Preparando dados Fictícios (Mock)..."
}

# 6. Executar a aplicação Java em modo Debug
Write-Host "[6/6] Iniciando o servidor no modo Debug..."
Write-Host "========================================="
Write-Host "✅ Ambiente quase pronto!"

if (-not $SKIP_MOCK) {
    Write-Host "IMPORTANTE: Após verificar no console que o Flyway finalizou a criação das tabelas,"
    Write-Host "abra outro terminal e rode: .\scripts\getReadyDevelop\populate_db.ps1"
    Write-Host "-----------------------------------------"
    Write-Host "🧑‍💻 Usuário Admin de Teste Que Será Criado:"
    Write-Host "    E-mail: admin@irr.com"
    Write-Host "    Senha:  123456"
} else {
    Write-Host "O banco de dados já está populado. Nenhuma ação extra é necessária."
}
Write-Host "========================================="
Write-Host "Iniciando Spring Boot..."

.\mvnw.cmd spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
