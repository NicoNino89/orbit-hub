package br.com.orbithub.identidade.service;

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
 * Título: R16 – Lembrete da revisão de acessos
 * Objetivo: Lembrar o administrador quando a revisão periódica dos acessos da equipe vence (KAN-75).
 * Decisões: Diária às 8h: enquanto a revisão estiver vencida, lembra uma vez por dia. O lembrete vai para o log como
 *           WARN; a notificação na plataforma entra com a central de notificações (KAN-146).
 */
@Component
class LembreteDeRevisaoDeAcessos extends RotinaBase {

    private static final Logger LOG = LoggerFactory.getLogger(LembreteDeRevisaoDeAcessos.class);

    private final EquipeService equipe;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o serviço da equipe.
     * Decisões: A situação da revisão já é calculada por ele.
     */
    LembreteDeRevisaoDeAcessos(EquipeService equipe) {
        super("R16", "Lembra o administrador da revisão de acessos da equipe vencida", "0 0 8 * * *", "identidade",
                List.of(Chaves.ACESSO_REVISAO));
        this.equipe = equipe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
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
        return new Resultado(1, 0, "Revisão de acessos vencida: administrador lembrado");
    }
}
