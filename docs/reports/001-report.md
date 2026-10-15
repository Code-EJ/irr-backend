# Relatório de Atuação Técnica — Instituto Referência em Resíduos (IRR)

**Responsável:** Enzo Ribas  
**Organização:** CODE[] Soluções em Tecnologia Jr.  
**Período de referência:** outubro de 2026  
**Situação atual:** ambiente de homologação implantado; homologação com o cliente iniciada em 15/10/2026.

## 1. Contexto e desafio assumido

O projeto IRR possui um histórico de desenvolvimento marcado por diferentes ciclos de equipe, transições de liderança técnica e continuidade limitada entre etapas. Ao assumir a frente de trabalho, meu desafio não era apenas desenvolver novas funcionalidades: era compreender a arquitetura existente, identificar o que já estava implementado, corrigir problemas acumulados e estabelecer uma base tecnicamente verificável para que o sistema pudesse avançar até a homologação.

O trabalho exigiu atuar simultaneamente sobre backend, frontend, integração entre serviços, segurança, dependências, infraestrutura, testes, documentação e comunicação com o cliente.

Minha atuação teve como objetivo transformar um projeto com histórico de desenvolvimento fragmentado em uma entrega organizada, testável, implantável e preparada para avaliação operacional.

## 2. Principais contribuições técnicas

### 2.1. Retomada e consolidação da base técnica

Conduzi um trabalho de retomada técnica do projeto, preservando a arquitetura modular existente e evitando reescritas desnecessárias. O foco foi corrigir problemas concretos e consolidar os fluxos necessários à operação do MVP.

Entre as frentes trabalhadas estão:

- Integração e compatibilidade entre frontend e backend.
- Preservação do contrato versionado da API em `/api/v1`.
- Validação de compatibilidade com o esquema OpenAPI e os consumidores existentes.
- Preservação das migrações de banco de dados V1–V4.
- Revisão de fluxos de autenticação, autorização e recuperação de acesso.
- Consolidação das funcionalidades operacionais e de Analytics.
- Organização de testes automatizados, documentação e evidências reproduzíveis.

Essa abordagem permitiu avançar sem introduzir mudanças desnecessárias nas regras de negócio ou invalidar decisões anteriores que precisavam ser preservadas.

### 2.2. Evolução da segurança e das dependências

Uma das frentes mais relevantes foi a atualização da plataforma Spring para uma linha aberta e compatível com as restrições de licenciamento da organização.

A decisão técnica foi migrar para Spring Boot 4.0.8 e Spring Framework 7.0.9, em vez de depender de uma correção que exigiria licença Enterprise ou simplesmente suprimir os alertas encontrados.

O trabalho exigiu adaptar componentes de integração, dependências, serialização JSON e testes de compatibilidade. Também foi necessário verificar se a mudança preservava o comportamento externo da API.

As evidências registradas incluem:

- 51 testes unitários aprovados.
- 101 testes de integração aprovados.
- Comparação do contrato OpenAPI, preservando as 116 operações da API v1.
- Auditoria de dependências Maven sobre 185 coordenadas, sem correspondências identificadas nessa auditoria e sem supressões.
- Inicialização validada do candidato a deploy contra PostgreSQL e Redis locais.
- Preservação das migrações existentes e de comportamentos importantes de serialização e idempotência.

É importante distinguir a correção dos problemas identificados na auditoria de dependências da segurança integral da aplicação. As análises de imagem de container ainda registravam vulnerabilidades residuais, que foram mantidas documentadas em vez de serem ocultadas.

### 2.3. Implantação e preparação da homologação

Também conduzi a preparação do ambiente isolado de homologação, incluindo decisões sobre infraestrutura, persistência, segredos e implantação.

O ambiente foi planejado separadamente da produção, utilizando registros sintéticos e serviços independentes. Essa separação evita misturar dados de teste com dados do cliente e reduz o risco de afetar o projeto já existente na plataforma de hospedagem.

O trabalho incluiu:

