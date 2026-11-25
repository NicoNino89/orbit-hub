-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 23:51
-- Cartão Jira: KAN-59
-- Título: Rodadas de investimento
-- Objetivo: Guardar a rodada da startup (estágio, instrumento, valores, prazo e termos específicos do instrumento), o
--           ciclo rascunho → aberta → em negociação → encerrada e o histórico das mudanças.
-- Decisões: A rodada é a fonte única dos termos de captação (KAN-25 guarda só o contexto). Uma rodada não encerrada por
--           startup (índice único parcial). Termos específicos numa tabela chave-valor validada pelo catálogo no código
--           (TermoDaRodada). Participantes e investidor líder chegam com o Match (S7).

CREATE TABLE rodada (
    id            UUID          PRIMARY KEY,
    startup_id    UUID          NOT NULL REFERENCES startup (id),
    situacao      VARCHAR(20)   NOT NULL,
    resultado     VARCHAR(20),
    estagio       VARCHAR(60),
    instrumento   VARCHAR(60),
    valor_alvo    NUMERIC(18,2),
    valor_minimo  NUMERIC(18,2),
    valor_maximo  NUMERIC(18,2),
    prazo         DATE,
    criada_por    UUID          NOT NULL REFERENCES usuario (id),
    criada_em     TIMESTAMPTZ   NOT NULL,
    atualizada_em TIMESTAMPTZ   NOT NULL,
    aberta_em     TIMESTAMPTZ,
    encerrada_em  TIMESTAMPTZ,
    versao        BIGINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_rodada_em_andamento ON rodada (startup_id) WHERE situacao <> 'ENCERRADA';

CREATE TABLE rodada_termo (
    rodada_id UUID        NOT NULL REFERENCES rodada (id) ON DELETE CASCADE,
    termo     VARCHAR(40) NOT NULL,
    valor     TEXT        NOT NULL,
    PRIMARY KEY (rodada_id, termo)
);

CREATE TABLE rodada_historico (
    id        UUID         PRIMARY KEY,
    rodada_id UUID         NOT NULL REFERENCES rodada (id) ON DELETE CASCADE,
    de        VARCHAR(20)  NOT NULL,
    para      VARCHAR(20)  NOT NULL,
    por       UUID         REFERENCES usuario (id),
    motivo    VARCHAR(300),
    em        TIMESTAMPTZ  NOT NULL
);
