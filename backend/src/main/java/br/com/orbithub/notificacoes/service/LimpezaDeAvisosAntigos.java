package br.com.orbithub.notificacoes.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.notificacoes.repository.NotificacaoRepository;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: R19 – Limpeza das notificações antigas
 * Objetivo: Apagar as notificações mais antigas que notificacoes.retencao_dias.
 * Decisões: Diária às 3h30; lidas ou não, saem pela idade. Idempotente.
 */
@Component
class LimpezaDeAvisosAntigos extends RotinaBase {

    static final String CHAVE_RETENCAO = "notificacoes.retencao_dias";

    private final NotificacaoRepository notificacoes;
    private final LeituraDeParametros parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Criação da rotina
     * Objetivo: Descrever a rotina e receber o repositório e os parâmetros.
     * Decisões: Injeção por construtor.
     */
    LimpezaDeAvisosAntigos(NotificacaoRepository notificacoes, LeituraDeParametros parametros) {
        super("R19", "Apaga as notificações antigas", "0 30 3 * * *", "notificacoes", List.of(CHAVE_RETENCAO));
        this.notificacoes = notificacoes;
        this.parametros = parametros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Apagar antigas
     * Objetivo: Remover as notificações além da retenção.
     * Decisões: Um item por notificação apagada.
     */
    @Override
    @Transactional
    public Resultado executar(Instant agora) {
        int apagadas = notificacoes.apagarAntesDe(agora.minus(parametros.duracao(CHAVE_RETENCAO)));
        return new Resultado(apagadas, 0, apagadas + " notificação(ões) apagada(s)");
    }
}
