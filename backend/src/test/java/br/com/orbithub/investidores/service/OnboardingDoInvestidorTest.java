package br.com.orbithub.investidores.service;

import static org.assertj.core.api.Assertions.assertThat;

import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import java.util.Arrays;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Testes do onboarding e da escala de maturidade
 * Objetivo: Conferir quais estados do onboarding liberam o catálogo e a ordem e os nomes da escala de maturidade.
 * Decisões: Sem banco.
 */
class OnboardingDoInvestidorTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Catálogo liberado
     * Objetivo: Concluído e pulado liberam o catálogo; pendente, não.
     * Decisões: Regra do cartão: concluir ou escolher pular.
     */
    @Test
    void liberaCatalogo() {
        assertThat(OnboardingDoInvestidor.Estado.CONCLUIDO.liberaCatalogo()).isTrue();
        assertThat(OnboardingDoInvestidor.Estado.PULADO.liberaCatalogo()).isTrue();
        assertThat(OnboardingDoInvestidor.Estado.PENDENTE.liberaCatalogo()).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Escala de maturidade
     * Objetivo: Conferir os 7 níveis, na ordem do cartão, com os nomes em português.
     * Decisões: A ordem importa para comparar níveis.
     */
    @Test
    void escalaDeMaturidade() {
        assertThat(Arrays.stream(MaturidadeDoNegocio.values()).map(MaturidadeDoNegocio::getRotulo))
                .containsExactly("Ideação", "Validação", "MVP", "Operação inicial", "Tração", "Crescimento", "Expansão");
    }
}
