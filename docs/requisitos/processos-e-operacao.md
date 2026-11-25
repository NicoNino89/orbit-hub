# Processos, rotinas e operação da plataforma

**Complemento à Especificação de Requisitos v1.0**
**Data:** novembro de 2026
**Status:** proposta para validação do time

Os documentos anteriores descrevem **o que cada usuário faz**. Este documento descreve **como a plataforma é operada**: quem decide cada coisa, quais processos existem, quais rotinas rodam sozinhas e quais regras podem ser alteradas sem mudar o código.

---

## 0. Princípios

1. **Regra de negócio não fica presa no código.** Prazos, limites, listas e textos legais são **parâmetros configuráveis** (seção 5), alterados pelo administrador com justificativa e histórico.
2. **Todo processo tem dono.** Cada processo define gatilho, responsável, etapas, prazo (SLA), resultado e o que fazer nas exceções (seção 2).
3. **Toda rotina automática é visível.** Cada rotina tem frequência, parâmetros, registro de execução e alerta em caso de falha (seção 4).
4. **Toda decisão relevante é registrada.** Quem decidiu, quando, com qual justificativa e com direito a recurso, quando couber (seção 3).
5. **Segregação de funções.** Ninguém revisa o próprio caso, e ações graves exigem duas pessoas (seção 1.3).
6. **Indicação de fase.** Cada item indica se entra no **MVP** ou em fase posterior (**F2**, **F3**).

---

## 1. Atores

### 1.1. Usuários da plataforma

| Ator | Descrição | Fase |
|---|---|---|
| Visitante | Pessoa sem cadastro. Vê apenas páginas institucionais | MVP |
| Investidor | Pessoa física com o papel de investidor | MVP |
| Membro de organização investidora | Administrador, membro ou observador de um fundo, rede, CVC etc. | MVP |
| Empreendedor | Fundador administrador, fundador ou colaborador de uma startup | MVP |
| Investidor líder | Papel temporário, dentro de uma rodada | MVP |
| Anunciante | Empresa que veicula publicidade | F2 |
| Prestador de serviços | Profissional ou empresa do marketplace | F2 |
| Instituição parceira | Aceleradora, universidade, entidade empresarial | F3 |

### 1.2. Equipe interna (backoffice)

A especificação menciona apenas um "painel administrativo". Na prática, a operação exige papéis internos distintos:

| Papel interno | Responsabilidades | Fase |
|---|---|---|
| **Administrador da plataforma** | Gerencia usuários internos e seus papéis, parâmetros, listas e textos; acompanha filas e indicadores; executa ações excepcionais | MVP |
| **Analista de verificação** | Confere identidade de pessoas e dados de empresas (CNPJ, gestoras na CVM) | MVP |
| **Curador** | Revisa startups antes da publicação; monta e ordena as edições temáticas do catálogo | MVP |
| **Moderador** | Analisa denúncias, aplica sanções, julga recursos de outros moderadores | MVP |
| **Suporte** | Atende dúvidas, problemas de acesso e disputas entre usuários | MVP |
| **Encarregado de dados (DPO)** | Responde às solicitações de titulares e conduz incidentes de dados pessoais (exigência da LGPD) | MVP |
| **Jurídico / compliance** | Publica termos e políticas, avalia casos com risco legal e regulatório | MVP |
| **Gestor de publicidade** | Cadastra anunciantes, aprova campanhas e acompanha a veiculação | F2 |
| **Curador de conteúdo educacional** | Publica cursos, trilhas e materiais | F2 |

Uma mesma pessoa pode acumular papéis em um time pequeno, desde que as regras de segregação abaixo sejam respeitadas.

### 1.3. Regras de segregação e controle

- Usuários internos **não usam a mesma conta** como investidor ou empreendedor. Se uma pessoa da equipe também investir, ela usa uma conta separada.
- Ninguém verifica, revisa ou modera um caso em que tenha interesse (por exemplo, a própria organização ou uma startup em que investe). O sistema bloqueia a atribuição do caso.
- **Ações que exigem duas pessoas** (a primeira propõe, a segunda aprova):
  - banimento definitivo de usuário ou organização;
  - remoção de uma startup publicada por fraude;
  - alteração de parâmetros que afetam prazos legais ou de privacidade;
  - exportação ou exclusão de dados pessoais fora do fluxo normal.
- Todo acesso de usuário interno a dados confidenciais (Nível 2) é **justificado e registrado**. O acesso só acontece dentro de um caso aberto (denúncia, disputa, solicitação do titular).
- O sistema registra **todas** as ações de usuários internos em uma trilha de auditoria imutável.

### 1.4. Quem cadastra o quê

| Cadastro | Quem faz | Como |
|---|---|---|
| Investidor e empreendedor | O próprio usuário | Autoatendimento |
| Organização investidora | Um investidor da organização | Autoatendimento, com verificação |
| Startup | Um fundador | Autoatendimento, com curadoria |
| Organização convidada (fundo, rede, instituição parceira) | Administrador ou atendimento N2 | **Convite institucional**: a equipe cria a organização em rascunho e convida o responsável, que conclui o cadastro e aceita os termos pessoalmente |
| Usuário interno | **Somente o administrador** | Pelo painel. O primeiro administrador é criado na implantação, por procedimento técnico documentado |
| Anunciante (F2) | O próprio anunciante ou o gestor de publicidade | Autoatendimento ou convite, com verificação |

