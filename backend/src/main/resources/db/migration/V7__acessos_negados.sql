-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 01:50
-- Cartão Jira: KAN-34
-- Título: Registro de acessos negados
-- Objetivo: Guardar as tentativas de acesso não autorizado (sem permissão ou com token inválido).
-- Decisões: Só o id da conta (quando há), o método, o caminho sem parâmetros de busca, o status e o
--           momento; nada de token, IP ou dado pessoal. Base para a trilha de auditoria (KAN-80).

CREATE TABLE acesso_negado (
    id          UUID         PRIMARY KEY,
    usuario_id  UUID,
    metodo      VARCHAR(10)  NOT NULL,
    caminho     VARCHAR(300) NOT NULL,
    status      INTEGER      NOT NULL,
    ocorrido_em TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_acesso_negado_ocorrido ON acesso_negado (ocorrido_em DESC);
CREATE INDEX ix_acesso_negado_usuario ON acesso_negado (usuario_id, ocorrido_em DESC);
