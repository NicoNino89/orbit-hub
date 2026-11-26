-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 22:40
-- Cartão Jira: KAN-170, KAN-171, KAN-172
-- Título: Vitrine da startup e banners institucionais
-- Objetivo: Guardar a capa e a chamada (frase curta) da startup e os banners e chamadas institucionais da página inicial.
-- Decisões: Capa e chamada são públicas como o nome. O perfil de risco já existe (V18, lista PERFIL_RISCO). Banner com
--           público, período e ordem; imagem no armazenamento privado.

ALTER TABLE startup ADD COLUMN capa_chave VARCHAR(512);
ALTER TABLE startup ADD COLUMN capa_tipo VARCHAR(40);
ALTER TABLE startup ADD COLUMN chamada VARCHAR(120);

CREATE TABLE banner_institucional (
    id            UUID          PRIMARY KEY,
    tipo          VARCHAR(10)   NOT NULL,
    titulo        VARCHAR(120)  NOT NULL,
    destaque      VARCHAR(60),
    texto         VARCHAR(300),
    botao         VARCHAR(40),
    link          VARCHAR(300),
    publico       VARCHAR(20)   NOT NULL,
    inicio        DATE          NOT NULL,
    fim           DATE,
    ordem         INTEGER       NOT NULL,
    imagem_chave  VARCHAR(512),
    imagem_tipo   VARCHAR(40),
    texto_alt     VARCHAR(200),
    ativo         BOOLEAN       NOT NULL,
    atualizado_por UUID         REFERENCES usuario (id),
    atualizado_em TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_banner_tipo CHECK (tipo IN ('BANNER', 'CHAMADA')),
    CONSTRAINT ck_banner_publico CHECK (publico IN ('TODOS', 'INVESTIDOR', 'EMPREENDEDOR'))
);

INSERT INTO banner_institucional (id, tipo, titulo, destaque, texto, botao, link, publico, inicio, ordem, ativo, atualizado_em) VALUES
(gen_random_uuid(), 'BANNER', 'Empresas que inspiram. Investidores que', 'acreditam.', 'Uma rede para conectar empreendedores, investidores e conhecimento.',
 'Saiba mais', '/ajuda', 'TODOS', DATE '2026-01-01', 1, TRUE, now()),
(gen_random_uuid(), 'CHAMADA', 'Conquiste seu espaço', NULL, 'Na maior rede de investimentos e empreendedorismo do Brasil.', 'Participe!', '/catalogo',
 'TODOS', DATE '2026-01-01', 1, TRUE, now());

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('startup.capa_max_mb', 'startup', 'Tamanho máximo da imagem de capa da startup', 'INTEIRO', 'MEGABYTES', '2', '2', 1, 5, NULL, FALSE);

-- Busca global (KAN-167): compara sem acentos comuns do português.
CREATE FUNCTION unaccent_simples(t TEXT) RETURNS TEXT LANGUAGE sql IMMUTABLE AS
$$ SELECT translate(t, 'áàâãäéèêëíìîïóòôõöúùûüçÁÀÂÃÄÉÈÊËÍÌÎÏÓÒÔÕÖÚÙÛÜÇ', 'aaaaaeeeeiiiiooooouuuucAAAAAEEEEIIIIOOOOOUUUUC') $$;
