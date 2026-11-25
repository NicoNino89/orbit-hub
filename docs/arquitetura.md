# Arquitetura do Orbit Hub

## Visão geral

- **Estilo:** cliente-servidor, com API REST.
- **Back-end:** monólito modular em Java 21 + Spring Boot 4.1, organizado em camadas.
- **Front-end:** SPA em React + TypeScript + Vite.
- **Dados:** PostgreSQL (migrações com Flyway) e armazenamento de arquivos em Blob Storage.
- **Nuvem:** Microsoft Azure, com infraestrutura declarada em Terraform.

## Camadas do back-end

```
Controller  →  Service  →  Repository
   (API)       (regras)     (persistência)
```

- **Controller:** recebe as requisições REST, valida a entrada e converte entre DTOs e o domínio.
- **Service:** concentra as regras de negócio e as transações.
- **Repository:** acesso a dados com Spring Data JPA.
- Entidades JPA não são expostas diretamente na API: a comunicação usa DTOs.

## Módulos

Cada módulo é um pacote de topo com suas próprias camadas. Um módulo só usa outro por meio de serviços públicos, nunca acessando os repositórios de outro módulo diretamente.

| Módulo | Responsabilidade | Épicos |
|---|---|---|
| `identidade` | Cadastro, autenticação, perfis, tokens, organizações e verificação | KAN-4 |
| `investidores` | Tipo de investidor, tese e preferências de investimento | KAN-5 |
| `empreendimentos` | Cadastro das startups, equipe, curadoria, visibilidade (Nível 1 e Nível 2) e bloqueios | KAN-6, KAN-7 |
| `catalogo` | Edições, filtros, favoritos, histórico | KAN-8, KAN-9 |
| `conexoes` | Manifestação de interesse, Match, ciclo de vida da conexão, rodadas com investidor líder e registro de investimentos | KAN-10 |
| `recomendacao` | Critérios de compatibilidade e justificativas | KAN-39 |
| `agenda` | Propostas, confirmação, lembretes e resultado de reuniões | Processo P6 |
| `moderacao` | Denúncias, sanções, recursos, disputas e violações de confidencialidade | Processos P8 a P11 |
| `publicidade` | Anunciantes, campanhas, aprovação, veiculação e biblioteca de anúncios | Fase 2 (P17) |
| `marketplace` | Prestadores, serviços, propostas, contratações, avaliações e pagamentos | KAN-115 (Fase 2) |
| `conhecimento` | Conteúdos, taxonomia, fluxo editorial, cursos, trilhas, acesso e Market Insights | KAN-116 (Fase 2) |
| `comunidade` | Feed, seguidores, grupos, perguntas e respostas, discussões, reputação e mensagens | KAN-117 (Fase 3) |
| `administracao` | Usuários e papéis internos, parâmetros e listas, filas de verificação e curadoria, edições do catálogo e termos versionados | KAN-62, KAN-66, P2 a P5, P14, P15 |
| `auditoria` | Trilha imutável das ações da equipe interna e acesso justificado a dados confidenciais (Nível 2) dentro de um caso aberto. Módulo próprio porque todos os outros registram nele | KAN-80 |

