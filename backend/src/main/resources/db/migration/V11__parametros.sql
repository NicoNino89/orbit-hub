-- Autor: Nicholas Bruno Pimentel
-- Data: 24/11/2026
-- Hora: 11:08
-- Cartão Jira: KAN-78
-- Título: Parâmetros configuráveis com histórico
-- Objetivo: Guardar os prazos e limites de regra de negócio, editáveis pelo administrador, e o histórico de cada
--           alteração (valor anterior, novo, autor, data, justificativa e, nos sensíveis, quem aprovou).
-- Decisões: O catálogo inicial é a seção 5 de docs/requisitos/processos-e-operacao.md (só os do MVP), mais os
--           prazos que estavam fixos no application.yml desde a S1 (login, sessão, links) e os da S2 (convite,
--           revisão de acessos, aprovação, guarda da trilha). O valor fica em texto e é interpretado pelo tipo:
--           INTEIRO (com unidade e limites), LISTA_INTEIROS (separada por vírgula) ou OPCAO (uma das opções).
--           Valores marcados "provisório" na descrição dependem de decisão do PO ou do jurídico. O histórico só
--           recebe inserções.

CREATE TABLE parametro (
    chave          VARCHAR(80)   PRIMARY KEY,
    grupo          VARCHAR(40)   NOT NULL,
    descricao      VARCHAR(300)  NOT NULL,
    tipo           VARCHAR(20)   NOT NULL,
    unidade        VARCHAR(20)   NOT NULL,
    valor          VARCHAR(200)  NOT NULL,
    valor_padrao   VARCHAR(200)  NOT NULL,
    minimo         BIGINT,
    maximo         BIGINT,
    opcoes         VARCHAR(300),
    sensivel       BOOLEAN       NOT NULL,
    atualizado_em  TIMESTAMPTZ,
    atualizado_por UUID          REFERENCES usuario (id),
    versao         BIGINT        NOT NULL DEFAULT 0
);

CREATE TABLE parametro_historico (
    id             UUID          PRIMARY KEY,
    chave          VARCHAR(80)   NOT NULL REFERENCES parametro (chave),
    valor_anterior VARCHAR(200)  NOT NULL,
    valor_novo     VARCHAR(200)  NOT NULL,
    autor_id       UUID          NOT NULL REFERENCES usuario (id),
    aprovado_por   UUID          REFERENCES usuario (id),
    proposta_id    UUID          REFERENCES proposta_aprovacao (id),
    justificativa  VARCHAR(1000) NOT NULL,
    alterado_em    TIMESTAMPTZ   NOT NULL
);

CREATE INDEX ix_parametro_historico_chave ON parametro_historico (chave, alterado_em DESC);