- Preparação do backend e frontend para implantação.
- Configuração da infraestrutura de dados com PostgreSQL e Redis.
- Tratamento das permissões de armazenamento persistente sem manter a aplicação executando como root.
- Documentação de variáveis, credenciais e procedimentos operacionais.
- Planejamento de implantação portátil, com separação entre homologação e produção.
- Definição explícita de critérios para validação do ambiente remoto.

A documentação produzida também registra que homologação e produção possuem requisitos distintos. A implantação de um ambiente de teste não equivale à autorização para publicar o sistema em produção.

### 2.4. Organização de decisões e documentação técnica

Além da implementação, produzi registros formais das decisões de engenharia nos ADRs 0025 a 0029. Esses documentos descrevem a preparação para lançamento, a estratégia de implantação portátil, a homologação isolada, o tratamento das permissões de armazenamento e a atualização da plataforma Spring.

Esse material transforma decisões técnicas em registros auditáveis, com alternativas consideradas, justificativas, critérios de verificação e limitações conhecidas. Isso facilita a manutenção futura e reduz a dependência de conhecimento informal.

### 2.5. Coordenação com o cliente e preparação operacional

A atuação também envolveu contato direto com Paulo para organizar a homologação, disponibilizar o acesso ao ambiente e coordenar a avaliação dos fluxos operacionais.

A preparação contempla cadastros, coletas, rotas, triagem, prensagem, estoque, vendas e relatórios, utilizando dados fictícios para a avaliação inicial.

Também foi necessário coordenar o funcionamento do ambiente de homologação, que permanece desligado fora dos períodos combinados para controlar o consumo de recursos.

A recuperação de senha por e-mail permanece como uma pendência explicitamente identificada e fora da primeira avaliação. Não trato essa funcionalidade como validada sem a confirmação de entrega real de e-mails.

## 3. Resultado e impacto da atuação

O principal resultado foi consolidar uma base técnica verificável e levar o IRR até a etapa de homologação com o cliente, depois de um histórico de transições de equipe.

Minha contribuição abrangeu mais do que desenvolvimento de funcionalidades: envolveu diagnóstico, decisões arquiteturais, compatibilidade, segurança, automação de testes, preparação de infraestrutura, documentação e articulação operacional.

As evidências técnicas disponíveis permitem demonstrar que o trabalho foi executado com critérios de verificação e rastreabilidade, e não apenas com base na percepção de que a aplicação compila ou inicia.

## 4. Situação atual e pendências

Em 15/10/2026, a homologação com o cliente foi iniciada. Isso representa um avanço importante, mas não deve ser confundido com aceite definitivo ou liberação para produção.

Permanecem como pontos de acompanhamento:

- Conclusão da validação dos fluxos pelo cliente.
- Verificação operacional do ambiente e da persistência dos arquivos.
- Validação de e-mail e recuperação de senha quando essa etapa for autorizada.
- Revisão das vulnerabilidades residuais de imagens e infraestrutura.
- Conclusão das avaliações de navegador, acessibilidade e segurança.
- Definição e aprovação das condições de implantação de produção.

## 5. Conclusão

Minha atuação no IRR demonstrou capacidade de assumir a continuidade técnica de um projeto complexo, recuperar contexto, identificar riscos, tomar decisões fundamentadas e organizar a passagem do desenvolvimento para a homologação.

O diferencial não está em uma funcionalidade isolada, mas na combinação entre implementação, segurança, testes, infraestrutura, documentação e coordenação com o cliente. O projeto avançou para uma etapa em que o sistema pode ser avaliado operacionalmente, com evidências técnicas e pendências explicitamente registradas.

**Evidências principais:**

- [IRR Backend — PR #79](https://github.com/Code-EJ/irr-backend/pull/79)
- [IRR Frontend — PR #67](https://github.com/Code-EJ/irr-frontend/pull/67)
- ADR-0025 — Preparação pré-release e consolidação das evidências.
- ADR-0026 — Inicialização controlada do armazenamento.
- ADR-0027 — Implantação portátil e separação entre homologação e produção.
- ADR-0028 — Homologação isolada com dados sintéticos.
- ADR-0029 — Atualização da plataforma Spring e validação de compatibilidade.