As rotinas automáticas (R1 a R14) ficam em cada módulo dono da regra, com agendamento e monitoramento comuns em `compartilhado`. Prazos e limites vêm sempre dos **parâmetros configuráveis**, nunca de valores fixos no código. Na prática (KAN-78): o catálogo fica na tabela `parametro` (migração V11, com tipo, unidade, limites e se é sensível); o código lê pela interface `compartilhado.parametros.LeituraDeParametros`, com as chaves em `Chaves`; o administrador altera em *Backoffice → Parâmetros*, com justificativa e histórico, e o parâmetro sensível passa pela segunda aprovação. Cada prazo já iniciado guarda a sua data de fim quando começa, então a alteração vale daqui para frente. Nos testes de unidade, `ParametrosFixos` traz os mesmos valores padrão. As **listas gerenciáveis** (segmentos, localizações, tipos de investidor, instrumentos, estágios, modelos de negócio, motivos, categorias, documentos de verificação e checklist de curadoria, KAN-79) ficam em `lista_item`: o sistema grava o código estável, a tela mostra o rótulo, itens nunca são apagados (só desativados) e os formulários leem os ativos em `GET /api/v1/listas/{LISTA}`; um módulo novo valida a escolha com `ListasService.exigirAtivo`. Detalhes em [`requisitos/processos-e-operacao.md`](requisitos/processos-e-operacao.md) e, para marketplace, conhecimento e comunidade, em [`requisitos/servicos-e-conhecimento.md`](requisitos/servicos-e-conhecimento.md).

Estrutura prevista do pacote base:

```
br.com.orbithub
├── identidade
│   ├── api          # controllers e DTOs
│   ├── service
│   ├── domain       # entidades e regras
│   └── repository
├── investidores
├── empreendimentos
├── catalogo
├── conexoes
├── recomendacao
└── compartilhado    # configuração, segurança, erros, utilitários
```

## Padrões de projeto (GoF)

| Categoria | Padrão | Onde será aplicado |
|---|---|---|
| Criacional | Factory Method | Criação dos perfis de usuário (investidor e empreendedor), cada um com seu onboarding (KAN-12) |
| Estrutural | Adapter | Integrações externas atrás de interfaces próprias: armazenamento de arquivos (disco local ou Azure Blob Storage), envio de e-mail (KAN-14) e validação de CNPJ (KAN-21) |
| Comportamental | Strategy | Cada critério de compatibilidade (segmento, estágio, faixa de aporte, localização, modelo de negócio) é uma estratégia que devolve sua justificativa (KAN-39). Na Fase 2, também a segmentação publicitária |

## Segurança

- Spring Security com **access token JWT** de curta duração.
- **Refresh token** em cookie `HttpOnly`, com apenas o hash guardado no banco, rotação a cada uso e revogação individual ou por usuário (KAN-57).
- Permissões verificadas no servidor para todas as operações (KAN-34):
  - toda rota nova nasce exigindo login; as públicas ficam listadas em `SegurancaConfig`;
  - papel por anotação na classe ou no método: `@SomenteInvestidor`, `@SomenteEmpreendedor`, `@SomenteAdmin` (`compartilhado.seguranca.Permissoes`);
  - dono do recurso por `@PreAuthorize("@acesso.ehDono('startup', #id)")`, com um `DonoDeRecurso` registrado pelo módulo dono do recurso; tipo sem regra é negado;
  - toda recusa responde em Problem Details (401 `nao_autenticado`, 403 `acesso_negado`) e fica registrada na tabela `acesso_negado` e no log;
  - o access token traz o id da sessão e é recusado na hora depois do logout ou da troca de senha (KAN-57).
- **Backoffice e trilha de auditoria (KAN-80):**
  - toda rota da equipe interna fica em `/api/v1/admin/**`. Rota de backoffice fora desse prefixo não é registrada;
  - cada requisição a essas rotas vira um registro em `registro_auditoria`: quem, ação, objeto, resultado (sucesso ou falha, com o status) e quando. O serviço dá nome de negócio à ação com `ContextoDeAuditoria.descrever(...)` (por exemplo, `DOCUMENTO_LEGAL_PUBLICADO`); sem isso, a ação fica registrada como método e rota;
  - a trilha é imutável no próprio banco: um gatilho recusa `UPDATE`, `DELETE` e `TRUNCATE`;
  - a equipe só vê dados de Nível 2 por `AcessoJustificadoService.exigir(...)`, que pede um caso aberto (cada módulo com casos registra um `CasoAberto`) e justificativa, e grava o acesso antes de devolver os dados. Para a startup, o autor aparece como "equipe Orbit Hub".
