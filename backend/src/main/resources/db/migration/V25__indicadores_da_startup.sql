-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 23:11
-- Cartão Jira: KAN-24
-- Título: Indicadores de tração da startup
-- Objetivo: Guardar cada valor declarado de um indicador com o mês de referência, mantendo o histórico.
-- Decisões: Um registro por valor declarado (nunca atualizado nem apagado): o valor atual é o de referência mais recente.
--           A data de lembrete na startup evita que a R8 avise todo dia. O limite de desatualização é o parâmetro
--           startup.indicador_desatualizado_meses, criado na V11.

CREATE TABLE startup_indicador (
    id             UUID          PRIMARY KEY,
    startup_id     UUID          NOT NULL REFERENCES startup (id) ON DELETE CASCADE,
    indicador      VARCHAR(40)   NOT NULL,
    valor          NUMERIC(18,2) NOT NULL,
    referencia     DATE          NOT NULL,
    registrado_por UUID          NOT NULL REFERENCES usuario (id),
    registrado_em  TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_startup_indicador ON startup_indicador (startup_id, indicador, referencia DESC, registrado_em DESC);

ALTER TABLE startup ADD COLUMN lembrete_indicadores_em TIMESTAMPTZ;
