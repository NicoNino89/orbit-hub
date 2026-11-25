-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 22:56
-- Cartão Jira: KAN-22
-- Título: Seções do cadastro da startup
-- Objetivo: Guardar os campos das seções descritivas do cadastro (problema e solução, produto e as seguintes).
-- Decisões: Uma linha por campo preenchido, com o código do catálogo no código (CampoDaStartup); campo vazio não tem
--           linha. Evita uma coluna por pergunta e um JSONB sem regra: o servidor valida pelo catálogo.

CREATE TABLE startup_campo (
    startup_id UUID        NOT NULL REFERENCES startup (id) ON DELETE CASCADE,
    campo      VARCHAR(60) NOT NULL,
    valor      TEXT        NOT NULL,
    PRIMARY KEY (startup_id, campo)
);
