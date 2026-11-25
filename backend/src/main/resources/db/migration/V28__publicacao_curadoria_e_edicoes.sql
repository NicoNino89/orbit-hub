-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 10:40
-- Cartão Jira: KAN-28, KAN-66, KAN-84, KAN-85
-- Título: Visibilidade, curadoria e edições temáticas
-- Objetivo: Guardar o que cada startup mostra no catálogo (Nível 1), a revisão da curadoria com o checklist e os
--           apontamentos, os valores aprovados que o catálogo exibe enquanto uma alteração relevante está em revisão, e
--           as edições temáticas montadas pelos curadores, com a ordem e o histórico.
-- Decisões: A visibilidade guarda só o que difere do padrão do código (VisibilidadeDaStartup). Os valores aprovados
--           (nome, segmento e estágio) ficam na própria startup e só mudam quando a curadoria aprova. Itens do checklist e
--           apontamentos por seção ficam na mesma tabela, separados pelo tipo. Edições: vigência em datas (dia inteiro,
--           horário de Brasília), situação calculada pela rotina R10.

ALTER TABLE startup
    ADD COLUMN nome_aprovado       VARCHAR(120),
    ADD COLUMN segmento_aprovado   VARCHAR(40),
    ADD COLUMN estagio_aprovado    VARCHAR(40),
    ADD COLUMN participa_edicoes   BOOLEAN NOT NULL DEFAULT TRUE;

CREATE TABLE startup_visibilidade (
    startup_id  UUID         NOT NULL REFERENCES startup (id),
    item        VARCHAR(60)  NOT NULL,
    publico     BOOLEAN      NOT NULL,
    PRIMARY KEY (startup_id, item)
);

CREATE TABLE revisao_curadoria (
    id              UUID          PRIMARY KEY,
    startup_id      UUID          NOT NULL REFERENCES startup (id),
    tipo            VARCHAR(20)   NOT NULL,
    situacao        VARCHAR(20)   NOT NULL,
    enviada_em      TIMESTAMPTZ   NOT NULL,
    enviada_por     UUID,
    responsavel_id  UUID,
    assumida_em     TIMESTAMPTZ,
    decidida_em     TIMESTAMPTZ,
    decidida_por    UUID,
    parecer         VARCHAR(2000),
    proposta_id     UUID,
    versao          BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX ix_revisao_curadoria_startup ON revisao_curadoria (startup_id, enviada_em DESC);
CREATE INDEX ix_revisao_curadoria_situacao ON revisao_curadoria (situacao, enviada_em);
CREATE UNIQUE INDEX ux_revisao_curadoria_aberta ON revisao_curadoria (startup_id)
    WHERE situacao IN ('EM_ANALISE', 'AGUARDANDO_APROVACAO');

CREATE TABLE revisao_curadoria_item (
    revisao_id  UUID           NOT NULL REFERENCES revisao_curadoria (id),
    ordem       INTEGER        NOT NULL,
    tipo        VARCHAR(20)    NOT NULL,
    codigo      VARCHAR(60)    NOT NULL,
    atendido    BOOLEAN,
    comentario  VARCHAR(1000),
    PRIMARY KEY (revisao_id, ordem)
);

CREATE TABLE edicao (
    id              UUID           PRIMARY KEY,
    titulo          VARCHAR(120)   NOT NULL,
    descricao       VARCHAR(1000)  NOT NULL,
    criterios       VARCHAR(1000)  NOT NULL,
    capa_chave      VARCHAR(512),
    capa_tipo       VARCHAR(40),
    inicio          DATE           NOT NULL,
    fim             DATE           NOT NULL,
    curador_id      UUID           NOT NULL,
    situacao        VARCHAR(20)    NOT NULL,
    criada_em       TIMESTAMPTZ    NOT NULL,
    atualizada_em   TIMESTAMPTZ    NOT NULL,
    versao          BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_edicao_vigencia CHECK (fim >= inicio)
);

CREATE INDEX ix_edicao_situacao ON edicao (situacao, inicio);

CREATE TABLE edicao_startup (
    edicao_id   UUID     NOT NULL REFERENCES edicao (id),
    ordem       INTEGER  NOT NULL,
    startup_id  UUID     NOT NULL REFERENCES startup (id),
    PRIMARY KEY (edicao_id, ordem)
);

CREATE TABLE edicao_historico (
    id         UUID           PRIMARY KEY,
    edicao_id  UUID           NOT NULL REFERENCES edicao (id),
    acao       VARCHAR(40)    NOT NULL,
    detalhe    VARCHAR(2000),
    por        UUID,
    em         TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_edicao_historico ON edicao_historico (edicao_id, em DESC);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('catalogo.capa_max_mb', 'catalogo', 'Tamanho máximo da capa de uma edição temática', 'INTEIRO', 'MEGABYTES', '2', '2', 1, 5, NULL, FALSE);
