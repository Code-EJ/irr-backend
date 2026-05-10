# Documentação de Enums — Projeto IRR

Este documento detalha todas as enumerações (Enums) utilizadas no domínio do projeto IRR, suas constantes e seus respectivos propósitos dentro da regra de negócio.

---

## 1. `UserRole`
**Pacote:** `org.code.api.domain.enums`

Define os níveis de permissão e perfis de acesso no sistema para controle RBAC (Role-Based Access Control).

| Valor | Descrição |
|---|---|
| `ADMINISTRATOR` | Acesso total ao sistema. Único capaz de realizar ações destrutivas globais (soft deletes) em qualquer entidade. |
| `CITY_HALL` | Perfil para Prefeituras parceiras. Pode criar e gerenciar seus próprios registros operacionais (Veículos, Coletas). |
| `ORGANIZATION` | Perfil para Organizações de Catadores. Similar à Prefeitura, gerencia apenas seus próprios dados operacionais. |
| `REPRESENTATIVE` | Perfil de leitura. Pode visualizar relatórios e dashboards, mas não tem permissão de modificação de dados. |

---

## 2. `DonorType`
**Pacote:** `org.code.api.domain.enums`

Define a natureza jurídica de um doador de materiais (Entidade `Donor`).

| Valor | Descrição |
|---|---|
| `PF` | Pessoa Física (utiliza CPF como documento). |
| `PJ` | Pessoa Jurídica (utiliza CNPJ como documento). |

---

## 3. `SortingType`
**Pacote:** `org.code.api.domain.enums`

Define a granularidade ou fase da triagem realizada no instituto (Entidade `Sorting`).

| Valor | Descrição |
|---|---|
| `GROSS` | Triagem Bruta (separação muito inicial ou macro). |
| `PRIMARY` | Triagem Primária (separação principal por categorias amplas). |
| `FINE` | Triagem Fina (separação granular por subtipos exatos de material). |

---

## 4. `OperationType`
**Pacote:** `org.code.api.domain.enums`

Define o tipo de operação de balanço (Entidade `InventoryLog`) responsável por gerar a modificação (entrada ou saída) no estoque de materiais.

| Valor | Descrição | Efeito Típico no Estoque |
|---|---|---|
| `COLLECTION_INPUT` | Entrada de material advindo de Coleta. | Positivo (+) |
| `DONATION_INPUT` | Entrada de material advindo de Doação. | Positivo (+) |
| `SALE_OUTPUT` | Saída de material devido a Venda. | Negativo (-) |
| `SORTING_INPUT` | Entrada de material resultante de uma Triagem. | Positivo (+) |
| `SORTING_OUTPUT` | Saída de material consumido para realizar uma Triagem. | Negativo (-) |
| `PRESSING_INPUT` | Entrada de fardo prensado resultante de Prensagem. | Positivo (+) |
| `PRESSING_OUTPUT` | Saída de material solto consumido em Prensagem. | Negativo (-) |
| `MANUAL_ADJUSTMENT` | Ajuste manual ou balanço corretivo realizado por um Admin. | Positivo ou Negativo (+/-) |