Regras:

- A equipe **nunca** aceita termos, define senhas ou preenche dados sensíveis em nome de um usuário.
- **Transferência de titularidade:** quando o único administrador de uma startup ou organização fica indisponível, outro membro solicita a titularidade pelo atendimento. Exige comprovação, análise do N2 e segunda aprovação.
- **Desligamento de usuário interno:** revoga o acesso imediatamente e devolve os casos abertos para a fila.
- **Revisão de acessos internos** a cada `acesso.revisao_meses` (padrão: 3).

### 1.5. Times internos e gestão das equipes

| Time | Filas | Líder do time |
|---|---|---|
| Verificação | Verificação de pessoas e empresas, revalidações | Acompanha a fila e julga recursos de verificação |
| Curadoria | Revisão de startups, edições temáticas | Acompanha a fila e julga recursos de curadoria |
| Moderação | Denúncias, violações de confidencialidade | Acompanha a fila e julga recursos de moderação |
| Atendimento (N1 e N2) | Chamados, cadastros assistidos, titularidade, disputas | Mantém respostas padrão e a central de ajuda |
| Privacidade | Solicitações de titulares, incidentes | Encarregado de dados |
| Publicidade (F2) | Anunciantes e campanhas | Gestor de publicidade |

Regras de gestão:

- Cada time tem **horário de atendimento** e calendário de feriados (`equipe.horario_atendimento`). Prazos em dias e horas úteis seguem esse calendário.
- **Distribuição de casos:** por rodízio entre os membros disponíveis ou manual pelo líder, configurável por fila. Conflitos de interesse bloqueiam a atribuição.
- **Ausências e férias:** quem está indisponível não recebe casos novos, e os abertos podem ser transferidos.
- **Escalonamento:** casos com 80% do prazo consumido alertam o líder; casos vencidos sobem para o administrador.

---

## 2. Processos

Formato de cada processo: **gatilho → responsável → etapas → prazo → resultado → exceções**.

### P1. Cadastro e onboarding (MVP)

- **Gatilho:** visitante inicia o cadastro.
- **Responsável:** o próprio usuário. Suporte em caso de problema.
- **Etapas:** dados básicos → confirmação de e-mail → aceite dos termos vigentes → escolha de papéis → onboarding de cada papel (tese do investidor ou cadastro da startup).
- **Prazo:** o link de confirmação de e-mail vale por `cadastro.link_confirmacao_horas`.
- **Resultado:** conta ativa, com papéis e aceites registrados.
- **Exceções:** e-mail não confirmado depois de `cadastro.prazo_confirmacao_dias` → cadastro removido automaticamente (rotina R12).

### P2. Verificação de identidade e de empresas (MVP)

- **Gatilho:** usuário solicita verificação, ou o sistema exige (por exemplo, para ver dados de rodada).
- **Responsável:** analista de verificação.
- **Etapas:**
  1. O usuário envia os documentos exigidos para o seu tipo (lista configurável).
  2. O caso entra na fila de verificação, por ordem de chegada.
  3. O analista confere os dados e decide: **verificado**, **pendente de complemento** (com motivo) ou **recusado** (com motivo).
  4. O usuário é notificado e, se recusado, pode recorrer uma vez (P11).
- **Prazo:** `verificacao.sla_dias_uteis` (padrão: 2 dias úteis).
- **Revalidação:** a verificação vence após `verificacao.validade_meses` (padrão: 12). O usuário é avisado antes do vencimento (rotina R9).
- **Exceções:** indício de fraude → o caso vai para a moderação (P8), e a conta pode ser suspensa preventivamente.

### P3. Curadoria e publicação de startups (MVP)

- **Gatilho:** fundador administrador solicita a publicação, ou faz uma alteração relevante numa startup publicada.
- **Responsável:** curador.
- **Etapas:**
  1. A startup entra na fila de curadoria, por ordem de chegada.
  2. O curador aplica o **checklist de curadoria** (configurável): campos obrigatórios, coerência das informações, identidade dos fundadores verificada, CNPJ (se houver), adequação do conteúdo.
  3. Decisão: **aprovada** (publicada), **devolvida para ajustes** (com comentários por campo) ou **recusada** (com motivo, por exemplo, fraude ou atividade ilícita).
- **Prazo:** `curadoria.sla_dias_uteis` (padrão: 3 dias úteis).
- **Resultado:** startup publicada, com o nome do curador e a data registrados.
- **Exceções:** recusa por fraude exige segunda aprovação (seção 1.3) e abre um caso de moderação.

### P4. Edições do catálogo e ordem de exibição (MVP)

A pergunta "**quem define a ordem**" tem respostas diferentes para cada tipo de lista:

| Lista | Quem define a composição | Quem define a ordem |
|---|---|---|
| **Seleção personalizada** | Algoritmo de compatibilidade (KAN-39) | Algoritmo: quantidade de critérios atendidos, depois a data mais recente. A regra é publicada para os usuários |
| **Novas oportunidades** | Automático: startups publicadas nos últimos `catalogo.novas_dias` | Data de publicação, da mais recente para a mais antiga |
| **Edições temáticas** (FinTech, AgTech etc.) | **Curador** | **Curador**, manualmente, ou por uma regra escolhida por ele (por exemplo, por data) |
| **Edições patrocinadas** | Curador, com o gestor de publicidade | Curador. O patrocinador **não** escolhe nem ordena as startups (F2) |

