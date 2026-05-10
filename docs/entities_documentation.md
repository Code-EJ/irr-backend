# Documentação de Entidades — Projeto IRR

Este documento detalha o dicionário de dados da aplicação, cobrindo todos os módulos (IAM, Hierarquia de Materiais, Cadastros, Operacional, Triagem, Prensagem, Mercado e Estoque). A fonte inquestionável da verdade baseia-se no `V1__Initial_Schema.sql` e subsequentes migrations.

---

## 1. Sistema e Acessos (IAM)

### `User`
Responsável pela autenticação, autorização e amarração de multitenancy (`creator_id`) por todo o sistema.

| Coluna | Tipo | Descrição / Constraints |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `email` | `VARCHAR` | Unique. Login de acesso. |
| `password_hash` | `TEXT` | Senha criptografada (BCrypt). |
| `full_name` | `VARCHAR` | Nome completo. |
| `user_role` | `Enum (UserRole)` | Nível de acesso (RBAC). |
| `is_active` | `BOOLEAN` | Soft Delete padrão. |
| `created_at` / `updated_at` | `TIMESTAMP` | Controle de auditoria de tempo. |

---

## 2. Hierarquia Tipológica de Materiais
A árvore de materiais categoriza os resíduos. Implementa **Optimistic Locking** (`version`) para evitar colisão de edições simultâneas (Ref: `V2__Add_Version_Column_Materials`).

### `MaterialCategory` (Nível 1)
| Coluna | Tipo | Descrição / Constraints |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `name` | `VARCHAR` | Nome da categoria (ex: Plástico). |
| `is_active` | `BOOLEAN` | Soft Delete. (Em cascata para filhos). |
| `creator_id` | `UUID` | FK -> `users`. |
| `version` | `INTEGER` | Controle de concorrência (Optimistic Lock). |

### `MaterialType` (Nível 2)
| Coluna | Tipo | Descrição / Constraints |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `category_id` | `UUID` | FK -> `material_category`. |
| `name` | `VARCHAR` | Nome do tipo (ex: PET). |
| `is_active` | `BOOLEAN` | Soft Delete. |
| `creator_id` | `UUID` | FK -> `users`. |
| `version` | `INTEGER` | Controle de concorrência (Optimistic Lock). |

### `MaterialSubtype` (Nível 3)
*Este é o nó folha utilizado operacionalmente para dar entrada, movimentar e vender estoque.*
| Coluna | Tipo | Descrição / Constraints |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `type_id` | `UUID` | FK -> `material_type`. |
| `name` | `VARCHAR` | Nome do subtipo (ex: PET Cristal). |
| `is_active` | `BOOLEAN` | Soft Delete. Não permite inativação se houver estoque vinculado. |
| `creator_id` | `UUID` | FK -> `users`. |
| `version` | `INTEGER` | Controle de concorrência (Optimistic Lock). |

---

## 3. Cadastros Base e Anexos

### `Attachment`
Centraliza todos os arquivos e comprovantes físicos.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `file_name` | `VARCHAR` | Nome do arquivo. |
| `file_type` | `VARCHAR` | MIME type ou formato. |
| `storage_url` | `TEXT` | URL ou path físico do S3/Bucket. |

### `Vehicle`
Frota de veículos disponíveis para coleta.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `license_plate` | `VARCHAR` | Placa do veículo. |
| `model` | `VARCHAR` | Modelo ou descrição. |
| `creator_id` | `UUID` | FK -> `users`. |

### `TeamMember`
Funcionários e membros da equipe (motoristas, coletores).
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `name` | `VARCHAR` | Nome do membro. |
| `role` | `VARCHAR` | Função desempenhada. |

### `Donor`
Pessoas ou empresas que realizam doações de materiais no instituto.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `name` | `VARCHAR` | Nome ou Razão Social. |
| `document` | `VARCHAR` | CPF ou CNPJ. |
| `donor_type` | `Enum (DonorType)`| `PF` ou `PJ`. |

---

## 4. Operacional (Logística Reversa e Entrada)

### `Collection` (Coleta)
Registro macro de uma coleta realizada nas ruas.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `realization_date` | `TIMESTAMP` | Data em que a coleta ocorreu. |
| `total_weight_kg` | `NUMERIC` | Peso total estimado/balança. |
| `vehicle_id` | `UUID` | FK -> `vehicle`. |
| `driver_id` | `UUID` | FK -> `team_member` (O motorista responsável). |
| `mtr_generator_id` | `UUID` | FK -> `attachment` (MTR Gerador). |
| `mtr_destinator_id` | `UUID` | FK -> `attachment` (MTR Destinador). |
| `collection_diary_id` | `UUID` | FK -> `attachment` (Diário de coleta). |
| `creator_id` | `UUID` | FK -> `users`. |

