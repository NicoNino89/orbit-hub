# Serviços, base de conhecimento e comunidade

Este documento complementa a especificação do produto e o documento de [processos e operação](processos-e-operacao.md). Ele define **quem cadastra e quem aprova** os serviços oferecidos e os conteúdos educativos, e como a base de conhecimento se integra à comunidade (a parte "rede social" do Orbit Hub).

| Épico | Fase | Escopo |
|---|---|---|
| KAN-115 | Fase 2 | Marketplace de serviços |
| KAN-116 | Fase 2 | Aprender: base de conhecimento e educação |
| KAN-117 | Fase 3 | Comunidade integrada à base de conhecimento |

---

## 0. Princípios

1. **Tudo o que aparece para o usuário passa por curadoria.** Serviços, conteúdos e grupos oficiais têm sempre um responsável interno que aprova antes da publicação.
2. **Conhecimento e comunidade são uma coisa só.** Todo conteúdo pode gerar discussão, e as melhores discussões podem virar conteúdo. As duas partes usam a mesma taxonomia de temas.
3. **A comunidade não é canal de captação.** Oferta de participação, pedido de investimento público e divulgação de valores de rodada só acontecem no catálogo, com as regras dele (risco regulatório, ver CVM 88).
4. **Conteúdo educativo não é recomendação de investimento.** Nenhum conteúdo, resposta ou especialista faz recomendação personalizada.
5. **Prazos, limites e percentuais são parâmetros**, nunca valores fixos no código (mesma regra de [processos e operação](processos-e-operacao.md)).

---

## 1. Papéis

### 1.1 Papéis externos novos

| Papel | Quem é | Como entra |
|---|---|---|
| **Prestador de serviço** | Pessoa ou empresa que oferece serviços ao ecossistema (jurídico, contábil, valuation, marketing, tecnologia, mentoria paga) | Cadastro próprio + verificação (KAN-118) |
| **Autor / especialista** | Membro que publica conteúdo na base ou tem selo de especialista em um tema | Convite da curadoria ou candidatura aprovada (KAN-128, KAN-134) |
| **Moderador de grupo** | Membro indicado pela equipe para cuidar do dia a dia de um grupo | Indicação da equipe (KAN-137) |

### 1.2 Papéis internos novos

| Papel | Responsabilidade | Não pode |
|---|---|---|
| **Curador de serviços** | Aprovar prestadores e serviços, tratar reclamações e disputas de contratação, manter categorias em uso | Aprovar serviço de prestador com quem tem vínculo |
| **Curador de conteúdo** | Aprovar autores, revisar e publicar conteúdos, montar trilhas, criar grupos oficiais, conduzir o ciclo comunidade → base | Publicar o próprio conteúdo sem segunda revisão |
| **Gestor comercial** | Cadastrar os serviços próprios da plataforma, planos e assinaturas, propor preços e comissões | Alterar parâmetros financeiros sem aprovação do admin |

Os papéis existentes continuam valendo: **admin** (categorias, parâmetros e papéis), **moderador** (denúncias e sanções, agora também na comunidade), **suporte N1/N2** (atendimento sobre contratações e acesso a conteúdo), **DPO** (dados pessoais em conteúdos e mensagens) e **jurídico** (termos do marketplace e dos autores).

### 1.3 Quem cadastra o quê

| Item | Quem cadastra | Quem aprova | História |
|---|---|---|---|
| Categorias de serviço | Admin | — (segunda aprovação se apagar categoria com serviços) | KAN-119 |
| Perfil de prestador | O próprio prestador | Curador de serviços | KAN-118 |
| Serviço de prestador | O prestador | Curador de serviços | KAN-119 |
| Serviço próprio da plataforma e planos | Gestor comercial | Admin | KAN-120 |
| Comissão e regras de pagamento | Gestor comercial (proposta) | Admin | KAN-125 |
| Taxonomia de temas | Curador de conteúdo | Admin | KAN-127 |
| Conteúdo da base | Autor ou curador | Curador de conteúdo (segunda revisão se o autor for curador) | KAN-128 |
| Curso e trilha | Curador de conteúdo | Curador de conteúdo + revisão de outro curador | KAN-129 |
| Mentoria e workshop | Especialista ou prestador | Curador de conteúdo | KAN-130 |
| Selo de especialista | Candidatura do membro | Curador de conteúdo | KAN-134 |
| Grupo oficial | Curador de conteúdo | — | KAN-137 |
| Grupo proposto pela comunidade | Membro verificado | Curador de conteúdo | KAN-137 |