Regras das edições temáticas:

- Cada edição tem título, descrição, critérios de inclusão, **período de vigência** e curador responsável.
- Frequência sugerida: `catalogo.frequencia_edicoes` (padrão: mensal).
- Uma startup só entra numa edição se estiver publicada e se o empreendedor não tiver optado por ficar fora de edições.
- Ao fim da vigência, a edição é arquivada automaticamente (rotina R10).
- Toda mudança de composição ou ordem fica registrada, com o curador responsável.

### P5. Manifestação de interesse e resposta (MVP)

- **Gatilho:** investidor manifesta interesse (KAN-45).
- **Responsável pela resposta:** fundador administrador da startup.
- **Prazo e lembretes:**
  - lembrete ao empreendedor em `conexao.lembrete_dias` (padrão: D+7 e D+21);
  - expiração automática em `conexao.expiracao_dias` (padrão: D+30), com aviso ao investidor (rotina R1).
- **Resultado:** Match ou recusa (KAN-48), ou expiração.

### P6. Reuniões entre investidor e empreendedor (MVP)

A especificação não trata de reuniões, mas elas são o centro da relação depois do Match.

**Tipos de reunião**

| Tipo | Quando acontece | Quem pode propor |
|---|---|---|
| Apresentação | Depois do Match | Qualquer uma das partes |
| Due diligence | Durante a análise (conexão "em análise") | Qualquer uma das partes |
| Negociação | Durante a negociação | Qualquer uma das partes |
| Reunião da rodada | Rodada aberta, com vários participantes | Fundador administrador ou investidor líder |
| Pitch Night / evento | Evento aberto a vários investidores | Administrador da plataforma ou instituição parceira (F3) |

**Fluxo de agendamento**

1. Quem propõe escolhe o tipo, o formato (vídeo ou presencial), a pauta e **até 3 opções de horário**.
2. A outra parte **aceita** um horário, **propõe outros** ou **recusa** (com motivo).
3. Confirmado o horário, as duas partes recebem o convite (arquivo `.ics`) e o link ou endereço.
4. Lembretes automáticos em `reuniao.lembretes` (padrão: 24 h e 1 h antes) (rotina R5).
5. **Depois da reunião**, cada parte registra o resultado: realizada, não compareceu, remarcada ou cancelada, e o **próximo passo** (seguir para análise, seguir para negociação, encerrar).
6. O registro de "realizada" pode avançar o estado da conexão (por exemplo, de Match para "em análise"), com confirmação de quem registrou.

**Regras**

- Só é possível propor reuniões em conexões ativas (Match, em análise ou em negociação) ou dentro de uma rodada.
- Cancelamento com menos de `reuniao.antecedencia_cancelamento_horas` (padrão: 24 h) fica registrado como **cancelamento tardio**.
- Ausências repetidas (`reuniao.limite_ausencias`, padrão: 2) geram um alerta ao suporte, que pode advertir o usuário.
- Propostas sem resposta expiram quando o primeiro horário sugerido passa.
- No MVP, a plataforma **não hospeda videochamadas**: o link é de uma ferramenta externa. Integração com Google Agenda e Outlook fica para F3.
- A pauta e as anotações de cada parte são privadas. Nada do que é registrado na reunião é compartilhado sem ação explícita de quem escreveu.

### P7. Condução e encerramento da rodada (MVP)

**Quem faz o quê**

| Ação | Quem faz | Observação |
|---|---|---|
| Abrir a rodada | Fundador administrador | Exige CNPJ validado |
| Convidar o líder | Fundador administrador | O convidado precisa aceitar |
| Convidar co-investidores | Investidor líder | Exige aprovação do empreendedor |
| Convocar reuniões da rodada | Fundador administrador ou líder | P6 |
| Prorrogar o prazo | Fundador administrador | Até `rodada.max_prorrogacoes` vezes (padrão: 1), por até `rodada.prorrogacao_max_dias` (padrão: 30). Os participantes são avisados |
| **Declarar o encerramento** | **Fundador administrador** | O líder pode **solicitar** o encerramento, mas quem declara é o empreendedor |
| Cancelar a rodada | Fundador administrador | Com motivo. Os participantes são avisados |
| Definir a alocação em excesso de demanda | Fundador administrador, junto com o líder, fora da plataforma no MVP | Ver regra de alocação abaixo |

**Encerramento automático**

- Quando o prazo da rodada vence, ela passa para **"aguardando resultado"** (rotina R6), e novos participantes não podem entrar.
- O empreendedor tem `rodada.prazo_registro_resultado_dias` (padrão: 15) para registrar o resultado (KAN-64), com lembretes.
- Sem registro nesse prazo, a rodada é marcada como **"encerrada sem resultado informado"**. Isso não conta como investimento concretizado e fica visível no histórico da startup.

**Regra de alocação recomendada (excesso de demanda)**

Como não há padrão único no mercado, a plataforma **registra** a política escolhida pelo empreendedor ao abrir a rodada, para dar transparência aos participantes:

- **Líder primeiro**, depois os demais por ordem de entrada na rodada (recomendada);
- proporcional às intenções;
- critério do empreendedor (discricionário).

