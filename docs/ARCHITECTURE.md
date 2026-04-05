# 🏗️ Guia de Arquitetura — back-irr-v2

Este documento descreve a organização técnica do projeto, os padrões adotados e o fluxo de desenvolvimento para novas funcionalidades. O projeto utiliza uma arquitetura em camadas com influências de **Ports & Adapters (Hexagonal)**, priorizando o desacoplamento da lógica de negócio das tecnologias externas.

---

## 📂 Estrutura de Pastas e Responsabilidades

### 1. `src/main/java/org/code/api/controllers/`
*   **O que é:** Camada de Entrada (Entrypoint).
*   **O que deve ser armazenado:** Classes `@RestController`.
*   **Responsabilidade:** Receber requisições HTTP, validar o formato básico dos dados (via Bean Validation nos DTOs), extrair atributos da sessão e delegar a execução para a camada de `Service`. **Nunca** deve conter lógica de negócio complexa.

### 2. `src/main/java/org/code/api/services/`
*   **O que é:** Camada de Aplicação/Negócio.
*   **O que deve ser armazenado:** Classes anotadas com `@Service`.
*   **Responsabilidade:** Implementar as regras de negócio e casos de uso. É o "cérebro" da aplicação. Ela orquestra as chamadas para os Repositórios e Portas de infraestrutura.

### 3. `src/main/java/org/code/api/domain/`
O coração do projeto, subdividido em:
*   **`models/`:** Entidades JPA que representam as tabelas do banco de dados e as regras essenciais do domínio.
*   **`ports/`:** Interfaces que definem contratos. Segue o princípio de inversão de dependência: o Service depende de uma interface (`Port`), e a implementação real fica na `Infrastructure`.
*   **`exception/`:** Exceções customizadas de negócio (ex: `AuthError`, `VehicleError`).
*   **`enums/`:** Enumeradores globais (ex: `UserType`, `DocumentType`).
*   **`common/`:** Classes base reutilizáveis, como a `TimeStampedEntity`.

### 4. `src/main/java/org/code/api/dto/`
*   **O que é:** Objetos de Transferência de Dados.
*   **Responsabilidade:** Definir o contrato de entrada (`Request`) e saída (`Response`) da API. Evita expor as entidades do banco de dados diretamente para o cliente.

### 5. `src/main/java/org/code/api/infrastructure/`
*   **O que é:** Implementações técnicas e Adapters.
*   **O que deve ser armazenado:**
    *   **`repositories/`:** Interfaces Spring Data JPA.
    *   **`security/`:** Provedores de Token (JWT), Criptografia (BCrypt) e configurações do Spring Security.
*   **Responsabilidade:** Lidar com detalhes de infraestrutura, banco de dados e segurança.

### 6. `src/main/java/org/code/api/filter/`
*   **Responsabilidade:** Interceptar requisições antes de chegarem aos controllers. O `BearerFilter`, por exemplo, é responsável por validar o token JWT e injetar o objeto `Session` no request.

### 7. `src/main/resources/db/migrations/`
*   **Responsabilidade:** Scripts SQL do Flyway para versionamento do banco de dados. Cada alteração no schema deve gerar um novo arquivo `V1.x__descricao.sql`.

---

## 🚀 Guia para Criação de uma Nova Feature (Ex: Document CRUD)

Para implementar uma nova funcionalidade seguindo o padrão do projeto, siga este fluxo passo a passo:

### Passo 0: Preparação
1.  **Crie uma nova branch:**
    Sempre inicie o trabalho a partir da branch principal (`main` ou `master` dependendo do projeto) criando uma nova branch:
    ```bash
    git checkout -b <nome-da-branch-da-feature>
    ```

### Passo 1: Persistência (Database)
1.  Crie a migration SQL em `src/main/resources/db/migrations/` (ex: `V1.12__create_document_table.sql`).
2.  Crie a Entidade JPA em `domain/models/document/Document.java` (estendendo `TimeStampedEntity`).
3.  Crie o Repositório em `infrastructure/repositories/DocumentRepository.java`.

### Passo 2: Contrato e Negócio (Domain & Service)
1.  Crie a interface de contrato em `domain/ports/DocumentPort.java`.
2.  Defina as exceções necessárias em `domain/exception/DocumentError.java`.
3.  Implemente a lógica de negócio em `services/DocumentService.java` (injetando o `DocumentRepository`).

### Passo 3: Comunicação (DTOs & Controller)
1.  Crie os DTOs em `dto/document/request/` e `dto/document/response/`.
2.  Crie o `DocumentController.java` em `controllers/`.
    *   Use `@RequestAttribute("session") Session session` para obter o usuário logado.
    *   Injete o `DocumentService`.

### Passo 4: Tratamento de Erros e Segurança
1.  Adicione o mapeamento das novas exceções no `controllers/ErrorHandler.java`.
2.  Certifique-se de que a rota está protegida (o `BearerFilter` já protege todas por padrão, a menos que configurado o contrário).

### Passo 5: Validação (Testes)
1.  Crie scripts de teste em `scripts/tests/` usando `curl` para validar o fluxo E2E (Ex: `20-document-create.sh`).
2.  (Opcional mas recomendado) Crie testes unitários para o `DocumentService` em `src/test/java/.../services/`.

---

## 🛠️ Padrões de Código
*   **Lombok:** Use `@Getter`, `@Setter`, `@Builder` e `@AllArgsConstructor` para reduzir boilerplate.
*   **Imutabilidade:** Prefira DTOs com campos finais sempre que possível.
*   **Erros:** Sempre retorne um erro semântico (ex: `DocumentError.NotFound`) em vez de exceções genéricas.
*   **Soft Delete:** Para deleção de registros principais, prefira usar um campo `ativo = false` em vez de deletar a linha do banco.

