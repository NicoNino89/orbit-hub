# Tipos de investidor e rodadas de investimento

**Complemento à Especificação de Requisitos v1.0**
**Data:** novembro de 2026
**Status:** proposta para validação do time

Esta seção complementa as seções 1.2 (Público-alvo), 2.3 (Conexões), 4 (Confidencialidade) e 5 (Formalização) da especificação. Ela trata de três temas que a versão 1.0 não cobre:

1. os diferentes **tipos de investidor**;
2. as **rodadas de investimento** com vários investidores e um **investidor líder**;
3. a **alternativa ao NDA bilateral** para liberar informações confidenciais.

---

## 1. Tipos de investidor

A especificação trata o investidor como um perfil único. No mercado, porém, há perfis com processos de decisão, tamanhos de investimento e papéis bem diferentes nas rodadas.

| Tipo | Descrição | Como decide | Papel típico na rodada |
|---|---|---|---|
| **Investidor-anjo** | Pessoa física que investe recursos próprios em estágio inicial, geralmente oferecendo também mentoria e rede de contatos | Individualmente, com rapidez | Co-investe. Às vezes lidera rodadas pequenas |
| **Rede ou grupo de anjos** | Associação de anjos que avaliam e investem em conjunto | Comitê ou votação interna, com um anjo responsável por cada deal | Lidera ou co-investe em pré-seed e seed |
| **Sindicato** | Grupo formado para um investimento específico, com um líder e co-investidores, muitas vezes por meio de um veículo único | O líder analisa e negocia; os demais seguem | O líder conduz e os demais acompanham |
| **Fundo de Venture Capital** | Gestora que administra um fundo com recursos de cotistas (no Brasil, normalmente um FIP, regulado pela CVM) | Comitê de investimento, tese formal e due diligence estruturada | Costuma liderar de seed a Série A e B; define termos e pode pedir assento no conselho |
| **Corporate Venture Capital (CVC)** | Empresa que investe em startups com objetivos financeiros e estratégicos | Critérios financeiros e de sinergia com o negócio | Lidera ou co-investe |
| **Family office** | Estrutura que administra o patrimônio de uma família | Flexível | Co-investe e, eventualmente, lidera |
| **Aceleradora** | Programa que oferece capital inicial e apoio em troca de participação | Seleção por programa ou turma | Primeiro investimento; raramente lidera rodadas maiores |
| **Outros** | Demais investidores não enquadrados acima | — | — |

### 1.1. Classificação regulatória

Independentemente do tipo, pessoas físicas podem ser classificadas como investidor **de varejo**, **qualificado** ou **profissional** (Resolução CVM 30). No Orbit Hub, a classificação é **autodeclarada**, com aceite registrado do termo de declaração.

### 1.2. Contas de organização

Fundos, redes, CVCs, family offices e aceleradoras são **organizações com vários membros**. A plataforma deve permitir:

- cadastrar a organização (nome, tipo, CNPJ, site e descrição);
- convidar membros com os papéis de **administrador**, **membro** e **observador**;
- atuar **em nome da organização** em manifestações de interesse, Matches, rodadas e acesso a informações confidenciais;
- registrar qual membro executou cada ação;
- retirar imediatamente o acesso de quem deixa a organização.

### 1.3. Novos dados na tese de investimento

Além do que já consta na seção 2.3.1, a tese passa a incluir:

| Campo | Opções |
|---|---|
| Tipo de investidor | Conforme a tabela da seção 1 |
| Ticket | Valor mínimo e máximo por investimento |
| Liderança | Lidera rodadas, apenas co-investe ou ambos |
| Follow-on | Reserva ou não capital para as próximas rodadas |
| Participação na gestão | Exige assento no conselho, aceita ser observador ou não participa |
| Instrumentos aceitos | Participação societária, mútuo conversível, contrato de investimento-anjo, opção de compra ou subscrição, debênture conversível e outros |

### 1.4. Verificação

- **Organizações:** conferência do CNPJ e, para gestoras de fundos, da autorização na CVM.
- **Pessoas físicas:** conferência de identidade e e-mail.
- O perfil exibe um selo: **verificado**, **em análise** ou **não verificado**.
- O empreendedor pode aceitar manifestações apenas de investidores verificados.

---

## 2. Instrumentos de investimento

O Marco Legal das Startups (Lei Complementar 182/2021) reconhece diferentes instrumentos de aporte. Enquanto o instrumento não é convertido em participação, o investidor não é considerado sócio e não responde pelas dívidas da empresa.