A política escolhida aparece para todos os participantes.

**Confirmação do resultado**

- Cada investidor indicado tem `rodada.prazo_confirmacao_dias` (padrão: 15) para confirmar ou contestar a participação (rotina R7).
- Sem resposta no prazo, a participação fica como **"não confirmada"** e não conta como investimento concretizado.
- Contestação → abre uma **disputa** (P10).

### P8. Denúncias e moderação (MVP)

**O que pode ser denunciado**

| Objeto | Fase |
|---|---|
| Perfil de usuário ou organização | MVP |
| Startup publicada | MVP |
| Comportamento em reunião ou conexão (assédio, má-fé) | MVP |
| Violação de confidencialidade | MVP |
| Publicação, comentário ou grupo da comunidade | F3 |
| Mensagem direta | F3 |
| Anúncio | F2 |

**Categorias** (lista configurável): fraude ou golpe, informação falsa ou enganosa, violação de confidencialidade, assédio ou discriminação, spam, conteúdo ilegal, oferta irregular de valores mobiliários, outro.

**Fluxo**

1. O usuário denuncia, escolhendo categoria e descrição. O denunciante não é identificado ao denunciado.
2. O sistema atribui **prioridade** pela categoria:

   | Prioridade | Exemplos | Prazo de análise |
   |---|---|---|
   | Crítica | Fraude, conteúdo ilegal, vazamento de dados | `moderacao.sla_critica_horas` (padrão: 24 h) |
   | Alta | Violação de confidencialidade, assédio, oferta irregular | `moderacao.sla_alta_horas` (padrão: 48 h) |
   | Normal | Spam, informação imprecisa | `moderacao.sla_normal_dias_uteis` (padrão: 5 dias úteis) |

3. O moderador analisa o caso, podendo consultar as trilhas de auditoria (seção 1.3).
4. Decisão, com motivo registrado:
   - sem ação;
   - remoção ou correção do conteúdo;
   - **advertência**;
   - **suspensão temporária** (`moderacao.suspensao_dias`, padrão: 7, 30 ou 90 dias, conforme a gravidade);
   - **banimento** (exige segunda aprovação).
5. O denunciado é notificado da decisão e do motivo. O denunciante recebe apenas a informação de que o caso foi analisado.

**Sanções progressivas** (padrão configurável): advertência → suspensão de 7 dias → suspensão de 30 dias → banimento. Casos graves (fraude, conteúdo ilegal) podem ir direto para banimento.

**Medida preventiva:** em casos críticos, o moderador pode suspender a conta ou despublicar a startup **antes** da decisão final, com registro do motivo.

**Efeitos da suspensão ou do banimento**

- Investidor: perde acessos ao Nível 2, conexões pendentes expiram e ele sai das rodadas abertas.
- Startup: sai do catálogo, e as rodadas abertas ficam suspensas, com aviso aos participantes.

### P9. Violação de confidencialidade (MVP)

- **Gatilho:** empreendedor denuncia vazamento de informação confidencial.
- **Responsáveis:** moderador (análise) e jurídico (avaliação legal).
- **Etapas:**
  1. Abre um caso com prioridade alta.
  2. O moderador levanta quem teve acesso ao Nível 2 da startup e quando (KAN-33).
  3. Os acessos do investidor suspeito ao Nível 2 de **todas** as startups são suspensos preventivamente.
  4. O jurídico avalia se cabe medida com base no termo de confidencialidade ou no NDA (KAN-63).
  5. Decisão e sanção conforme P8.
- **Limite:** a plataforma fornece os registros de acesso, mas não decide a responsabilidade civil entre as partes.

### P10. Disputas entre usuários (MVP)

- **Exemplos:** participação contestada no resultado da rodada, desacordo sobre reuniões, cobrança de compromisso informal.
- **Responsável:** suporte, com apoio do jurídico quando necessário.
- **Regra:** a plataforma **media e registra**, mas **não arbitra** questões contratuais entre as partes. Investimentos são combinados fora da plataforma, e a plataforma deixa isso claro (aviso de natureza não vinculante).
- **Resultado possível:** correção do registro, se as duas partes concordarem; caso contrário, o item fica marcado como **"em disputa"** e não conta como investimento concretizado.

### P11. Recursos (MVP)

- O usuário pode recorrer **uma vez** de: recusa de verificação, recusa de curadoria, sanção de moderação.
- Prazo para recorrer: `recurso.prazo_dias` (padrão: 15).
- O recurso é julgado por **outra pessoa** do mesmo papel, ou pelo administrador.
- Prazo de resposta: `recurso.sla_dias_uteis` (padrão: 5 dias úteis).
- A decisão do recurso é final na plataforma.

### P12. Atendimento e serviços ao usuário (MVP)

**Catálogo de serviços**

A plataforma publica, na central de ajuda, o catálogo dos serviços que oferece, com público, responsável, canal e prazo:

