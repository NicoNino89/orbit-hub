package br.com.orbithub.empreendimentos.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28
 * Título: Testes da visibilidade da startup
 * Objetivo: Conferir os padrões (público e confidencial), a escolha da equipe e os itens fixos.
 * Decisões: Sem banco.
 */
class VisibilidadeDaStartupTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Padrões e escolhas
     * Objetivo: Problema público e faturamento confidencial por padrão; a escolha vence o padrão; estágio sempre público.
     * Decisões: Código desconhecido nunca é público.
     */
    @Test
    void padroesEEscolhas() {
        assertThat(VisibilidadeDaStartup.publico("PROBLEMA", Map.of())).isTrue();
        assertThat(VisibilidadeDaStartup.publico("FONTES_FATURAMENTO", Map.of())).isFalse();
        assertThat(VisibilidadeDaStartup.publico("LOCALIZACAO", Map.of())).isFalse();
        assertThat(VisibilidadeDaStartup.publico("DADOS_DA_RODADA", Map.of())).isFalse();
        assertThat(VisibilidadeDaStartup.publico("FUNDADORES", Map.of())).isTrue();
        assertThat(VisibilidadeDaStartup.publico("PROBLEMA", Map.of("PROBLEMA", false))).isFalse();
        assertThat(VisibilidadeDaStartup.publico("ESTAGIO", Map.of("ESTAGIO", false))).isTrue();
        assertThat(VisibilidadeDaStartup.publico("INVENTADO", Map.of("INVENTADO", true))).isFalse();
        assertThat(VisibilidadeDaStartup.itens()).extracting(VisibilidadeDaStartup.Item::codigo).doesNotContain("ESTAGIO")
                .startsWith("LOCALIZACAO");
        assertThat(VisibilidadeDaStartup.item("INDICADORES")).get().extracting(VisibilidadeDaStartup.Item::secao)
                .isEqualTo(SecaoDaStartup.TRACAO);
    }
}
