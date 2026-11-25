-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 17:04
-- Cartão Jira: KAN-19
-- Título: Preferências complementares de investimento
-- Objetivo: Guardar, de forma opcional, modelos de negócio preferidos, modalidades, horizonte, perfil de risco,
--           experiência como investidor e interesses estratégicos, com opções de listas padronizadas.
-- Decisões: Horizonte, risco e experiência têm uma resposta só (coluna); modelos, modalidades e interesses aceitam
--           várias (tabela). As cinco listas novas nascem com itens iniciais e o administrador ajusta no painel (KAN-79).
--           O perfil de risco descreve o estágio de risco que o investidor aceita entre startups (todas são de alto
--           risco); não é o questionário de suitability da CVM, que a plataforma não aplica (não recomenda investimento).

ALTER TABLE perfil_investimento
    ADD COLUMN horizonte   VARCHAR(40),
    ADD COLUMN perfil_risco VARCHAR(40),
    ADD COLUMN experiencia VARCHAR(40);

CREATE TABLE perfil_investimento_modelo (
    perfil_id UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    modelo    VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, modelo)
);

CREATE TABLE perfil_investimento_modalidade (
    perfil_id  UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    modalidade VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, modalidade)
);

CREATE TABLE perfil_investimento_interesse (
    perfil_id UUID        NOT NULL REFERENCES perfil_investimento (id) ON DELETE CASCADE,
    interesse VARCHAR(40) NOT NULL,
    PRIMARY KEY (perfil_id, interesse)
);

INSERT INTO lista_item (id, lista, codigo, rotulo, grupo, ordem, criado_em, atualizado_em)
SELECT gen_random_uuid(), v.lista, v.codigo, v.rotulo, NULL, v.ordem, now(), now()
FROM (VALUES
    ('MODALIDADE_INVESTIMENTO', 'PARTICIPACAO_SOCIETARIA', 'Participação societária', 1),
    ('MODALIDADE_INVESTIMENTO', 'DIVIDA_CONVERSIVEL', 'Dívida conversível em participação', 2),
    ('MODALIDADE_INVESTIMENTO', 'PARTICIPACAO_NA_RECEITA', 'Participação na receita (revenue-based)', 3),
    ('MODALIDADE_INVESTIMENTO', 'OUTRAS', 'Outras', 99),
    ('HORIZONTE_INVESTIMENTO', 'ATE_3_ANOS', 'Até 3 anos', 1),
    ('HORIZONTE_INVESTIMENTO', 'DE_3_A_5_ANOS', 'De 3 a 5 anos', 2),
    ('HORIZONTE_INVESTIMENTO', 'DE_5_A_8_ANOS', 'De 5 a 8 anos', 3),
    ('HORIZONTE_INVESTIMENTO', 'MAIS_DE_8_ANOS', 'Mais de 8 anos', 4),
    ('PERFIL_RISCO', 'MODERADO', 'Moderado: negócios com receita e tração', 1),
    ('PERFIL_RISCO', 'ARROJADO', 'Arrojado: negócios em validação ou com MVP', 2),
    ('PERFIL_RISCO', 'AGRESSIVO', 'Agressivo: ideias e tecnologias ainda não testadas', 3),
    ('EXPERIENCIA_INVESTIDOR', 'PRIMEIRO_INVESTIMENTO', 'Primeiro investimento em startups', 1),
    ('EXPERIENCIA_INVESTIDOR', 'ATE_5', 'Até 5 investimentos em startups', 2),
    ('EXPERIENCIA_INVESTIDOR', 'DE_6_A_20', 'De 6 a 20 investimentos em startups', 3),
    ('EXPERIENCIA_INVESTIDOR', 'MAIS_DE_20', 'Mais de 20 investimentos em startups', 4),
    ('INTERESSE_ESTRATEGICO', 'RETORNO_FINANCEIRO', 'Retorno financeiro', 1),
    ('INTERESSE_ESTRATEGICO', 'INOVACAO_ABERTA', 'Inovação aberta para a minha empresa', 2),
    ('INTERESSE_ESTRATEGICO', 'IMPACTO', 'Impacto socioambiental', 3),
    ('INTERESSE_ESTRATEGICO', 'MENTORIA', 'Mentoria e apoio a empreendedores', 4),
    ('INTERESSE_ESTRATEGICO', 'NOVOS_MERCADOS', 'Acesso a novos mercados e tecnologias', 5),
    ('INTERESSE_ESTRATEGICO', 'OUTROS', 'Outros', 99)
) AS v(lista, codigo, rotulo, ordem);