| Serviço | Para quem | Quem presta | Canal | Prazo |
|---|---|---|---|---|
| Onboarding e orientação de uso | Todos | Atendimento N1 | Central de ajuda e chamado | `suporte.sla_primeira_resposta_horas` |
| Verificação de identidade e de empresa | Investidores, organizações, fundadores, startups | Verificação | Fluxo de verificação | `verificacao.sla_dias_uteis` |
| Curadoria da startup | Empreendedores | Curadoria | Fluxo de publicação | `curadoria.sla_dias_uteis` |
| Inclusão em edições temáticas | Empreendedores | Curadoria | Automático, com opção de sair | Por edição |
| Cadastro assistido de organização | Fundos, redes, instituições | Atendimento N2 | Convite institucional | `suporte.sla_n2_dias_uteis` |
| Recuperação de acesso e titularidade | Todos | Atendimento N2 | Chamado | `suporte.sla_n2_dias_uteis` |
| Mediação de disputas | Investidores e empreendedores | Atendimento N2 e jurídico | Chamado | `disputa.sla_dias_uteis` |
| Análise de denúncias | Todos | Moderação | Botão "denunciar" | Por prioridade |
| Recursos | Todos | Mesmo time, outra pessoa | Fluxo de recurso | `recurso.sla_dias_uteis` |
| Direitos do titular (LGPD) | Todos | Encarregado de dados | Canal de privacidade | `lgpd.prazo_resposta_dias` |

Na F2 entram o atendimento ao anunciante, os serviços do marketplace e a capacitação. Na F3, eventos e Pitch Nights.

**Modelo de atendimento**

| Nível | Quem | O que resolve |
|---|---|---|
| Autoatendimento | Central de ajuda | Dúvidas comuns |
| N1 | Atendimento | Uso da plataforma, acessos simples, orientação sobre verificação e curadoria |
| N2 | Atendimento sênior | Recuperação de conta, titularidade, cadastro assistido, disputas, casos que o N1 não resolveu |
| Especialistas | Moderação, privacidade, jurídico, equipe técnica | Denúncias, LGPD, questões legais e falhas técnicas |

**Regras**

- **Canais no MVP:** formulário na plataforma (usuário identificado), e-mail e formulário público para quem perdeu o acesso, com confirmação de identidade.
- **Ciclo do chamado:** aberto → em atendimento → aguardando usuário → resolvido → fechado. Sem resposta do usuário em `suporte.prazo_aguardando_dias` (padrão: 7), o chamado é fechado.
- **Prazos:** primeira resposta em `suporte.sla_primeira_resposta_horas` (padrão: 24 h úteis); N2 em `suporte.sla_n2_dias_uteis` (padrão: 3 dias úteis).
- **Encaminhamentos:** privacidade → encarregado de dados; questão legal → jurídico; denúncia → moderação; falha técnica → equipe técnica.
- **Satisfação:** ao fechar, o usuário avalia o atendimento. A nota entra nos indicadores.
- **Base de conhecimento:** o time de atendimento mantém a central de ajuda e as respostas padrão, criando artigos a partir dos chamados mais frequentes.
- O atendimento não acessa dados confidenciais fora de um caso aberto e justificado.

### P13. Privacidade e LGPD (MVP)

**Solicitações do titular** (acesso, correção, portabilidade, exclusão, revogação de consentimento)

- Canal próprio, com confirmação de identidade.
- Responsável: encarregado de dados.
- Prazo: a LGPD prevê resposta imediata em formato simplificado, ou em até **15 dias** para a declaração completa. Parâmetro: `lgpd.prazo_resposta_dias`.
- A exclusão segue a KAN-17. Dados que precisam ser guardados por obrigação legal ficam retidos de forma restrita, pelo prazo de `lgpd.retencao_legal_meses`.

**Incidentes de segurança com dados pessoais**

1. Quem detectar comunica imediatamente o encarregado e a equipe técnica.
2. Contenção técnica e avaliação de risco.
3. Se houver risco ou dano relevante aos titulares, comunicação à ANPD e aos titulares no prazo definido pela regulamentação da ANPD.
4. Registro do incidente, das medidas e das lições aprendidas.

**Retenção de dados** (parâmetros `retencao.*`)

| Dado | Retenção sugerida |
|---|---|
| Histórico de visualizações do investidor | 12 meses |
| Documentos de verificação | Enquanto a verificação for válida, mais o prazo legal |
| Trilhas de auditoria | Prazo definido pelo jurídico |
| Contas não confirmadas | `cadastro.prazo_confirmacao_dias` |

### P14. Termos, políticas e avisos (MVP)

- **Documentos versionados:** Termos de Uso, Política de Privacidade, Termo de Confidencialidade da Plataforma, Política de Moderação e, na F2, Política de Publicidade.
- **Responsáveis:** jurídico redige; administrador publica.
- **Nova versão:** publicação com data de vigência; os usuários aceitam a nova versão no próximo acesso. Para o termo de confidencialidade, o aceite é exigido antes do próximo acesso ao Nível 2.
- **Avisos padronizados** (por exemplo, "não representa compromisso de investimento") também são textos versionados e configuráveis.

### P15. Gestão de parâmetros e listas (MVP)

- **Quem altera:** administrador da plataforma. Parâmetros sensíveis (seção 1.3) exigem segunda aprovação.
- **Como:** pelo painel administrativo, com justificativa obrigatória.
- **Histórico:** toda alteração registra valor anterior, novo valor, autor, data e justificativa.
- **Efeito:** alterações valem **daqui para frente**. Prazos já iniciados seguem o valor da época, salvo decisão explícita registrada.
- **Listas gerenciáveis:** segmentos, localizações, tipos de investidor, instrumentos, estágios, motivos de encerramento, categorias de denúncia, checklist de curadoria, documentos de verificação.

