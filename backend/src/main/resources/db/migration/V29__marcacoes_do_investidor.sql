-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 11:03
-- Cartão Jira: KAN-41, KAN-43, KAN-44
-- Título: Favoritos, histórico e descartes do investidor
-- Objetivo: Guardar, por pessoa, as startups salvas nos favoritos, as visualizadas (a última visita de cada uma) e as
--           descartadas da seleção personalizada.
-- Decisões: Por pessoa, não pela organização da atuação: são escolhas privadas de navegação. Uma linha por pessoa e
--           startup em cada tabela; a visita atualiza a data. Nada disso é visível para o empreendedor.

CREATE TABLE catalogo_favorito (
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    startup_id  UUID         NOT NULL REFERENCES startup (id),
    salvo_em    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (usuario_id, startup_id)
);

CREATE TABLE catalogo_visualizacao (
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    startup_id  UUID         NOT NULL REFERENCES startup (id),
    vista_em    TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (usuario_id, startup_id)
);

CREATE INDEX ix_catalogo_visualizacao ON catalogo_visualizacao (usuario_id, vista_em DESC);

CREATE TABLE catalogo_descarte (
    usuario_id     UUID         NOT NULL REFERENCES usuario (id),
    startup_id     UUID         NOT NULL REFERENCES startup (id),
    descartada_em  TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (usuario_id, startup_id)
);
