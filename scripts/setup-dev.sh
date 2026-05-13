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
DB_USER="irr_admin"
DB_PASS="root"
DB_NAME="irr"
CONTAINER_NAME=$(docker compose ps -q | head -n 1)

# Precisamos garantir que o Flyway rode as migrations ANTES dos inserts. 
# O jeito mais seguro no Spring Boot sem plugin configurado é forçar o Flyway a rodar rapidamente.
# Mas como o usuário quer os inserts por script, faremos isso via psql após o Flyway atuar na inicialização,
# OU executamos o Flyway via Maven.

# 3. Instalar Dependências
echo "[3/6] Instalando dependências (Maven)..."
docker compose run --rm backend ./mvnw clean install -DskipTests

# 4 e 5. Inserir Admin e popular o banco de dados.
echo "[4/6] Verificando se o banco já está populado..."

# Cria o mock file de qualquer forma para não quebrar o populate_db se rodado manualmente
cat << 'SQL' > scripts/getReadyDevelop/00-admin-user.sql
-- Insere um administrador padrao com a senha '123456' ($2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq)
INSERT INTO users (id, email, password_hash, full_name, user_role, is_active, created_at, updated_at)
VALUES (gen_random_uuid(), 'admin@irr.com', '$2a$10$wMx4v6kD6YgqyQZeIXbCg.mozjTEA4ZWTHs5Ekluh8Ez.6fATOXWq', 'Administrador Root', 'ADMINISTRATOR', true, NOW(), NOW())
ON CONFLICT (email) DO NOTHING;
SQL

# Checa se o banco está de pé e se o admin já existe
SKIP_MOCK=false
USER_COUNT=$(PGPASSWORD=root psql -h localhost -p 5435 -U irr_admin -d irr -t -A -c "SELECT COUNT(*) FROM users WHERE email = 'admin@irr.com';" 2>/dev/null || echo "0")
if [ "$USER_COUNT" -gt "0" ]; then
    echo "Banco de dados já configurado e populado. Pulando preparação de mock."
    SKIP_MOCK=true
fi

if [ "$SKIP_MOCK" = false ]; then
    echo "Preparando dados Fictícios (Mock)..."
    echo "Criando as tabelas necessárias com base nos arquivos de migração (V1 e V2)..."
    
    echo "Inicializando o projeto Java via container para o Flyway versionar as tabelas..."
    docker rm -f irr_flyway_init > /dev/null 2>&1 || true
    docker compose run --rm -d --name irr_flyway_init backend ./mvnw spring-boot:run -Dspring-boot.run.jvmArguments="-Dspring.main.web-application-type=none"
    
    echo "Aguardando as tabelas serem versionadas pelo Flyway..."
    while ! docker logs irr_flyway_init 2>&1 | grep -q -E "Successfully applied|Started IrrApplication|Tomcat started on port"; do
        sleep 2
        if [ -z "$(docker ps -q -f name=irr_flyway_init)" ]; then
            break
        fi
    done
    
    echo "Encerrando o container do projeto Java..."
    docker stop irr_flyway_init > /dev/null 2>&1 || true

    echo "Populando o banco de dados e inserindo o usuário Admin..."
    ./scripts/getReadyDevelop/populate_db.sh
fi

# 6. Executar a aplicação Java em modo Debug
echo "[6/6] Iniciando o servidor no modo Debug..."
echo "========================================="
echo "✅ Ambiente quase pronto!"

if [ "$SKIP_MOCK" = true ]; then
    echo "O banco de dados já estava populado. Nenhuma ação extra foi necessária."
fi

echo "========================================="
echo "Iniciando Spring Boot via Docker Compose..."

# Rotina em background: abre um novo terminal com as credenciais após o Spring Boot iniciar
(
    sleep 3
    while ! docker logs irr_backend 2>&1 | grep -q "Started IrrApplication"; do
        sleep 2
        if [ -z "$(docker ps -q -f name=irr_backend)" ]; then
            break
        fi
    done

    # Cria um script temporário para exibir a mensagem no novo terminal
    TMP_SCRIPT=$(mktemp /tmp/irr_ready_XXXXXX.sh)
    cat > "$TMP_SCRIPT" << 'MSG_SCRIPT'
#!/bin/bash
clear
echo ""
echo "  ╔═══════════════════════════════════════╗"
echo "  ║   ✅  IRR - Aplicação Pronta!         ║"
echo "  ╠═══════════════════════════════════════╣"
echo "  ║                                       ║"
echo "  ║  Credenciais do Administrador:        ║"
echo "  ║     E-mail:  admin@irr.com            ║"
echo "  ║     Senha:   123456                   ║"
echo "  ║                                       ║"
echo "  ║  API:   http://localhost:8081         ║"
echo "  ║  Debug: porta 5005                    ║"
echo "  ║                                       ║"
echo "  ╚═══════════════════════════════════════╝"
echo ""
read -rp "  Pressione Enter para fechar esta janela..."
MSG_SCRIPT
    chmod +x "$TMP_SCRIPT"

    if command -v gnome-terminal &>/dev/null; then
        gnome-terminal --title="IRR - Ambiente Pronto!" -- bash "$TMP_SCRIPT"
    elif command -v xterm &>/dev/null; then
        xterm -title "IRR - Ambiente Pronto!" -fa "Monospace" -fs 12 -e bash "$TMP_SCRIPT"
    elif command -v konsole &>/dev/null; then
        konsole --title "IRR - Ambiente Pronto!" -e bash "$TMP_SCRIPT"
    elif command -v xfce4-terminal &>/dev/null; then
        xfce4-terminal --title="IRR - Ambiente Pronto!" -e "bash $TMP_SCRIPT"
    else
        command -v notify-send &>/dev/null && \
            notify-send "IRR - Aplicacao Pronta!" "API: http://localhost:8081 | Admin: admin@irr.com / 123456"
        echo ""
        echo "  ╔════════════════════════════════════╗"
        echo "  ║  ✅ IRR - Aplicacao Pronta!        ║"
        echo "  ╠════════════════════════════════════╣"
        echo "  ║  E-mail: admin@irr.com             ║"
        echo "  ║  Senha:  123456                    ║"
        echo "  ║  API:    http://localhost:8081     ║"
        echo "  ╚════════════════════════════════════╝"
    fi
) &

docker compose up backend