### P16. Qualidade e higiene da base (MVP)

| Situação | Rotina | Ação |
|---|---|---|
| Indicador de tração com mais de `startup.indicador_desatualizado_meses` (padrão: 6) | R8 | Marca como desatualizado e avisa o empreendedor |
| Startup publicada sem nenhuma atualização há `startup.inatividade_meses` (padrão: 9) | R8 | Aviso; sem resposta em 30 dias, despublicação automática (pode ser republicada) |
| Conta sem acesso há `conta.inatividade_meses` (padrão: 12) | R11 | Aviso; sem resposta, a conta é desativada (os dados seguem a política de retenção) |
| Organização sem administrador ativo | R11 | Alerta ao suporte |

### P17. Publicidade (F2)

A especificação define os formatos de anúncio, mas não diz **onde as campanhas são cadastradas**, quem as aprova nem como são controladas.

**Cadastro**

- O **anunciante** é uma empresa verificada (P2), com conta própria de anunciante.
- A **campanha** registra:
  - objetivo e período;
  - formato (página patrocinada, anúncio compacto ou edição patrocinada);
  - segmentação (interesses, categorias, tipo de usuário);
  - limite de frequência por usuário;
  - orçamento e forma de cobrança;
  - criativos (imagens, textos e link de destino).
- Tudo fica no módulo `publicidade`, com uma **biblioteca de anúncios** pública aos usuários cadastrados: todo anúncio veiculado fica consultável, com o anunciante identificado.

**Aprovação**

1. O anunciante envia a campanha.
2. O gestor de publicidade confere a **Política de Publicidade**:
   - categorias proibidas (por exemplo, promessas de rentabilidade, ofertas de valores mobiliários fora de plataforma regulada, conteúdo enganoso);
   - identificação obrigatória como publicidade;
   - link de destino seguro.
3. Decisão: aprovada, devolvida para ajustes ou recusada.
4. Prazo: `publicidade.sla_aprovacao_dias_uteis` (padrão: 3 dias úteis).

**Veiculação e controle**

- Anúncios aparecem **apenas entre** as páginas do catálogo, nunca dentro da apresentação de uma startup.
- Limite de frequência por usuário: `publicidade.frequencia_max`.
- O usuário pode ocultar um anúncio e denunciá-lo (P8).
- A rotina R13 inicia e encerra campanhas conforme o período e consolida as métricas (impressões, cliques).
- O anunciante acompanha um relatório da campanha. Dados pessoais dos usuários **nunca** são entregues ao anunciante, apenas métricas agregadas.

**Separação de recomendação e publicidade**

- Patrocínio nunca altera a seleção personalizada nem a ordem das edições não patrocinadas.
- Uma **startup patrocinada** é identificada de forma diferente de um anúncio de serviço.
- Recomendação: startups patrocinadas **não exibem dados de captação** no espaço patrocinado, para evitar caracterização de oferta pública.

---

## 3. Matriz de decisões

| Decisão | Quem decide | Quem pode contestar | Registro |
|---|---|---|---|
| Aprovar verificação | Analista de verificação | O próprio usuário (recurso) | Caso de verificação |
| Publicar startup | Curador | Fundador administrador (recurso) | Caso de curadoria |
| Compor e ordenar edições temáticas | Curador | Administrador | Histórico da edição |
| Ordenar a seleção personalizada | Algoritmo, com regra publicada | — | Regra versionada |
| Aceitar ou recusar interesse (Match) | Fundador administrador | — | Conexão |
| Liberar ou revogar o Nível 2 | Fundador administrador | — | Histórico de acesso |
| Definir o investidor líder | Fundador administrador, com aceite do líder | — | Rodada |
| Declarar o encerramento da rodada | Fundador administrador (ou rotina, por prazo) | Participantes (disputa) | Rodada |
| Confirmar participação no investimento | Cada investidor | Empreendedor (disputa) | Resultado da rodada |
| Aplicar sanção | Moderador; banimento com segunda aprovação | O sancionado (recurso) | Caso de moderação |
| Alterar parâmetros e listas | Administrador; sensíveis com segunda aprovação | — | Histórico de parâmetros |
| Publicar nova versão de termos | Jurídico e administrador | — | Versão do documento |
| Responder ao titular de dados | Encarregado de dados | ANPD | Caso LGPD |
| Aprovar campanha publicitária (F2) | Gestor de publicidade | Anunciante (recurso) | Caso de campanha |

---

## 4. Rotinas automáticas

Toda rotina deve ser **idempotente** (rodar duas vezes não causa efeito duplicado), registrar cada execução e disparar alerta em caso de falha (KAN-56).

