package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: R15 – Alerta de propostas de segunda aprovação paradas
 * Objetivo: Alertar o administrador das propostas de ações graves sem decisão há mais de aprovacao.prazo_horas (KAN-81).
 * Decisões: De hora em hora. Idempotente: cada proposta é marcada como alertada e não gera um segundo alerta. O alerta
 *           vai para o log como WARN e para quem pode aprovar, pela central de notificações (KAN-146).
 */
@Component
class AlertaDePropostasParadas extends RotinaBase {

    private static final Logger LOG = LoggerFactory.getLogger(AlertaDePropostasParadas.class);

    private final SegundaAprovacaoService aprovacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o serviço de segunda aprovação.
     * Decisões: Ele já marca e devolve as vencidas.
     */
    AlertaDePropostasParadas(SegundaAprovacaoService aprovacoes) {
        super("R15", "Alerta o administrador das propostas de segunda aprovação paradas", "0 5 * * * *",
                "administracao", List.of(Chaves.APROVACAO_PRAZO));
        this.aprovacoes = aprovacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Alertar as vencidas
     * Objetivo: Marcar e alertar as propostas paradas além do prazo.
     * Decisões: Um item por proposta alertada.
     */
    @Override
    public Resultado executar(Instant agora) {
        List<PropostaDeAprovacao> vencidas = aprovacoes.vencidasParaAlertar();
        vencidas.forEach(p -> {
            LOG.warn("Proposta de segunda aprovação parada além do prazo: {} {}", p.getTipo(), p.getId());
            aprovacoes.avisarQuemAprova(p, "Proposta parada aguardando decisão");
        });
        return new Resultado(vencidas.size(), 0, vencidas.size() + " proposta(s) alertada(s)");
    }
}
