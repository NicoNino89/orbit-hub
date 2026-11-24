-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 14:16
-- Cartão Jira: KAN-61
-- Título: Perfil de investimento
-- Objetivo: Guardar como cada investidor (pessoa ou organização) investe: tipo, ticket mínimo e máximo, liderança,
--           follow-on, participação na gestão e instrumentos aceitos.
-- Decisões: Um perfil por dono, que é a pessoa ou a organização (nunca os dois). Os campos da tese (segmentos,
--           estágios, maturidade e localização) chegam com o KAN-18, na mesma tabela. Valores em reais, com centavos.
--           Classificação regulatória (CVM 30) fica de fora até controlar algum acesso (minimização, LGPD).

CREATE TABLE perfil_investimento (
    id                  UUID           PRIMARY KEY,
    pessoa_id           UUID           REFERENCES usuario (id),
    organizacao_id      UUID           REFERENCES organizacao (id),
    tipo                VARCHAR(40)    NOT NULL,
    tipo_outro          VARCHAR(120),
    ticket_minimo       NUMERIC(15, 2) NOT NULL,
    ticket_maximo       NUMERIC(15, 2) NOT NULL,
    lideranca           VARCHAR(20)    NOT NULL,
    follow_on           BOOLEAN        NOT NULL,
    participacao_gestao VARCHAR(30)    NOT NULL,
    atualizado_por      UUID           NOT NULL REFERENCES usuario (id),
    criado_em           TIMESTAMPTZ    NOT NULL,
    atualizado_em       TIMESTAMPTZ    NOT NULL,
    versao              BIGINT         NOT NULL DEFAULT 0,
    CONSTRAINT ck_perfil_um_dono CHECK ((pessoa_id IS NULL) <> (organizacao_id IS NULL)),
    CONSTRAINT ck_perfil_ticket CHECK (ticket_minimo > 0 AND ticket_maximo >= ticket_minimo),
    CONSTRAINT uk_perfil_pessoa UNIQUE (pessoa_id),
    CONSTRAINT uk_perfil_organizacao UNIQUE (organizacao_id)
);

CREATE TABLE perfil_investimento_instrumento (
    perfil_id   UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    instrumento VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, instrumento)
);

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
VALUES (gen_random_uuid(), 'INSTRUMENTO', 'OUTROS_LC_182',
        'Outros instrumentos previstos no Marco Legal das Startups (LC 182/2021)', NULL, 99, now(), now());
