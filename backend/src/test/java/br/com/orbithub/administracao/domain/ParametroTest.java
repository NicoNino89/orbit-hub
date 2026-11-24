package br.com.orbithub.administracao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.administracao.domain.Parametro.Tipo;
import br.com.orbithub.administracao.domain.Parametro.Unidade;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Testes do parâmetro
 * Objetivo: Garantir que valores fora dos limites são recusados, que listas e opções são validadas e normalizadas e
 *           a conversão para duração.
 * Decisões: Parâmetros montados em memória com os limites do catálogo.
 */
class ParametroTest {

    static final Instant AGORA = Instant.parse("2026-11-24T14:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Inteiro com limites
     * Objetivo: Conferir que o inteiro aceita a faixa e recusa fora dela ou texto.
     * Decisões: A mensagem traz a faixa e a unidade.
     */
    @Test
    void inteiroRespeitaOsLimites() {
        Parametro p = Parametro.novo("conexao.expiracao_dias", "conexao", "d", Tipo.INTEIRO, Unidade.DIAS, "30", 7L, 90L,
                null, false);
        assertThat(p.validar(" 45 ")).isEqualTo("45");
        assertThat(p.validar("7")).isEqualTo("7");
        assertThatThrownBy(() -> p.validar("6")).hasMessage("Valor fora dos limites aceitos: de 7 a 90 dias.");
        assertThatThrownBy(() -> p.validar("91")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.validar("trinta")).hasMessage("Informe um número inteiro.");
        assertThatThrownBy(() -> p.validar(null)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Lista e opção
     * Objetivo: Conferir que a lista sai ordenada e sem repetição e que a opção precisa estar entre as aceitas.
     * Decisões: Lista vazia é recusada.
     */
    @Test
    void validaListasEOpcoes() {
        Parametro lista = Parametro.novo("conexao.lembrete_dias", "conexao", "d", Tipo.LISTA_INTEIROS, Unidade.DIAS,
                "7,21", 1L, 60L, null, false);
        assertThat(lista.validar("21, 7, 7, 14")).isEqualTo("7,14,21");
        assertThatThrownBy(() -> lista.validar(" , ")).hasMessage("Informe pelo menos um valor.");
        assertThatThrownBy(() -> lista.validar("7,61")).isInstanceOf(IllegalArgumentException.class);

        Parametro opcao = Parametro.novo("catalogo.frequencia_edicoes", "catalogo", "d", Tipo.OPCAO, Unidade.TEXTO,
                "MENSAL", null, null, "SEMANAL,QUINZENAL,MENSAL", false);
        assertThat(opcao.opcoesAceitas()).containsExactly("SEMANAL", "QUINZENAL", "MENSAL");
        assertThat(opcao.validar("QUINZENAL")).isEqualTo("QUINZENAL");
        assertThatThrownBy(() -> opcao.validar("ANUAL"))
                .hasMessage("Escolha uma das opções: SEMANAL, QUINZENAL, MENSAL.");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Aplicar e converter
     * Objetivo: Conferir que aplicar devolve o anterior, recusa valor igual e que a duração segue a unidade.
     * Decisões: Meses não viram Duration.
     */
    @Test
    void aplicaEConverteParaDuracao() {
        Parametro p = Parametro.novo("sessao.inatividade_minutos", "seguranca", "d", Tipo.INTEIRO, Unidade.MINUTOS, "30",
                5L, 1440L, null, true);
        assertThat(p.duracao()).isEqualTo(Duration.ofMinutes(30));
        UUID bia = UUID.randomUUID();
        assertThat(p.aplicar("45", bia, AGORA)).isEqualTo("30");
        assertThat(p.getValor()).isEqualTo("45");
        assertThat(p.getAtualizadoEm()).isEqualTo(AGORA);
        assertThat(p.duracao()).isEqualTo(Duration.ofMinutes(45));
        assertThatThrownBy(() -> p.aplicar("45", bia, AGORA)).hasMessage("O valor informado é igual ao atual.");

        assertThat(Parametro.novo("a", "g", "d", Tipo.INTEIRO, Unidade.HORAS, "2", 1L, 9L, null, false).duracao())
                .isEqualTo(Duration.ofHours(2));
        assertThat(Parametro.novo("b", "g", "d", Tipo.INTEIRO, Unidade.DIAS, "3", 1L, 9L, null, false).duracao())
                .isEqualTo(Duration.ofDays(3));
        assertThatThrownBy(() -> Parametro.novo("c", "g", "d", Tipo.INTEIRO, Unidade.MESES, "3", 1L, 9L, null, false)
                .duracao()).isInstanceOf(IllegalStateException.class);
        assertThat(Unidade.DIAS_UTEIS.getTexto()).isEqualTo("dias úteis");
    }
}
