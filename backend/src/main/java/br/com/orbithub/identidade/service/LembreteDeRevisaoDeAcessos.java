package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: R16 – Lembrete da revisão de acessos
 * Objetivo: Lembrar o administrador quando a revisão periódica dos acessos da equipe vence (KAN-75).
 * Decisões: Diária às 8h: enquanto a revisão estiver vencida, lembra uma vez por dia. O lembrete vai para o log como
 *           WARN e para os administradores ativos, pela central de notificações (KAN-146).
 */
@Component
class LembreteDeRevisaoDeAcessos extends RotinaBase {

    private static final Logger LOG = LoggerFactory.getLogger(LembreteDeRevisaoDeAcessos.class);

    private final EquipeService equipe;
    private final ConsultaDeContas contas;
    private final NotificacoesService notificacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o serviço da equipe.
     * Decisões: A situação da revisão já é calculada por ele.
     */
    LembreteDeRevisaoDeAcessos(EquipeService equipe, ConsultaDeContas contas, NotificacoesService notificacoes) {
        super("R16", "Lembra o administrador da revisão de acessos da equipe vencida", "0 0 8 * * *", "identidade",
                List.of(Chaves.ACESSO_REVISAO));
        this.equipe = equipe;
        this.contas = contas;
        this.notificacoes = notificacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Conferir a revisão
     * Objetivo: Lembrar se a revisão está vencida.
     * Decisões: Um item quando lembra; zero quando está em dia.
     */
    @Override
    public Resultado executar(Instant agora) {
        EquipeService.SituacaoRevisao situacao = equipe.situacaoRevisao();
        if (!situacao.vencida()) {
            return new Resultado(0, 0, "Revisão de acessos em dia");
        }
        LOG.warn("Revisão de acessos da equipe vencida desde {}", situacao.proximaEm());
        notificacoes.notificarTodos(contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN)),
                TipoDeNotificacao.EQUIPE_REVISAO_ACESSOS, "Revisão de acessos vencida",
                "Confira quem continua com cada papel na equipe e confirme a revisão.", "/admin/equipe");
        return new Resultado(1, 0, "Revisão de acessos vencida: administrador lembrado");
    }
}
