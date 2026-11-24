-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 12:36
-- Cartão Jira: KAN-146
-- Título: Central de notificações
-- Objetivo: Guardar as notificações de cada pessoa (lidas e não lidas), as preferências de canal por tipo de evento,
--           a opção de resumo diário e a fila de e-mails a enviar.
-- Decisões: O e-mail não é enviado na hora: vai para a fila (notificacao_email) e uma rotina envia, com novas
--           tentativas em caso de falha, para o envio nunca travar a ação que gerou o aviso. A preferência só é gravada
--           quando a pessoa muda o padrão do tipo. O texto do e-mail é montado no envio, a partir da notificação; a
--           fila não guarda endereço de e-mail.

CREATE TABLE notificacao (
    id          UUID          PRIMARY KEY,
    usuario_id  UUID          NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo        VARCHAR(40)   NOT NULL,
    titulo      VARCHAR(160)  NOT NULL,
    mensagem    VARCHAR(1000) NOT NULL,
    link        VARCHAR(300),
    na_plataforma BOOLEAN     NOT NULL,
    criada_em   TIMESTAMPTZ   NOT NULL,
    lida_em     TIMESTAMPTZ
);

CREATE INDEX ix_notificacao_usuario ON notificacao (usuario_id, criada_em DESC);
CREATE INDEX ix_notificacao_nao_lida ON notificacao (usuario_id) WHERE lida_em IS NULL AND na_plataforma;

CREATE TABLE notificacao_email (
    id               UUID         PRIMARY KEY,
    notificacao_id   UUID         NOT NULL REFERENCES notificacao (id) ON DELETE CASCADE,
    usuario_id       UUID         NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    no_resumo        BOOLEAN      NOT NULL,
    situacao         VARCHAR(12)  NOT NULL,
    tentativas       INTEGER      NOT NULL DEFAULT 0,
    proxima_tentativa_em TIMESTAMPTZ NOT NULL,
    ultimo_erro      VARCHAR(200),
    enviado_em       TIMESTAMPTZ
);

CREATE INDEX ix_notificacao_email_fila ON notificacao_email (situacao, no_resumo, proxima_tentativa_em);

CREATE TABLE preferencia_notificacao (
    usuario_id UUID        NOT NULL REFERENCES usuario (id) ON DELETE CASCADE,
    tipo       VARCHAR(40) NOT NULL,
    canal      VARCHAR(12) NOT NULL,
    PRIMARY KEY (usuario_id, tipo)
);

CREATE TABLE preferencia_resumo (
    usuario_id    UUID    PRIMARY KEY REFERENCES usuario (id) ON DELETE CASCADE,
    resumo_diario BOOLEAN NOT NULL
);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('notificacoes.retencao_dias', 'privacidade', 'Notificações mais antigas que isto são apagadas (R19)', 'INTEIRO', 'DIAS', '180', '180', 30, 730, NULL, FALSE),
('notificacoes.max_tentativas_email', 'conta', 'Tentativas de envio de um e-mail de aviso antes de desistir', 'INTEIRO', 'QUANTIDADE', '5', '5', 1, 20, NULL, FALSE);
