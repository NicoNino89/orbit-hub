-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 17:19
-- Cartão Jira: KAN-62
-- Título: Verificação de identidade
-- Objetivo: Guardar os pedidos de verificação de pessoas físicas e organizações investidoras (e, a partir da S4, de
--           startups e fundadores), os documentos enviados e o resultado, que dá o selo do perfil.
-- Decisões: Um pedido por vez em aberto para cada sujeito; o histórico fica (revalidação cria outro pedido). O arquivo
--           do documento fica no armazenamento (Blob em nuvem), aqui só a chave e os metadados. Documentos são dados
--           pessoais (LGPD): o prazo de guarda depois da decisão é o parâmetro verificacao.retencao_documentos_meses.

CREATE TABLE pedido_verificacao (
    id             UUID          PRIMARY KEY,
    sujeito_tipo   VARCHAR(20)   NOT NULL,
    sujeito_id     UUID          NOT NULL,
    situacao       VARCHAR(25)   NOT NULL,
    criado_por     UUID          NOT NULL REFERENCES usuario (id),
    criado_em      TIMESTAMPTZ   NOT NULL,
    enviado_em     TIMESTAMPTZ,
    decidido_por   UUID          REFERENCES usuario (id),
    decidido_em    TIMESTAMPTZ,
    motivo         VARCHAR(60),
    observacao     VARCHAR(1000),
    valido_ate     TIMESTAMPTZ,
    versao         BIGINT        NOT NULL DEFAULT 0
);

CREATE UNIQUE INDEX uk_pedido_verificacao_aberto ON pedido_verificacao (sujeito_tipo, sujeito_id)
    WHERE situacao IN ('RASCUNHO', 'EM_ANALISE', 'PENDENTE_COMPLEMENTO');
CREATE INDEX ix_pedido_verificacao_sujeito ON pedido_verificacao (sujeito_tipo, sujeito_id, criado_em DESC);
CREATE INDEX ix_pedido_verificacao_fila ON pedido_verificacao (situacao, enviado_em);

CREATE TABLE documento_verificacao (
    id            UUID          PRIMARY KEY,
    pedido_id     UUID          NOT NULL REFERENCES pedido_verificacao (id) ON DELETE CASCADE,
    tipo          VARCHAR(60)   NOT NULL,
    chave_arquivo VARCHAR(512)  NOT NULL,
    nome_original VARCHAR(200)  NOT NULL,
    tipo_conteudo VARCHAR(60)   NOT NULL,
    tamanho       BIGINT        NOT NULL,
    enviado_em    TIMESTAMPTZ   NOT NULL,
    CONSTRAINT uk_documento_verificacao_tipo UNIQUE (pedido_id, tipo)
);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('verificacao.documento_max_mb', 'verificacao', 'Tamanho máximo de cada documento enviado para verificação', 'INTEIRO', 'MEGABYTES', '5', '5', 1, 5, NULL, FALSE),
('verificacao.retencao_documentos_meses', 'privacidade', 'Guarda dos documentos de verificação depois da decisão (provisório: definir com o jurídico)', 'INTEIRO', 'MESES', '6', '6', 1, 60, NULL, TRUE);
