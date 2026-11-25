package br.com.orbithub.atendimento.service;

import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-89, KAN-108
 * Título: Rotinas do atendimento
 * Objetivo: Agrupar a R20 (fechamento dos chamados parados). A R14 passou para o módulo de operação (KAN-108).
 * Decisões: Uma classe por rotina, aninhadas aqui.
 */
final class RotinasDoAtendimento {

    private RotinasDoAtendimento() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-89
     * Título: R20 – Chamados parados
     * Objetivo: Fechar os chamados que esperam o usuário ou foram resolvidos há mais de suporte.prazo_aguardando_dias.
     * Decisões: Diária, às 4h.
     */
    @Component
    static class FechamentoDeChamados extends RotinaBase {

        private final AtendimentoService atendimento;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-89
         * Título: Criação
         * Objetivo: Registrar a rotina.
         * Decisões: Parâmetro suporte.prazo_aguardando_dias.
         */
        FechamentoDeChamados(AtendimentoService atendimento) {
            super("R20", "Fecha os chamados parados à espera do usuário", "0 0 4 * * *", "atendimento", List.of(AtendimentoService.PRAZO_AGUARDANDO));
            this.atendimento = atendimento;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 16:19
         * Cartão Jira: KAN-89
         * Título: Executar
         * Objetivo: Fechar os chamados parados.
         * Decisões: Ver AtendimentoService.fecharParados.
         */
        @Override
        public Resultado executar(Instant agora) {
            int n = atendimento.fecharParados();
            return new Resultado(n, 0, n + " chamado(s) fechado(s)");
        }
    }
}
