package br.com.orbithub.verificacao.service;

import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import br.com.orbithub.investidores.service.OrganizacoesService;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import br.com.orbithub.verificacao.domain.PedidoDeVerificacao;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:33
 * Cartão Jira: KAN-83
 * Título: Avisos da verificação
 * Objetivo: Avisar o sujeito de um pedido (a pessoa ou os administradores da organização) e a administração da
 *           plataforma, pela central de notificações.
 * Decisões: Resultado da verificação usa o tipo VERIFICACAO_RESULTADO (o usuário escolhe o canal); encaminhamento por
 *           fraude usa EQUIPE_ENCAMINHAMENTO (obrigatório). O texto nunca traz dado dos documentos.
 */
@Component
class AvisosDaVerificacao {

    static final String LINK = "/investidor/verificacao";

    private final NotificacoesService notificacoes;
    private final OrganizacoesService organizacoes;
    private final ConsultaDeContas contas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Criação dos avisos
     * Objetivo: Receber notificações, organizações e contas.
     * Decisões: Injeção por construtor.
     */
    AvisosDaVerificacao(NotificacoesService notificacoes, OrganizacoesService organizacoes, ConsultaDeContas contas) {
        this.notificacoes = notificacoes;
        this.organizacoes = organizacoes;
        this.contas = contas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Avisar o sujeito
     * Objetivo: Mandar um aviso sobre o pedido para quem responde por ele.
     * Decisões: Organização: todos os administradores ativos.
     */
    void avisarSujeito(PedidoDeVerificacao pedido, String titulo, String mensagem) {
        notificacoes.notificarTodos(destinatarios(pedido), TipoDeNotificacao.VERIFICACAO_RESULTADO, titulo, mensagem, LINK);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Avisar a administração
     * Objetivo: Avisar os administradores da plataforma de um caso encaminhado (indício de fraude).
     * Decisões: Até o módulo de moderação (KAN-71), quem recebe é o administrador.
     */
    void avisarAdministracao(PedidoDeVerificacao pedido, String motivo) {
        notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)),
                TipoDeNotificacao.EQUIPE_ENCAMINHAMENTO, "Verificação recusada por indício de fraude",
                "O pedido de verificação " + pedido.getId() + " foi recusado com o motivo \"" + motivo
                        + "\" e precisa de análise da moderação.", "/admin/verificacao/" + pedido.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Nome do sujeito
     * Objetivo: Devolver o nome da pessoa ou da organização, para a fila.
     * Decisões: "Conta removida" quando não encontrar.
     */
    String nomeDoSujeito(PedidoDeVerificacao pedido) {
        return pedido.getSujeitoTipo() == SujeitoDaVerificacao.ORGANIZACAO
                ? organizacoes.nome(pedido.getSujeitoId()).orElse("Organização removida")
                : contas.nome(pedido.getSujeitoId()).orElse("Conta removida");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Destinatários
     * Objetivo: Listar quem recebe o aviso de um pedido.
     * Decisões: Pessoa física: ela mesma.
     */
    private List<UUID> destinatarios(PedidoDeVerificacao pedido) {
        return pedido.getSujeitoTipo() == SujeitoDaVerificacao.ORGANIZACAO
                ? organizacoes.administradoresDe(pedido.getSujeitoId()) : List.of(pedido.getSujeitoId());
    }
}
