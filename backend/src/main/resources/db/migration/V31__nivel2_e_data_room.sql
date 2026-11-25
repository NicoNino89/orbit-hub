-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 12:04
-- Cartão Jira: KAN-30, KAN-31, KAN-32, KAN-33, KAN-63, KAN-145, KAN-105
-- Título: Nível 2 (acessos confidenciais) e Data Room básico
-- Objetivo: Guardar a regra de confidencialidade de cada startup (termo da plataforma ou NDA bilateral), as confirmações
--           do NDA por conexão, o aceite do termo pelas organizações, as liberações de acesso ao Nível 2 com validade,
--           o histórico imutável de acessos e os documentos do Data Room com versões.
-- Decisões: Uma liberação ativa por conexão. O histórico só recebe inclusões (sem atualização nem exclusão pela API).
--           Documentos substituídos continuam guardados, marcados como versão anterior. Link temporário guarda só o
--           hash do token. Formatos do Data Room por conjunto (parâmetro de opção) e tamanho por parâmetro.

ALTER TABLE startup
    ADD COLUMN regra_confidencialidade VARCHAR(20) NOT NULL DEFAULT 'TERMO_PLATAFORMA';

CREATE TABLE organizacao_aceite_termo (
    organizacao_id  UUID         NOT NULL REFERENCES organizacao (id),
    documento_id    UUID         NOT NULL REFERENCES documento_legal (id),
    aceito_por      UUID         NOT NULL REFERENCES usuario (id),
    aceito_em       TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (organizacao_id, documento_id)
);

CREATE TABLE dataroom_documento (
    id            UUID           PRIMARY KEY,
    startup_id    UUID           NOT NULL REFERENCES startup (id),
    categoria     VARCHAR(40)    NOT NULL,
    nome          VARCHAR(160)   NOT NULL,
    descricao     VARCHAR(300),
    chave         VARCHAR(512)   NOT NULL,
    tipo          VARCHAR(100)   NOT NULL,
    tamanho       BIGINT         NOT NULL,
    versao        INTEGER        NOT NULL,
    origem_id     UUID,
    atual         BOOLEAN        NOT NULL,
    enviado_por   UUID           NOT NULL REFERENCES usuario (id),
    enviado_em    TIMESTAMPTZ    NOT NULL,
    removido_em   TIMESTAMPTZ
);

CREATE INDEX ix_dataroom_documento ON dataroom_documento (startup_id, categoria, enviado_em DESC);

CREATE TABLE nda_confirmacao (
    conexao_id    UUID          NOT NULL REFERENCES conexao (id),
    lado          VARCHAR(20)   NOT NULL,
    confirmado_por UUID         NOT NULL REFERENCES usuario (id),
    confirmado_em TIMESTAMPTZ   NOT NULL,
    PRIMARY KEY (conexao_id, lado)
);

CREATE TABLE acesso_nivel2 (
    id               UUID          PRIMARY KEY,
    startup_id       UUID          NOT NULL REFERENCES startup (id),
    conexao_id       UUID          NOT NULL REFERENCES conexao (id),
    investidor_chave UUID          NOT NULL,
    liberado_por     UUID          NOT NULL REFERENCES usuario (id),
    liberado_em      TIMESTAMPTZ   NOT NULL,
    valido_ate       TIMESTAMPTZ   NOT NULL,
    encerrado_em     TIMESTAMPTZ,
    encerrado_motivo VARCHAR(20),
    encerrado_por    UUID,
    avisado_em       TIMESTAMPTZ,
    versao           BIGINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX ux_acesso_nivel2_ativo ON acesso_nivel2 (conexao_id) WHERE encerrado_em IS NULL;
CREATE INDEX ix_acesso_nivel2_startup ON acesso_nivel2 (startup_id, liberado_em DESC);
CREATE INDEX ix_acesso_nivel2_vencimento ON acesso_nivel2 (valido_ate) WHERE encerrado_em IS NULL;

CREATE TABLE acesso_nivel2_evento (
    id               UUID          PRIMARY KEY,
    startup_id       UUID          NOT NULL REFERENCES startup (id),
    acesso_id        UUID          REFERENCES acesso_nivel2 (id),
    investidor_chave UUID          NOT NULL,
    tipo             VARCHAR(20)   NOT NULL,
    documento_id     UUID,
    por              UUID,
    detalhe          VARCHAR(300),
    em               TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_acesso_nivel2_evento ON acesso_nivel2_evento (startup_id, em DESC);

CREATE TABLE dataroom_link (
    token_hash    VARCHAR(64)   PRIMARY KEY,
    documento_id  UUID          NOT NULL REFERENCES dataroom_documento (id),
    acesso_id     UUID          NOT NULL REFERENCES acesso_nivel2 (id),
    usuario_id    UUID          NOT NULL REFERENCES usuario (id),
    expira_em     TIMESTAMPTZ   NOT NULL
);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('dataroom.tamanho_max_mb', 'dataroom', 'Tamanho máximo de cada documento do Data Room', 'INTEIRO', 'MEGABYTES', '10', '10', 1, 25, NULL, FALSE),
('dataroom.formatos_aceitos', 'dataroom', 'Formatos aceitos no Data Room', 'OPCAO', 'TEXTO', 'PDF_OFFICE_IMAGENS', 'PDF_OFFICE_IMAGENS', NULL, NULL,
 'PDF,PDF_OFFICE,PDF_OFFICE_IMAGENS', FALSE),
('dataroom.link_minutos', 'dataroom', 'Validade do link temporário de um documento do Data Room', 'INTEIRO', 'MINUTOS', '10', '10', 1, 60, NULL, FALSE);

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
SELECT gen_random_uuid(), v.lista, v.codigo, v.rotulo, NULL, v.ordem, now(), now()
FROM (VALUES
    ('CATEGORIA_DATAROOM', 'PITCH_DECK', 'Pitch Deck', 1),
    ('CATEGORIA_DATAROOM', 'SOCIETARIO', 'Societário', 2),
    ('CATEGORIA_DATAROOM', 'FINANCEIRO', 'Financeiro', 3),
    ('CATEGORIA_DATAROOM', 'INDICADORES', 'Indicadores', 4),
    ('CATEGORIA_DATAROOM', 'PROJECOES', 'Projeções', 5),
    ('CATEGORIA_DATAROOM', 'CONTRATOS', 'Contratos', 6),
    ('CATEGORIA_DATAROOM', 'COMPLEMENTARES', 'Complementares', 7),
    ('CATEGORIA_DATAROOM', 'NDA', 'NDA da startup', 8)
) AS v(lista, codigo, rotulo, ordem);
