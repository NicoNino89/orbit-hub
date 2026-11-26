-- Autor: Nicholas Bruno Pimentel
-- Data: 25/11/2026
-- Hora: 20:54
-- Cartão Jira: KAN-17, KAN-76, KAN-77, KAN-82, KAN-90, KAN-91, KAN-93, KAN-107, KAN-108
-- Título: LGPD, operação e equipes
-- Objetivo: Guardar a exclusão da conta, o último acesso e o aviso de inatividade; os pedidos do titular de dados; os
--           incidentes com dados pessoais; os líderes, as ausências e o rodízio dos times; o segundo alerta de prazo das
--           filas; e os parâmetros novos de LGPD, distribuição e retenção.
-- Decisões: A conta excluída fica anonimizada na própria linha (as referências continuam válidas). Os prazos de
--           retenção passam a ser sensíveis (segunda aprovação). Distribuição automática por rodízio por fila, ligada
--           por parâmetro.

ALTER TABLE usuario ADD COLUMN excluida_em TIMESTAMPTZ;
ALTER TABLE usuario ADD COLUMN ultimo_acesso_em TIMESTAMPTZ;
ALTER TABLE usuario ADD COLUMN aviso_inatividade_em TIMESTAMPTZ;
UPDATE usuario SET ultimo_acesso_em = criado_em WHERE ultimo_acesso_em IS NULL;

ALTER TABLE caso_moderacao ADD COLUMN vencido_alertado_em TIMESTAMPTZ;
ALTER TABLE recurso ADD COLUMN vencido_alertado_em TIMESTAMPTZ;
ALTER TABLE chamado ADD COLUMN vencido_alertado_em TIMESTAMPTZ;
ALTER TABLE pedido_verificacao ADD COLUMN alertado_em TIMESTAMPTZ;
ALTER TABLE pedido_verificacao ADD COLUMN vencido_alertado_em TIMESTAMPTZ;
ALTER TABLE revisao_curadoria ADD COLUMN alertado_em TIMESTAMPTZ;
ALTER TABLE revisao_curadoria ADD COLUMN vencido_alertado_em TIMESTAMPTZ;

CREATE TABLE pedido_titular (
    id                  UUID           PRIMARY KEY,
    usuario_id          UUID           NOT NULL REFERENCES usuario (id),
    tipo                VARCHAR(30)    NOT NULL,
    detalhes            VARCHAR(2000),
    situacao            VARCHAR(20)    NOT NULL,
    prazo               TIMESTAMPTZ    NOT NULL,
    responsavel_id      UUID           REFERENCES usuario (id),
    resposta            VARCHAR(4000),
    respondido_por      UUID           REFERENCES usuario (id),
    respondido_em       TIMESTAMPTZ,
    alertado_em         TIMESTAMPTZ,
    vencido_alertado_em TIMESTAMPTZ,
    criado_em           TIMESTAMPTZ    NOT NULL
);

CREATE INDEX ix_pedido_titular_fila ON pedido_titular (situacao, prazo);
CREATE INDEX ix_pedido_titular_usuario ON pedido_titular (usuario_id, criado_em DESC);

CREATE TABLE incidente (
    id                       UUID           PRIMARY KEY,
    titulo                   VARCHAR(160)   NOT NULL,
    descricao                VARCHAR(4000)  NOT NULL,
    dados_afetados           VARCHAR(2000),
    titulares_afetados       VARCHAR(500),
    detectado_em             TIMESTAMPTZ    NOT NULL,
    registrado_por           UUID           NOT NULL REFERENCES usuario (id),
    contencao                VARCHAR(4000),
    risco                    VARCHAR(10),
    decisoes                 VARCHAR(4000),
    prazo_comunicacao        TIMESTAMPTZ    NOT NULL,
    comunicado_anpd_em       TIMESTAMPTZ,
    comunicado_titulares_em  TIMESTAMPTZ,
    licoes                   VARCHAR(4000),
    acoes_preventivas        VARCHAR(4000),
    situacao                 VARCHAR(20)    NOT NULL,
    encerrado_em             TIMESTAMPTZ,
    atualizado_em            TIMESTAMPTZ    NOT NULL
);

CREATE TABLE incidente_historico (
    id            UUID           PRIMARY KEY,
    incidente_id  UUID           NOT NULL REFERENCES incidente (id),
    autor_id      UUID           NOT NULL REFERENCES usuario (id),
    acao          VARCHAR(40)    NOT NULL,
    detalhe       VARCHAR(2000),
    em            TIMESTAMPTZ    NOT NULL
);

CREATE TABLE time_lider (
    time       VARCHAR(20)   PRIMARY KEY,
    membro_id  UUID          NOT NULL REFERENCES usuario (id),
    definido_em TIMESTAMPTZ  NOT NULL
);

CREATE TABLE membro_ausencia (
    id         UUID          PRIMARY KEY,
    membro_id  UUID          NOT NULL REFERENCES usuario (id),
    inicio     DATE          NOT NULL,
    fim        DATE          NOT NULL,
    motivo     VARCHAR(200),
    criada_em  TIMESTAMPTZ   NOT NULL,
    CONSTRAINT ck_membro_ausencia_datas CHECK (fim >= inicio)
);

CREATE INDEX ix_membro_ausencia ON membro_ausencia (membro_id, inicio, fim);

CREATE TABLE fila_rodizio (
    fila            VARCHAR(20)   PRIMARY KEY,
    ultimo_membro   UUID,
    atualizado_em   TIMESTAMPTZ   NOT NULL
);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
('lgpd.prazo_comunicacao_dias_uteis', 'privacidade', 'Prazo para comunicar à ANPD e aos titulares um incidente de risco relevante (provisório: validar com o jurídico)', 'INTEIRO', 'DIAS_UTEIS', '3', '3', 1, 15, NULL, TRUE),
('equipe.distribuicao_moderacao', 'equipe', 'Como os casos novos de moderação ganham responsável', 'OPCAO', 'TEXTO', 'RODIZIO', 'RODIZIO', NULL, NULL, 'RODIZIO,MANUAL', FALSE),
('equipe.distribuicao_atendimento', 'equipe', 'Como os chamados novos ganham responsável', 'OPCAO', 'TEXTO', 'RODIZIO', 'RODIZIO', NULL, NULL, 'RODIZIO,MANUAL', FALSE),
('equipe.distribuicao_privacidade', 'equipe', 'Como os pedidos do titular ganham responsável', 'OPCAO', 'TEXTO', 'RODIZIO', 'RODIZIO', NULL, NULL, 'RODIZIO,MANUAL', FALSE),
('conta.aviso_inatividade_dias', 'conta', 'Depois do aviso de inatividade, a conta sem novo acesso é desativada neste prazo (R11)', 'INTEIRO', 'DIAS', '30', '30', 7, 90, NULL, FALSE),
('retencao.chamados_meses', 'privacidade', 'Chamados fechados têm o texto anonimizado depois deste prazo (R12)', 'INTEIRO', 'MESES', '24', '24', 6, 120, NULL, TRUE),
('retencao.denuncias_meses', 'privacidade', 'Denúncias de casos decididos têm o texto anonimizado depois deste prazo (R12)', 'INTEIRO', 'MESES', '60', '60', 12, 120, NULL, TRUE);

UPDATE parametro SET sensivel = TRUE WHERE chave LIKE 'retencao.%' OR chave = 'lgpd.retencao_legal_meses';