INSERT INTO parametro (chave, grupo, descricao, tipo, unidade, valor, valor_padrao, minimo, maximo, opcoes, sensivel) VALUES
-- Cadastro e conta
('cadastro.link_confirmacao_horas', 'cadastro', 'Validade do link de confirmação de e-mail enviado no cadastro', 'INTEIRO', 'HORAS', '24', '24', 1, 168, NULL, FALSE),
('cadastro.prazo_confirmacao_dias', 'cadastro', 'Prazo para confirmar o e-mail antes de o cadastro não confirmado ser removido (R12)', 'INTEIRO', 'DIAS', '7', '7', 1, 60, NULL, FALSE),
('conta.link_redefinicao_minutos', 'conta', 'Validade do link de redefinição de senha', 'INTEIRO', 'MINUTOS', '30', '30', 10, 1440, NULL, FALSE),
('conta.link_troca_email_horas', 'conta', 'Validade do link de confirmação do novo e-mail', 'INTEIRO', 'HORAS', '24', '24', 1, 168, NULL, FALSE),
('conta.inatividade_meses', 'conta', 'Conta sem acesso há este tempo recebe aviso e, sem resposta, é desativada (R11)', 'INTEIRO', 'MESES', '12', '12', 3, 60, NULL, FALSE),
-- Segurança
('login.max_tentativas', 'seguranca', 'Falhas seguidas de senha ou de código que bloqueiam o login temporariamente', 'INTEIRO', 'QUANTIDADE', '5', '5', 3, 20, NULL, TRUE),
('login.bloqueio_minutos', 'seguranca', 'Duração do bloqueio temporário do login', 'INTEIRO', 'MINUTOS', '15', '15', 1, 1440, NULL, TRUE),
('sessao.inatividade_minutos', 'seguranca', 'Sem uso por este tempo, a sessão expira e a pessoa entra de novo', 'INTEIRO', 'MINUTOS', '30', '30', 5, 1440, NULL, TRUE),
('sessao.duracao_maxima_horas', 'seguranca', 'Limite absoluto de uma sessão, mesmo em uso contínuo', 'INTEIRO', 'HORAS', '12', '12', 1, 72, NULL, TRUE),
-- Equipe interna e governança
('convite.validade_dias', 'equipe', 'Validade dos convites (equipe interna e organizações)', 'INTEIRO', 'DIAS', '14', '14', 1, 60, NULL, FALSE),
('acesso.revisao_meses', 'equipe', 'Intervalo da revisão dos acessos da equipe interna', 'INTEIRO', 'MESES', '3', '3', 1, 12, NULL, TRUE),
('aprovacao.prazo_horas', 'equipe', 'Proposta de ação grave sem decisão por este tempo alerta o administrador', 'INTEIRO', 'HORAS', '48', '48', 1, 168, NULL, FALSE),
('operacao.alerta_sla_percentual', 'equipe', 'Casos com este percentual do prazo consumido alertam o líder do time', 'INTEIRO', 'PERCENTUAL', '80', '80', 50, 95, NULL, FALSE),
('equipe.horario_atendimento', 'equipe', 'Horário de atendimento dos times (provisório: um horário para todos até o calendário por time)', 'OPCAO', 'TEXTO', 'SEG_SEX_09_18', 'SEG_SEX_09_18', NULL, NULL, 'SEG_SEX_08_17,SEG_SEX_09_18,SEG_SEX_10_19,SEG_SAB_09_18', FALSE),
-- Verificação
('verificacao.sla_dias_uteis', 'verificacao', 'Prazo para concluir uma verificação de identidade ou de empresa', 'INTEIRO', 'DIAS_UTEIS', '2', '2', 1, 15, NULL, FALSE),
('verificacao.validade_meses', 'verificacao', 'Validade de uma verificação aprovada, antes da revalidação (R9)', 'INTEIRO', 'MESES', '12', '12', 3, 36, NULL, FALSE),
('verificacao.prazo_complemento_dias', 'verificacao', 'Prazo para o usuário enviar documentos complementares pedidos', 'INTEIRO', 'DIAS', '15', '15', 3, 60, NULL, FALSE),
-- Curadoria e catálogo
('curadoria.sla_dias_uteis', 'curadoria', 'Prazo para a curadoria analisar uma startup enviada', 'INTEIRO', 'DIAS_UTEIS', '3', '3', 1, 15, NULL, FALSE),
('catalogo.novas_dias', 'catalogo', 'Por quanto tempo uma startup aparece como "nova" no catálogo', 'INTEIRO', 'DIAS', '30', '30', 7, 90, NULL, FALSE),
('catalogo.frequencia_edicoes', 'catalogo', 'Frequência das edições temáticas do catálogo', 'OPCAO', 'TEXTO', 'MENSAL', 'MENSAL', NULL, NULL, 'SEMANAL,QUINZENAL,MENSAL,BIMESTRAL', FALSE),
-- Conexões e Nível 2
('conexao.lembrete_dias', 'conexao', 'Dias após a manifestação de interesse em que o empreendedor é lembrado (R2)', 'LISTA_INTEIROS', 'DIAS', '7,21', '7,21', 1, 60, NULL, FALSE),
('conexao.expiracao_dias', 'conexao', 'Manifestação de interesse sem resposta expira depois deste prazo (R1)', 'INTEIRO', 'DIAS', '30', '30', 7, 90, NULL, FALSE),
('conexao.nova_tentativa_dias', 'conexao', 'Espera para o investidor manifestar interesse de novo na mesma startup após recusa ou expiração', 'INTEIRO', 'DIAS', '90', '90', 0, 365, NULL, FALSE),
('nivel2.validade_dias', 'conexao', 'Validade do acesso aos dados confidenciais (Nível 2) liberado pelo empreendedor (R3)', 'INTEIRO', 'DIAS', '90', '90', 7, 365, NULL, TRUE),
('nivel2.aviso_dias', 'conexao', 'Antecedência do aviso de vencimento do acesso ao Nível 2 (R4)', 'INTEIRO', 'DIAS', '7', '7', 1, 30, NULL, FALSE),
-- Reuniões
('reuniao.lembretes', 'reuniao', 'Antecedências dos lembretes de reunião (R5)', 'LISTA_INTEIROS', 'HORAS', '24,1', '24,1', 1, 168, NULL, FALSE),
('reuniao.antecedencia_cancelamento_horas', 'reuniao', 'Cancelamento com menos antecedência que isto conta como ausência', 'INTEIRO', 'HORAS', '24', '24', 1, 72, NULL, FALSE),
('reuniao.limite_ausencias', 'reuniao', 'Ausências sem aviso que levam a restrição de agendamento', 'INTEIRO', 'QUANTIDADE', '2', '2', 1, 10, NULL, FALSE),
('reuniao.antecedencia_minima_horas', 'reuniao', 'Antecedência mínima para propor uma reunião', 'INTEIRO', 'HORAS', '24', '24', 1, 168, NULL, FALSE),
('reuniao.max_contrapropostas', 'reuniao', 'Número máximo de contrapropostas de horário por reunião', 'INTEIRO', 'QUANTIDADE', '3', '3', 1, 10, NULL, FALSE),
('reuniao.prazo_registro_dias', 'reuniao', 'Prazo para registrar o resultado da reunião', 'INTEIRO', 'DIAS', '3', '3', 1, 15, NULL, FALSE),
-- Rodadas
('rodada.max_prorrogacoes', 'rodada', 'Número máximo de prorrogações do prazo de uma rodada', 'INTEIRO', 'QUANTIDADE', '1', '1', 0, 5, NULL, FALSE),
('rodada.prorrogacao_max_dias', 'rodada', 'Prorrogação máxima de cada vez', 'INTEIRO', 'DIAS', '30', '30', 1, 90, NULL, FALSE),
('rodada.prazo_registro_resultado_dias', 'rodada', 'Prazo para registrar o resultado depois do fim da rodada (R6)', 'INTEIRO', 'DIAS', '15', '15', 1, 60, NULL, FALSE),
('rodada.prazo_confirmacao_dias', 'rodada', 'Prazo para cada investidor confirmar a participação (R7)', 'INTEIRO', 'DIAS', '15', '15', 1, 60, NULL, FALSE),
-- Qualidade da base
('startup.indicador_desatualizado_meses', 'startup', 'Indicador de tração sem atualização por este tempo é marcado como desatualizado (R8)', 'INTEIRO', 'MESES', '6', '6', 1, 24, NULL, FALSE),
('startup.inatividade_meses', 'startup', 'Startup publicada sem atualização por este tempo recebe aviso (R8)', 'INTEIRO', 'MESES', '9', '9', 3, 36, NULL, FALSE),
-- Moderação, recursos e atendimento
('moderacao.sla_critica_horas', 'moderacao', 'Prazo para tratar denúncia crítica', 'INTEIRO', 'HORAS', '24', '24', 1, 72, NULL, FALSE),
('moderacao.sla_alta_horas', 'moderacao', 'Prazo para tratar denúncia de prioridade alta', 'INTEIRO', 'HORAS', '48', '48', 1, 168, NULL, FALSE),
('moderacao.sla_normal_dias_uteis', 'moderacao', 'Prazo para tratar denúncia de prioridade normal', 'INTEIRO', 'DIAS_UTEIS', '5', '5', 1, 30, NULL, FALSE),
('moderacao.suspensao_dias', 'moderacao', 'Durações de suspensão aplicáveis, da mais leve à mais grave', 'LISTA_INTEIROS', 'DIAS', '7,30,90', '7,30,90', 1, 365, NULL, FALSE),
('recurso.prazo_dias', 'moderacao', 'Prazo para o usuário recorrer de uma decisão', 'INTEIRO', 'DIAS', '15', '15', 5, 60, NULL, FALSE),
('recurso.sla_dias_uteis', 'moderacao', 'Prazo para julgar um recurso', 'INTEIRO', 'DIAS_UTEIS', '5', '5', 1, 30, NULL, FALSE),
('suporte.sla_primeira_resposta_horas', 'atendimento', 'Prazo da primeira resposta a um chamado', 'INTEIRO', 'HORAS_UTEIS', '24', '24', 1, 72, NULL, FALSE),
('suporte.sla_n2_dias_uteis', 'atendimento', 'Prazo do atendimento de nível 2', 'INTEIRO', 'DIAS_UTEIS', '3', '3', 1, 15, NULL, FALSE),
('suporte.prazo_aguardando_dias', 'atendimento', 'Chamado aguardando o usuário por este tempo é encerrado', 'INTEIRO', 'DIAS', '7', '7', 1, 30, NULL, FALSE),
('disputa.sla_dias_uteis', 'atendimento', 'Prazo para concluir uma disputa entre usuários', 'INTEIRO', 'DIAS_UTEIS', '10', '10', 1, 30, NULL, FALSE),
-- Privacidade e retenção
('lgpd.prazo_resposta_dias', 'privacidade', 'Prazo para responder ao titular de dados (LGPD art. 19: até 15 dias)', 'INTEIRO', 'DIAS', '15', '15', 1, 15, NULL, TRUE),
('lgpd.retencao_legal_meses', 'privacidade', 'Guarda legal dos dados depois da exclusão da conta (provisório: definir com o jurídico)', 'INTEIRO', 'MESES', '60', '60', 6, 240, NULL, TRUE),
('retencao.historico_visualizacoes_meses', 'privacidade', 'Guarda do histórico de visualizações do catálogo', 'INTEIRO', 'MESES', '12', '12', 1, 60, NULL, TRUE),
('auditoria.retencao_meses', 'privacidade', 'Guarda da trilha de auditoria (provisório: definir com o jurídico)', 'INTEIRO', 'MESES', '60', '60', 12, 240, NULL, TRUE);
