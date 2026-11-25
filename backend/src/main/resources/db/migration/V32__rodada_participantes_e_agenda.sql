-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 14:23
-- Cartão Jira: KAN-59, KAN-64, KAN-106, KAN-109, KAN-94, KAN-95, KAN-96, KAN-97, KAN-98
-- Título: Participantes, líder e resultado da rodada; agenda de reuniões
-- Objetivo: Completar a rodada (participantes com intenção, convites do líder, investidor líder, política de alocação,
--           prorrogações, pedido de encerramento, espera do resultado e investimentos confirmados pelas partes) e guardar
--           as reuniões (propostas com opções, contrapropostas, confirmação, participantes, registros, ausências,
--           anotações privadas e reuniões coletivas da rodada).
-- Decisões: Participante guarda a situação da conexão antes de entrar, para o cancelamento devolver. O investidor é a
--           organização ou a pessoa (mesma chave das conexões). Valores individuais ficam só com a startup e o próprio
--           investidor. Reuniões com até 3 opções por proposta; horários em UTC (a tela mostra no fuso de cada um).

ALTER TABLE rodada
    ADD COLUMN politica_alocacao        VARCHAR(30) NOT NULL DEFAULT 'LIDER_PRIMEIRO',
    ADD COLUMN lider_chave              UUID,
    ADD COLUMN lider_convidado_chave    UUID,
    ADD COLUMN prorrogacoes             INTEGER     NOT NULL DEFAULT 0,
    ADD COLUMN aguardando_desde         TIMESTAMPTZ,
    ADD COLUMN aguardando_lembrete_em   TIMESTAMPTZ,
    ADD COLUMN encerramento_pedido_em   TIMESTAMPTZ,
    ADD COLUMN valor_captado            NUMERIC(18,2),
    ADD COLUMN instrumento_utilizado    VARCHAR(60),
    ADD COLUMN data_fechamento          DATE;

CREATE TABLE rodada_participante (
    id                 UUID           PRIMARY KEY,
    rodada_id          UUID           NOT NULL REFERENCES rodada (id),
    investidor_chave   UUID           NOT NULL,
    pessoa_id          UUID           NOT NULL REFERENCES usuario (id),
    organizacao_id     UUID           REFERENCES organizacao (id),
    conexao_id         UUID           REFERENCES conexao (id),
    situacao           VARCHAR(30)    NOT NULL,
    convidado_por      UUID           REFERENCES usuario (id),
    intencao           NUMERIC(18,2),
    condicoes          VARCHAR(1000),
    conexao_anterior   VARCHAR(20),
    entrou_em          TIMESTAMPTZ    NOT NULL,
    atualizado_em      TIMESTAMPTZ    NOT NULL
);

CREATE UNIQUE INDEX ux_rodada_participante ON rodada_participante (rodada_id, investidor_chave) WHERE situacao <> 'SAIU';

CREATE TABLE rodada_investimento (
    id                UUID           PRIMARY KEY,
    rodada_id         UUID           NOT NULL REFERENCES rodada (id),
    participante_id   UUID           NOT NULL REFERENCES rodada_participante (id),
    valor             NUMERIC(18,2),
    situacao          VARCHAR(20)    NOT NULL,
    criado_em         TIMESTAMPTZ    NOT NULL,
    lembrado_em       TIMESTAMPTZ,
    respondido_em     TIMESTAMPTZ,
    respondido_por    UUID
);

CREATE TABLE reuniao (
    id                     UUID           PRIMARY KEY,
    conexao_id             UUID           REFERENCES conexao (id),
    rodada_id              UUID           REFERENCES rodada (id),
    startup_id             UUID           NOT NULL REFERENCES startup (id),
    tipo                   VARCHAR(20)    NOT NULL,
    formato                VARCHAR(20)    NOT NULL,
    local                  VARCHAR(500)   NOT NULL,
    pauta                  VARCHAR(1000)  NOT NULL,
    duracao_minutos        INTEGER        NOT NULL,
    coletiva               BOOLEAN        NOT NULL,
    situacao               VARCHAR(20)    NOT NULL,
    lado_da_vez            VARCHAR(20),
    contrapropostas        INTEGER        NOT NULL DEFAULT 0,
    inicio                 TIMESTAMPTZ,
    convocada_por          UUID           NOT NULL REFERENCES usuario (id),
    motivo                 VARCHAR(300),
    tardia                 BOOLEAN        NOT NULL DEFAULT FALSE,
    lembretes              INTEGER        NOT NULL DEFAULT 0,
    lembrete_registro_em   TIMESTAMPTZ,
    resumo                 VARCHAR(2000),
    criada_em              TIMESTAMPTZ    NOT NULL,
    atualizada_em          TIMESTAMPTZ    NOT NULL,
    CONSTRAINT ck_reuniao_origem CHECK (conexao_id IS NOT NULL OR rodada_id IS NOT NULL)
);

CREATE INDEX ix_reuniao_conexao ON reuniao (conexao_id);
CREATE INDEX ix_reuniao_situacao ON reuniao (situacao, inicio);

CREATE TABLE reuniao_opcao (
    reuniao_id  UUID         NOT NULL REFERENCES reuniao (id),
    ordem       INTEGER      NOT NULL,
    inicio      TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (reuniao_id, ordem)
);

CREATE TABLE reuniao_participante (
    reuniao_id  UUID          NOT NULL REFERENCES reuniao (id),
    usuario_id  UUID          NOT NULL REFERENCES usuario (id),
    lado        VARCHAR(20)   NOT NULL,
    resposta    VARCHAR(20)   NOT NULL DEFAULT 'PENDENTE',
    PRIMARY KEY (reuniao_id, usuario_id)
);

CREATE TABLE reuniao_registro (
    reuniao_id    UUID          NOT NULL REFERENCES reuniao (id),
    lado          VARCHAR(20)   NOT NULL,
    resultado     VARCHAR(30)   NOT NULL,
    proximo_passo VARCHAR(30),
    por           UUID          NOT NULL REFERENCES usuario (id),
    em            TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (reuniao_id, lado)
);

CREATE TABLE reuniao_anotacao (
    reuniao_id     UUID           NOT NULL REFERENCES reuniao (id),
    usuario_id     UUID           NOT NULL REFERENCES usuario (id),
    texto          VARCHAR(4000)  NOT NULL,
    compartilhada  BOOLEAN        NOT NULL,
    em             TIMESTAMPTZ    NOT NULL,
    PRIMARY KEY (reuniao_id, usuario_id)
);

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
SELECT gen_random_uuid(), v.lista, v.codigo, v.rotulo, NULL, v.ordem, now(), now()
FROM (VALUES
    ('MOTIVO_RECUSA_REUNIAO', 'SEM_DISPONIBILIDADE', 'Sem disponibilidade nos horários', 1),
    ('MOTIVO_RECUSA_REUNIAO', 'CEDO_DEMAIS', 'Ainda é cedo para uma reunião', 2),
    ('MOTIVO_RECUSA_REUNIAO', 'PREFERE_MENSAGEM', 'Prefere conversar por mensagem', 3),
    ('MOTIVO_RECUSA_REUNIAO', 'OUTRO', 'Outro motivo', 99)
) AS v(lista, codigo, rotulo, ordem);