| Rotina | Frequência | O que faz | Parâmetros |
|---|---|---|---|
| R1 – Expirar manifestações | Diária | Expira conexões pendentes e avisa o investidor | `conexao.expiracao_dias` |
| R2 – Lembretes de resposta | Diária | Lembra o empreendedor de manifestações pendentes | `conexao.lembrete_dias` |
| R3 – Expirar acessos ao Nível 2 | De hora em hora | Encerra liberações vencidas | `nivel2.validade_dias` |
| R4 – Avisar vencimento do Nível 2 | Diária | Avisa o empreendedor e o investidor antes do vencimento | `nivel2.aviso_dias` |
| R5 – Lembretes de reunião | A cada 15 minutos | Envia lembretes e expira propostas sem resposta | `reuniao.lembretes` |
| R6 – Rodadas vencidas | Diária | Passa rodadas vencidas para "aguardando resultado" e cobra o registro | `rodada.prazo_registro_resultado_dias` |
| R7 – Confirmação de participação | Diária | Cobra confirmações e marca as não confirmadas no prazo | `rodada.prazo_confirmacao_dias` |
| R8 – Qualidade das startups | Diária, às 6h15 | Lembra a equipe dos indicadores de tração desatualizados, no máximo uma vez a cada 30 dias (KAN-24); avisa os fundadores administradores das startups publicadas sem atualização e, sem atualização em 30 dias, despublica (KAN-27) | `startup.indicador_desatualizado_meses`, `startup.inatividade_meses` |
| R9 – Revalidação de verificação | Diária, às 6h | Encerra pedidos sem complemento no prazo, avisa antes do vencimento, vence verificações e apaga os documentos depois da guarda (KAN-83) | `verificacao.validade_meses`, `verificacao.aviso_revalidacao_dias`, `verificacao.prazo_complemento_dias`, `verificacao.retencao_documentos_meses` |
| R10 – Edições do catálogo | Diária, às 0h05 | Põe no ar as edições agendadas que começaram e arquiva as que terminaram (KAN-85) | — |
| R11 – Contas inativas | Semanal | Avisa e desativa contas inativas; alerta organizações sem administrador | `conta.inatividade_meses` |
| R12 – Limpeza e retenção | Diária | Remove cadastros não confirmados e aplica a retenção de dados | `retencao.*`, `cadastro.*` |
| R13 – Campanhas (F2) | De hora em hora | Inicia e encerra campanhas; consolida métricas | `publicidade.*` |
| R14 – Monitor de prazos (SLA) | De hora em hora | Alerta sobre casos de verificação, curadoria, moderação, suporte e LGPD próximos do prazo | `*.sla_*` |
| R15 – Propostas paradas | De hora em hora | Alerta o administrador das propostas de segunda aprovação sem decisão no prazo (KAN-81) | `aprovacao.prazo_horas` |
| R16 – Revisão de acessos | Diária | Lembra o administrador quando a revisão dos acessos da equipe vence (KAN-75) | `acesso.revisao_meses` |
| R17 – Envio dos avisos por e-mail | A cada minuto | Envia os e-mails da central de notificações e tenta de novo os que falharam (1, 5, 15, 60 minutos, depois de 6 em 6 horas), até desistir (KAN-146) | `notificacoes.max_tentativas_email` |
| R18 – Resumo diário dos avisos | Diária, às 7h | Junta num único e-mail os avisos de quem escolheu o resumo; avisos obrigatórios não entram (KAN-146) | — |
| R19 – Limpeza das notificações | Diária, às 3h30 | Apaga as notificações mais antigas que a retenção (KAN-146) | `notificacoes.retencao_dias` |
| R20 – Chamados parados | Diária, às 4h | Fecha os chamados que esperam o usuário ou foram resolvidos há mais que o prazo (KAN-89) | `suporte.prazo_aguardando_dias` |

O mecanismo comum (agendamento no horário de Brasília, trava entre instâncias, registro de cada execução, pausa, execução manual e alertas de falha e de atraso) está em `compartilhado.rotinas` (KAN-104). Em produção, as rotinas só rodam com a API ligada: com escala a zero, nenhuma réplica fica no ar para disparar o horário.

A central de notificações (KAN-146) é o único caminho de aviso para as pessoas: os módulos chamam `NotificacoesService.notificar`, na mesma transação do evento, e o serviço grava o aviso no sino e/ou põe o e-mail na fila conforme a preferência da pessoa. O envio é feito pela R17, então a ação nunca espera nem falha por causa do e-mail. Avisos obrigatórios (segurança da conta, termos atualizados, prazos que encerram conexão e avisos à equipe) chegam sempre pelos dois canais, na hora. As rotinas dos serviços e do conhecimento (fase futura) continuam a numeração a partir da R21.

---

## 5. Parâmetros configuráveis

