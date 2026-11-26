package br.com.orbithub.notificacoes.domain;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Tipos de notificação
 * Objetivo: Enumerar os eventos que geram aviso, com o grupo para a tela, se são obrigatórios e o canal padrão.
 * Decisões: Obrigatórios (segurança da conta, termos, prazos que encerram conexão, avisos para a equipe) sempre vão
 *           pelos dois canais e a pessoa não pode desligar. Os eventos das próximas sprints já estão aqui, para a tela
 *           de preferências mostrar o catálogo completo; cada um passa a ser disparado quando a funcionalidade chegar.
 */
public enum TipoDeNotificacao {
    INTERESSE_RECEBIDO("Conexões", "Nova manifestação de interesse", false, Canal.AMBOS),
    MATCH("Conexões", "Match confirmado", false, Canal.AMBOS),
    MENSAGEM("Conexões", "Nova mensagem numa conexão", false, Canal.AMBOS),
    INTERESSE_RECUSADO("Conexões", "Interesse recusado", false, Canal.PLATAFORMA),
    INTERESSE_EXPIRADO("Conexões", "Interesse expirado", false, Canal.PLATAFORMA),
    INTERESSE_RETIRADO("Conexões", "Interesse retirado", false, Canal.PLATAFORMA),
    PRAZO_CONEXAO("Conexões", "Prazo que encerra uma conexão", true, Canal.AMBOS),
    NIVEL2_LIBERADO("Informações confidenciais", "Acesso ao Nível 2 liberado", false, Canal.AMBOS),
    NIVEL2_REVOGADO("Informações confidenciais", "Acesso ao Nível 2 revogado", false, Canal.AMBOS),
    NIVEL2_VENCENDO("Informações confidenciais", "Acesso ao Nível 2 perto de vencer", false, Canal.AMBOS),
    REUNIAO_CONVITE("Reuniões", "Convite para reunião", false, Canal.AMBOS),
    REUNIAO_LEMBRETE("Reuniões", "Lembrete de reunião", false, Canal.AMBOS),
    REUNIAO_ALTERADA("Reuniões", "Reunião alterada ou cancelada", false, Canal.AMBOS),
    VERIFICACAO_RESULTADO("Análises", "Resultado da verificação", false, Canal.AMBOS),
    MODERACAO("Moderação", "Denúncias, decisões da moderação e recursos", true, Canal.AMBOS),
    ATENDIMENTO("Atendimento", "Respostas aos seus chamados", false, Canal.AMBOS),
    CURADORIA_RESULTADO("Análises", "Resultado da curadoria", false, Canal.AMBOS),
    NOVA_OPORTUNIDADE("Oportunidades", "Nova startup compatível com a sua tese", false, Canal.PLATAFORMA),
    RODADA_AVISO("Oportunidades", "Avisos de rodada", false, Canal.PLATAFORMA),
    ORGANIZACAO("Organizações", "Convites e mudanças nas suas organizações", false, Canal.AMBOS),
    STARTUP("Startups", "Convites e mudanças nas equipes das suas startups", false, Canal.AMBOS),
    TERMOS_ATUALIZADOS("Conta", "Termos e políticas atualizados", true, Canal.AMBOS),
    SEGURANCA_CONTA("Conta", "Segurança da conta (senha e e-mail)", true, Canal.AMBOS),
    EQUIPE_APROVACAO("Equipe", "Ação grave aguardando a sua aprovação", true, Canal.AMBOS),
    EQUIPE_REVISAO_ACESSOS("Equipe", "Revisão de acessos vencida", true, Canal.AMBOS),
    EQUIPE_ENCAMINHAMENTO("Equipe", "Caso encaminhado a outro time (por exemplo, indício de fraude)", true, Canal.AMBOS);

    private final String grupo;
    private final String descricao;
    private final boolean obrigatoria;
    private final Canal padrao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Criação do tipo
     * Objetivo: Guardar grupo, descrição, obrigatoriedade e canal padrão.
     * Decisões: Textos em português, para a tela.
     */
    TipoDeNotificacao(String grupo, String descricao, boolean obrigatoria, Canal padrao) {
        this.grupo = grupo;
        this.descricao = descricao;
        this.obrigatoria = obrigatoria;
        this.padrao = padrao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Grupo
     * Objetivo: Devolver o grupo do tipo, para a tela agrupar.
     * Decisões: Somente leitura.
     */
    public String getGrupo() {
        return grupo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Descrição
     * Objetivo: Devolver o nome do evento para a tela.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Obrigatória
     * Objetivo: Dizer se o aviso não pode ser desligado.
     * Decisões: Obrigatório vai sempre pelos dois canais, imediato.
     */
    public boolean isObrigatoria() {
        return obrigatoria;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Canal padrão
     * Objetivo: Devolver o canal usado quando a pessoa não escolheu outro.
     * Decisões: Somente leitura.
     */
    public Canal getPadrao() {
        return padrao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Canal de entrega
     * Objetivo: Enumerar por onde o aviso chega.
     * Decisões: Sem "nenhum": quem não quer o aviso na plataforma ainda o vê na lista ao entrar.
     */
    public enum Canal {
        PLATAFORMA,
        EMAIL,
        AMBOS;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:34
         * Cartão Jira: KAN-146
         * Título: Inclui a plataforma
         * Objetivo: Dizer se o canal mostra o aviso no sino.
         * Decisões: PLATAFORMA ou AMBOS.
         */
        public boolean naPlataforma() {
            return this != EMAIL;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:34
         * Cartão Jira: KAN-146
         * Título: Inclui e-mail
         * Objetivo: Dizer se o canal manda e-mail.
         * Decisões: EMAIL ou AMBOS.
         */
        public boolean porEmail() {
            return this != PLATAFORMA;
        }
    }
}
