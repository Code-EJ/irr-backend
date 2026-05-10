# IRR v2 — Documentação de Endpoints REST

> **Base URL:** `http://localhost:8081`
> **Autenticação:** Bearer Token JWT no header `Authorization: Bearer <token>`
> **Content-Type:** `application/json`

---

## Índice

- [Sessão (Autenticação)](#1-sessão-autenticação---apísession)
- [Veículos](#2-veículos---apivehicles)
- [Materiais — Categorias](#3-materiais--categorias---apimaterialscategories)
- [Materiais — Tipos](#4-materiais--tipos---apimaterialstypes)
- [Materiais — Subtipos](#5-materiais--subtipos---apimaterialssubtypes)
- [Erros Globais](#erros-globais)

---

## 1. Sessão (Autenticação) — `/api/session`

> Rotas públicas — **não requerem token JWT**.

---

### `POST /api/session/register`

Cria um novo usuário com role `REPRESENTATIVE` e retorna um token JWT.

**Request Body:**
```json
{
  "fullName": "João da Silva",
  "email": "joao@email.com",
  "password": "senhaSegura123"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `fullName` | `string` | ✅ | max 255 caracteres |
| `email` | `string` | ✅ | formato email válido, max 255 |
| `password` | `string` | ✅ | entre 8 e 72 caracteres |

**Response `201 Created`:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente ou inválido |
| `400` | `password_too_long` | Senha excede 72 bytes |
| `409` | `email_occupied` | E-mail já cadastrado |

---

### `POST /api/session/authenticate`

Autentica um usuário existente e retorna um token JWT.

**Request Body:**
```json
{
  "email": "joao@email.com",
  "password": "senhaSegura123"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `email` | `string` | ✅ | formato email válido, max 255 |
| `password` | `string` | ✅ | max 72 caracteres |

**Response `200 OK`:**
```json
{
  "token": "eyJhbGciOiJSUzI1NiJ9..."
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente ou inválido |
| `401` | `wrong_credentials` | E-mail não encontrado ou senha incorreta |

---

## 2. Veículos — `/api/vehicles`

> Todas as rotas **requerem token JWT** no header `Authorization`.
> Todas as operações de leitura/escrita são **isoladas por `creator_id`** — cada usuário vê apenas seus próprios registros.
>
> **Permissões:**
> - `ADMINISTRATOR`: Permissão total (CRUD + desativação mesmo com vínculo)
> - `ORGANIZATION`, `CITY_HALL`: Create, Read, Update, Delete (bloqueado se houver coletas vinculadas)
> - `REPRESENTATIVE`: Somente leitura

---

### `POST /api/vehicles`

Cria um novo veículo.

**Autenticação:** `ADMINISTRATOR`, `ORGANIZATION` ou `CITY_HALL`

**Request Body:**
```json
{
  "licensePlate": "ABC-1234",
  "model": "Fiat Ducato"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `licensePlate` | `string` | ✅ | max 20 caracteres, normalizado para UPPER |
| `model` | `string` | ❌ | max 100 caracteres |

**Response `201 Created`:**

Header `Location: /api/vehicles/{id}`

```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "licensePlate": "ABC-1234",
  "model": "Fiat Ducato",
  "isActive": true,
  "createdAt": "2026-05-10T13:00:00-03:00",
  "updatedAt": "2026-05-10T13:00:00-03:00",
  "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente ou inválido |
| `403` | `access_denied` | Role insuficiente |
| `409` | `vehicle_plate_occupied` | Placa já cadastrada |

---

### `POST /api/vehicles/batch`

Cria múltiplos veículos em uma única transação atômica (máx. 100 por lote).

**Autenticação:** `ADMINISTRATOR`, `ORGANIZATION` ou `CITY_HALL`

**Request Body:**
```json
{
  "vehicles": [
    { "licensePlate": "ABC-1234", "model": "Fiat Ducato" },
    { "licensePlate": "XYZ-5678", "model": "VW Delivery" }
  ]
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `vehicles` | `array` | ✅ | 1 a 100 itens |
| `vehicles[].licensePlate` | `string` | ✅ | max 20 caracteres |
| `vehicles[].model` | `string` | ❌ | max 100 caracteres |

> ⚠️ **Transação atômica:** Se qualquer item falhar, **nenhum** é salvo.

**Response `201 Created`:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "licensePlate": "ABC-1234",
    "model": "Fiat Ducato",
    "isActive": true,
    "createdAt": "2026-05-10T13:00:00-03:00",
    "updatedAt": "2026-05-10T13:00:00-03:00",
    "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
  }
]
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente, lista vazia ou > 100 itens |
| `403` | `access_denied` | Role insuficiente |
| `409` | `vehicle_plate_occupied` | Placa duplicada (intra-lote ou já existente no banco) |

---

### `GET /api/vehicles`

Lista veículos do usuário autenticado com paginação e filtros opcionais.

**Autenticação:** Qualquer usuário autenticado

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `licensePlate` | `string` | ❌ | Filtro parcial por placa (LIKE `%termo%`, case-insensitive) |
| `model` | `string` | ❌ | Filtro parcial por modelo (LIKE `%termo%`, case-insensitive) |
| `page` | `integer` | ❌ | Página (default: `0`) |
| `size` | `integer` | ❌ | Itens por página (default: `20`) |
| `sort` | `string` | ❌ | Campo de ordenação (default: `licensePlate`) |

**Exemplos:**
```
GET /api/vehicles
GET /api/vehicles?licensePlate=ABC
GET /api/vehicles?model=Fiat&page=0&size=10
GET /api/vehicles?sort=createdAt,desc
```

**Response `200 OK`:**
```json
{
  "content": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "licensePlate": "ABC-1234",
      "model": "Fiat Ducato",
      "isActive": true,
      "createdAt": "2026-05-10T13:00:00-03:00",
      "updatedAt": "2026-05-10T13:00:00-03:00",
      "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
    }
  ],
  "pageable": {
    "pageNumber": 0,
    "pageSize": 20,
    "sort": { "sorted": true }
  },
  "totalElements": 1,
  "totalPages": 1,
  "last": true
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `401` | `invalid_token` | Token ausente ou inválido |
| `403` | `expired_token` | Token expirado |

---

### `GET /api/vehicles/{id}`

Retorna um veículo específico pelo ID (somente se pertencer ao usuário autenticado).

**Autenticação:** Qualquer usuário autenticado

**Path Variable:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | ID do veículo |

**Response `200 OK`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "licensePlate": "ABC-1234",
  "model": "Fiat Ducato",
  "isActive": true,
  "createdAt": "2026-05-10T13:00:00-03:00",
  "updatedAt": "2026-05-10T13:00:00-03:00",
  "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `401` | `invalid_token` | Token ausente ou inválido |
| `403` | `expired_token` | Token expirado |
| `404` | `vehicle_not_found` | Veículo não encontrado ou não pertence ao usuário |

---

### `PUT /api/vehicles/{id}`

Atualiza um veículo existente.

**Autenticação:** `ADMINISTRATOR`, `ORGANIZATION` ou `CITY_HALL`

**Path Variable:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | ID do veículo |

**Request Body:**
```json
{
  "licensePlate": "ABC-1234",
  "model": "VW Delivery",
  "isActive": true
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `licensePlate` | `string` | ✅ | max 20 caracteres |
| `model` | `string` | ❌ | max 100 caracteres |
| `isActive` | `boolean` | ✅ | — |

**Response `200 OK`:**
```json
{
  "id": "550e8400-e29b-41d4-a716-446655440000",
  "licensePlate": "ABC-1234",
  "model": "VW Delivery",
  "isActive": true,
  "createdAt": "2026-05-10T13:00:00-03:00",
  "updatedAt": "2026-05-10T14:00:00-03:00",
  "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente ou inválido |
| `403` | `access_denied` | Role insuficiente |
| `404` | `vehicle_not_found` | Veículo não encontrado ou não pertence ao usuário |
| `409` | `vehicle_plate_occupied` | Nova placa já pertence a outro veículo |
| `422` | `inactive_vehicle` | Veículo já está inativo |

---

### `PUT /api/vehicles/batch`

Atualiza múltiplos veículos em uma única transação atômica (máx. 100 por lote).

**Autenticação:** `ADMINISTRATOR`, `ORGANIZATION` ou `CITY_HALL`

**Request Body:**
```json
{
  "vehicles": [
    {
      "id": "550e8400-e29b-41d4-a716-446655440000",
      "licensePlate": "ABC-1234",
      "model": "Fiat Ducato Novo",
      "isActive": true
    },
    {
      "id": "660f9511-f30c-52e5-b827-557766551111",
      "licensePlate": "XYZ-5678",
      "model": "VW Delivery",
      "isActive": false
    }
  ]
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `vehicles` | `array` | ✅ | 1 a 100 itens |
| `vehicles[].id` | `UUID` | ✅ | — |
| `vehicles[].licensePlate` | `string` | ✅ | max 20 caracteres |
| `vehicles[].model` | `string` | ❌ | max 100 caracteres |
| `vehicles[].isActive` | `boolean` | ✅ | — |

> ⚠️ **Transação atômica:** Se qualquer item falhar, **nenhum** é atualizado.

**Response `200 OK`:**
```json
[
  {
    "id": "550e8400-e29b-41d4-a716-446655440000",
    "licensePlate": "ABC-1234",
    "model": "Fiat Ducato Novo",
    "isActive": true,
    "createdAt": "2026-05-10T13:00:00-03:00",
    "updatedAt": "2026-05-10T15:00:00-03:00",
    "creatorId": "e1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890"
  }
]
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo obrigatório ausente, lista vazia ou > 100 itens |
| `403` | `access_denied` | Role insuficiente |
| `404` | `vehicle_not_found` | Veículo não encontrado ou não pertence ao usuário |
| `409` | `vehicle_plate_occupied` | Placa duplicada (intra-lote ou conflito com existente) |
| `422` | `inactive_vehicle` | Veículo já está inativo |

---

### `DELETE /api/vehicles/{id}`

Desativa um veículo (soft delete — não remove fisicamente do banco).

**Autenticação:** `ADMINISTRATOR`, `ORGANIZATION` ou `CITY_HALL`

> ⚠️ **Regra de Negócio:** Se o veículo possui coletas vinculadas (`collection`):
> - **Administrador** → pode desativar (soft delete), histórico preservado
> - **Organização / Prefeitura** → bloqueado com `409 vehicle_has_collection_binding` e mensagem para contatar o instituto

**Path Variable:**

| Parâmetro | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | ID do veículo |

**Response `204 No Content`** *(sem corpo)*

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `403` | `access_denied` | Role insuficiente (REPRESENTATIVE) |
| `404` | `vehicle_not_found` | Veículo não encontrado ou não pertence ao usuário |
| `409` | `vehicle_has_collection_binding` | Não-admin tentando excluir veículo com coletas |
| `422` | `inactive_vehicle` | Veículo já está inativo | |

---

## 3. Materiais — Categorias — `/api/materials/categories`

> Todas as rotas **requerem token JWT**.
> Leitura: qualquer autenticado. **Escrita/Exclusão: apenas ADMINISTRATOR.**
> Isolamento multilocatário via `creator_id`. Suporta **Optimistic Locking** via campo `version`.

---

### `POST /api/materials/categories`

Cria uma nova categoria de material.

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{
  "name": "Plástico"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | `string` | ✅ | max 100 caracteres, único por tenant |

**Response `201 Created`:**
```json
{
  "id": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
  "name": "Plástico",
  "isActive": true,
  "version": 0,
  "createdAt": "2026-05-10T14:00:00-03:00",
  "updatedAt": "2026-05-10T14:00:00-03:00"
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Nome ausente ou inválido |
| `403` | `access_denied` | Não é ADMINISTRATOR |
| `409` | `material_name_occupied` | Nome já existe neste nível/tenant |

---

### `GET /api/materials/categories`

Lista categorias com filtro opcional por nome.

**Autenticação:** Qualquer autenticado

**Query Parameters:**

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `name` | `string` | ❌ | Filtro parcial (LIKE `%termo%`, case-insensitive) |
| `page` | `integer` | ❌ | Página (default: `0`) |
| `size` | `integer` | ❌ | Itens por página (default: `20`) |
| `sort` | `string` | ❌ | Ordenação (default: `name`) |

**Response `200 OK`:** Página de `MaterialCategoryResponseDTO`

---

### `GET /api/materials/categories/{id}`

Retorna uma categoria pelo ID.

**Autenticação:** Qualquer autenticado

**Response `200 OK`:** `MaterialCategoryResponseDTO`

| Status | `error` | Quando ocorre |
|---|---|---|
| `404` | `material_not_found` | Categoria não encontrada ou não pertence ao usuário |

---

### `PUT /api/materials/categories/{id}`

Atualiza uma categoria existente. **Requer `version` para controle de concorrência.**

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{
  "name": "Plástico Reciclado",
  "version": 0
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `name` | `string` | ✅ | max 100 caracteres |
| `version` | `long` | ✅ | Deve ser igual à versão atual (Optimistic Lock) |

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `403` | `access_denied` | Não é ADMINISTRATOR |
| `404` | `material_not_found` | Categoria não encontrada |
| `409` | `material_name_occupied` | Novo nome já existe |
| `409` | `concurrent_modification` | Outro usuário modificou antes (version mismatch) |
| `422` | `inactive_material` | Categoria já está inativa |

---

### `DELETE /api/materials/categories/{id}`

Desativa uma categoria (soft delete). **Cascata:** desativa todos os Types e Subtypes filhos.

**Autenticação:** `ADMINISTRATOR`

> ⚠️ **Regra de Negócio:** Se a categoria possui subtipos com vínculos de estoque (InventoryBalance), **apenas Administradores** podem desativá-la. Não-administradores recebem `409` com instrução para contatar o instituto.

**Response `204 No Content`**

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `403` | `access_denied` | Não é ADMINISTRATOR |
| `404` | `material_not_found` | Categoria não encontrada |
| `409` | `material_has_inventory_binding` | Possui vínculo de estoque (não-admin) |
| `422` | `inactive_material` | Já está inativa |

---

## 4. Materiais — Tipos — `/api/materials/types`

> Mesmas regras de autenticação e isolamento que Categorias. Cada Tipo pertence a uma Categoria.

---

### `POST /api/materials/types`

Cria um novo tipo de material vinculado a uma categoria.

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{
  "categoryId": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
  "name": "PET"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `categoryId` | `UUID` | ✅ | Deve existir e pertencer ao tenant |
| `name` | `string` | ✅ | max 100, único por categoria/tenant |

**Response `201 Created`:**
```json
{
  "id": "b2c3d4e5-f6a7-8901-b2c3-d4e5f6a78901",
  "categoryId": "a1b2c3d4-e5f6-7890-a1b2-c3d4e5f67890",
  "name": "PET",
  "isActive": true,
  "version": 0,
  "createdAt": "2026-05-10T14:00:00-03:00",
  "updatedAt": "2026-05-10T14:00:00-03:00"
}
```

**Erros possíveis:**

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Campo ausente ou inválido |
| `403` | `access_denied` | Não é ADMINISTRATOR |
| `404` | `material_parent_not_found` | Categoria pai não encontrada |
| `409` | `material_name_occupied` | Nome já existe nesta categoria |
| `422` | `inactive_material` | Categoria pai está inativa |

---

### `GET /api/materials/types`

Lista tipos com filtros opcionais.

**Autenticação:** Qualquer autenticado

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `categoryId` | `UUID` | ❌ | Filtra por categoria pai |
| `name` | `string` | ❌ | Filtro parcial por nome |

**Response `200 OK`:** Página de `MaterialTypeResponseDTO`

---

### `GET /api/materials/types/{id}`

**Autenticação:** Qualquer autenticado | **Response `200 OK`:** `MaterialTypeResponseDTO`

---

### `PUT /api/materials/types/{id}`

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{ "name": "PET Cristal", "version": 0 }
```

Mesmos erros que Category (404, 409 name/version, 422 inativo).

---

### `DELETE /api/materials/types/{id}`

**Autenticação:** `ADMINISTRATOR`

Desativa tipo + subtypes filhos em cascata. Mesma regra de vínculo de estoque que Category.

**Response `204 No Content`**

---

## 5. Materiais — Subtipos — `/api/materials/subtypes`

> Nível mais granular. Referenciado pelas tabelas operacionais (coleta, venda, inventário).

---

### `POST /api/materials/subtypes`

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{
  "typeId": "b2c3d4e5-f6a7-8901-b2c3-d4e5f6a78901",
  "name": "PET Transparente"
}
```

| Campo | Tipo | Obrigatório | Validação |
|---|---|---|---|
| `typeId` | `UUID` | ✅ | Deve existir e pertencer ao tenant |
| `name` | `string` | ✅ | max 100, único por tipo/tenant |

**Response `201 Created`:** `MaterialSubtypeResponseDTO`

---

### `GET /api/materials/subtypes`

**Autenticação:** Qualquer autenticado

| Parâmetro | Tipo | Obrigatório | Descrição |
|---|---|---|---|
| `typeId` | `UUID` | ❌ | Filtra por tipo pai |
| `name` | `string` | ❌ | Filtro parcial por nome |

**Response `200 OK`:** Página de `MaterialSubtypeResponseDTO`

---

### `GET /api/materials/subtypes/{id}`

**Autenticação:** Qualquer autenticado | **Response `200 OK`:** `MaterialSubtypeResponseDTO`

---

### `PUT /api/materials/subtypes/{id}`

**Autenticação:** `ADMINISTRATOR`

**Request Body:**
```json
{ "name": "PET Transparente Limpo", "version": 0 }
```

---

### `DELETE /api/materials/subtypes/{id}`

**Autenticação:** `ADMINISTRATOR`

Desativa o subtipo. Se possuir `InventoryBalance` vinculado:
- **Admin** → desativa (soft delete)
- **Não-admin** → rejeita com `409 material_has_inventory_binding`

**Response `204 No Content`**

---

## Erros Globais

Estes erros podem ocorrer em **qualquer endpoint** autenticado:

| Status | `error` | Quando ocorre |
|---|---|---|
| `400` | `bad_request` | Payload inválido (campos obrigatórios ausentes ou com formato inválido) |
| `401` | `invalid_token` | Token JWT ausente, malformado ou com assinatura inválida |
| `403` | `expired_token` | Token JWT expirado |
| `403` | `access_denied` | Usuário não possui a role necessária para o endpoint |
| `409` | `concurrent_modification` | Conflito de versão (Optimistic Locking) |

**Formato padrão de erro:**
```json
{
  "error": "codigo_do_erro",
  "message": "Descrição legível do problema",
  "campo_extra": "valor adicional quando aplicável"
}
```

---

## Resumo dos Endpoints

| Método | Rota | Auth | Descrição |
|---|---|---|---|
| `POST` | `/api/session/register` | ❌ Público | Registrar novo usuário |
| `POST` | `/api/session/authenticate` | ❌ Público | Autenticar e obter token |
| `POST` | `/api/vehicles` | ADMIN, ORG, CITY_HALL | Criar veículo (unitário) |
| `POST` | `/api/vehicles/batch` | ADMIN, ORG, CITY_HALL | Criar veículos (em massa) |
| `GET` | `/api/vehicles` | Autenticado | Listar veículos com filtros |
| `GET` | `/api/vehicles/{id}` | Autenticado | Buscar veículo por ID |
| `PUT` | `/api/vehicles/{id}` | ADMIN, ORG, CITY_HALL | Atualizar veículo (unitário) |
| `PUT` | `/api/vehicles/batch` | ADMIN, ORG, CITY_HALL | Atualizar veículos (em massa) |
| `DELETE` | `/api/vehicles/{id}` | ADMIN, ORG, CITY_HALL | Desativar veículo |
| `POST` | `/api/materials/categories` | ADMIN | Criar categoria |
| `GET` | `/api/materials/categories` | Autenticado | Listar categorias |
| `GET` | `/api/materials/categories/{id}` | Autenticado | Buscar categoria |
| `PUT` | `/api/materials/categories/{id}` | ADMIN | Atualizar categoria |
| `DELETE` | `/api/materials/categories/{id}` | ADMIN | Desativar categoria (cascata) |
| `POST` | `/api/materials/types` | ADMIN | Criar tipo |
| `GET` | `/api/materials/types` | Autenticado | Listar tipos |
| `GET` | `/api/materials/types/{id}` | Autenticado | Buscar tipo |
| `PUT` | `/api/materials/types/{id}` | ADMIN | Atualizar tipo |
| `DELETE` | `/api/materials/types/{id}` | ADMIN | Desativar tipo (cascata) |
| `POST` | `/api/materials/subtypes` | ADMIN | Criar subtipo |
| `GET` | `/api/materials/subtypes` | Autenticado | Listar subtipos |
| `GET` | `/api/materials/subtypes/{id}` | Autenticado | Buscar subtipo |
| `PUT` | `/api/materials/subtypes/{id}` | ADMIN | Atualizar subtipo |
| `DELETE` | `/api/materials/subtypes/{id}` | ADMIN | Desativar subtipo |
