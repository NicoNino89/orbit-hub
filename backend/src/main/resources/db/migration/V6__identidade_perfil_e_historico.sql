-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 01:40
-- Cartão Jira: KAN-16
-- Título: Perfil e histórico da conta
-- Objetivo: Guardar telefone e foto do perfil e o histórico de mudanças da conta (papéis, e-mail e senha).
-- Decisões: A foto fica no armazenamento de arquivos (Blob na nuvem); aqui só a chave. O histórico guarda o
--           tipo do evento e um detalhe sem dado pessoal (por exemplo, o papel), nunca e-mails ou senhas.

ALTER TABLE usuario ADD COLUMN telefone VARCHAR(20);
ALTER TABLE usuario ADD COLUMN foto_chave VARCHAR(300);

CREATE TABLE historico_conta (
    id          UUID        PRIMARY KEY,
    usuario_id  UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    evento      VARCHAR(40) NOT NULL,
    detalhe     VARCHAR(120),
    ocorrido_em TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_historico_conta_usuario ON historico_conta (usuario_id, ocorrido_em DESC);