*(Nota: Tabela associativa `collection_team` mapeia `collection_id` <-> `team_member_id` para listar coletores).*

### `Donation` (Doação)
Entrada passiva de material no pátio do instituto.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `donation_date` | `TIMESTAMP` | Data da doação. |
| `total_weight_kg` | `NUMERIC` | Peso total. |
| `donor_id` | `UUID` | FK -> `donor`. |
| `proof_attachment_id` | `UUID` | FK -> `attachment` (Recibo ou foto). |

### `InputItem` (Itens de Entrada)
Discriminação dos materiais recolhidos (seja por Coleta ou por Doação).
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `collection_id` | `UUID` | FK -> `collection` (Nullable). |
| `donation_id` | `UUID` | FK -> `donation` (Nullable). |
| `material_subtype_id`| `UUID` | FK -> `material_subtype`. Material exato recebido. |
| `weight_kg` | `NUMERIC` | Peso aferido deste material. |
| `volume_m3` | `NUMERIC` | Volume a granel ocupado. |

---

## 5. Tratamento de Materiais

### `Sorting` (Triagem) e `SortedItem`
Agrupa materiais da entrada (`InputItem`) que passaram por processo de triagem. Separa as impurezas (`reject`) do material recuperado (`SortedItem`).

| Tabela | Campos Importantes |
|---|---|
| `Sorting` | `id`, `sorting_date`, `sorting_type` (Enum), `creator_id`. |
| `SortedItem` | `sorting_id`, `input_item_id` (origem), `material_subtype_id`, `weight_kg`, `volume_m3`, `reject_weight_kg`, `reject_volume_m3`. |

### `Pressing` (Prensagem) e `PressedBale` (Fardo)
Agrupa materiais já triados (`SortedItem`) transformando seu volume em fardos compactados.

| Tabela | Campos Importantes |
|---|---|
| `Pressing` | `id`, `pressing_date`, `creator_id`. |
| `PressedBale`| `pressing_id`, `sorted_item_id` (origem), `material_subtype_id`, `weight_kg`, `initial_volume_m3`, `final_volume_m3`. Demonstra a eficácia da compactação. |

---

## 6. Mercado (Saídas)

### `Buyer` (Comprador) e `Sale` (Venda)
| Tabela | Campos Importantes |
|---|---|
| `Buyer` | `id`, `name`, `document`, `creator_id`. |
| `Sale` | `id`, `sale_date`, `buyer_id`, Anexos (NFe, MTR, CDF), `total_value`, `creator_id`. |

### `SaleItem`
Discriminação dos fardos ou materiais vendidos.
| Coluna | Tipo | Descrição |
|---|---|---|
| `sale_id` | `UUID` | FK -> `sale`. |
| `material_subtype_id`| `UUID` | FK -> `material_subtype`. |
| `weight_kg` / `volume_m3` | `NUMERIC` | Quantidades transacionadas. |
| `unit_price` | `NUMERIC` | Preço praticado no momento da venda. |

---

## 7. Estoque (Inventory)

### `InventoryLog` (Log Transacional)
Cada inserção nas tabelas operacionais (Coleta, Doação, Venda) gera gatilhos na lógica de negócios para registrar no `InventoryLog`. Tabela imutável.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `material_subtype_id`| `UUID` | FK -> `material_subtype`. |
| `quantity_kg` | `NUMERIC` | Quantidade movimentada. |
| `quantity_m3` | `NUMERIC` | Volume movimentado. |
| `operation_type` | `Enum (OperationType)`| A justificativa do log (ex: `COLLECTION_INPUT`, `SALE_OUTPUT`). |

### `InventoryBalance` (Fotografia Atual)
Retrato consolidado do saldo de estoque do instituto, projetado a partir dos Logs.
| Coluna | Tipo | Descrição |
|---|---|---|
| `id` | `UUID` | Primary Key. |
| `material_subtype_id`| `UUID` | FK -> `material_subtype`. |
| `current_weight_kg` | `NUMERIC` | Saldo total em quilos (hoje). |
| `current_volume_m3` | `NUMERIC` | Saldo de espaço ocupado em metros cúbicos (hoje). |
| `last_updated_at` | `TIMESTAMP` | Última modificação. |
