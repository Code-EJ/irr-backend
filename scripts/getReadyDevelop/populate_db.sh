#!/bin/bash
set -e

# Navega para a raiz do projeto para garantir que os caminhos relativos funcionem
cd "$(dirname "$0")/../.."

# Executa todos os scripts usando PGPASSWORD e psql contra o banco local exposto na porta (verificando docker-compose ou default 5435/5432)
# Usa porta 5435 que é a que vc tem do log

PGPASSWORD=root psql -h localhost -p 5435 -U irr_admin -d irr -f scripts/getReadyDevelop/00-admin-user.sql
PGPASSWORD=root psql -h localhost -p 5435 -U irr_admin -d irr -f scripts/getReadyDevelop/01-mock-materials.sql
PGPASSWORD=root psql -h localhost -p 5435 -U irr_admin -d irr -f scripts/getReadyDevelop/02-mock-donors-vehicles.sql

echo "Banco de Dados preenchido com sucesso!"

echo "========================================="
echo "✅ Populamento Concluído!"
echo "-----------------------------------------"
echo "🧑‍💻 Credenciais do Usuário Administrador:"
echo "    E-mail: admin@irr.com"
echo "    Senha:  123456"
echo "========================================="
