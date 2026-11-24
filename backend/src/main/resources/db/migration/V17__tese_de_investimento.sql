-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 14:25
-- Cartão Jira: KAN-18
-- Título: Tese de investimento
-- Objetivo: Acrescentar ao perfil de investimento os segmentos, os estágios de rodada, as maturidades de negócio e as
--           localizações de interesse, e registrar quem pulou o onboarding para completar depois.
-- Decisões: Uma tabela por campo de escolha múltipla, com o código da lista (ou da escala de maturidade). Pular o
--           onboarding é da pessoa (não da organização): ela pode ver o resto da plataforma e completar depois.

CREATE TABLE perfil_investimento_segmento (
    perfil_id UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    segmento  VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, segmento)
);

CREATE TABLE perfil_investimento_estagio (
    perfil_id UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    estagio   VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, estagio)
);

CREATE TABLE perfil_investimento_maturidade (
    perfil_id  UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    maturidade VARCHAR(30) NOT NULL,
    PRIMARY KEY (perfil_id, maturidade)
);

CREATE TABLE perfil_investimento_localizacao (
    perfil_id   UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    localizacao VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, localizacao)
);

CREATE TABLE investidor_onboarding (
    usuario_id UUID        PRIMARY KEY REFERENCES usuario (id),
    pulado_em  TIMESTAMPTZ NOT NULL
);