---

## 💻 Configuração do Ambiente

### 0. Clonagem e Acesso (SSH)
Antes de tudo, você precisa de acesso ao repositório via SSH (recomendado para maior segurança).

1.  **Gerar chave SSH (se não tiver):**
    ```bash
    ssh-keygen -t ed25519 -C "seu_email@exemplo.com"
    # Pressione Enter para o caminho padrão e defina uma senha (passphrase) se desejar.
    ```
2.  **Adicionar chave ao Agent:**
    ```bash
    eval "$(ssh-agent -s)"
    ssh-add ~/.ssh/id_ed25519
    ```
3.  **Adicionar ao GitHub/GitLab:**
    Copie o conteúdo da chave pública:
    ```bash
    cat ~/.ssh/id_ed25519.pub
    ```
    E cole nas configurações de SSH do seu perfil na plataforma de Git.

4.  **Clonar o repositório:**
    ```bash
    git clone git@github.com:Code-EJ/back-irr-v2.git
    cd back-irr-v2
    ```

### 1. Pré-requisitos Obrigatórios
*   **Java 21 (JDK):** Versão mínima recomendada para suporte ao Spring Boot 3.2+.
*   **Maven 3.9.x+:** Gerenciador de dependências oficial. O uso do `mvnw` é desencorajado neste ambiente.
*   **Docker & Docker Compose:** Necessários para provisionar o PostgreSQL 16.

### 2. Passo a Passo por Sistema Operacional

#### 🐧 Linux (Ubuntu/Debian)
1.  **Gerenciador de Java (SDKMAN):**
    ```bash
    curl -s "https://get.sdkman.io" | bash
    source "$HOME/.sdkman/bin/sdkman-init.sh"
    sdk install java 21.0.2-tem
    sdk install maven 3.9.6
    ```
2.  **Configuração de Permissões Docker:**
    ```bash
    sudo usermod -aG docker $USER
    newgrp docker # Aplica as mudanças sem deslogar
    ```
3.  **Verificação:**
    ```bash
    java -version  # Deve retornar openjdk 21...
    mvn -version   # Deve retornar Apache Maven 3.9...
    docker ps      # Não deve retornar erro de permissão
    ```

#### 🍎 macOS (Intel & Apple Silicon)
1.  **Homebrew & Ferramentas:**
    ```bash
    /bin/bash -c "$(curl -fsSL https://raw.githubusercontent.com/Homebrew/install/HEAD/install.sh)"
    brew install openjdk@21 maven docker docker-compose
    ```
2.  **Variáveis de Ambiente (Zsh):**
    Adicione ao seu `~/.zshrc`:
    ```bash
    export JAVA_HOME="/opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk/Contents/Home"
    export PATH="/opt/homebrew/opt/maven@3.9/bin:$PATH"
    export PATH="/opt/homebrew/opt/openjdk@21/bin:$PATH"
    ```
    Depois, execute: `source ~/.zshrc`.
3.  **Link Simbólico (Importante para IDEs):**
    ```bash
    sudo ln -sfn /opt/homebrew/opt/openjdk@21/libexec/openjdk.jdk /Library/Java/JavaVirtualMachines/openjdk-21.jdk
    ```

#### 🪟 Windows (Powershell / WSL 2)
1.  **Instalação via Winget (Nativo):**
    Abra o PowerShell como Administrador:
    ```powershell
    winget install EclipseAdoptium.Temurin.21.JDK
    winget install Apache.Maven
    ```
2.  **Configuração de Variáveis (Manual se necessário):**
    *   `JAVA_HOME`: `C:\Program Files\Eclipse Foundation\jdk-21.x.x`
    *   `MAVEN_HOME`: `C:\Program Files\Maven\apache-maven-3.9.x`
    *   Adicione `%JAVA_HOME%\bin` e `%MAVEN_HOME%\bin` ao seu `PATH`.
3.  **WSL 2 (Altamente Recomendado):**
    Se optar pelo WSL, siga os passos do **Linux** dentro do terminal do Ubuntu após instalar o [Docker Desktop](https://www.docker.com/products/docker-desktop/) e habilitar a integração em *Settings > Resources > WSL Integration*.

### 3. Inicialização e Verificação do Projeto

1.  **Geração das Chaves RSA (Obrigatório):**
    A API utiliza chaves RSA para assinar e validar tokens JWT. Caso os arquivos `src/main/resources/public.key` e `private.key` não existam, gere-os via terminal (requer OpenSSL instalado):

    ```bash
    # 1. Gerar chave privada (formato PKCS#8 para compatibilidade com Java)
    openssl genpkey -algorithm RSA -out private.key -pkeyopt rsa_keygen_bits:2048
    
    # 2. Extrair a chave pública
    openssl rsa -in private.key -pubout -out public.key
    
    # 3. Mover para a pasta de resources
    mv *.key src/main/resources/
    ```

2.  **Provisionamento do Banco:**
    No diretório raiz:
    ```bash
    docker compose up -d
    ```
    Aguarde o log `database system is ready to accept connections`.

3.  **Build e Execução:**
    ```bash
    mvn clean install -DskipTests # Garante que as dependências e o Lombok foram processados
    mvn spring-boot:run
    ```

4.  **Verificação Final:**
    A aplicação estará disponível em `http://localhost:8081`. O Flyway executará as migrações automaticamente no primeiro boot.

---

## 🛠️ Ferramentas Recomendadas
*   **IDE:** IntelliJ IDEA (com plugin **Lombok** e **Annotation Processing** habilitado).
*   **Database Client:** DBeaver ou TablePlus.
*   **API Client:** Insomnia ou Postman.
