-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 10:52
-- Cartão Jira: KAN-81
-- Título: Ações com segunda aprovação
-- Objetivo: Guardar as propostas de ações graves (banimento definitivo, remoção de startup por fraude,
--           transferência de titularidade, parâmetro sensível, dados pessoais fora do fluxo), que só são
--           executadas depois que uma segunda pessoa aprova.
-- Decisões: Os dados de que a ação precisa ficam em pares chave/valor (proposta_aprovacao_dado), para cada
--           módulo guardar o que precisa sem JSON solto. "versao" evita duas decisões ao mesmo tempo. Um índice
--           único parcial impede duas propostas pendentes do mesmo tipo sobre o mesmo objeto. "alertada_em"
--           marca o alerta de proposta parada há mais de aprovacao.prazo_horas, para não alertar duas vezes.

CREATE TABLE proposta_aprovacao (
    id                    UUID          PRIMARY KEY,
    tipo                  VARCHAR(40)   NOT NULL,
    objeto_tipo           VARCHAR(60)   NOT NULL,
    objeto_id             VARCHAR(80)   NOT NULL,
    resumo                VARCHAR(300)  NOT NULL,
    justificativa         VARCHAR(1000) NOT NULL,
    proposto_por          UUID          NOT NULL REFERENCES usuario (id),
    proposto_em           TIMESTAMPTZ   NOT NULL,
    situacao              VARCHAR(20)   NOT NULL,
    decidido_por          UUID          REFERENCES usuario (id),
    decidido_em           TIMESTAMPTZ,
    justificativa_decisao VARCHAR(1000),
    alertada_em           TIMESTAMPTZ,
    versao                BIGINT        NOT NULL DEFAULT 0
);

CREATE INDEX ix_proposta_situacao ON proposta_aprovacao (situacao, proposto_em);
CREATE UNIQUE INDEX uk_proposta_pendente_por_objeto ON proposta_aprovacao (tipo, objeto_tipo, objeto_id)
    WHERE situacao = 'PENDENTE';

CREATE TABLE proposta_aprovacao_dado (
    proposta_id UUID          NOT NULL REFERENCES proposta_aprovacao (id),
    chave       VARCHAR(60)   NOT NULL,
    valor       VARCHAR(2000),
    PRIMARY KEY (proposta_id, chave)
);
