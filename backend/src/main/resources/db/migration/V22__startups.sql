-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 22:33
-- Cartão Jira: KAN-21
-- Título: Cadastro da startup
-- Objetivo: Guardar a startup com os dados cadastrais (nome, razão social, CNPJ, segmento, localização, site, redes,
--           logotipo), os fundadores com a participação societária e a equipe que gerencia a startup.
-- Decisões: CNPJ opcional (empresa ainda não constituída) e único quando informado. A participação societária é a
--           composição societária (cap table), sempre confidencial (Nível 2): nunca sai para o catálogo. A equipe nasce
--           com quem criou, como fundador administrador; os convites e os outros papéis entram com o KAN-65. O ciclo de
--           vida (rascunho → em revisão → publicada…) é do KAN-27; aqui a startup nasce em rascunho.

CREATE TABLE startup (
    id             UUID          PRIMARY KEY,
    nome           VARCHAR(120)  NOT NULL,
    nome_fantasia  VARCHAR(120),
    razao_social   VARCHAR(200),
    cnpj           VARCHAR(14),
    segmento       VARCHAR(40)   NOT NULL,
    localizacao    VARCHAR(40)   NOT NULL,
    site           VARCHAR(200),
    logotipo_chave VARCHAR(512),
    logotipo_tipo  VARCHAR(40),
    situacao       VARCHAR(30)   NOT NULL,
    criada_por     UUID          NOT NULL REFERENCES usuario (id),
    criada_em      TIMESTAMPTZ   NOT NULL,
    atualizada_em  TIMESTAMPTZ   NOT NULL,
    versao         BIGINT        NOT NULL DEFAULT 0,
    CONSTRAINT uk_startup_cnpj UNIQUE (cnpj),
    CONSTRAINT ck_startup_cnpj CHECK (cnpj IS NULL OR cnpj ~ '^[0-9]{14}$')
);

CREATE TABLE startup_rede (
    startup_id UUID         NOT NULL REFERENCES startup (id) ON DELETE CASCADE,
    rede       VARCHAR(20)  NOT NULL,
    url        VARCHAR(300) NOT NULL,
    PRIMARY KEY (startup_id, rede)
);

CREATE TABLE startup_fundador (
    startup_id   UUID          NOT NULL REFERENCES startup (id) ON DELETE CASCADE,
    nome         VARCHAR(120)  NOT NULL,
    cargo        VARCHAR(80),
    participacao NUMERIC(5, 2),
    ordem        INTEGER       NOT NULL,
    PRIMARY KEY (startup_id, ordem),
    CONSTRAINT ck_startup_fundador_participacao CHECK (participacao IS NULL OR (participacao >= 0 AND participacao <= 100))
);

CREATE TABLE startup_membro (
    id          UUID         PRIMARY KEY,
    startup_id  UUID         NOT NULL REFERENCES startup (id),
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    papel       VARCHAR(30)  NOT NULL,
    entrou_em   TIMESTAMPTZ  NOT NULL,
    saiu_em     TIMESTAMPTZ,
    versao      BIGINT       NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_startup_membro_ativo ON startup_membro (startup_id, usuario_id) WHERE saiu_em IS NULL;
CREATE INDEX ix_startup_membro_usuario ON startup_membro (usuario_id) WHERE saiu_em IS NULL;

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('startup.logotipo_max_mb', 'startup', 'Tamanho máximo do logotipo da startup', 'INTEIRO', 'MEGABYTES', '1', '1', 1, 5, NULL, FALSE);
