-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 16:19
-- Cartão Jira: KAN-86, KAN-88, KAN-89, KAN-99, KAN-100, KAN-101, KAN-102, KAN-103
-- Título: Moderação, recursos e atendimento
-- Objetivo: Guardar as denúncias agrupadas em casos com prioridade e prazo, as sanções (com a suspensão da conta), os
--           recursos contra verificação, curadoria e sanção, os chamados de atendimento com mensagens, níveis, prazos,
--           avaliação e disputas, a central de ajuda e as respostas padrão.
-- Decisões: Prioridade da denúncia pelo grupo da categoria (CRITICA, ALTA, NORMAL). Um caso aberto por objeto. O
--           denunciante nunca aparece para o denunciado. A suspensão da conta é uma data em usuario.suspensa_ate. Chamado
--           sem conta só abre depois da confirmação por e-mail (token com hash).

ALTER TABLE usuario ADD COLUMN suspensa_ate TIMESTAMPTZ;

UPDATE lista_item SET grupo = 'CRITICA' WHERE lista = 'CATEGORIA_DENUNCIA' AND codigo IN ('FRAUDE', 'PERFIL_FALSO');
UPDATE lista_item SET grupo = 'ALTA' WHERE lista = 'CATEGORIA_DENUNCIA' AND codigo IN ('OFERTA_IRREGULAR', 'VAZAMENTO_CONFIDENCIAL', 'ASSEDIO');
UPDATE lista_item SET grupo = 'NORMAL' WHERE lista = 'CATEGORIA_DENUNCIA' AND codigo IN ('INFORMACAO_FALSA', 'CONTEUDO_IMPROPRIO', 'SPAM', 'OUTRO');

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
SELECT gen_random_uuid(), v.lista, v.codigo, v.rotulo, v.grupo, v.ordem, now(), now()
FROM (VALUES
    ('CATEGORIA_DENUNCIA', 'CONTEUDO_ILEGAL', 'Conteúdo ilegal', 'CRITICA', 9),
    ('CATEGORIA_DENUNCIA', 'VAZAMENTO_DADOS', 'Vazamento de dados pessoais', 'CRITICA', 10),
    ('MOTIVO_DECISAO_MODERACAO', 'SEM_VIOLACAO', 'Não houve violação', 'SEM_ACAO', 1),
    ('MOTIVO_DECISAO_MODERACAO', 'PROVAS_INSUFICIENTES', 'Provas insuficientes', 'SEM_ACAO', 2),
    ('MOTIVO_DECISAO_MODERACAO', 'VIOLACAO_LEVE', 'Violação leve da Política de Moderação', 'SANCAO', 3),
    ('MOTIVO_DECISAO_MODERACAO', 'VIOLACAO_GRAVE', 'Violação grave da Política de Moderação', 'SANCAO', 4),
    ('MOTIVO_DECISAO_MODERACAO', 'FRAUDE_CONFIRMADA', 'Fraude confirmada', 'SANCAO', 5),
    ('MOTIVO_DECISAO_MODERACAO', 'VAZAMENTO_CONFIRMADO', 'Violação de confidencialidade confirmada', 'SANCAO', 6)
) AS v(lista, codigo, rotulo, grupo, ordem);

