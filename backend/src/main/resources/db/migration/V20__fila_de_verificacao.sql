-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 17:33
-- Cartão Jira: KAN-83
-- Título: Fila de verificação e revalidação
-- Objetivo: Guardar quem assumiu cada pedido, o prazo do complemento, o aviso de revalidação, o encaminhamento por
--           fraude e a limpeza dos documentos; e os motivos de decisão da análise, numa lista gerenciável.
-- Decisões: O grupo do motivo diz a que decisão ele pertence: VERIFICADO, COMPLEMENTO, RECUSA ou FRAUDE (recusa que
--           também encaminha à moderação). Assim, um motivo novo de fraude entra pelo painel, sem código.

ALTER TABLE pedido_verificacao
    ADD COLUMN responsavel_id          UUID REFERENCES usuario (id),
    ADD COLUMN assumido_em             TIMESTAMPTZ,
    ADD COLUMN prazo_complemento_ate   TIMESTAMPTZ,
    ADD COLUMN aviso_revalidacao_em    TIMESTAMPTZ,
    ADD COLUMN encaminhado_moderacao   BOOLEAN NOT NULL DEFAULT FALSE,
    ADD COLUMN documentos_apagados_em  TIMESTAMPTZ;

CREATE INDEX ix_pedido_verificacao_responsavel ON pedido_verificacao (responsavel_id) WHERE situacao = 'EM_ANALISE';

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
SELECT gen_random_uuid(), 'MOTIVO_DECISAO_VERIFICACAO', v.codigo, v.rotulo, v.grupo, v.ordem, now(), now()
FROM (VALUES
    ('DOCUMENTOS_CONFEREM', 'Documentos conferem com os dados da conta', 'VERIFICADO', 1),
    ('DOCUMENTO_ILEGIVEL', 'Documento ilegível, cortado ou com reflexo', 'COMPLEMENTO', 10),
    ('DOCUMENTO_VENCIDO', 'Documento vencido', 'COMPLEMENTO', 11),
    ('FALTA_DOCUMENTO', 'Falta documento ou página', 'COMPLEMENTO', 12),
    ('DADOS_DIVERGENTES', 'Dados do documento diferentes dos da conta', 'COMPLEMENTO', 13),
    ('CNPJ_IRREGULAR', 'CNPJ inativo ou irregular na Receita Federal', 'RECUSA', 20),
    ('SEM_AUTORIZACAO_CVM', 'Gestora sem autorização da CVM', 'RECUSA', 21),
    ('NAO_ATENDE_REQUISITOS', 'Não atende aos requisitos da plataforma', 'RECUSA', 22),
    ('DOCUMENTO_ADULTERADO', 'Indício de documento adulterado', 'FRAUDE', 30),
    ('IDENTIDADE_DE_TERCEIRO', 'Indício de uso da identidade de outra pessoa ou empresa', 'FRAUDE', 31)
) AS v(codigo, rotulo, grupo, ordem);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('verificacao.aviso_revalidacao_dias', 'verificacao', 'Antecedência do aviso de que a verificação vai vencer (R9)', 'INTEIRO', 'DIAS', '30', '30', 1, 90, NULL, FALSE);
