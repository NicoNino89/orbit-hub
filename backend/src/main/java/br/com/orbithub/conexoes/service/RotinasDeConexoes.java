package br.com.orbithub.conexoes.service;

import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.rotinas.RotinaBase;
import br.com.orbithub.conexoes.domain.Conexao;
import br.com.orbithub.conexoes.repository.ConexaoRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-105
 * Título: Rotinas das conexões (R1 e R2)
 * Objetivo: Expirar as manifestações pendentes sem resposta no prazo (R1) e lembrar os fundadores das pendentes (R2).
 * Decisões: Duas rotinas diárias, cada uma um componente. R3 e R4 (Nível 2) chegam com o KAN-30, na S8.
 */
final class RotinasDeConexoes {

    private RotinasDeConexoes() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: R1 – Expirar manifestações
     * Objetivo: Passar para expirada a conexão pendente criada há mais de conexao.expiracao_dias e avisar o investidor.
     * Decisões: Diária às 5h30.
     */
    @Component
    static class ExpirarManifestacoes extends RotinaBase {

        static final String PRAZO = "conexao.expiracao_dias";
        private final ConexaoRepository conexoes;
        private final ConexoesService servico;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:25
         * Cartão Jira: KAN-105
         * Título: ExpirarManifestacoes
         * Objetivo: Implementar ExpirarManifestacoes da estratégia ou rotina.
         * Decisões: Ver a classe.
         */
        ExpirarManifestacoes(ConexaoRepository conexoes, ConexoesService servico, LeituraDeParametros parametros) {
            super("R1", "Expira as manifestações de interesse sem resposta no prazo", "0 30 5 * * *", "conexoes", List.of(PRAZO));
            this.conexoes = conexoes;
            this.servico = servico;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:25
         * Cartão Jira: KAN-105
         * Título: rotina
         * Objetivo: Implementar rotina da estratégia ou rotina.
         * Decisões: Ver a classe.
         */
        @Override
        @Transactional
        public Resultado executar(Instant agora) {
            List<Conexao> vencidas = conexoes.findBySituacaoAndCriadaEmBefore(Conexao.Situacao.PENDENTE,
                    agora.minus(Duration.ofDays(parametros.inteiro(PRAZO))));
            vencidas.forEach(servico::expirar);
            return new Resultado(vencidas.size(), 0, vencidas.size() + " manifestação(ões) expirada(s)");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: R2 – Lembretes de resposta
     * Objetivo: Lembrar os fundadores administradores das manifestações pendentes em cada prazo de conexao.lembrete_dias.
     * Decisões: Diária às 9h. Um lembrete por prazo cumprido (contados na conexão), sem repetir.
     */
    @Component
    static class LembretesDeResposta extends RotinaBase {

        static final String PRAZOS = "conexao.lembrete_dias";
        private final ConexaoRepository conexoes;
        private final ConexoesService servico;
        private final LeituraDeParametros parametros;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:25
         * Cartão Jira: KAN-105
         * Título: LembretesDeResposta
         * Objetivo: Implementar LembretesDeResposta da estratégia ou rotina.
         * Decisões: Ver a classe.
         */
        LembretesDeResposta(ConexaoRepository conexoes, ConexoesService servico, LeituraDeParametros parametros) {
            super("R2", "Lembra os fundadores das manifestações de interesse pendentes", "0 0 9 * * *", "conexoes", List.of(PRAZOS));
            this.conexoes = conexoes;
            this.servico = servico;
            this.parametros = parametros;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:25
         * Cartão Jira: KAN-105
         * Título: rotina
         * Objetivo: Implementar rotina da estratégia ou rotina.
         * Decisões: Ver a classe.
         */
        @Override
        @Transactional
        public Resultado executar(Instant agora) {
            List<Long> prazos = parametros.listaDeInteiros(PRAZOS);
            int lembradas = 0;
            for (Conexao c : conexoes.findBySituacaoAndCriadaEmBefore(Conexao.Situacao.PENDENTE, agora)) {
                long cumpridos = prazos.stream().filter(d -> !c.getCriadaEm().plus(Duration.ofDays(d)).isAfter(agora)).count();
                if (cumpridos > c.getLembretes()) {
                    servico.lembrar(c);
                    lembradas++;
                }
            }
            return new Resultado(lembradas, 0, lembradas + " lembrete(s) enviado(s)");
        }
    }
}
