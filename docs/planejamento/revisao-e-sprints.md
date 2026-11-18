# Revisão de ponta a ponta e plano de sprints

Este documento faz duas coisas:

1. **Revisão de cobertura:** confere se cada proposta da apresentação institucional tem cartões no Jira.
2. **Ordem de execução:** organiza todos os cartões em sprints, respeitando as dependências entre eles, e define como trabalhamos (quem programa, quem testa e quando algo está pronto).

---

## 1. Revisão de cobertura (apresentação × Jira)

### 1.1 Matriz por slide

| Slide | Proposta | Cartões | Situação |
|---|---|---|---|
| 1 e 4 | Quatro participantes: investidores, empreendedores, prestadores, instituições e parceiros | KAN-4, KAN-60, KAN-118, **KAN-143** | Instituições não tinham cartão → **criado KAN-143** |
| 2 e 3 | Cinco pilares: descobrir, conectar, contratar, aprender, relacionar | KAN-8, KAN-10, KAN-115, KAN-116, KAN-117 | Coberto |
| 5 | Catálogo com virada de páginas, curadoria, experiência editorial | KAN-35 a KAN-40, KAN-58, KAN-85 | Coberto |
| 6 | Tese do investidor, edições temáticas, filtros, recomendação transparente | KAN-18 a KAN-20, KAN-38, KAN-39, KAN-61, KAN-85 | Coberto. "Novas oportunidades" sem aviso → **criado KAN-146** |
| 7 | Apresentação em três páginas, sete estágios, acompanhar, salvar, demonstrar interesse | KAN-21 a KAN-27, KAN-36, KAN-41, KAN-45 | Coberto |
| 8 | Vitrine pública × ambiente privado, liberação progressiva | KAN-28 a KAN-33, KAN-63, KAN-67 | Faltava o **envio** dos documentos confidenciais → **criado KAN-145** (MVP) |
| 9 | Fluxo do interesse ao Data Room, critérios explícitos, Match não é garantia | KAN-45 a KAN-49, KAN-59, KAN-63 | Coberto |
| 10 | Data Room: categorias, acesso individual, histórico, versões, revogação, marca d'água, documentos assinados | KAN-30 a KAN-33, KAN-145, **KAN-149, KAN-150, KAN-152** | Marca d'água, versões e assinatura não tinham cartão → **criados** |
| 11 | Governança: autenticação, sessões, verificação, assinatura eletrônica, LGPD, instrumentos (NDA, MoU, LOI, Term Sheet) | KAN-13 a KAN-17, KAN-57, KAN-62, KAN-90 a KAN-93, **KAN-151, KAN-152** | Instrumentos e assinatura → **criados** |
| 12 | Comunidade: publicações, conquistas, discussões, eventos, Pitch Nights, grupos | KAN-135 a KAN-141, **KAN-157** | Eventos e Pitch Nights → **criado KAN-157** |
| 13 | Marketplace: buscar, propor, contratar, avaliar; seis categorias | KAN-118 a KAN-126 | Coberto |
| 14 | Educação para os dois lados: cursos, trilhas, mentorias, workshops, eventos | KAN-127 a KAN-131, KAN-157 | Coberto |
| 15 | Publicidade: três formatos, segmentação, identificação | KAN-110 a KAN-114 | Coberto |
| 16 | Market Insights gratuito, pago e patrocinado | KAN-132 | Coberto; fase corrigida para 3 |
| 17 | Modelo de negócio: marketplace, cursos, eventos, publicidade, **taxa reduzida sobre investimentos** | KAN-120, KAN-125, KAN-131, KAN-111, **KAN-147** | Taxa sobre investimentos → **criado KAN-147** |
| 18 | Motor de recomendação para oportunidades, serviços e conteúdos; histórico **se autorizado** | KAN-39, KAN-121, KAN-133, **KAN-148** | Consentimento do histórico → **criado KAN-148** |
| 19 | Integração de tudo | Todos os épicos | Coberto |
| 20 | Três jornadas (investidor, empreendedor, prestador), incluindo **acompanhamento** | Épicos do MVP, KAN-115, **KAN-153, KAN-154** | Gestão de negociação e pós-investimento → **criados** |
| 21 | Quatro fases do produto | Rótulos `fase-1` a `fase-4`, **KAN-144** | Fase 4 não tinha cartão → **criado KAN-144** |
| 22 | Oito diretrizes | Regras de negócio distribuídas nos cartões | Coberto |
| 23 | Ciclo de vida: descoberta, conexão, investimento, desenvolvimento, acompanhamento | KAN-8, KAN-10, KAN-64, KAN-115, KAN-154 | Coberto |

