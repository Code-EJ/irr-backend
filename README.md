# Backend — Projeto IRR

## Visão Geral

O backend do Projeto IRR é responsável pela implementação da lógica de negócio, gerenciamento de dados e exposição de uma API REST utilizada pelo frontend da aplicação.

A aplicação foi desenvolvida utilizando Spring Boot e tem como objetivo fornecer uma estrutura organizada e escalável para o controle de entradas e saídas de materiais, bem como o gerenciamento das entidades do sistema.

---

## Objetivo

O backend tem como principais objetivos:

- Implementar as regras de negócio do sistema
- Gerenciar e persistir os dados no banco de dados
- Expor endpoints REST para consumo pelo frontend
- Garantir organização, consistência e escalabilidade da aplicação

---

## Tecnologias Utilizadas

- Java
- Spring Boot
- Spring Web
- Spring Data JPA
- Hibernate
- Banco de dados relacional (PostgreSQL, MySQL ou similar)
- Maven ou Gradle

---

## Arquitetura da Aplicação

O backend segue uma arquitetura em camadas, organizada da seguinte forma:

```
Controller  →  Service  →  Repository  →  Banco de Dados
```

- **Controller**: responsável por receber requisições HTTP
- **Service**: responsável pela lógica de negócio
- **Repository**: responsável pela comunicação com o banco de dados
- **Database**: responsável pela persistência dos dados

Essa separação facilita a manutenção, testes e evolução do sistema.

---

## Estrutura do Projeto

Exemplo de organização:

```
src/main/java/
│
├── controller/      # Endpoints da API
├── service/         # Regras de negócio
├── repository/      # Acesso ao banco de dados
├── model/           # Entidades do sistema
├── dto/             # Objetos de transferência de dados (se houver)
└── config/          # Configurações da aplicação
```

---

## Como Executar o Projeto

### Pré-requisitos

- Java (versão 17 ou superior recomendada)
- Maven ou Gradle
- Banco de dados configurado

---

### Configuração

Configurar o arquivo `application.properties` ou `application.yml` com as informações do banco de dados:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/seu_banco
spring.datasource.username=usuario
spring.datasource.password=senha
```

---

### Execução

Com Maven:

```bash
mvn spring-boot:run
```

Ou executar diretamente a classe principal da aplicação no IDE.

O backend será iniciado normalmente em:

```
http://localhost:8080
```

---

## API REST

O backend expõe endpoints REST para manipulação dos dados do sistema.

Exemplos de rotas:

```
GET /usuarios
POST /usuarios

GET /doacoes
POST /doacoes

GET /materialEntrada
GET /materialSaida
```

As rotas completas devem ser documentadas no arquivo de rotas da API.

---

## Entidades do Sistema

O sistema é baseado em entidades que representam os principais dados do domínio, como:

- Usuário
- Documento
- Doação
- Material de Entrada
- Material de Saída
- Tipologia
- Subtipologia

Essas entidades são mapeadas utilizando JPA e persistidas no banco de dados.

---

## Integração com o Frontend

O backend se comunica com o frontend através de requisições HTTP.

O frontend consome os endpoints expostos pela API para:

- recuperar dados
- enviar novos registros
- atualizar informações
- excluir registros

---

## Boas Práticas

- Separar responsabilidades entre camadas (Controller, Service, Repository)
- Evitar lógica de negócio em Controllers
- Utilizar DTOs para comunicação externa quando necessário
- Manter nomes de entidades e campos consistentes
- Escrever código limpo e organizado

---

## Observações

> Este projeto está em desenvolvimento e pode sofrer alterações na estrutura e nas funcionalidades.

> A modelagem do banco de dados e as rotas da API podem evoluir ao longo do projeto.

---

## Contribuição

Para contribuir com o projeto:

1. Criar uma branch para a funcionalidade ou correção
2. Desenvolver a alteração proposta
3. Realizar commits claros e organizados
4. Abrir um Pull Request para revisão

---

## Projeto IRR

Este backend faz parte do Projeto IRR, desenvolvido pela Code JR, com foco em aprendizado, organização e evolução contínua dos membros da equipe.