- **Equipe interna (KAN-75):**
  - conta da equipe é separada da conta de investidor ou empreendedor (coluna `interna`): nunca recebe papel público, e conta pública nunca recebe papel interno. Papéis: administrador, analista de verificação, curador, moderador, suporte N1 e N2, encarregado de dados e jurídico;
  - `/api/v1/admin/**` exige um papel interno no filtro, e cada tela exige o seu no método (`@SomenteAdmin`, `@AdminOuEncarregado`, `@SomenteEquipe`);
  - só o administrador cadastra a equipe, por convite: o membro escolhe a própria senha. O primeiro administrador nasce na implantação ([`operacao/primeiro-administrador.md`](operacao/primeiro-administrador.md));
  - entrada em dois fatores: a senha devolve um desafio, e só o código TOTP (RFC 6238, aplicativo autenticador) abre a sessão. O segredo fica cifrado com AES-GCM; códigos errados contam no mesmo bloqueio do login, e um código vale uma única vez;
  - trocar papéis, desligar ou redefinir o segundo fator derruba as sessões do membro na hora. O desligamento devolve os casos abertos às filas (`CasosDoMembro`, registrado por cada fila);
  - revisão de acessos a cada `acesso.revisao_meses` (padrão: 3), com a fotografia dos membros confirmados.
- **Segunda aprovação (KAN-81):** ações graves (banimento definitivo, remoção de startup por fraude, transferência de titularidade, parâmetro sensível, dados pessoais fora do fluxo) nunca são aplicadas direto. O módulo dono chama `SegundaAprovacaoService.propor(...)` e registra um `ExecutorDeAcaoGrave` (com os papéis que podem aprovar); o executor só roda quando outra pessoa aprova, na mesma transação. Quem propõe não decide; proposta parada além de `aprovacao.prazo_horas` (padrão: 48) alerta o administrador.
- **Central de notificações (KAN-146):** módulo `notificacoes`. Os outros módulos avisam as pessoas só por `NotificacoesService.notificar` (ou `notificarTodos`), dentro da própria transação. O serviço grava o aviso do sino e, se a preferência incluir e-mail, um item na fila `notificacao_email`; a rotina R17 envia e tenta de novo, e a R18 manda o resumo diário. Cada pessoa escolhe por tipo de evento: plataforma, e-mail ou os dois. Avisos obrigatórios (segurança da conta, termos, prazos que encerram conexão, avisos à equipe) ignoram a escolha. O e-mail leva só título, mensagem e link da tela, nunca dado confidencial.
- **Organizações investidoras (KAN-60):** fundos, redes de anjos, CVCs, family offices e aceleradoras ficam no módulo `investidores`. Papéis na organização: administrador (gerencia membros e dados), membro (atua em nome dela) e observador (só vê); ela nunca fica sem administrador. A entrada é por convite com token (só o hash fica no banco), aceito pela conta com o mesmo e-mail e o papel de investidor.
  - **Em nome de quem:** a tela manda a organização escolhida no cabeçalho `X-Orbit-Organizacao`, e toda funcionalidade de investidor resolve a atuação por `AtuacaoService.paraAgir` ou `paraVer`. O vínculo é lido do banco a cada requisição, então quem sai perde o acesso na hora. Toda ação guarda a pessoa (`Atuacao.pessoa()`), mesmo quando é em nome da organização.
