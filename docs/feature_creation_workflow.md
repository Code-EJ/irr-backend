# Fluxo Padrão para Criação de Funcionalidades (Features) — Projeto IRR

Este documento estabelece o padrão arquitetural e o passo a passo obrigatório para a criação de novas funcionalidades (Features) no back-end do projeto IRR. Toda vez que uma nova funcionalidade for solicitada, o desenvolvedor (ou IA) deve seguir ESTRITAMENTE a ordem e as regras abaixo.

A arquitetura do projeto utiliza **Arquitetura Hexagonal (Ports and Adapters)** combinada com princípios de **Clean Architecture**.

---

## Passo 0: Fonte da Verdade (O Banco de Dados)
O banco de dados é a fonte inquestionável da verdade. NUNCA invente campos ou adivinhe relacionamentos ao construir entidades.
**Ação:** Antes de codificar qualquer coisa, analise as tabelas correspondentes nos arquivos físicos de migração (`V1__Initial_Schema.sql`, `V2__Add_Version_Column_Materials.sql`, etc.) para entender os campos, tipos de dados e Foreign Keys.

---

## Passo 1: Entidades (Models)
**Local:** `src/main/java/org/code/api/domain/models/`

Crie a classe Java e anote com `@Entity` e `@Table`. Mapeie as colunas de acordo com o SQL, utilizando o Lombok (`@Getter`, `@Setter`, `@NoArgsConstructor`, `@AllArgsConstructor`, `@Builder`) para otimizar o código.

### ⚠️ Regras Globais de Entidade (Padrão Ouro)
1. **Chave Primária:** Todas as tabelas usam UUID.
   * *Regra JPA:* Anote com `@Id`, `@GeneratedValue(strategy = GenerationType.UUID)` e defina o tipo como `java.util.UUID`.
2. **Exclusão Lógica (Soft Delete):** Nenhuma exclusão física é permitida.
   * *Regra JPA:* Adicione o campo `private Boolean isActive;`.
   * *Regra de Busca:* Anote a classe com `@SQLRestriction("is_active = true")` (ou equivalente da sua versão do Hibernate) para que as buscas ignorem registros inativos.
3. **Auditoria de Tempo:**
   * *Regra JPA:* Utilize `@CreationTimestamp` no campo `createdAt` (com `updatable = false`) e `@UpdateTimestamp` no campo `updatedAt`. O tipo deve ser `java.time.OffsetDateTime` ou `java.time.Instant`.
4. **Auditoria de Usuário (Multitenancy):**
   * *Regra JPA:* Mapeie a coluna `creator_id` como um relacionamento `@ManyToOne(fetch = FetchType.LAZY)` apontando para a entidade `User`.
5. **Tipos de Dados Específicos:**
   * *Pesos, Volumes e Dinheiro (NUMERIC 15,4 ou 15,2):* Mapear sempre como `java.math.BigDecimal`. NUNCA use Double ou Float para evitar erros de arredondamento.
6. **Relacionamentos e Lazy Loading:**
   * Todos os relacionamentos `@ManyToOne` e `@OneToOne` devem ter `fetch = FetchType.LAZY` declarado explicitamente para evitar N+1 queries.
   * NUNCA use Arrays (ex: `integer[]`) para relacionamentos. Use `@OneToMany` ou `@ManyToMany` com `@JoinTable`.
7. **Documentos/Anexos:**
   * Notas fiscais, MTRs e recibos devem apontar para a entidade `Attachment` através de Foreign Keys (ex: `mtr_generator_id`).

---

## Passo 2: Repositórios (Infrastructure)
**Local:** `src/main/java/org/code/api/infrastructure/repositories/`

Crie uma interface estendendo `JpaRepository<Entidade, UUID>`.
1. **Regra de Isolamento (Multitenancy):** Sempre que possível, inclua métodos customizados que filtrem buscas pela coluna de dono (`creator_id`). Exemplo: `Optional<Entidade> findByIdAndCreatorId(UUID id, UUID creatorId);` e `Page<Entidade> findAllByCreatorId(UUID creatorId, Pageable pageable);`.

---

## Passo 3: Objetos de Transferência (DTOs)
**Local:** `src/main/java/org/code/api/dto/{feature}/request/` e `/response/`

Crie os **Records** de Request e Response correspondentes.
1. DTOs são objetos simples e imutáveis usados exclusivamente nas rotas HTTP.
2. **Validação de Entrada:** Insira as anotações do Jakarta Validation (`@NotBlank`, `@NotNull`, `@Positive`, etc.) nas classes de Request para barrar dados inválidos antes mesmo de baterem na camada de negócio.

---

## Passo 4: Portas (Domain Ports)
**Local:** `src/main/java/org/code/api/domain/ports/`

Crie a Interface (Contrato) que dita as ações que os casos de uso devem fazer.
1. O Contrato trafega **sempre DTOs e primitivos/UUIDs**. NUNCA trafegue Entidades JPA nas assinaturas dos métodos da interface da porta para evitar vazamento do domínio. Exemplo: `ResponseDTO create(RequestDTO data);`

---

## Passo 5: Serviços (Business Logic)
**Local:** `src/main/java/org/code/api/services/`

Crie a classe implementando a Interface da Porta (ex: `implements FeaturePort`). Anote com `@Service`.
1. Injete as dependências necessárias (Repositories, Autenticação).
2. Aplique as lógicas e validações de negócio pesadas (verificações de colisão, checagem de saldos, etc.).
3. **Regra de Isolamento (Multitenancy):** Certifique-se de vincular a autoria do registro mapeando o usuário atual (via `AuthenticatedUserProvider`) para o campo `creator_id` da Entidade em todas as operações de criação. Em operações de leitura e alteração, garanta que o recurso acessado pertence àquele usuário.

---

## Passo 6: Controladores (Endpoints & REST)
**Local:** `src/main/java/org/code/api/controllers/`

Crie o adaptador de entrada HTTP anotado com `@RestController` e `@RequestMapping("/api/{recurso}")`.
1. Injete a **Interface da Porta** (`FeaturePort`), nunca a implementação do serviço.
2. Adicione os métodos mapeados (`@GetMapping`, `@PostMapping`, etc.) com injeção dos `@Valid @RequestBody` ou parâmetros necessários.
3. ⚠️ **REGRA CRÍTICA DE SEGURANÇA (RBAC):** O projeto não bloqueia rotas globalmente no Security Filter. Portanto, você **deve** usar a anotação `@PreAuthorize` do Spring Security em **absolutamente todos** os métodos dos Controllers.
   * *Exemplos comuns:* `@PreAuthorize("isAuthenticated()")`, `@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'ORGANIZATION')")`, `@PreAuthorize("hasRole('ADMINISTRATOR')")`.

---

### Resumo do Fluxo (Bottom-Up)
1. Ler e analisar o `.sql` de Migração
2. Entidade `@Entity` (`domain/models`)
3. Repositório `JpaRepository` (`infrastructure/repositories`)
4. Request/Response `Records` (`dto`)
5. Interface `Port` (`domain/ports`)
6. Implementação `@Service` (`services`)
7. Controlador `@RestController` + `@PreAuthorize` (`controllers`)
