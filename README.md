# 📦 Backend — Projeto IRR

<p align="center">
    <img src="https://img.shields.io/badge/Java-21+-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white" alt="Java 21+"/>
    <img src="https://img.shields.io/badge/Spring_Boot-6DB33F?style=for-the-badge&logo=spring&logoColor=white" alt="Spring Boot"/>
    <img src="https://img.shields.io/badge/PostgreSQL-316192?style=for-the-badge&logo=postgresql&logoColor=white" alt="PostgreSQL"/>
    <img src="https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white" alt="Maven"/>
</p>

## 📖 Visão Geral

O backend do **Projeto IRR** é responsável pela lógica de negócio, persistência de dados e exposição de uma API REST para consumo pelo frontend.

A aplicação foi desenvolvida com **Java 21+** e **Spring Boot**, seguindo princípios de **Clean Architecture** para manter baixo acoplamento, alta coesão e facilidade de evolução.

## 🎯 Objetivos

- **Integridade de dados:** persistência segura e consistente em banco relacional.
- **Regras de negócio:** validações e fluxos de entrada/saída de materiais e usuários.
- **Integração:** endpoints REST padronizados para a camada de apresentação.
- **Escalabilidade:** estrutura modular para suportar crescimento contínuo.

## 🛠️ Tecnologias Utilizadas

**Core**
- Java 21+
- Spring Boot (Web, Validation, Security)
- Spring Data JPA / Hibernate

**Infraestrutura e Dados:**
- PostgreSQL (SGDB)
- Apache Maven (Build Tool)

## 🏗️ Arquitetura (Clean Architecture)

A arquitetura pode ser vista com mais detalhes na pasta [`docs/architecture/readme.md`](docs/architecture/readme.md).

### Camadas

- **Entities (Domain):** regras de negócio centrais e independentes de frameworks.
- **Use Cases (Application):** orquestração dos fluxos da aplicação.
- **Interface Adapters:** controllers, DTOs e mapeamentos de entrada/saída.
- **Infrastructure:** implementações técnicas (JPA, banco, integrações externas).

## 📂 Estrutura do Projeto

```text
src/main/java/org/code/api/
│
├── controllers/        # Controladores REST e endpoints da API
├── domain/             # Entidades e regras de domínio
├── dto/                # Data Transfer Objects
├── repositories/       # Repositórios
├── services/           # Serviços
├── interfaces/         # Contratos e abstrações entre camadas
└── utils/              # Utilitários e funções auxiliares compartilhadas
```

## 🚀 Como Executar o Projeto

### Pré-requisitos

- Java 21 ou superior instalado e configurado no `PATH`
- Maven instalado
- PostgreSQL rodando localmente (ou via Docker)

### 1. Configuração do Banco de Dados

Configure suas credenciais locais em `src/main/resources/application.properties`:

```properties
spring.application.name=irr

spring.datasource.url=jdbc:mysql://localhost:3306/your_database_name
spring.datasource.username=your_username
spring.datasource.password=your_password   

```

> Dica: usando Docker, você pode subir o banco rapidamente:
>
> ```bash
> docker run --name irr-postgres -e POSTGRES_PASSWORD=sua_senha -p 5432:5432 -d postgres
> ```

### 2. Compilação e Execução

No terminal, na raiz do projeto:

```bash
# Baixar dependências e compilar
mvn clean install

# Subir a aplicação
mvn spring-boot:run
```

<!-- A API estará disponível em: `http://localhost:8081`

## 🔌 Documentação da API (Endpoints)

Recomendado: acesse o Swagger UI para testar as rotas interativamente em:

`http://localhost:8081/swagger-ui.html`

Principais recursos expostos:

| Recurso  | Rota Base                 | Descrição                                 |
|----------|---------------------------|-------------------------------------------|
| Usuários | `/api/usuarios`           | Gestão de acesso e perfis.                |
| Doações  | `/api/doacoes`            | Registro e acompanhamento de doações.     |
| Entradas | `/api/materiais/entrada`  | Registro de inbound de materiais.         |
| Saídas   | `/api/materiais/saida`    | Registro de outbound de materiais.        | -->

<!-- ## 🧬 Principais Entidades do Domínio

- **Usuário:** atores que interagem com o sistema.
- **Documento:** registros fiscais ou de controle.
- **Doação:** entidade que conecta materiais a doadores/receptores.
- **Tipologia / Subtipologia:** categorização em árvore dos materiais. -->

## 🤝 Como Contribuir

1. Faça o fork ou clone do projeto.
2. Crie uma branch para sua feature ou correção:  
    `git checkout -b feature/minha-feature`
3. Desenvolva e teste suas alterações.
4. Faça commits descritivos seguindo Conventional Commits  
    (ex.: `feat: adiciona endpoint de doação`).
5. Faça push para sua branch:  
    `git push origin feature/minha-feature`
6. Abra um Pull Request.