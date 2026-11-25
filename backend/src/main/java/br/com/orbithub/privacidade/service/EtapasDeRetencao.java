package br.com.orbithub.privacidade.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.EtapaDeRetencao;
import java.sql.Timestamp;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-93, KAN-107
 * Título: Etapas de retenção dos dados pessoais
 * Objetivo: Aplicar, pela R12, os prazos de guarda: cadastros não confirmados, histórico de visualizações, chamados
 *           fechados, denúncias decididas e a guarda legal das contas excluídas.
 * Decisões: Cadastro não confirmado é anonimizado (não apagado), como a exclusão de conta. Chamados e denúncias têm só
 *           o texto livre e o contato anonimizados; os números (para indicadores) ficam.
 */
final class EtapasDeRetencao {

    private static final String ANONIMIZADO = "[anonimizado]";

    private EtapasDeRetencao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93
     * Título: Meses atrás
     * Objetivo: Calcular o limite de um prazo em meses.
     * Decisões: Meses de calendário, em UTC.
     */
    static Timestamp mesesAtras(Instant agora, long meses) {
        return Timestamp.from(agora.atOffset(ZoneOffset.UTC).minusMonths(meses).toInstant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-107, KAN-12
     * Título: Cadastros não confirmados
     * Objetivo: Anonimizar as contas que não confirmaram o e-mail no prazo.
     * Decisões: cadastro.prazo_confirmacao_dias; contas da equipe ficam de fora (o convite tem fluxo próprio).
     */
    @Component
    static class CadastrosNaoConfirmados implements EtapaDeRetencao {

        private final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-107
         * Título: Criação
         * Objetivo: Receber o banco e os parâmetros.
         * Decisões: Injeção por construtor.
         */
        CadastrosNaoConfirmados(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            this.jdbc = jdbc;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-107
         * Título: Tipo
         * Objetivo: Nomear o tipo.
         * Decisões: Texto do resumo.
         */
        @Override
        public String tipo() {
            return "cadastro(s) não confirmado(s)";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-107
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Prazo de confirmação do cadastro.
         */
        @Override
        public List<String> parametros() {
            return List.of("cadastro.prazo_confirmacao_dias");
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-107
         * Título: Aplicar
         * Objetivo: Anonimizar os cadastros vencidos.
         * Decisões: Mesmo formato da exclusão de conta.
         */
        @Override
        public int aplicar(Instant agora) {
            Timestamp limite = Timestamp.from(agora.minus(java.time.Duration.ofDays(parametros.inteiro("cadastro.prazo_confirmacao_dias"))));
            return jdbc.update("UPDATE usuario SET nome = 'Conta excluída', email = 'excluida-' || id || '@anonimo.orbithub.com.br', senha_hash = '{noop}' "
                    + "|| gen_random_uuid(), telefone = NULL, status = 'DESATIVADO', excluida_em = ?, atualizado_em = ?, versao = versao + 1 "
                    + "WHERE status = 'PENDENTE_CONFIRMACAO' AND NOT interna AND criado_em < ? AND excluida_em IS NULL", Timestamp.from(agora),
                    Timestamp.from(agora), limite);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93
     * Título: Histórico de visualizações
     * Objetivo: Apagar as visitas do catálogo mais antigas que retencao.historico_visualizacoes_meses.
     * Decisões: Exclusão.
     */
    @Component
    static class Visualizacoes implements EtapaDeRetencao {

        private final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Receber o banco e os parâmetros.
         * Decisões: Injeção por construtor.
         */
        Visualizacoes(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            this.jdbc = jdbc;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Tipo
         * Objetivo: Nomear o tipo.
         * Decisões: Texto do resumo.
         */
        @Override
        public String tipo() {
            return "visualização(ões) do catálogo";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Meses.
         */
        @Override
        public List<String> parametros() {
            return List.of("retencao.historico_visualizacoes_meses");
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Apagar as visitas antigas.
         * Decisões: Pela data da visita.
         */
        @Override
        public int aplicar(Instant agora) {
            return jdbc.update("DELETE FROM catalogo_visualizacao WHERE vista_em < ?", mesesAtras(agora, parametros.inteiro("retencao.historico_visualizacoes_meses")));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93, KAN-89
     * Título: Chamados fechados
     * Objetivo: Anonimizar o texto e o contato dos chamados fechados há mais de retencao.chamados_meses.
     * Decisões: Mantém categoria, serviço, prazos e nota (indicadores).
     */
    @Component
    static class Chamados implements EtapaDeRetencao {

        private final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Receber o banco e os parâmetros.
         * Decisões: Injeção por construtor.
         */
        Chamados(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            this.jdbc = jdbc;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Tipo
         * Objetivo: Nomear o tipo.
         * Decisões: Texto do resumo.
         */
        @Override
        public String tipo() {
            return "chamado(s) anonimizado(s)";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Meses.
         */
        @Override
        public List<String> parametros() {
            return List.of("retencao.chamados_meses");
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Anonimizar os chamados antigos e as mensagens deles.
         * Decisões: Pela última atualização.
         */
        @Override
        public int aplicar(Instant agora) {
            Timestamp limite = mesesAtras(agora, parametros.inteiro("retencao.chamados_meses"));
            jdbc.update("UPDATE chamado_mensagem SET texto = ? WHERE texto <> ? AND chamado_id IN (SELECT id FROM chamado WHERE situacao = 'FECHADO' "
                    + "AND atualizado_em < ?)", ANONIMIZADO, ANONIMIZADO, limite);
            return jdbc.update("UPDATE chamado SET descricao = ?, email_contato = '', nome_contato = ?, comentario_avaliacao = NULL WHERE situacao = 'FECHADO' "
                    + "AND atualizado_em < ? AND descricao <> ?", ANONIMIZADO, ANONIMIZADO, limite, ANONIMIZADO);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93, KAN-99
     * Título: Denúncias decididas
     * Objetivo: Anonimizar o texto e as evidências das denúncias de casos decididos há mais de retencao.denuncias_meses.
     * Decisões: O caso e a sanção ficam (histórico do denunciado).
     */
    @Component
    static class Denuncias implements EtapaDeRetencao {

        private final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Receber o banco e os parâmetros.
         * Decisões: Injeção por construtor.
         */
        Denuncias(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            this.jdbc = jdbc;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Tipo
         * Objetivo: Nomear o tipo.
         * Decisões: Texto do resumo.
         */
        @Override
        public String tipo() {
            return "denúncia(s) anonimizada(s)";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Meses.
         */
        @Override
        public List<String> parametros() {
            return List.of("retencao.denuncias_meses");
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Anonimizar as denúncias antigas.
         * Decisões: Pela data da decisão do caso.
         */
        @Override
        public int aplicar(Instant agora) {
            return jdbc.update("UPDATE denuncia SET descricao = ?, evidencias = NULL WHERE descricao <> ? AND caso_id IN (SELECT id FROM caso_moderacao "
                    + "WHERE situacao = 'DECIDIDO' AND decidido_em < ?)", ANONIMIZADO, ANONIMIZADO,
                    mesesAtras(agora, parametros.inteiro("retencao.denuncias_meses")));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-17, KAN-90
     * Título: Guarda legal das contas excluídas
     * Objetivo: Apagar, depois de lgpd.retencao_legal_meses, o que ficou guardado da conta excluída (detalhes dos pedidos
     *           do titular e o histórico de mudanças da conta).
     * Decisões: A linha anonimizada da conta continua, para as referências.
     */
    @Component
    static class GuardaLegal implements EtapaDeRetencao {

        private final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-17
         * Título: Criação
         * Objetivo: Receber o banco e os parâmetros.
         * Decisões: Injeção por construtor.
         */
        GuardaLegal(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            this.jdbc = jdbc;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-17
         * Título: Tipo
         * Objetivo: Nomear o tipo.
         * Decisões: Texto do resumo.
         */
        @Override
        public String tipo() {
            return "registro(s) de guarda legal apagado(s)";
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-17
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Meses.
         */
        @Override
        public List<String> parametros() {
            return List.of("lgpd.retencao_legal_meses");
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 20:54
         * Cartão Jira: KAN-17
         * Título: Aplicar
         * Objetivo: Apagar o que passou da guarda legal.
         * Decisões: Pela data da exclusão.
         */
        @Override
        public int aplicar(Instant agora) {
            Timestamp limite = mesesAtras(agora, parametros.inteiro("lgpd.retencao_legal_meses"));
            int pedidos = jdbc.update("UPDATE pedido_titular SET detalhes = NULL, resposta = ? WHERE resposta IS DISTINCT FROM ? AND usuario_id IN "
                    + "(SELECT id FROM usuario WHERE excluida_em < ?)", ANONIMIZADO, ANONIMIZADO, limite);
            int historico = jdbc.update("DELETE FROM historico_conta WHERE usuario_id IN (SELECT id FROM usuario WHERE excluida_em < ?)", limite);
            return pedidos + historico;
        }
    }
}
