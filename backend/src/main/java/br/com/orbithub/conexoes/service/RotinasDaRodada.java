package br.com.orbithub.conexoes.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 14:23
 * Cartão Jira: KAN-106
 * Título: Rotinas da rodada (R6 e R7)
 * Objetivo: Tratar todo dia as rodadas vencidas (R6) e as confirmações de participação pendentes (R7).
 * Decisões: A regra fica na condução da rodada.
 */
final class RotinasDaRodada {

    private RotinasDaRodada() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-106
     * Título: R6 – Rodadas vencidas
     * Objetivo: Passar as vencidas para aguardando resultado e encerrar as sem registro no prazo.
     * Decisões: Diária às 6h30.
     */
    @Component
    static class RodadasVencidas extends RotinaBase {

        private final ConducaoDaRodadaService conducao;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 14:23
         * Cartão Jira: KAN-106
         * Título: Criação da R6
         * Objetivo: Registrar código, horário e parâmetro.
         * Decisões: Módulo conexões.
         */
        RodadasVencidas(ConducaoDaRodadaService conducao) {
            super("R6", "Trata as rodadas com prazo vencido", "0 30 6 * * *", "conexoes", List.of(ConducaoDaRodadaService.PRAZO_RESULTADO));
            this.conducao = conducao;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 14:23
         * Cartão Jira: KAN-106
         * Título: Executar a R6
         * Objetivo: Chamar o tratamento das vencidas.
         * Decisões: Devolve quantas mudaram.
         */
        @Override
        public Resultado executar(Instant agora) {
            int n = conducao.tratarRodadasVencidas();
            return new Resultado(n, 0, n + " rodada(s) tratada(s)");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-106
     * Título: R7 – Confirmação de participação
     * Objetivo: Lembrar e vencer as confirmações pendentes.
     * Decisões: Diária às 9h30.
     */
    @Component
    static class ConfirmacaoDeParticipacao extends RotinaBase {

        private final ConducaoDaRodadaService conducao;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 14:23
         * Cartão Jira: KAN-106
         * Título: Criação da R7
         * Objetivo: Registrar código, horário e parâmetro.
         * Decisões: Módulo conexões.
         */
        ConfirmacaoDeParticipacao(ConducaoDaRodadaService conducao) {
            super("R7", "Lembra e vence as confirmações de participação", "0 30 9 * * *", "conexoes",
                    List.of(ConducaoDaRodadaService.PRAZO_CONFIRMACAO));
            this.conducao = conducao;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 14:23
         * Cartão Jira: KAN-106
         * Título: Executar a R7
         * Objetivo: Chamar o tratamento das confirmações.
         * Decisões: Devolve quantas trataram.
         */
        @Override
        public Resultado executar(Instant agora) {
            int n = conducao.tratarConfirmacoes();
            return new Resultado(n, 0, n + " confirmação(ões) tratada(s)");
        }
    }
}