---

## 2. Marketplace de serviços (KAN-115)

### 2.1 Tipos de serviço

| Tipo | Exemplo | Quem oferece |
|---|---|---|
| Serviço de prestador | Due diligence jurídica, contabilidade, valuation, pitch deck | Prestadores verificados |
| Serviço próprio da plataforma | Destaque no catálogo, revisão de perfil, assinatura premium | Orbit Hub (gestor comercial) |
| Mentoria e workshop | Sessão individual, oficina em grupo | Especialistas e prestadores (detalhe no módulo Aprender) |

Serviços próprios **nunca** alteram a ordem de compatibilidade nem a curadoria do catálogo. Destaques pagos são sempre identificados como patrocinados.

### 2.2 Processo M1 — Entrada de prestador

1. O prestador se cadastra, informa CNPJ ou CPF, área de atuação e, quando a profissão é regulamentada, o registro profissional (OAB, CRC e afins).
2. O curador de serviços verifica documento e registro em até `marketplace.sla_verificacao_prestador_dias_uteis`.
3. Resultado: aprovado, pendente de ajuste ou recusado, sempre com motivo.
4. O prestador aceita o termo do marketplace (versionado pelo jurídico).

### 2.3 Processo M2 — Publicação de serviço

1. O prestador cadastra o serviço: categoria, descrição, entregáveis, prazo estimado, forma de preço (fixo, a partir de, sob proposta).
2. O curador revisa clareza, categoria e promessas proibidas (garantia de captação, garantia de retorno).
3. O serviço publicado é revisado a cada `marketplace.revisao_servico_meses` ou quando recebe reclamação.

### 2.4 Processo M3 — Contratação

```
Solicitação ─▶ Proposta ─▶ Aceita ─▶ Em execução ─▶ Entregue ─▶ Concluída
     │             │                       │             │
     ▼             ▼                       ▼             ▼
 Cancelada     Recusada/              Cancelada      Em disputa ─▶ Resolvida
               Expirada
```

- Propostas expiram em `marketplace.proposta_validade_dias` (KAN-122).
- O cliente confirma a entrega em até `marketplace.prazo_aceite_entrega_dias`; sem resposta, a entrega é considerada aceita (KAN-123).
- Disputas são tratadas pelo curador de serviços em até `marketplace.sla_disputa_dias_uteis`, com recurso ao N2 (KAN-126).

### 2.5 Avaliações e reputação (KAN-124)

- Só quem concluiu uma contratação avalia, uma vez por contratação.
- O prestador pode responder publicamente, mas não apagar a avaliação.
- A nota média só aparece a partir de `marketplace.avaliacoes_minimas` avaliações.

### 2.6 Pagamentos e comissão (KAN-125)

- O pagamento passa por um provedor externo, atrás de um **Adapter** (mesma regra das demais integrações).
- A comissão da plataforma é o parâmetro `marketplace.comissao_percentual`, aprovado pelo admin.
- O valor fica retido até a conclusão; em disputa, fica bloqueado até a decisão.
- A plataforma não guarda dados de cartão.

### 2.7 Atendimento

| Situação | Primeiro atendimento | Escalonamento |
|---|---|---|
| Dúvida sobre como contratar | Suporte N1 | — |
| Atraso ou problema na entrega | Suporte N1 | Curador de serviços |
| Disputa de pagamento | Curador de serviços | Suporte N2 + admin |
| Denúncia de prestador | Moderação | Curador de serviços |

---

## 3. Base de conhecimento — Aprender (KAN-116)

### 3.1 Modelo de conteúdo (KAN-127)

