-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 22:48
-- Cartão Jira: KAN-65
-- Título: Equipe da startup
-- Objetivo: Guardar as seções que cada colaborador pode editar, o motivo da saída de um membro e os convites para a
--           equipe da startup.
-- Decisões: Mesmo modelo das organizações investidoras (KAN-60): convite por e-mail com token (só o hash), aceito pela
--           conta com o mesmo e-mail e o papel de empreendedor. As seções do convite de colaborador vão como texto
--           separado por vírgulas (códigos do enum de seções).

ALTER TABLE startup_membro ADD COLUMN motivo_saida VARCHAR(20);

CREATE TABLE startup_membro_secao (
    membro_id UUID        NOT NULL REFERENCES startup_membro (id) ON DELETE CASCADE,
    secao     VARCHAR(40) NOT NULL,
    PRIMARY KEY (membro_id, secao)
);

CREATE TABLE startup_convite (
    id            UUID         PRIMARY KEY,
    startup_id    UUID         NOT NULL REFERENCES startup (id),
    email         VARCHAR(254) NOT NULL,
    papel         VARCHAR(30)  NOT NULL,
    secoes        VARCHAR(500),
    token_hash    VARCHAR(64)  NOT NULL,
    convidado_por UUID         NOT NULL REFERENCES usuario (id),
    criado_em     TIMESTAMPTZ  NOT NULL,
    expira_em     TIMESTAMPTZ  NOT NULL,
    aceito_em     TIMESTAMPTZ,
    aceito_por    UUID         REFERENCES usuario (id),
    cancelado_em  TIMESTAMPTZ,
    CONSTRAINT uk_startup_convite_token UNIQUE (token_hash)
);

CREATE INDEX ix_startup_convite_pendente ON startup_convite (startup_id) WHERE aceito_em IS NULL AND cancelado_em IS NULL;