- **Perfil de investimento (KAN-61):** um por dono (a pessoa ou a organização da atuação). Guarda tipo de investidor, ticket mínimo e máximo, liderança, follow-on, participação na gestão e instrumentos (da lista gerenciável). Na organização, o tipo é o da própria organização e só o administrador altera. Classificação regulatória (CVM 30) fica de fora até controlar algum acesso (minimização, LGPD).
- **Tese de investimento e onboarding (KAN-18):** a tese faz parte do perfil de investimento: segmentos, estágios da rodada e localizações (das listas, ao menos um de cada) e a maturidade do negócio (opcional, escala fixa `compartilhado.escalas.MaturidadeDoNegocio`, a mesma da startup). O onboarding fica concluído quando o dono da atuação tem perfil; a pessoa pode pular para completar depois. `OnboardingDoInvestidor.estado(...).liberaCatalogo()` é o que o catálogo (S6) consulta.
- **Verificação de identidade (KAN-62):** módulo `verificacao`. Pedido por sujeito (pessoa física ou organização da atuação; startup e fundador na S4), do rascunho à decisão, com os documentos exigidos da lista `DOCUMENTO_VERIFICACAO` pelo grupo do sujeito (autorização da CVM só para fundos de VC). Documentos em PDF, JPEG ou PNG (conferidos pelos bytes), até `verificacao.documento_max_mb`, no armazenamento; acesso só do dono e da equipe de verificação; guarda por `verificacao.retencao_documentos_meses`. `VerificacoesService.selo(...)` é o serviço público do selo (verificado, em análise, não verificado) para catálogo, interesse e Nível 2.
- **Fila de verificação (KAN-83):** analista de verificação ou administrador, em `/api/v1/admin/verificacoes`. Ordem de chegada, prazo em dias úteis (`compartilhado.tempo.DiasUteis`, sem feriados por enquanto). Só decide quem assumiu o pedido (ou o administrador), sempre com motivo da lista `MOTIVO_DECISAO_VERIFICACAO`, cujo grupo é a decisão (VERIFICADO, COMPLEMENTO, RECUSA ou FRAUDE). Fraude recusa e avisa a administração até existir a moderação (KAN-71). Leitura de documento e decisão vão para a trilha de auditoria. A rotina R9 encerra, avisa, vence e apaga documentos.
- **Conflito de interesse (KAN-87):** `administracao.service.ConflitosDeInteresse`, serviço público para todas as filas. Cada membro da equipe declara vínculos (organização ou startup pelo CNPJ, pessoa pelo e-mail) e pode se declarar impedido num caso, com motivo. Quem tem conflito não assume nem decide (inclusive o administrador); se nenhum analista ativo estiver livre, a fila marca o caso para o administrador. Os vínculos aparecem na revisão periódica de acessos (KAN-75). Curadoria, moderação, disputa e recurso chamam o mesmo serviço quando chegarem.
- **Startups (KAN-21):** módulo `empreendimentos`. A startup vai no endereço (`/api/v1/startups/{id}`), porque o empreendedor pode ter várias; o acesso é pela equipe (`startup_membro`), e quem não é membro recebe 404. Quem cria vira fundador administrador. CNPJ opcional (dígitos conferidos e único); sem ele, "empresa ainda não constituída" e sem rodada. A situação cadastral na Receita é conferida na verificação da startup (KAN-62). A participação dos fundadores é o cap table (Nível 2), só para a equipe.
- Separação entre informações públicas (Nível 1) e confidenciais (Nível 2), com liberação explícita pelo empreendedor (KAN-28 a KAN-33).
- Tratamento de dados pessoais conforme a LGPD.

## Infraestrutura (Azure for Students)

| Recurso | Uso |
|---|---|
| Azure Container Apps | Back-end, com escala a zero |
| Azure Container Apps (web) | Front-end: nginx servindo o build e repassando `/api` para a API ([ADR 0003](adr/0003-front-end-no-container-apps.md)) |
| Azure Database for PostgreSQL | Um servidor Flexible B1ms, com bancos de homologação e produção |
| Azure Blob Storage | Logotipos e documentos |
| Azure Key Vault | Segredos |
| Application Insights | Logs, métricas e rastreamento |
| GitHub Container Registry | Imagens Docker |

Toda a infraestrutura é declarada em Terraform na pasta `infra/` (KAN-55).