### 1.2 O que foi criado ou corrigido nesta revisão

| Cartão | Tipo | Fase | Conteúdo |
|---|---|---|---|
| KAN-145 | História (KAN-7) | **MVP** | Data Room básico: envio e organização dos documentos confidenciais |
| KAN-146 | História (KAN-4) | **MVP** | Central de notificações e preferências de aviso |
| KAN-147 | História (KAN-10) | 2 | Taxa reduzida sobre investimentos, **bloqueada** até validação jurídica |
| KAN-148 | História (KAN-8) | 3 | Recomendação com histórico, mediante consentimento |
| KAN-142 | Épico | 3 | Data Room avançado e gestão de negociações (KAN-149 a KAN-154) |
| KAN-143 | Épico | 3 | Eventos, programas e parceiros institucionais (KAN-155 a KAN-157) |
| KAN-144 | Épico | 4 | Expansão: aplicativos, integrações, IA e novos mercados (KAN-158 a KAN-161) |
| KAN-132, KAN-133 | Correção | 2 → 3 | Market Insights e recomendação unificada seguem a Fase 3 da apresentação |

### 1.3 Ordem de grandeza do backlog

| Fase | Épicos | Histórias |
|---|---|---|
| 1 — MVP | 14 | 93 |
| 2 — Monetização | 3 (+ KAN-147 no épico KAN-10) | 21 |
| 3 — Inteligência e ecossistema social | 3 (+ KAN-148 no épico KAN-8) | 19 |
| 4 — Expansão | 1 | 4 (descoberta) |
| **Total** | **21** | **137** |

Todas as 137 histórias estão distribuídas nas sprints abaixo.

> Os itens de exemplo KAN-1 a KAN-3 continuam no projeto e precisam ser apagados manualmente.

---

## 2. Como vamos trabalhar

### 2.1 Papéis

| Papel | Quem | Responsabilidades |
|---|---|---|
| **Product Owner e testador humano** | Você | Priorizar, aprovar o planejamento da sprint, testar cada história em homologação seguindo o roteiro, aprovar o merge, registrar bugs, decidir as pendências de negócio (D1 a D15) |
| **Desenvolvedor** | Claude | Implementar back-end, front-end, banco, infraestrutura e pipeline; abrir PR por história |
| **Testes automatizados** | Claude | Escrever e manter todos os testes automatizados e o roteiro de teste manual de cada história |

### 2.2 Fluxo de cada história

```
 Claude                                                     Você
 ──────                                                     ────
 1. Cria a branch  <tipo>/KAN-<n>-descricao
 2. Implementa + testes automatizados
 3. Abre PR com: resumo, evidências dos testes
    e ROTEIRO DE TESTE MANUAL
 4. Pipeline verde (build, testes, cobertura,
    SonarCloud) → deploy automático em homologação
                                               5. Testa em homologação pelo roteiro
                                               6a. Aprovou → autoriza o merge
                                               6b. Achou problema → Bug no Jira
                                                   ligado à história
 7. Merge na main → história "Concluído"
    (correção de bug volta ao passo 2)
```

O merge só acontece com a sua aprovação, como já fazemos hoje.

### 2.3 Pirâmide de testes automatizados

| Nível | Ferramenta | O que cobre |
|---|---|---|
| Unidade | JUnit 5 + Mockito | Regras de negócio na camada Service, estratégias de compatibilidade, cálculos de prazo |
| Integração | Spring Boot Test + **Testcontainers** (PostgreSQL real) | Repositórios, migrações Flyway, transações, segurança dos endpoints |
| API | MockMvc / REST Assured | Contratos REST, códigos de erro, permissões por perfil (KAN-34) |
| Front-end | Vitest + React Testing Library | Componentes, formulários e validações |
| Ponta a ponta | Playwright | Jornadas críticas: cadastro, publicação, catálogo, interesse → Match → liberação do Nível 2 |
| Segurança | OWASP ZAP (varredura básica no pipeline) e análise de dependências | Vulnerabilidades conhecidas e cabeçalhos de segurança |

Portões de qualidade no pipeline:

- todos os testes passando;
- cobertura mínima de **80% nas camadas de serviço e domínio** (JaCoCo);
- *quality gate* do SonarCloud aprovado;
- nenhuma vulnerabilidade crítica nas dependências.

> Vitest, React Testing Library, Playwright e OWASP ZAP **completam a stack** definida para o front-end e para segurança; o restante já estava previsto.

### 2.4 Teste humano

Cada PR traz um **roteiro de teste manual** curto, com:

