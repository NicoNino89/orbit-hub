package br.com.orbithub.administracao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.orbithub.administracao.domain.Parametro;
import br.com.orbithub.administracao.domain.Parametro.Tipo;
import br.com.orbithub.administracao.domain.Parametro.Unidade;
import br.com.orbithub.administracao.repository.ParametroRepository;
import java.time.Duration;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Testes da leitura de parâmetros
 * Objetivo: Garantir a leitura por tipo (inteiro, duração, lista, opção) e as recusas de chave inexistente e de
 *           tipo errado.
 * Decisões: Repositório simulado com três parâmetros do catálogo.
 */
class ParametrosJpaTest {

    ParametrosJpa leitura;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Preparação
     * Objetivo: Montar a leitura com um inteiro, uma lista e uma opção.
     * Decisões: Valores padrão do catálogo.
     */
    @BeforeEach
    void preparar() {
        ParametroRepository repositorio = mock(ParametroRepository.class);
        when(repositorio.findById("conexao.expiracao_dias")).thenReturn(Optional.of(Parametro.novo(
                "conexao.expiracao_dias", "conexao", "d", Tipo.INTEIRO, Unidade.DIAS, "30", 7L, 90L, null, false)));
        when(repositorio.findById("conexao.lembrete_dias")).thenReturn(Optional.of(Parametro.novo(
                "conexao.lembrete_dias", "conexao", "d", Tipo.LISTA_INTEIROS, Unidade.DIAS, "7,21", 1L, 60L, null, false)));
        when(repositorio.findById("catalogo.frequencia_edicoes")).thenReturn(Optional.of(Parametro.novo(
                "catalogo.frequencia_edicoes", "catalogo", "d", Tipo.OPCAO, Unidade.TEXTO, "MENSAL", null, null,
                "SEMANAL,MENSAL", false)));
        when(repositorio.findById("inexistente")).thenReturn(Optional.empty());
        leitura = new ParametrosJpa(repositorio);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Leitura por tipo
     * Objetivo: Conferir inteiro, duração, lista e opção.
     * Decisões: Lista em ordem crescente.
     */
    @Test
    void leCadaTipo() {
        assertThat(leitura.inteiro("conexao.expiracao_dias")).isEqualTo(30);
        assertThat(leitura.duracao("conexao.expiracao_dias")).isEqualTo(Duration.ofDays(30));
        assertThat(leitura.listaDeInteiros("conexao.lembrete_dias")).containsExactly(7L, 21L);
        assertThat(leitura.opcao("catalogo.frequencia_edicoes")).isEqualTo("MENSAL");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Erros de programação
     * Objetivo: Conferir que chave inexistente e tipo errado lançam IllegalStateException.
     * Decisões: São erros do código, não do usuário.
     */
    @Test
    void recusaChaveInexistenteETipoErrado() {
        assertThatThrownBy(() -> leitura.inteiro("inexistente")).isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("inexistente");
        assertThatThrownBy(() -> leitura.inteiro("conexao.lembrete_dias")).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> leitura.opcao("conexao.expiracao_dias")).isInstanceOf(IllegalStateException.class);
    }
}
