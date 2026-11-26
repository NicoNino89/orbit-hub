package br.com.orbithub.privacidade.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.EtapaDeRetencao;
import java.sql.Timestamp;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 21:10
 * Cartão Jira: KAN-93, KAN-107
 * Título: Etapas de retenção dos dados pessoais
 * Objetivo: Aplicar, pela R12, os prazos de guarda: cadastros não confirmados, histórico de visualizações, chamados
 *           fechados, denúncias decididas e a guarda legal das contas excluídas.
 * Decisões: Cadastro não confirmado é anonimizado (não apagado), como a exclusão de conta. Chamados e denúncias têm só
 *           o texto livre e o contato anonimizados; os números (para indicadores) ficam. Base comum (Template Method):
 *           cada etapa só diz o tipo, o parâmetro e o que fazer com o limite.
 */
final class EtapasDeRetencao {

    private static final String ANONIMIZADO = "[anonimizado]";

    private EtapasDeRetencao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-93
     * Título: Etapa com um prazo
     * Objetivo: Guardar o banco, o tipo e o parâmetro do prazo e calcular o limite (em meses ou dias).
     * Decisões: Meses de calendário, em UTC.
     */
    abstract static class Base implements EtapaDeRetencao {

        protected final JdbcTemplate jdbc;
        private final LeituraDeParametros parametros;
        private final String tipo;
        private final String parametro;
        private final boolean emDias;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Receber o banco, os parâmetros e a descrição da etapa.
         * Decisões: emDias para o prazo do cadastro; o resto em meses.
         */
        Base(JdbcTemplate jdbc, LeituraDeParametros parametros, String tipo, String parametro, boolean emDias) {
            this.jdbc = jdbc;
            this.parametros = parametros;
            this.tipo = tipo;
            this.parametro = parametro;
            this.emDias = emDias;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Tipo
         * Objetivo: Nomear o tipo de dado.
         * Decisões: Texto do resumo da R12.
         */
        @Override
        public String tipo() {
            return tipo;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Parâmetros
         * Objetivo: Listar o prazo usado.
         * Decisões: Um parâmetro por etapa.
         */
        @Override
        public List<String> parametros() {
            return List.of(parametro);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Calcular o limite e chamar a etapa.
         * Decisões: O limite é o instante antes do qual o dado venceu.
         */
        @Override
        public int aplicar(Instant agora) {
            long prazo = parametros.inteiro(parametro);
            Instant limite = emDias ? agora.minus(Duration.ofDays(prazo)) : agora.atOffset(ZoneOffset.UTC).minusMonths(prazo).toInstant();
            return aplicar(Timestamp.from(agora), Timestamp.from(limite));
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Aplicar até o limite
         * Objetivo: Excluir ou anonimizar o que venceu antes do limite.
         * Decisões: Devolve a quantidade tratada.
         */
        abstract int aplicar(Timestamp agora, Timestamp limite);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-107, KAN-12
     * Título: Cadastros não confirmados
     * Objetivo: Anonimizar as contas que não confirmaram o e-mail no prazo.
     * Decisões: cadastro.prazo_confirmacao_dias; contas da equipe ficam de fora (o convite tem fluxo próprio).
     */
    @Component
    static class CadastrosNaoConfirmados extends Base {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-107
         * Título: Criação
         * Objetivo: Descrever a etapa.
         * Decisões: Prazo em dias.
         */
        CadastrosNaoConfirmados(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            super(jdbc, parametros, "cadastro(s) não confirmado(s)", "cadastro.prazo_confirmacao_dias", true);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-107
         * Título: Aplicar
         * Objetivo: Anonimizar os cadastros vencidos.
         * Decisões: Mesmo formato da exclusão de conta.
         */
        @Override
        int aplicar(Timestamp agora, Timestamp limite) {
            return jdbc.update("UPDATE usuario SET nome = 'Conta excluída', email = 'excluida-' || id || '@anonimo.orbithub.com.br', senha_hash = '{noop}' "
                    + "|| gen_random_uuid(), telefone = NULL, status = 'DESATIVADO', excluida_em = ?, atualizado_em = ?, versao = versao + 1 "
                    + "WHERE status = 'PENDENTE_CONFIRMACAO' AND NOT interna AND criado_em < ? AND excluida_em IS NULL", agora, agora, limite);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-93
     * Título: Histórico de visualizações
     * Objetivo: Apagar as visitas do catálogo mais antigas que retencao.historico_visualizacoes_meses.
     * Decisões: Exclusão.
     */
    @Component
    static class Visualizacoes extends Base {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Descrever a etapa.
         * Decisões: Prazo em meses.
         */
        Visualizacoes(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            super(jdbc, parametros, "visualização(ões) do catálogo", "retencao.historico_visualizacoes_meses", false);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Apagar as visitas antigas.
         * Decisões: Pela data da visita.
         */
        @Override
        int aplicar(Timestamp agora, Timestamp limite) {
            return jdbc.update("DELETE FROM catalogo_visualizacao WHERE vista_em < ?", limite);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-93, KAN-89
     * Título: Chamados fechados
     * Objetivo: Anonimizar o texto e o contato dos chamados fechados há mais de retencao.chamados_meses.
     * Decisões: Mantém categoria, serviço, prazos e nota (indicadores).
     */
    @Component
    static class Chamados extends Base {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Descrever a etapa.
         * Decisões: Prazo em meses.
         */
        Chamados(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            super(jdbc, parametros, "chamado(s) anonimizado(s)", "retencao.chamados_meses", false);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Anonimizar os chamados antigos e as mensagens deles.
         * Decisões: Pela última atualização.
         */
        @Override
        int aplicar(Timestamp agora, Timestamp limite) {
            jdbc.update("UPDATE chamado_mensagem SET texto = ? WHERE texto <> ? AND chamado_id IN (SELECT id FROM chamado WHERE situacao = 'FECHADO' "
                    + "AND atualizado_em < ?)", ANONIMIZADO, ANONIMIZADO, limite);
            return jdbc.update("UPDATE chamado SET descricao = ?, email_contato = '', nome_contato = ?, comentario_avaliacao = NULL WHERE situacao = 'FECHADO' "
                    + "AND atualizado_em < ? AND descricao <> ?", ANONIMIZADO, ANONIMIZADO, limite, ANONIMIZADO);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-93, KAN-99
     * Título: Denúncias decididas
     * Objetivo: Anonimizar o texto e as evidências das denúncias de casos decididos há mais de retencao.denuncias_meses.
     * Decisões: O caso e a sanção ficam (histórico do denunciado).
     */
    @Component
    static class Denuncias extends Base {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Criação
         * Objetivo: Descrever a etapa.
         * Decisões: Prazo em meses.
         */
        Denuncias(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            super(jdbc, parametros, "denúncia(s) anonimizada(s)", "retencao.denuncias_meses", false);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-93
         * Título: Aplicar
         * Objetivo: Anonimizar as denúncias antigas.
         * Decisões: Pela data da decisão do caso.
         */
        @Override
        int aplicar(Timestamp agora, Timestamp limite) {
            return jdbc.update("UPDATE denuncia SET descricao = ?, evidencias = NULL WHERE descricao <> ? AND caso_id IN (SELECT id FROM caso_moderacao "
                    + "WHERE situacao = 'DECIDIDO' AND decidido_em < ?)", ANONIMIZADO, ANONIMIZADO, limite);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 21:10
     * Cartão Jira: KAN-17, KAN-90
     * Título: Guarda legal das contas excluídas
     * Objetivo: Apagar, depois de lgpd.retencao_legal_meses, o que ficou guardado da conta excluída (detalhes dos pedidos
     *           do titular e o histórico de mudanças da conta).
     * Decisões: A linha anonimizada da conta continua, para as referências.
     */
    @Component
    static class GuardaLegal extends Base {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-17
         * Título: Criação
         * Objetivo: Descrever a etapa.
         * Decisões: Prazo em meses.
         */
        GuardaLegal(JdbcTemplate jdbc, LeituraDeParametros parametros) {
            super(jdbc, parametros, "registro(s) de guarda legal apagado(s)", "lgpd.retencao_legal_meses", false);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 21:10
         * Cartão Jira: KAN-17
         * Título: Aplicar
         * Objetivo: Apagar o que passou da guarda legal.
         * Decisões: Pela data da exclusão.
         */
        @Override
        int aplicar(Timestamp agora, Timestamp limite) {
            int pedidos = jdbc.update("UPDATE pedido_titular SET detalhes = NULL, resposta = ? WHERE resposta IS DISTINCT FROM ? AND usuario_id IN "
                    + "(SELECT id FROM usuario WHERE excluida_em < ?)", ANONIMIZADO, ANONIMIZADO, limite);
            return pedidos + jdbc.update("DELETE FROM historico_conta WHERE usuario_id IN (SELECT id FROM usuario WHERE excluida_em < ?)", limite);
        }
    }
}
