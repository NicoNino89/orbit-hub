-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 01:10
-- Cartão Jira: KAN-13
-- Título: Bloqueio temporário de login
-- Objetivo: Contar falhas seguidas de login por e-mail e bloquear novas tentativas por um tempo.
-- Decisões: A chave é o hash SHA-256 do e-mail normalizado, e não o id da conta: e-mails sem conta também
--           são bloqueados, então a mensagem de bloqueio não revela se a conta existe, e o e-mail em si não
--           fica guardado aqui.

CREATE TABLE bloqueio_login (
    email_hash    VARCHAR(64) PRIMARY KEY,
    falhas        INTEGER     NOT NULL,
    bloqueado_ate TIMESTAMPTZ,
    atualizado_em TIMESTAMPTZ NOT NULL
);
