-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 00:20
-- Cartão Jira: KAN-57
-- Título: Usuários e sessões (refresh tokens)
-- Objetivo: Criar a conta de usuário com seus papéis e a tabela de refresh tokens revogáveis.
-- Decisões: O banco guarda só o hash SHA-256 do refresh token, nunca o valor. Cada login abre uma
--           família (sessão); a rotação cria um token novo na mesma família e marca o anterior como
--           usado. E-mail guardado em minúsculas, com unicidade garantida pelo banco.

CREATE TABLE usuario (
    id            UUID         PRIMARY KEY,
    nome          VARCHAR(120) NOT NULL,
    email         VARCHAR(254) NOT NULL,
    senha_hash    VARCHAR(100) NOT NULL,
    status        VARCHAR(30)  NOT NULL,
    criado_em     TIMESTAMPTZ  NOT NULL,
    atualizado_em TIMESTAMPTZ  NOT NULL,
    versao        BIGINT       NOT NULL DEFAULT 0,
    CONSTRAINT uk_usuario_email UNIQUE (email),
    CONSTRAINT ck_usuario_email_minusculo CHECK (email = lower(email))
);

CREATE TABLE usuario_papel (
    usuario_id UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    papel      VARCHAR(30) NOT NULL,
    PRIMARY KEY (usuario_id, papel)
);

CREATE TABLE refresh_token (
    id                UUID        PRIMARY KEY,
    usuario_id        UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    familia_id        UUID        NOT NULL,
    hash              VARCHAR(64) NOT NULL,
    status            VARCHAR(20) NOT NULL,
    emitido_em        TIMESTAMPTZ NOT NULL,
    expira_em         TIMESTAMPTZ NOT NULL,
    familia_expira_em TIMESTAMPTZ NOT NULL,
    usado_em          TIMESTAMPTZ,
    revogado_em       TIMESTAMPTZ,
    motivo_revogacao  VARCHAR(40),
    CONSTRAINT uk_refresh_token_hash UNIQUE (hash)
);

CREATE INDEX ix_refresh_token_usuario ON refresh_token (usuario_id, status);
CREATE INDEX ix_refresh_token_familia ON refresh_token (familia_id, status);
