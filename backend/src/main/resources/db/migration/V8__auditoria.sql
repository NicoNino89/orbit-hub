-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 10:00
-- Cartão Jira: KAN-80
-- Título: Trilha de auditoria imutável
-- Objetivo: Registrar toda ação da equipe interna: quem, o quê, quando, sobre qual objeto e com qual resultado.
-- Decisões: A imutabilidade é garantida no próprio banco: um gatilho recusa UPDATE, DELETE e TRUNCATE,
--           então nem a aplicação nem um administrador consegue editar ou apagar pela plataforma. O nome do
--           autor é copiado no registro, para a trilha continuar legível mesmo se a conta mudar. O expurgo
--           pelo prazo de guarda (parâmetro auditoria.retencao_meses, KAN-78) será uma rotina própria, com
--           mecanismo controlado, na rotina de retenção R12 (KAN-93).

CREATE TABLE registro_auditoria (
    id            UUID         PRIMARY KEY,
    ator_id       UUID,
    ator_nome     VARCHAR(120),
    acao          VARCHAR(120) NOT NULL,
    objeto_tipo   VARCHAR(60),
    objeto_id     VARCHAR(80),
    resultado     VARCHAR(10)  NOT NULL,
    status_http   INTEGER,
    detalhe       VARCHAR(500),
    justificativa VARCHAR(1000),
    ocorrido_em   TIMESTAMPTZ  NOT NULL
);

CREATE INDEX ix_auditoria_ocorrido ON registro_auditoria (ocorrido_em DESC);
CREATE INDEX ix_auditoria_ator ON registro_auditoria (ator_id, ocorrido_em DESC);
CREATE INDEX ix_auditoria_acao ON registro_auditoria (acao, ocorrido_em DESC);
CREATE INDEX ix_auditoria_objeto ON registro_auditoria (objeto_tipo, objeto_id, ocorrido_em DESC);

-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 10:00
-- Cartão Jira: KAN-80
-- Título: Recusa de alteração na trilha
-- Objetivo: Impedir que um registro de auditoria seja alterado ou apagado.
-- Decisões: Função de gatilho que sempre levanta erro; vale para UPDATE, DELETE (por linha) e TRUNCATE.
CREATE FUNCTION recusar_alteracao_auditoria() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'A trilha de auditoria é imutável: % não é permitido', TG_OP
        USING ERRCODE = 'insufficient_privilege';
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER tg_auditoria_imutavel
    BEFORE UPDATE OR DELETE ON registro_auditoria
    FOR EACH ROW EXECUTE FUNCTION recusar_alteracao_auditoria();

CREATE TRIGGER tg_auditoria_sem_truncate
    BEFORE TRUNCATE ON registro_auditoria
    FOR EACH STATEMENT EXECUTE FUNCTION recusar_alteracao_auditoria();