- dados de teste prontos (usuários e startups fictícias, como a NovaSafra da apresentação, criados por *seed* apenas em homologação);
- passos e resultado esperado;
- o que observar além do roteiro: textos, usabilidade, telas no celular.

Regras:

- Nada de dados pessoais reais em homologação.
- Bugs entram no Jira como **Bug**, ligados à história, com passos, resultado esperado e captura de tela.
- Gravidade: **bloqueante** (impede o roteiro), **alta** (regra de negócio errada), **média** (funciona com contorno) e **baixa** (visual ou texto).
- História com bug bloqueante ou alto não é aprovada.

### 2.5 Definição de pronto (DoD)

Uma história só está pronta quando:

1. todos os critérios de aceite estão atendidos;
2. os testes automatizados estão escritos e passando no pipeline;
3. os portões de qualidade foram aprovados;
4. a história está publicada em homologação;
5. **você testou e aprovou**;
6. a documentação foi atualizada (README, ADR ou OpenAPI, quando couber);
7. o merge foi feito na `main`.

### 2.6 Cadência

- **Sprints de 1 semana**, recomendadas porque o desenvolvimento é rápido e o gargalo é o teste humano. Cada sprint tem de 6 a 9 histórias, o que significa de 2 a 4 horas de teste por semana.
- Se o seu tempo de teste for menor, as mesmas sprints funcionam com **2 semanas**, sem mudar a ordem.

Ritos de cada sprint:

| Rito | Quando | Duração |
|---|---|---|
| Planejamento | Início da sprint: proponho o escopo e você aprova | 15 min |
| Teste e aprovação | Ao longo da sprint, PR por PR | — |
| Revisão (demo) | Fim da sprint: demonstração em homologação | 15 min |
| Retrospectiva | Logo após a revisão: o que manter e o que mudar | 10 min |

---

## 3. Sprints do MVP (Fase 1)

A ordem segue as dependências: não dá para listar startups no catálogo sem o cadastro, nem liberar o Nível 2 sem o Match.

| Sprint | Objetivo | Histórias | Você testa |
|---|---|---|---|
| **S0** | Fundação: repositório, pipeline, containers, infraestrutura e homologação no ar | KAN-50, KAN-51, KAN-52, KAN-53, KAN-55, KAN-56, KAN-58 | Acessar homologação, ver a página de saúde e o Swagger; avaliar a **PoC da virada de páginas** no computador, tablet e celular |
| **S1** | Identidade: cadastro, login, sessão, permissões e termos | KAN-12, KAN-13, KAN-14, KAN-15, KAN-16, KAN-34, KAN-57, KAN-92 | Criar conta, entrar e sair, recuperar senha, sessão expirar, aceitar termos, tentar acessar o que não pode |
| **S2** | Backoffice base: usuários internos, parâmetros, listas, auditoria, notificações e produção | KAN-54, KAN-75, KAN-78, KAN-79, KAN-80, KAN-81, KAN-104, KAN-146 | Entrar como admin, alterar um parâmetro, ver auditoria, testar a segunda aprovação, receber notificações; ver a primeira publicação em produção |
| **S3** | Investidores e organizações: tese, tipos, verificação | KAN-18, KAN-19, KAN-20, KAN-60, KAN-61, KAN-62, KAN-83, KAN-87 | Onboarding de investidor pessoa física e de fundo; pedir e aprovar verificação; conferir o bloqueio de conflito de interesse |
| **S4** | Cadastro de startups e rodada | KAN-21, KAN-22, KAN-23, KAN-24, KAN-25, KAN-26, KAN-27, KAN-59, KAN-65 | Cadastrar uma startup completa com equipe, salvar rascunho, abrir rodada com investidor líder e campos por instrumento |
| **S5** | Publicação: Nível 1, apresentação em três páginas, curadoria e edições | KAN-28, KAN-29, KAN-36, KAN-66, KAN-84, KAN-85 | Escolher campos públicos, pré-visualizar, enviar para curadoria, aprovar como curador, montar uma edição temática |
| **S6** | Catálogo: navegação, filtros, recomendação, favoritos | KAN-35, KAN-37, KAN-38, KAN-39, KAN-40, KAN-41, KAN-42, KAN-43, KAN-44 | Folhear o catálogo (mouse, teclado, toque), filtrar, conferir as justificativas, salvar e descartar |
| **S7** | Interesse e Match | KAN-45, KAN-46, KAN-47, KAN-48, KAN-49, KAN-67, KAN-105 | Demonstrar interesse, aceitar ou recusar, ver o painel de conexões, bloquear um investidor, ver a expiração automática |
| **S8** | Confidencialidade e Data Room básico | KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-145 | Enviar documentos, liberar o acesso com termo, abrir como investidor, revogar e conferir o histórico de acessos |
| **S9** | Rodada e agenda | KAN-64, KAN-94, KAN-95, KAN-96, KAN-97, KAN-98, KAN-106, KAN-109 | Propor e confirmar reunião, remarcar, registrar resultado, encerrar a rodada e registrar o investimento |
| **S10** | Moderação e atendimento | KAN-86, KAN-88, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103 | Denunciar, moderar, aplicar sanção, recorrer, abrir chamado e acompanhar prazos |
| **S11** | LGPD, operação e lançamento do MVP | KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108 | Excluir conta, pedir dados (titular), ver o painel de operação, checar rotinas; **teste de regressão completo** antes do lançamento |

