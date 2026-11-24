-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 12:17
-- Cartão Jira: KAN-104
-- Título: Rotinas agendadas com registro
-- Objetivo: Guardar cada execução das rotinas automáticas (início, fim, itens, erros, resultado) e a pausa feita pelo
--           administrador.
-- Decisões: A chave única (rotina, ciclo) é a trava distribuída: com várias instâncias da API, a primeira que grava a
--           execução de um ciclo é a única que roda; as outras desistem. Execução manual tem ciclo próprio. O estado
--           (pausada) fica em tabela à parte, porque as rotinas em si são definidas no código. Entra também o parâmetro
--           da retenção dos tokens vencidos, usado pela rotina de limpeza (R12).

CREATE TABLE rotina_execucao (
    id            UUID         PRIMARY KEY,
    rotina        VARCHAR(10)  NOT NULL,
    ciclo         VARCHAR(60)  NOT NULL,
    disparo       VARCHAR(12)  NOT NULL,
    disparado_por UUID         REFERENCES usuario (id),
    iniciada_em   TIMESTAMPTZ  NOT NULL,
    terminada_em  TIMESTAMPTZ,
    itens         INTEGER      NOT NULL DEFAULT 0,
    erros         INTEGER      NOT NULL DEFAULT 0,
    resultado     VARCHAR(15)  NOT NULL,
    mensagem      VARCHAR(500),
    CONSTRAINT uk_rotina_ciclo UNIQUE (rotina, ciclo)
);

CREATE INDEX ix_rotina_execucao_rotina ON rotina_execucao (rotina, iniciada_em DESC);

CREATE TABLE rotina_estado (
    rotina      VARCHAR(10)  PRIMARY KEY,
    pausada     BOOLEAN      NOT NULL,
    alterada_por UUID        REFERENCES usuario (id),
    alterada_em TIMESTAMPTZ  NOT NULL
);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('retencao.tokens_dias', 'privacidade', 'Links de e-mail e sessões vencidos são apagados depois deste prazo (R12)', 'INTEIRO', 'DIAS', '30', '30', 1, 365, NULL, FALSE);
