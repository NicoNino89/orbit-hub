-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 14:05
-- Cartão Jira: KAN-60
-- Título: Organizações investidoras
-- Objetivo: Guardar fundos, redes de anjos e outras entidades investidoras, os membros com o papel de cada um e os
--           convites por e-mail.
-- Decisões: O membro que sai continua na tabela, com a data de saída, para o histórico de quem agiu em nome da
--           organização; só um vínculo ativo por pessoa e organização. O CNPJ é único entre as organizações (só
--           dígitos). O convite guarda apenas o hash do token e vale por convite.validade_dias; pode ir para quem
--           ainda não tem conta.

CREATE TABLE organizacao (
    id            UUID          PRIMARY KEY,
    nome          VARCHAR(160)  NOT NULL,
    tipo          VARCHAR(40)   NOT NULL,
    cnpj          VARCHAR(14)   NOT NULL,
    site          VARCHAR(200),
    descricao     VARCHAR(2000),
    criada_por    UUID          NOT NULL REFERENCES usuario (id),
    criada_em     TIMESTAMPTZ   NOT NULL,
    atualizada_em TIMESTAMPTZ   NOT NULL,
    versao        BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_organizacao_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_organizacao_cnpj CHECK (cnpj ~ '^[0-9]{14}$')
);

CREATE TABLE organizacao_membro (
    id             UUID         PRIMARY KEY,
    organizacao_id UUID         NOT NULL REFERENCES organizacao (id),
    usuario_id     UUID         NOT NULL REFERENCES usuario (id),
    papel          VARCHAR(20)  NOT NULL,
    entrou_em      TIMESTAMPTZ  NOT NULL,
    saiu_em        TIMESTAMPTZ,
    motivo_saida   VARCHAR(20),
    versao         BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_organizacao_membro_ativo ON organizacao_membro (organizacao_id, usuario_id) WHERE saiu_em IS NULL;
CREATE INDEX ix_organizacao_membro_usuario ON organizacao_membro (usuario_id) WHERE saiu_em IS NULL;

CREATE TABLE organizacao_convite (
    id             UUID         PRIMARY KEY,
    organizacao_id UUID         NOT NULL REFERENCES organizacao (id),
    email          VARCHAR(254) NOT NULL,
    papel          VARCHAR(20)  NOT NULL,
    token_hash     VARCHAR(64)  NOT NULL,
    convidado_por  UUID         NOT NULL REFERENCES usuario (id),
    criado_em      TIMESTAMPTZ  NOT NULL,
    expira_em      TIMESTAMPTZ  NOT NULL,
    aceito_em      TIMESTAMPTZ,
    aceito_por     UUID         REFERENCES usuario (id),
    cancelado_em   TIMESTAMPTZ,
    CONSTRAINT uk_organizacao_convite_token UNIQUE (token_hash)
);

CREATE INDEX ix_organizacao_convite_pendente ON organizacao_convite (organizacao_id)
    WHERE aceito_em IS NULL AND cancelado_em IS NULL;
