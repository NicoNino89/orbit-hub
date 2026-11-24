-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 01:00
-- Cartão Jira: KAN-12
-- Título: Tokens de uso único da conta
-- Objetivo: Guardar os links enviados por e-mail: confirmação de conta (KAN-12), redefinição de senha
--           (KAN-14) e confirmação de troca de e-mail (KAN-16).
-- Decisões: Só o hash SHA-256 do token é guardado; cada token tem finalidade, validade e uso único. "dado"
--           guarda o novo e-mail na troca de e-mail, para o endereço só mudar depois da confirmação.

CREATE TABLE token_conta (
    id         UUID         PRIMARY KEY,
    usuario_id UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    finalidade VARCHAR(30)  NOT NULL,
    hash       VARCHAR(64)  NOT NULL,
    dado       VARCHAR(254),
    criado_em  TIMESTAMPTZ  NOT NULL,
    expira_em  TIMESTAMPTZ  NOT NULL,
    usado_em   TIMESTAMPTZ,
    CONSTRAINT uk_token_conta_hash UNIQUE (hash)
);

CREATE INDEX ix_token_conta_usuario ON token_conta (usuario_id, finalidade);
