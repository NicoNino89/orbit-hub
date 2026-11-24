-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 10:20
-- Cartão Jira: KAN-75
-- Título: Equipe interna: contas separadas, dois fatores, desligamento e revisão de acessos
-- Objetivo: Marcar as contas da equipe interna, guardar o segundo fator (TOTP), registrar o desligamento e as
--           revisões periódicas de acesso.
-- Decisões: A conta interna é uma conta à parte (coluna "interna"): nunca recebe papel de investidor ou
--           empreendedor, e uma conta pública nunca recebe papel interno. O segredo do TOTP fica cifrado com
--           AES-GCM (a chave sai da configuração, nunca do banco); "totp_pendente" guarda o segredo durante a
--           configuração, até o primeiro código válido. "totp_ultimo_passo" impede reusar o mesmo código. As
--           contas que já tinham ADMIN (criadas pelo banco na S1) passam a ser internas e terão de configurar
--           o segundo fator no próximo login. A revisão guarda, para cada membro, os papéis confirmados naquele
--           momento.

ALTER TABLE usuario ADD COLUMN interna BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE usuario ADD COLUMN totp_segredo VARCHAR(200);
ALTER TABLE usuario ADD COLUMN totp_pendente VARCHAR(200);
ALTER TABLE usuario ADD COLUMN totp_ultimo_passo BIGINT;
ALTER TABLE usuario ADD COLUMN desligado_em TIMESTAMPTZ;

UPDATE usuario SET interna = TRUE
WHERE id IN (SELECT usuario_id FROM usuario_papel WHERE papel = 'ADMIN');

CREATE INDEX ix_usuario_interna ON usuario (interna) WHERE interna;

CREATE TABLE revisao_acesso (
    id           UUID          PRIMARY KEY,
    revisor_id   UUID          NOT NULL REFERENCES usuario (id),
    realizada_em TIMESTAMPTZ   NOT NULL,
    observacao   VARCHAR(1000)
);

CREATE INDEX ix_revisao_acesso_realizada ON revisao_acesso (realizada_em DESC);

CREATE TABLE revisao_acesso_item (
    revisao_id UUID         NOT NULL REFERENCES revisao_acesso (id),
    membro_id  UUID         NOT NULL REFERENCES usuario (id),
    nome       VARCHAR(120) NOT NULL,
    papeis     VARCHAR(300) NOT NULL,
    PRIMARY KEY (revisao_id, membro_id)
);
