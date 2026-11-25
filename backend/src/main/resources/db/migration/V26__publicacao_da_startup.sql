-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 23:40
-- Cartão Jira: KAN-27
-- Título: Publicação da startup
-- Objetivo: Guardar quando a startup foi enviada e publicada, o aviso de inatividade da R8 e o histórico das mudanças de
--           situação (rascunho, em revisão, publicada, devolvida, despublicada).
-- Decisões: O histórico registra quem mudou (nulo quando foi a R8) e o motivo, para a curadoria (KAN-66) e a auditoria.

ALTER TABLE startup ADD COLUMN enviada_em TIMESTAMPTZ;
ALTER TABLE startup ADD COLUMN publicada_em TIMESTAMPTZ;
ALTER TABLE startup ADD COLUMN aviso_inatividade_em TIMESTAMPTZ;

CREATE TABLE startup_situacao_historico (
    id         UUID         PRIMARY KEY,
    startup_id UUID         NOT NULL REFERENCES startup (id) ON DELETE CASCADE,
    de         VARCHAR(20)  NOT NULL,
    para       VARCHAR(20)  NOT NULL,
    por        UUID         REFERENCES usuario (id),
    motivo     VARCHAR(300),
    em         TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_startup_situacao_historico ON startup_situacao_historico (startup_id, em DESC);