| Tipo | Descrição |
|---|---|
| Artigo / guia | Texto com fontes, autor e data de revisão |
| Verbete do glossário | Definição curta (cap, valuation, mútuo conversível, investidor líder…) |
| Aula e curso | Vídeo ou texto em módulos |
| Trilha | Sequência de conteúdos por objetivo ("Preparar a primeira rodada") |
| Modelo e ferramenta | Planilhas, modelos de documento, checklists |
| Relatório (Market Insights) | Dados agregados e anonimizados da plataforma (KAN-132) |

Todo conteúdo tem: temas da taxonomia comum, público (investidor, empreendedor, ambos), nível (introdutório, intermediário, avançado), autor, revisor e **data da próxima revisão**.

### 3.2 Processo C1 — Fluxo editorial (KAN-128)

```
Rascunho ─▶ Em revisão ─▶ Publicado ─▶ Em atualização ─▶ Publicado
                │                │
                ▼                ▼
          Devolvido ao       Arquivado
             autor
```

1. O autor escreve o rascunho e envia para revisão.
2. O curador revisa precisão técnica, linguagem, fontes e ausência de recomendação de investimento, em até `conhecimento.sla_revisao_dias_uteis`.
3. Conteúdo sobre regulação ou tributação passa também pelo jurídico.
4. Conteúdos vencidos (`conhecimento.revisao_periodica_meses`) entram na fila de atualização; sem atualização, são marcados como "possivelmente desatualizado".

### 3.3 Acesso (KAN-131)

| Modelo | Exemplo |
|---|---|
| Gratuito | Glossário, guias básicos |
| Pago avulso | Curso específico |
| Assinatura | Trilhas avançadas, Market Insights completo |

O autor recebe repasse conforme `conhecimento.repasse_autor_percentual`.

### 3.4 Market Insights (KAN-132)

- Só dados agregados e anonimizados.
- Nenhum recorte é publicado com menos de `insights.amostra_minima` registros, para impedir a identificação de startups ou investidores.
- Metodologia publicada junto com cada relatório.

### 3.5 Busca e recomendação (KAN-133)

- Uma busca unificada para conteúdos, perguntas, discussões, serviços e especialistas.
- A recomendação usa **Strategy** (mesmo padrão do matchmaking): cada critério (perfil, estágio da startup, trilha em andamento, temas seguidos) devolve sua justificativa.

---

## 4. Comunidade integrada (KAN-117)

### 4.1 Recursos

| Recurso | História | Ligação com a base |
|---|---|---|
| Feed e publicações | KAN-135 | Compartilhar conteúdo e serviço |
| Seguir perfis | KAN-136 | Seguir autores e especialistas |
| Grupos temáticos | KAN-137 | Grupos de trilha, conteúdos fixados |
| Perguntas e respostas | KAN-138 | Sugestão de conteúdo antes de perguntar; validação por especialista |
| Discussões nos conteúdos | KAN-139 | Comentário de erro vira caso para o curador |
| Ciclo comunidade → base | KAN-140 | Melhores respostas viram conteúdo, com crédito |
| Moderação e mensagens diretas | KAN-141 | — |

### 4.2 Ciclo do conhecimento

```
        ┌──────────── conteúdo publicado gera discussão ────────────┐
        │                                                           ▼
 ┌──────────────┐                                          ┌────────────────┐
 │    Base de   │                                          │   Comunidade   │
 │ conhecimento │                                          │ (feed, grupos, │
 │              │                                          │  perguntas)    │
 └──────────────┘                                          └────────────────┘
        ▲                                                           │
        └── curadoria transforma a melhor resposta em conteúdo ─────┘
                 (consentimento do autor + crédito + reputação)
```

### 4.3 Regras de conteúdo da comunidade

- Proibido oferecer participação, pedir investimento publicamente ou divulgar valores de captação.
- Proibido divulgar informação confidencial recebida na plataforma (Nível 2 e Data Room).
- Autopromoção de serviços é identificada e tem limite de frequência (`comunidade.limite_autopromocao_semana`).
- Seguir uma startup **não** é manifestação de interesse.
- A lista de quem um investidor segue é privada.

### 4.4 Reputação (KAN-134)