| Instrumento | Uso típico |
|---|---|
| Mútuo conversível | Muito comum em seed. É um empréstimo que pode se converter em participação |
| Participação societária direta | Aquisição de cotas ou ações |
| Contrato de investimento-anjo (LC 155/2016) | Aporte em microempresas e empresas de pequeno porte, sem que o anjo se torne sócio |
| Opção de compra ou subscrição | Direito de adquirir participação no futuro |
| Debênture conversível | Para sociedades anônimas |

A rodada informa qual instrumento será usado, e a tese do investidor indica quais instrumentos ele aceita.

---

## 3. Rodadas de investimento

### 3.1. Conceito

Uma **rodada** é um processo de captação com objetivo, prazo e instrumento definidos, do qual podem participar vários investidores. O **investidor líder** costuma:

- definir os termos (instrumento, valuation e direitos);
- conduzir a due diligence, aproveitada pelos demais;
- colocar o maior aporte e dar credibilidade à rodada;
- coordenar os co-investidores.

### 3.2. Regras

**Rodada**
- Dados: estágio, instrumento, valor alvo, mínimo, máximo, prazo e valuation pré-money (opcional e confidencial por padrão).
- Status: rascunho, aberta, em negociação e encerrada (concluída ou cancelada).
- Apenas uma rodada aberta por empreendimento.

**Participantes**
- Investidores ou organizações com Match ativo.
- Cada participante registra seu valor de **intenção** e condições preliminares.

**Investidor líder**
- No máximo um por rodada.
- Indicado pelo empreendedor entre os participantes e confirmado pelo próprio investidor.
- A plataforma destaca quem declarou que lidera rodadas.
- A rodada pode ficar aberta sem líder, com a indicação "rodada sem líder definido".
- Trocas de líder ficam registradas.

**Visibilidade (padrão configurável pelo empreendedor)**
- Todos os participantes sabem quem é o líder.
- Co-investidores não veem os valores dos demais.
- O líder vê os participantes e a soma das intenções.
- O acesso às informações confidenciais continua individual e autorizado pelo empreendedor.

**Excesso de demanda**
- A plataforma sinaliza quando a soma das intenções ultrapassa o valor máximo.
- No MVP, a alocação é decidida pelo empreendedor com o líder, fora do sistema.

**Natureza não vinculante**
- Rodadas, intenções e liderança **não representam compromisso de investimento**, o que deve ficar claro em todas as telas.

### 3.3. Fases

| Fase | Escopo |
|---|---|
| MVP | Rodada, participantes, líder, painel de intenções e sinalização de excesso de demanda |
| Fase 3 | Negociação de termos pelo líder, assinatura eletrônica dos instrumentos, alocação assistida e taxa de performance do líder de sindicato |

---

## 4. Alteração na regra de confidencialidade (seções 4.3 e 5)

Muitos fundos de Venture Capital não assinam NDA na fase inicial de análise. Exigir sempre um NDA bilateral pode afastar esses investidores.

**Regra proposta:**

- Todo investidor aceita, no cadastro, o **Termo de Confidencialidade da Plataforma**, com registro de data, hora e versão.
- Para cada startup, o empreendedor escolhe a regra de liberação das informações confidenciais:
  - **Termo da plataforma** (padrão); ou
  - **NDA bilateral**.
- Em ambos os casos, a **autorização explícita do empreendedor** continua obrigatória.

---

## 5. Considerações regulatórias

- A figura do **investidor líder** e do **sindicato** é regulada nas ofertas públicas de financiamento coletivo de investimento (Resolução CVM 88), que exigem plataforma registrada na CVM.
- O Orbit Hub se posiciona como plataforma de **conexão**: não estrutura ofertas, não recebe nem movimenta recursos e apresenta oportunidades apenas a usuários cadastrados.
- A remuneração sobre investimentos concretizados (seção 8.2) e a exibição de valores de captação devem ser avaliadas à luz dessa regulação.
- **Recomendação:** validar o enquadramento com assessoria jurídica especializada antes do lançamento.

---

## 6. Rastreabilidade com o Jira

| Tema | Issues |
|---|---|
| Contas de organização | KAN-60 |
| Tipo de investidor, ticket, liderança e instrumentos | KAN-61 |
| Verificação de investidores e organizações | KAN-62 |
| Rodada com investidor líder e co-investidores | KAN-59 |
| Termo de confidencialidade da plataforma | KAN-63 |
| Critérios de compatibilidade | KAN-39 |
