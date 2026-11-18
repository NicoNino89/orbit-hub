# Tipos de investidor, rodadas e regras de negócio revisadas

**Complemento à Especificação de Requisitos v1.0**
**Data:** novembro de 2026
**Status:** proposta para validação do time

Esta seção complementa as seções 1.2 (Público-alvo), 2.1 (Descobrir), 2.3 (Conexões), 3 (Cadastro dos empreendimentos), 4 (Confidencialidade), 5 (Formalização) e 8.2 (Receitas complementares) da especificação. Ela trata de:

1. os diferentes **tipos de investidor** e as **contas de organização**;
2. os **instrumentos de investimento** e os **estágios** usados no mercado;
3. as **rodadas de investimento** com **investidor líder** e co-investidores;
4. o **ciclo de vida das conexões** e o registro de investimentos concretizados;
5. as regras de **confidencialidade, verificação e curadoria** revisadas;
6. considerações regulatórias.

---

## 1. Tipos de investidor

A especificação trata o investidor como um perfil único. No mercado, porém, há perfis com processos de decisão, tamanhos de investimento e papéis bem diferentes nas rodadas.

| Tipo | Descrição | Como decide | Papel típico na rodada |
|---|---|---|---|
| **Investidor-anjo** | Pessoa física que investe recursos próprios em estágio inicial, geralmente oferecendo também mentoria e rede de contatos | Individualmente, com rapidez | Co-investe. Às vezes lidera rodadas pequenas |
| **Rede ou grupo de anjos** | Associação de anjos que avaliam e investem em conjunto | Comitê ou votação interna, com um anjo responsável por cada deal | Lidera ou co-investe em pré-seed e seed |
| **Sindicato** | Grupo formado para um investimento específico, com um líder e co-investidores | O líder analisa e negocia; os demais seguem | O líder conduz e os demais acompanham |
| **Fundo de Venture Capital** | Gestora que administra um fundo com recursos de cotistas (no Brasil, normalmente um FIP, regulado pela CVM) | Comitê de investimento, tese formal e due diligence estruturada | Costuma liderar de seed a Série A e B; define termos e pode pedir assento no conselho |
| **Corporate Venture Capital (CVC)** | Empresa que investe em startups com objetivos financeiros e estratégicos | Critérios financeiros e de sinergia com o negócio | Lidera ou co-investe |
| **Family office** | Estrutura que administra o patrimônio de uma família | Flexível | Co-investe e, eventualmente, lidera |
| **Aceleradora** | Programa que oferece capital inicial e apoio em troca de participação | Seleção por programa ou turma | Primeiro investimento; raramente lidera rodadas maiores |
| **Outros** | Demais investidores não enquadrados acima | — | — |

### 1.1. Papéis de uma conta

- Uma mesma conta pode ter os papéis de **investidor** e de **empreendedor**, porque muitos fundadores também investem como anjos. O segundo papel pode ser ativado depois do cadastro.

### 1.2. Contas de organização

Fundos, redes, CVCs, family offices e aceleradoras são **organizações com vários membros**. Do lado do empreendedor, a startup também é uma **equipe**.

| Lado | Papéis |
|---|---|
| Organização investidora | Administrador, membro e observador |
| Equipe da startup | Fundador administrador, fundador e colaborador |

Regras comuns:

- é possível atuar **em nome da organização** em manifestações de interesse, Matches, rodadas e acesso a informações confidenciais;
- toda ação registra qual membro a executou;
- quem deixa a organização ou a equipe perde o acesso imediatamente;
- toda organização e toda startup precisam ter **pelo menos um administrador**.

### 1.3. Novos dados na tese de investimento

| Campo | Opções |
|---|---|
| Tipo de investidor | Conforme a tabela da seção 1 |
| Estágio da rodada preferido | Pré-seed, seed, Série A, Série B ou posterior |
| Maturidade do negócio preferida (opcional) | Escala de 7 níveis da seção 3.10 da especificação |
| Ticket | Valor mínimo e máximo por investimento |
| Liderança | Lidera rodadas, apenas co-investe ou ambos |
| Follow-on | Reserva ou não capital para as próximas rodadas |
| Participação na gestão | Exige assento no conselho, aceita ser observador ou não participa |
| Instrumentos aceitos | Ver seção 2.1 |

A **classificação regulatória** (varejo, qualificado ou profissional, conforme a Resolução CVM 30) **não será coletada no MVP**. Ela só entra quando passar a controlar algum acesso. Coletar o dado sem uso contraria o princípio de minimização da LGPD.

---

## 2. Instrumentos e estágios

### 2.1. Instrumentos de investimento

O Marco Legal das Startups (Lei Complementar 182/2021) reconhece diferentes instrumentos de aporte. Enquanto o instrumento não é convertido em participação, o investidor não é considerado sócio e não responde pelas dívidas da empresa. Cada instrumento tem termos próprios, e a rodada pede os campos correspondentes:

| Instrumento | Termos específicos |
|---|---|
| Participação societária | Valuation pré-money e participação oferecida |
| Mútuo conversível | Valuation cap, desconto na conversão, taxa de juros, prazo de vencimento e eventos de conversão |
| Contrato de investimento-anjo (LC 155/2016) | Prazo, forma de remuneração e condições de resgate |
| Opção de compra ou subscrição | Preço ou valuation de exercício e prazo |
| Debênture conversível | Valor, remuneração, vencimento e condições de conversão |

### 2.2. Duas escalas de estágio

| Escala | Descreve | Onde é usada |
|---|---|---|
| **Maturidade do negócio** (7 níveis: Ideação, Validação, MVP, Operação inicial, Tração, Crescimento, Expansão) | A empresa | Cadastro da startup e tese (opcional) |
| **Estágio da rodada** (pré-seed, seed, Série A, Série B ou posterior) | A captação | Rodada e tese |

As duas escalas são independentes. Fundos costumam definir a tese pelo estágio da rodada.

---

## 3. Rodadas de investimento

### 3.1. Conceito

Uma **rodada** é um processo de captação com objetivo, prazo e instrumento definidos, do qual podem participar vários investidores. O **investidor líder** costuma definir os termos, conduzir a due diligence, colocar o maior aporte e coordenar os co-investidores.

### 3.2. Regras

**Fonte única dos termos de captação**
- Valores, instrumento, valuation e participação oferecida existem **somente na rodada**. O cadastro da startup guarda apenas o contexto: destinação dos recursos, prazo de uso e metas.
- Sem rodada aberta, o catálogo mostra "sem rodada aberta no momento".

**Abertura**
- Somente startups **com CNPJ validado** podem abrir rodada. Startups sem CNPJ podem aparecer no catálogo, com o selo "empresa ainda não constituída".
- Somente o fundador administrador abre, edita e encerra a rodada.
- Campos: estágio, instrumento, valor alvo, mínimo, máximo, prazo e os termos específicos do instrumento (seção 2.1).
- Status: rascunho, aberta, em negociação e encerrada (concluída ou cancelada).
- Apenas uma rodada aberta por startup.

**Participantes**
- Investidores ou organizações com conexão em Match ou posterior.
- O **líder pode convidar co-investidores**, que entram após aprovação do empreendedor. A aprovação cria o Match automaticamente.
- Cada participante registra seu valor de **intenção** e condições preliminares.

**Investidor líder**
- No máximo um por rodada, indicado pelo empreendedor e confirmado pelo próprio investidor.
- A plataforma destaca quem declarou que lidera rodadas.
- A rodada pode ficar aberta sem líder.

**Visibilidade (padrão configurável pelo empreendedor)**
- Todos os participantes sabem quem é o líder.
- Co-investidores não veem os valores dos demais.
- O líder vê os participantes e a soma das intenções.

**Excesso de demanda**
- A plataforma sinaliza quando a soma das intenções ultrapassa o valor máximo. No MVP, a alocação é decidida pelo empreendedor com o líder, fora do sistema.

**Natureza não vinculante**
- Rodadas, intenções e liderança **não representam compromisso de investimento**.

### 3.3. Registro do resultado

- Ao encerrar a rodada, o empreendedor registra o resultado: valor captado, instrumento utilizado, data do fechamento e participantes.
- **Cada investidor confirma** a própria participação. Só participações confirmadas pelas duas partes contam como **investimento concretizado**.
- Esse registro sustenta o histórico, as métricas do ecossistema e uma eventual remuneração sobre investimentos (seção 8.2 da especificação), que depende de validação jurídica e não faz parte do MVP.

### 3.4. Fases

| Fase | Escopo |
|---|---|
| MVP | Rodada, participantes, líder, convites do líder, painel de intenções, excesso de demanda e registro do resultado |
| Fase 3 | Negociação de termos pelo líder, assinatura eletrônica, compromisso firme, alocação assistida e taxa de performance do líder de sindicato |

---

## 4. Ciclo de vida da conexão

| Estado | Significado |
|---|---|
| Pendente | Interesse enviado, aguardando o empreendedor |
| Recusada | O empreendedor recusou |
| Expirada | Sem resposta em 30 dias |
| Retirada | O investidor desistiu antes do Match |
| Match | O empreendedor aceitou |
| Em análise | O investidor está analisando a oportunidade |
| Em negociação | As partes discutem termos ou o investidor entrou numa rodada |
| Investiu | Investimento confirmado pelas duas partes |
| Encerrada | Qualquer lado encerrou, informando o motivo |

Regras:

- só há **uma conexão ativa** por investidor (pessoa ou organização) e startup;
- recusada, expirada, retirada, investiu e encerrada são estados finais. Depois de 90 dias de um estado final negativo, o investidor pode manifestar interesse de novo;
- ao chegar a um estado final, termina o acesso às informações confidenciais.

---

## 5. Confidencialidade, verificação e curadoria

### 5.1. Dados de captação restritos

- O catálogo só é acessível a usuários cadastrados.
- Dados da rodada (valores, instrumento e estágio) aparecem **apenas para investidores verificados**.
- Valuation e termos específicos do instrumento são sempre informações confidenciais (Nível 2).

### 5.2. Liberação das informações confidenciais

- Disponível apenas para conexões em Match, em análise ou em negociação.
- Exige a **autorização explícita do fundador administrador**.
- Tem **validade** (padrão de 90 dias, renovável) e termina automaticamente quando a conexão chega a um estado final, quando o investidor é bloqueado ou quando o empreendedor revoga.

### 5.3. Alternativa ao NDA bilateral (altera as seções 4.3 e 5)

Muitos fundos de Venture Capital não assinam NDA na fase inicial de análise. Exigir sempre um NDA bilateral pode afastar esses investidores.

- Todo investidor aceita, no cadastro, o **Termo de Confidencialidade da Plataforma**, com registro de data, hora e versão.
- Para cada startup, o empreendedor escolhe a regra: **termo da plataforma** (padrão) ou **NDA bilateral**.
- Em ambos os casos, a autorização explícita do empreendedor continua obrigatória.
- A redação do termo deve prever a startup como beneficiária, para que ela possa exigir o cumprimento. A redação final cabe à assessoria jurídica.

### 5.4. Verificação

| Quem | O que é conferido |
|---|---|
| Organização investidora | CNPJ e, para gestoras de fundos, autorização na CVM |
| Investidor pessoa física | Identidade (CPF e documento) e e-mail |
| Startup | CNPJ, quando houver |
| Fundador | Identidade (CPF e documento) e e-mail |

- Os perfis exibem o selo **verificado**, **em análise** ou **não verificado**.
- O empreendedor pode aceitar manifestações apenas de investidores verificados.

### 5.5. Curadoria

- Antes de aparecer no catálogo, toda startup passa por **revisão** da plataforma: campos obrigatórios, identidade dos fundadores, CNPJ e adequação do conteúdo.
- Alterações relevantes depois da publicação passam por nova revisão.

### 5.6. Qualidade dos indicadores

- Cada indicador de tração tem **data de referência** e aparece como "declarado pelo empreendedor, não auditado".
- Indicadores com mais de 6 meses aparecem como desatualizados.

### 5.7. Bloqueio

- O empreendedor pode bloquear investidores ou organizações específicos, como concorrentes.
- Investidores bloqueados deixam de ver a startup, não podem manifestar interesse nem participar de rodadas, e perdem o acesso às informações confidenciais.
- O bloqueio é silencioso: o investidor não é notificado.

---

## 6. Considerações regulatórias

- A figura do **investidor líder** e do **sindicato** é regulada nas ofertas públicas de financiamento coletivo de investimento (Resolução CVM 88), que exigem plataforma registrada na CVM.
- O Orbit Hub se posiciona como plataforma de **conexão**:
  - não estrutura ofertas;
  - não recebe nem movimenta recursos;
  - restringe o catálogo a usuários cadastrados e os dados de captação a investidores verificados.
- A remuneração sobre investimentos concretizados (seção 8.2) é o ponto mais sensível e deve ser avaliada à luz dessa regulação.
- **Recomendação:** validar o enquadramento com assessoria jurídica especializada antes do lançamento. Este documento não constitui parecer jurídico.

---

## 7. Rastreabilidade com o Jira

| Tema | Issues |
|---|---|
| Papéis da conta | KAN-12, KAN-16 |
| Tese: estágio da rodada e maturidade | KAN-18 |
| Contas de organização | KAN-60 |
| Equipe da startup | KAN-65 |
| Tipo de investidor, ticket, liderança e instrumentos | KAN-61 |
| Verificação de investidores, organizações, startups e fundadores | KAN-62 |
| CNPJ obrigatório para abrir rodada | KAN-21 |
| Indicadores com data de referência | KAN-24 |
| Contexto da necessidade de investimento | KAN-25 |
| Publicação e curadoria | KAN-27, KAN-66 |
| Dados de captação restritos | KAN-28, KAN-38 |
| Liberação do Nível 2 com validade | KAN-30 |
| Critérios de compatibilidade | KAN-39 |
| Manifestação de interesse, Match e ciclo de vida | KAN-45, KAN-48, KAN-49 |
| Rodada com investidor líder e co-investidores | KAN-59 |
| Termo de confidencialidade da plataforma | KAN-63 |
| Registro do investimento concretizado | KAN-64 |
| Bloqueio de investidores | KAN-67 |