| Parâmetro | Padrão | Sensível |
|---|---|---|
| `cadastro.link_confirmacao_horas` | 24 | Não |
| `cadastro.prazo_confirmacao_dias` | 7 | Não |
| `verificacao.sla_dias_uteis` | 2 | Não |
| `verificacao.validade_meses` | 12 | Não |
| `verificacao.documento_max_mb` | 5 | Não |
| `verificacao.aviso_revalidacao_dias` | 30 | Não |
| `verificacao.retencao_documentos_meses` | 6 (provisório) | Sim |
| `curadoria.sla_dias_uteis` | 3 | Não |
| `catalogo.novas_dias` | 30 | Não |
| `catalogo.frequencia_edicoes` | Mensal | Não |
| `catalogo.capa_max_mb` | 2 | Não |
| `conexao.lembrete_dias` | 7 e 21 | Não |
| `conexao.expiracao_dias` | 30 | Não |
| `conexao.nova_tentativa_dias` | 90 | Não |
| `nivel2.validade_dias` | 90 | Sim |
| `nivel2.aviso_dias` | 7 | Não |
| `reuniao.lembretes` | 24 h e 1 h | Não |
| `reuniao.antecedencia_cancelamento_horas` | 24 | Não |
| `reuniao.limite_ausencias` | 2 | Não |
| `rodada.max_prorrogacoes` | 1 | Não |
| `rodada.prorrogacao_max_dias` | 30 | Não |
| `rodada.prazo_registro_resultado_dias` | 15 | Não |
| `rodada.prazo_confirmacao_dias` | 15 | Não |
| `startup.indicador_desatualizado_meses` | 6 | Não |
| `startup.inatividade_meses` | 9 | Não |
| `conta.inatividade_meses` | 12 | Não |
| `moderacao.sla_critica_horas` | 24 | Não |
| `moderacao.sla_alta_horas` | 48 | Não |
| `moderacao.sla_normal_dias_uteis` | 5 | Não |
| `moderacao.suspensao_dias` | 7, 30 e 90 | Não |
| `recurso.prazo_dias` | 15 | Não |
| `recurso.sla_dias_uteis` | 5 | Não |
| `suporte.sla_primeira_resposta_horas` | 24 úteis | Não |
| `suporte.sla_n2_dias_uteis` | 3 | Não |
| `suporte.prazo_aguardando_dias` | 7 | Não |
| `disputa.sla_dias_uteis` | 10 | Não |
| `verificacao.prazo_complemento_dias` | 15 | Não |
| `reuniao.antecedencia_minima_horas` | 24 | Não |
| `reuniao.max_contrapropostas` | 3 | Não |
| `reuniao.prazo_registro_dias` | 3 | Não |
| `convite.validade_dias` | 14 | Não |
| `acesso.revisao_meses` | 3 | Sim |
| `aprovacao.prazo_horas` | 48 | Não |
| `notificacoes.retencao_dias` | 180 | Não |
| `notificacoes.max_tentativas_email` | 5 | Não |
| `operacao.alerta_sla_percentual` | 80% | Não |
| `equipe.horario_atendimento` | Por time | Não |
| `publicidade.intervalo_paginas` (F2) | A definir | Não |
| `lgpd.prazo_resposta_dias` | 15 | Sim |
| `lgpd.retencao_legal_meses` | Definido pelo jurídico | Sim |
| `retencao.historico_visualizacoes_meses` | 12 | Sim |
| `publicidade.sla_aprovacao_dias_uteis` (F2) | 3 | Não |
| `publicidade.frequencia_max` (F2) | A definir | Não |

---

## 6. Decisões em aberto para o time

| # | Decisão | Recomendação |
|---|---|---|
| D1 | A plataforma terá **mensagens diretas** no MVP, ou a comunicação fica só nas reuniões e no contato liberado após o Match? | Sem chat no MVP; reuniões (P6) e contato liberado após o Match. Mensagens com moderação na F3 |
| D2 | Videochamada dentro da plataforma? | Não. Link externo no MVP; integração com agendas na F3 |
| D3 | Os padrões da seção 5 estão adequados? | Validar com investidores e empreendedores reais antes do lançamento |
| D4 | Quem forma a equipe interna no lançamento? | Definir nomes por papel e garantir pelo menos duas pessoas para as ações que exigem segunda aprovação |
| D5 | Frequência das edições temáticas | Mensal |
| D6 | Política de alocação padrão das rodadas | "Líder primeiro, depois ordem de entrada" |
| D7 | Moderação feita só pela equipe ou também pela comunidade (F3)? | Só pela equipe; a comunidade apenas denuncia |
| D8 | Conteúdo da Política de Publicidade (F2) | Redigir com o jurídico antes de abrir a publicidade |
| D9 | Forma de cobrança dos anunciantes (F2) | Pré-pago por campanha, para simplificar |
| D10 | Startups patrocinadas podem exibir dados de captação? | Não (seção P17) |

---

## 7. Rastreabilidade com o Jira

| Épico | Histórias | Conteúdo | Fase |
|---|---|---|---|
| KAN-68 Backoffice e administração | KAN-75 a KAN-82 | Usuários internos, gestão das equipes, cadastro assistido e titularidade, parâmetros, listas, auditoria, segunda aprovação, painel de operação | MVP |
| KAN-69 Verificação e curadoria | KAN-83 a KAN-87 | Fila de verificação, checklist de curadoria, edições temáticas, recursos, conflito de interesse | MVP |
| KAN-70 Agenda e reuniões | KAN-94 a KAN-98 | Proposta, resposta, lembretes, resultado e reuniões da rodada | MVP |
| KAN-71 Moderação e disputas | KAN-99 a KAN-103 | Denúncias, fila, sanções, violação de confidencialidade, disputas | MVP |
| KAN-72 Suporte, privacidade e termos | KAN-88 a KAN-93 | Catálogo de serviços, central de atendimento, LGPD, incidentes, termos, retenção | MVP |
| KAN-73 Rotinas automáticas | KAN-104 a KAN-108 | Infraestrutura de rotinas, R1–R4, R6–R7, R8–R12, R14 | MVP |
| KAN-10 Interesse e Match | KAN-109 | Condução da rodada: prorrogação, encerramento e alocação | MVP |
| KAN-74 Publicidade | KAN-110 a KAN-114 | Anunciantes, campanhas, aprovação, veiculação, biblioteca e relatórios | F2 |
