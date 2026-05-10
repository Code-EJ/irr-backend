#!/bin/bash
set -e

# Navega para o diretório raiz do projeto garantindo o caminho absoluto e infalível
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
cd "$PROJECT_ROOT"

echo "========================================="
echo "⚙️  Configuração do Ambiente de Dev - IRR "
echo "========================================="

# 1. Gerar Chaves RSA (OpenSSL)
echo "[1/6] Verificando chaves RSA..."
mkdir -p src/main/resources
if [ ! -f src/main/resources/private.key ] || [ ! -f src/main/resources/public.key ]; then
    echo "Gerando novas chaves RSA..."
    openssl genrsa -out src/main/resources/private.key 2048
    openssl rsa -in src/main/resources/private.key -pubout -out src/main/resources/public.key
    echo "Chaves geradas com sucesso!"
else
    echo "Chaves RSA já existem. Pulando..."
fi

# 2. Subir Container Docker
echo "[2/6] Verificando banco de dados (Docker)..."
if docker compose ps | grep -q "Up"; then
    echo "Container do banco de dados já está rodando. Pulando inicialização."
else
    echo "Subindo banco de dados (Docker)..."
    docker compose up -d
    echo "Aguardando o PostgreSQL ficar pronto..."
    sleep 5
fi

# Extraindo credenciais do arquivo application.properties (ou padrão do Docker)
DB_USER="postgres"
DB_PASS="postgres"
DB_NAME="irrv2"
CONTAINER_NAME=$(docker compose ps -q | head -n 1)

# Precisamos garantir que o Flyway rode as migrations ANTES dos inserts. 
# O jeito mais seguro no Spring Boot sem plugin configurado é forçar o Flyway a rodar rapidamente.
# Mas como o usuário quer os inserts por script, faremos isso via psql após o Flyway atuar na inicialização,
# OU executamos o Flyway via Maven.

# 3. Instalar Dependências
echo "[3/6] Instalando dependências (Maven)..."
./mvnw clean install -DskipTests

# 4 e 5. Inserir Admin e popular o banco de dados.
echo "[4/6] Verificando se o banco já está populado..."

# Cria o mock file de qualquer forma para não quebrar o populate_db se rodado manualmente
cat << 'SQL' > scripts/getReadyDevelop/00-admin-user.sql
-- Insere um administrador padrao com a senha '123456' ($2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$EixZaYVK1fsbw1ZfbX3OXePaWxn96p36WQoeG6Lruj3vjQJU/B6cK', 'Administrador Root', 'ADMINISTRADOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
SQL

# Checa se o banco está de pé e se o admin já existe
SKIP_MOCK=false
if PGPASSWORD=root psql -h localhost -p 5435 -U irr_admin -d irr -c "SELECT 1 FROM users WHERE email = 'admin@irr.com';" > /dev/null 2>&1; then
    echo "Banco de dados já configurado e populado. Pulando preparação de mock."
    SKIP_MOCK=true
fi

if [ "$SKIP_MOCK" = false ]; then
    echo "Preparando dados Fictícios (Mock)..."
fi

# 6. Executar a aplicação Java em modo Debug
echo "[6/6] Iniciando o servidor no modo Debug..."
echo "========================================="
echo "✅ Ambiente quase pronto!"

if [ "$SKIP_MOCK" = false ]; then
    echo "IMPORTANTE: Após verificar no console que o Flyway finalizou a criação das tabelas,"
    echo "abra outro terminal e rode: ./scripts/getReadyDevelop/populate_db.sh"
    echo "-----------------------------------------"
    echo "🧑‍💻 Usuário Admin de Teste Que Será Criado:"
    echo "    E-mail: admin@irr.com"
    echo "    Senha:  123456"
else
    echo "O banco de dados já está populado. Nenhuma ação extra é necessária."
fi
echo "========================================="
echo "Iniciando Spring Boot..."

./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-agentlib:jdwp=transport=dt_socket,server=y,suspend=n,address=*:5005"