### 3.1 Marcos

| Marco | Ao final de | O que dá para demonstrar |
|---|---|---|
| **M1 — Plataforma no ar** | S2 | Pipeline completo, homologação e produção, login e backoffice |
| **M2 — Jornada principal navegável** | S7 | Investidor descobre a startup no catálogo, demonstra interesse e recebe o Match. É a melhor versão para uma **apresentação parcial na disciplina** |
| **M3 — MVP completo** | S11 | As três camadas: produto, operação (backoffice, moderação, atendimento) e conformidade (LGPD) |

### 3.2 Decisões suas que bloqueiam sprints

| Precisa estar decidido até | Decisão |
|---|---|
| S0 | Aprovação da PoC da virada de páginas (KAN-58) |
| S2 | Valores padrão dos parâmetros principais (prazos de conexão, validade de acesso, SLAs) |
| S3 | Documentos exigidos na verificação de cada tipo de investidor |
| S8 | Texto do termo de confidencialidade da plataforma (KAN-63), com validação jurídica |
| S11 | Prazos da política de retenção (KAN-93) |

---

## 4. Ordem das fases seguintes

### Fase 2 — Monetização

| Sprint | Objetivo | Histórias |
|---|---|---|
| S12 | Prestadores, serviços e pagamentos (base) | KAN-118, KAN-119, KAN-125 |
| S13 | Fluxo do marketplace | KAN-120, KAN-121, KAN-122, KAN-123, KAN-124, KAN-126 |
| S14 | Base de conhecimento | KAN-127, KAN-128, KAN-129, KAN-131 |
| S15 | Mentorias e publicidade (cadastro e aprovação) | KAN-130, KAN-110, KAN-111, KAN-112 |
| S16 | Veiculação de anúncios, especialistas e taxa | KAN-113, KAN-114, KAN-134, KAN-147 |

KAN-147 só entra se o jurídico liberar (D12). Caso contrário, sai da sprint e fica bloqueada.

### Fase 3 — Inteligência e ecossistema social

| Sprint | Objetivo | Histórias |
|---|---|---|
| S17 | Comunidade I: feed, seguir, grupos, moderação e mensagens | KAN-135, KAN-136, KAN-137, KAN-141 |
| S18 | Comunidade II: perguntas, discussões, ciclo comunidade → base, histórico consentido | KAN-138, KAN-139, KAN-140, KAN-148 |
| S19 | Insights, recomendação unificada e parceiros | KAN-132, KAN-133, KAN-155, KAN-156 |
| S20 | Eventos e negociação | KAN-157, KAN-153, KAN-151 |
| S21 | Data Room avançado e pós-investimento | KAN-149, KAN-150, KAN-152, KAN-154 |

### Fase 4 — Expansão

As investigações KAN-158 a KAN-161 (aplicativos, API, IA e novos mercados) são priorizadas com base nos dados de uso do MVP. Cada uma termina em um ADR antes de virar sprint.

---

## 5. Riscos do plano

| Risco | Efeito | Como tratamos |
|---|---|---|
| Pouco tempo para teste humano | Histórias acumulam sem aprovação | Sprints de 2 semanas sem mudar a ordem; roteiros curtos e objetivos |
| Crédito de US$ 100 do Azure | Ambientes param no meio do semestre | Escala a zero, banco parado fora de uso, alertas de orçamento (ver `infra/README.md`) |
| Decisões jurídicas pendentes | Termos e taxa travam sprints | Funcionalidades protegidas por parâmetro; desenvolvemos e ligamos depois |
| Virada de páginas não funcionar bem no celular | Diferencial da apresentação comprometido | PoC na S0, antes de qualquer tela do catálogo |
| Escopo do MVP grande para a disciplina | MVP completo não cabe no prazo | Marco M2 (S7) já entrega a jornada principal demonstrável |
