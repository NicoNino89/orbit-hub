-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 22:58
-- Cartão Jira: KAN-176
-- Título: Mensagens da conexão
-- Objetivo: Guardar a conversa entre a startup e o investidor dentro de uma conexão (a partir do Match) e a última
--           leitura de cada pessoa, para o contador de não lidas.
-- Decisões: Texto até 2.000 caracteres; documentos confidenciais continuam no Data Room. Conversa só leitura quando a
--           conexão termina.

CREATE TABLE conexao_mensagem (
    id          UUID           PRIMARY KEY,
    conexao_id  UUID           NOT NULL REFERENCES conexao (id),
    autor_id    UUID           NOT NULL REFERENCES usuario (id),
    lado        VARCHAR(20)    NOT NULL,
    texto       VARCHAR(2000)  NOT NULL,
    em          TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_conexao_mensagem ON conexao_mensagem (conexao_id, em);

CREATE TABLE conexao_leitura (
    conexao_id  UUID         NOT NULL REFERENCES conexao (id),
    usuario_id  UUID         NOT NULL REFERENCES usuario (id),
    lido_em     TIMESTAMPTZ  NOT NULL,
    PRIMARY KEY (conexao_id, usuario_id)
);
