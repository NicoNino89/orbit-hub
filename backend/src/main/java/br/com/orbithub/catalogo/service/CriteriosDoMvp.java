package br.com.orbithub.catalogo.service;

import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import java.util.Optional;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-39
 * Título: Critérios de compatibilidade do MVP
 * Objetivo: Implementar os oito critérios da seleção personalizada: segmento, estágio da rodada, maturidade, ticket,
 *           localização, modelo de negócio, instrumento e liderança.
 * Decisões: Uma estratégia por critério, cada uma num componente; a ordem (@Order) é a das justificativas na tela.
 *           Ticket compatível quando o mínimo do investidor não passa do máximo da rodada (ou do alvo, sem máximo).
 */
final class CriteriosDoMvp {

    private CriteriosDoMvp() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Segmento
     * Objetivo: Segmento da startup entre os segmentos de interesse.
     * Decisões: Segmento aprovado.
     */
    @Component
    @Order(1)
    static class Segmento implements CriterioDeCompatibilidade {
        @Override
        public Optional<String> avaliar(Contexto c) {
            return c.perfil().segmentos().contains(c.segmento())
                    ? Optional.of("Atua em " + c.segmentoRotulo() + ", um dos seus segmentos") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Estágio da rodada
     * Objetivo: Estágio da rodada aberta entre os estágios preferidos.
     * Decisões: Depende da rodada.
     */
    @Component
    @Order(2)
    static class EstagioDaRodada implements CriterioDeCompatibilidade {
        @Override
        public boolean daRodada() {
            return true;
        }

        @Override
        public Optional<String> avaliar(Contexto c) {
            String estagio = c.rodada().estagio();
            return estagio != null && c.perfil().estagios().contains(estagio)
                    ? Optional.of("Rodada " + c.estagioRotulo() + ", estágio em que você investe") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Maturidade
     * Objetivo: Maturidade do negócio entre as preferidas.
     * Decisões: Investidor sem preferência de maturidade não pontua.
     */
    @Component
    @Order(3)
    static class Maturidade implements CriterioDeCompatibilidade {
        @Override
        public Optional<String> avaliar(Contexto c) {
            return c.maturidade() != null && c.perfil().maturidades() != null && c.perfil().maturidades().contains(c.maturidade())
                    ? Optional.of("Está em " + c.maturidade().getRotulo() + ", maturidade que você prefere") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Ticket
     * Objetivo: A rodada comporta o ticket mínimo do investidor.
     * Decisões: Compara com o máximo da rodada ou, sem ele, com o alvo.
     */
    @Component
    @Order(4)
    static class Ticket implements CriterioDeCompatibilidade {
        @Override
        public boolean daRodada() {
            return true;
        }

        @Override
        public Optional<String> avaliar(Contexto c) {
            var teto = c.rodada().valorMaximo() != null ? c.rodada().valorMaximo() : c.rodada().valorAlvo();
            var minimo = c.perfil().ticketMinimo();
            return teto != null && minimo != null && minimo.compareTo(teto) <= 0
                    ? Optional.of("A rodada comporta o seu ticket") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Localização
     * Objetivo: Startup numa localização de interesse.
     * Decisões: Usa a localização mesmo se confidencial, sem mostrar qual é.
     */
    @Component
    @Order(5)
    static class Localizacao implements CriterioDeCompatibilidade {
        @Override
        public Optional<String> avaliar(Contexto c) {
            return c.localizacao() != null && c.perfil().localizacoes().contains(c.localizacao())
                    ? Optional.of("Fica em uma região do seu interesse") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Modelo de negócio
     * Objetivo: Modelo da startup entre os preferidos (preferências complementares, KAN-19).
     * Decisões: Sem preferências complementares não pontua.
     */
    @Component
    @Order(6)
    static class ModeloDeNegocio implements CriterioDeCompatibilidade {
        @Override
        public Optional<String> avaliar(Contexto c) {
            PerfilDeInvestimento.Complementares comp = c.perfil().complementares();
            return c.modelo() != null && comp != null && comp.modelos() != null && comp.modelos().contains(c.modelo())
                    ? Optional.of("Modelo " + c.modeloRotulo() + ", entre os seus preferidos") : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Instrumento
     * Objetivo: Instrumento da rodada entre os aceitos.
     * Decisões: Depende da rodada.
     */
    @Component
    @Order(7)
    static class Instrumento implements CriterioDeCompatibilidade {
        @Override
        public boolean daRodada() {
            return true;
        }

        @Override
        public Optional<String> avaliar(Contexto c) {
            String instrumento = c.rodada().instrumento();
            return instrumento != null && c.perfil().instrumentos().contains(instrumento)
                    ? Optional.of("Rodada por " + c.instrumentoRotulo().toLowerCase(java.util.Locale.ROOT) + ", instrumento que você aceita")
                    : Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Liderança
     * Objetivo: Rodada sem líder e investidor que lidera rodadas.
     * Decisões: Até o Match (S7), toda rodada aberta está sem líder.
     */
    @Component
    @Order(8)
    static class Lideranca implements CriterioDeCompatibilidade {
        @Override
        public boolean daRodada() {
            return true;
        }

        @Override
        public Optional<String> avaliar(Contexto c) {
            PerfilDeInvestimento.Lideranca l = c.perfil().lideranca();
            return c.semLider() && (l == PerfilDeInvestimento.Lideranca.LIDERA || l == PerfilDeInvestimento.Lideranca.AMBOS)
                    ? Optional.of("A rodada procura um investidor líder") : Optional.empty();
        }
    }
}