Pontos vêm de respostas aceitas, respostas validadas, conteúdos publicados e avaliações no marketplace. Reputação **não** influencia a compatibilidade do matchmaking nem a ordem do catálogo. Sanções da moderação podem zerar ou suspender pontos.

### 4.5 Mensagens diretas (decisão D1)

Ficam fora do MVP e entram na Fase 3, apenas entre usuários com conexão ativa, membros de um mesmo grupo que aceitaram mensagens, ou cliente e prestador numa contratação. A equipe só lê mensagens dentro de um caso de denúncia.

---

## 5. Rotinas automáticas novas

Continuam a numeração de [processos e operação](processos-e-operacao.md).

| Rotina | O que faz | Frequência |
|---|---|---|
| R15 | Expira propostas do marketplace vencidas | Diária |
| R16 | Aceita automaticamente entregas sem resposta no prazo e libera o pagamento | Diária |
| R17 | Coloca na fila de atualização os conteúdos com revisão vencida | Semanal |
| R18 | Sugere a especialistas as perguntas sem resposta | Diária |
| R19 | Arquiva grupos inativos (sinaliza para a curadoria) | Mensal |
| R20 | Recalcula reputação e selos | Diária |
| R21 | Gera o indicador mensal de conteúdos nascidos da comunidade | Mensal |

---

## 6. Parâmetros novos

| Parâmetro | Padrão sugerido | Dono |
|---|---|---|
| `marketplace.sla_verificacao_prestador_dias_uteis` | 5 | Curador de serviços |
| `marketplace.revisao_servico_meses` | 12 | Curador de serviços |
| `marketplace.proposta_validade_dias` | 10 | Gestor comercial |
| `marketplace.prazo_aceite_entrega_dias` | 7 | Gestor comercial |
| `marketplace.sla_disputa_dias_uteis` | 10 | Curador de serviços |
| `marketplace.avaliacoes_minimas` | 3 | Curador de serviços |
| `marketplace.comissao_percentual` | a definir | Admin |
| `conhecimento.sla_revisao_dias_uteis` | 10 | Curador de conteúdo |
| `conhecimento.revisao_periodica_meses` | 12 | Curador de conteúdo |
| `conhecimento.sla_candidato_dias_uteis` | 10 | Curador de conteúdo |
| `conhecimento.repasse_autor_percentual` | a definir | Admin |
| `insights.amostra_minima` | a definir | DPO |
| `comunidade.grupo_inativo_meses` | 6 | Curador de conteúdo |
| `comunidade.pergunta_sem_resposta_dias` | 7 | Curador de conteúdo |
| `comunidade.limite_novas_conversas` | a definir | Moderação |
| `comunidade.limite_autopromocao_semana` | a definir | Moderação |

Os valores "a definir" dependem de decisão de negócio e entram na lista de decisões em aberto.

---

## 7. Decisões em aberto

| # | Decisão | Quem decide |
|---|---|---|
| D11 | Percentual de comissão do marketplace e de repasse aos autores | Admin + gestor comercial |
| D12 | Provedor de pagamento e modelo de retenção do valor | Admin + jurídico |
| D13 | Amostra mínima do Market Insights | DPO |
| D14 | Se prestadores pessoa física sem CNPJ podem vender serviços | Jurídico |
| D15 | Se conteúdos pagos podem ser criados por qualquer autor ou só por especialistas com selo | Curador de conteúdo |

---

## 8. Módulos no back-end

| Módulo | Responsabilidade |
|---|---|
| `marketplace` | Prestadores, serviços, propostas, contratações, avaliações, pagamentos |
| `conhecimento` | Conteúdos, taxonomia, fluxo editorial, cursos, trilhas, acesso, Market Insights |
| `comunidade` | Feed, seguidores, grupos, perguntas e respostas, discussões, reputação, mensagens |

---

## 9. Rastreabilidade no Jira

| Épico | Histórias |
|---|---|
| KAN-115 Marketplace de serviços | KAN-118 a KAN-126 |
| KAN-116 Aprender | KAN-127 a KAN-134 |
| KAN-117 Comunidade | KAN-135 a KAN-141 |