CREATE TABLE caso_moderacao (
    id                 UUID           PRIMARY KEY,
    objeto_tipo        VARCHAR(20)    NOT NULL,
    objeto_id          UUID           NOT NULL,
    prioridade         VARCHAR(10)    NOT NULL,
    categoria          VARCHAR(60)    NOT NULL,
    origem             VARCHAR(20)    NOT NULL,
    situacao           VARCHAR(20)    NOT NULL,
    prazo              TIMESTAMPTZ    NOT NULL,
    responsavel_id     UUID,
    decisao            VARCHAR(30),
    motivo             VARCHAR(60),
    observacao         VARCHAR(2000),
    parecer_juridico   VARCHAR(4000),
    decidido_por       UUID,
    decidido_em        TIMESTAMPTZ,
    alertado_em        TIMESTAMPTZ,
    criado_em          TIMESTAMPTZ    NOT NULL,
    atualizado_em      TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX ux_caso_moderacao_aberto ON caso_moderacao (objeto_tipo, objeto_id) WHERE situacao <> 'DECIDIDO';
CREATE INDEX ix_caso_moderacao_fila ON caso_moderacao (situacao, prazo);

CREATE TABLE denuncia (
    id              UUID           PRIMARY KEY,
    caso_id         UUID           NOT NULL REFERENCES caso_moderacao (id),
    denunciante_id  UUID           REFERENCES usuario (id),
    categoria       VARCHAR(60)    NOT NULL,
    descricao       VARCHAR(4000)  NOT NULL,
    evidencias      VARCHAR(2000),
    criada_em       TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_denuncia_denunciante ON denuncia (denunciante_id, criada_em DESC);

CREATE TABLE sancao (
    id           UUID           PRIMARY KEY,
    caso_id      UUID           REFERENCES caso_moderacao (id),
    alvo_tipo    VARCHAR(20)    NOT NULL,
    alvo_id      UUID           NOT NULL,
    tipo         VARCHAR(40)    NOT NULL,
    dias         INTEGER,
    inicio       TIMESTAMPTZ    NOT NULL,
    fim          TIMESTAMPTZ,
    motivo       VARCHAR(60)    NOT NULL,
    aplicada_por UUID           NOT NULL REFERENCES usuario (id),
    proposta_id  UUID,
    situacao     VARCHAR(20)    NOT NULL,
    revogada_em  TIMESTAMPTZ
);

CREATE INDEX ix_sancao_alvo ON sancao (alvo_tipo, alvo_id, inicio DESC);

CREATE TABLE recurso (
    id              UUID           PRIMARY KEY,
    origem_tipo     VARCHAR(20)    NOT NULL,
    origem_id       UUID           NOT NULL,
    autor_id        UUID           NOT NULL REFERENCES usuario (id),
    decidido_antes  UUID,
    argumentos      VARCHAR(4000)  NOT NULL,
    anexos          VARCHAR(2000),
    situacao        VARCHAR(20)    NOT NULL,
    prazo           TIMESTAMPTZ    NOT NULL,
    julgado_por     UUID,
    julgado_em      TIMESTAMPTZ,
    motivo          VARCHAR(2000),
    alertado_em     TIMESTAMPTZ,
    criado_em       TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ux_recurso_origem UNIQUE (origem_tipo, origem_id)
);

CREATE TABLE chamado (
    id                     UUID           PRIMARY KEY,
    usuario_id             UUID           REFERENCES usuario (id),
    email_contato          VARCHAR(254)   NOT NULL,
    nome_contato           VARCHAR(120)   NOT NULL,
    categoria              VARCHAR(60)    NOT NULL,
    servico                VARCHAR(40)    NOT NULL,
    descricao              VARCHAR(4000)  NOT NULL,
    prioridade             VARCHAR(10)    NOT NULL,
    nivel                  VARCHAR(20)    NOT NULL,
    situacao               VARCHAR(30)    NOT NULL,
    responsavel_id         UUID,
    prazo_primeira_resposta TIMESTAMPTZ   NOT NULL,
    primeira_resposta_em   TIMESTAMPTZ,
    aguardando_desde       TIMESTAMPTZ,
    outra_parte_id         UUID           REFERENCES usuario (id),
    referencia_tipo        VARCHAR(30),
    referencia_id          UUID,
    resultado_disputa      VARCHAR(20),
    nota                   INTEGER,
    comentario_avaliacao   VARCHAR(1000),
    token_confirmacao_hash VARCHAR(64),
    alertado_em            TIMESTAMPTZ,
    resolvido_em           TIMESTAMPTZ,
    escalonamento          VARCHAR(500),
    criado_em              TIMESTAMPTZ    NOT NULL,
    atualizado_em          TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_chamado_nota CHECK (nota IS NULL OR (nota BETWEEN 1 AND 5))
);

CREATE INDEX ix_chamado_fila ON chamado (situacao, nivel, criado_em);
CREATE INDEX ix_chamado_usuario ON chamado (usuario_id, criado_em DESC);

CREATE TABLE chamado_mensagem (
    id          UUID           PRIMARY KEY,
    chamado_id  UUID           NOT NULL REFERENCES chamado (id),
    autor_id    UUID           REFERENCES usuario (id),
    equipe      BOOLEAN        NOT NULL,
    interna     BOOLEAN        NOT NULL,
    texto       VARCHAR(4000)  NOT NULL,
    em          TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_chamado_mensagem ON chamado_mensagem (chamado_id, em);

CREATE TABLE artigo_ajuda (
    id             UUID           PRIMARY KEY,
    titulo         VARCHAR(160)   NOT NULL,
    conteudo       VARCHAR(8000)  NOT NULL,
    publicado      BOOLEAN        NOT NULL,
    atualizado_por UUID           REFERENCES usuario (id),
    atualizado_em  TIMESTAMPTZ    NOT NULL
);

CREATE TABLE resposta_padrao (
    id             UUID           PRIMARY KEY,
    titulo         VARCHAR(160)   NOT NULL,
    texto          VARCHAR(4000)  NOT NULL,
    atualizado_por UUID           REFERENCES usuario (id),
    atualizado_em  TIMESTAMPTZ    NOT NULL
);

INSERT INTO artigo_ajuda (id, titulo, conteudo, publicado, atualizado_em) VALUES
(gen_random_uuid(), 'Como funciona a verificação', 'Envie os documentos pedidos em Verificação. O time de verificação analisa em até 2 dias úteis e você recebe o resultado nas notificações.', TRUE, now()),
(gen_random_uuid(), 'Como publicar a startup', 'Preencha os campos obrigatórios, escolha o que é público na aba Visibilidade e envie para publicação. A curadoria revisa em até 3 dias úteis.', TRUE, now()),
(gen_random_uuid(), 'O que é o Match', 'O Match acontece quando a startup aceita o seu interesse. Ele não é compromisso de investimento e não libera as informações confidenciais.', TRUE, now());
