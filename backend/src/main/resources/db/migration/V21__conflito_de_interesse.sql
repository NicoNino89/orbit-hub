-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 17:43
-- Cartão Jira: KAN-87
-- Título: Conflito de interesse
-- Objetivo: Guardar os vínculos que cada membro da equipe declara (organizações, startups, pessoas) e os impedimentos
--           declarados em casos específicos, para não atribuir a alguém um caso em que tenha interesse.
-- Decisões: Organização e startup são reconhecidas pelo CNPJ (a conta interna é separada das contas públicas, então o
--           vínculo não pode vir da participação na plataforma); pessoa, pelo e-mail. O impedimento vale por caso
--           (tipo + identificador), para servir a verificação, curadoria, moderação, disputa e recurso.

CREATE TABLE vinculo_membro (
    id         UUID         PRIMARY KEY,
    membro_id  UUID         NOT NULL REFERENCES usuario (id),
    tipo       VARCHAR(20)  NOT NULL,
    cnpj       VARCHAR(14),
    email      VARCHAR(254),
    descricao  VARCHAR(200) NOT NULL,
    criado_em  TIMESTAMPTZ  NOT NULL,
    CONSTRAINT ck_vinculo_membro_alvo CHECK (
        (tipo IN ('ORGANIZACAO', 'STARTUP') AND cnpj ~ '^[0-9]{14}$' AND email IS NULL)
        OR (tipo = 'PESSOA' AND email IS NOT NULL AND cnpj IS NULL))
);

CREATE INDEX ix_vinculo_membro_membro ON vinculo_membro (membro_id);
CREATE INDEX ix_vinculo_membro_cnpj ON vinculo_membro (cnpj) WHERE cnpj IS NOT NULL;

CREATE TABLE impedimento_caso (
    id        UUID         PRIMARY KEY,
    caso_tipo VARCHAR(40)  NOT NULL,
    caso_id   UUID         NOT NULL,
    membro_id UUID         NOT NULL REFERENCES usuario (id),
    motivo    VARCHAR(500) NOT NULL,
    criado_em TIMESTAMPTZ  NOT NULL,
    CONSTRAINT uk_impedimento_caso UNIQUE (caso_tipo, caso_id, membro_id)
);
