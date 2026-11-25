-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 11:25
-- Cartão Jira: KAN-45, KAN-46, KAN-48, KAN-49, KAN-67, KAN-105
-- Título: Conexões entre investidores e startups, e bloqueios
-- Objetivo: Guardar cada manifestação de interesse e o seu ciclo (pendente → Match → análise → negociação → investiu,
--           ou recusada, expirada, retirada, encerrada), o histórico das mudanças com quem fez, os lembretes da R2, os
--           bloqueios de investidores pela startup e a escolha da startup de aceitar só investidores verificados.
-- Decisões: O investidor é a pessoa ou a organização (pessoa e organização são investidores diferentes): a chave é a
--           organização, quando houver, senão a pessoa. Uma conexão não final por investidor e startup (índice único
--           parcial). O motivo de encerramento fica só com quem encerrou e com a plataforma. Bloqueio por pessoa ou por
--           organização, com histórico de bloqueios e desbloqueios.

CREATE TABLE conexao (
    id                    UUID           PRIMARY KEY,
    startup_id            UUID           NOT NULL REFERENCES startup (id),
    pessoa_id             UUID           NOT NULL REFERENCES usuario (id),
    organizacao_id        UUID           REFERENCES organizacao (id),
    investidor_chave      UUID           NOT NULL,
    situacao              VARCHAR(20)    NOT NULL,
    valor_preliminar      NUMERIC(18,2),
    condicoes             VARCHAR(1000),
    pedido                VARCHAR(20)    NOT NULL,
    mensagem              VARCHAR(1000),
    motivo                VARCHAR(60),
    motivo_lado           VARCHAR(20),
    lembretes             INTEGER        NOT NULL DEFAULT 0,
    criada_em             TIMESTAMPTZ    NOT NULL,
    atualizada_em         TIMESTAMPTZ    NOT NULL,
    versao                BIGINT         NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_conexao_ativa ON conexao (startup_id, investidor_chave)
    WHERE situacao IN ('PENDENTE', 'MATCH', 'EM_ANALISE', 'EM_NEGOCIACAO');
CREATE INDEX ix_conexao_startup ON conexao (startup_id, atualizada_em DESC);
CREATE INDEX ix_conexao_investidor ON conexao (investidor_chave, atualizada_em DESC);
CREATE INDEX ix_conexao_situacao ON conexao (situacao, criada_em);

CREATE TABLE conexao_historico (
    id          UUID          PRIMARY KEY,
    conexao_id  UUID          NOT NULL REFERENCES conexao (id),
    de          VARCHAR(20),
    para        VARCHAR(20)   NOT NULL,
    por         UUID,
    lado        VARCHAR(20)   NOT NULL,
    em          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_conexao_historico ON conexao_historico (conexao_id, em);

ALTER TABLE startup ADD COLUMN apenas_verificados BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE startup_bloqueio (
    startup_id  UUID          NOT NULL REFERENCES startup (id),
    alvo_tipo   VARCHAR(20)   NOT NULL,
    alvo_id     UUID          NOT NULL,
    rotulo      VARCHAR(200)  NOT NULL,
    criado_por  UUID          NOT NULL REFERENCES usuario (id),
    criado_em   TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (startup_id, alvo_tipo, alvo_id)
);

CREATE TABLE startup_bloqueio_historico (
    id          UUID          PRIMARY KEY,
    startup_id  UUID          NOT NULL REFERENCES startup (id),
    acao        VARCHAR(20)   NOT NULL,
    alvo_tipo   VARCHAR(20)   NOT NULL,
    rotulo      VARCHAR(200)  NOT NULL,
    por         UUID          NOT NULL REFERENCES usuario (id),
    em          TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_startup_bloqueio_historico ON startup_bloqueio_historico (startup_id, em DESC);
