package br.com.orbithub.administracao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:06
 * Cartão Jira: KAN-79
 * Título: Testes do item de lista
 * Objetivo: Garantir o código gerado do rótulo, que renomear não muda o código, a desativação e as recusas.
 * Decisões: Teste de unidade da entidade.
 */
class ItemDeListaTest {

    static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:06
     * Cartão Jira: KAN-79
     * Título: Código estável
     * Objetivo: Conferir o código gerado do rótulo e que renomear mantém o código.
     * Decisões: Sem acentos, maiúsculas e sublinhado.
     */
    @Test
    void geraCodigoEMantemAoRenomear() {
        ItemDeLista item = ItemDeLista.novo(Lista.SEGMENTO, null, " Saúde (healthtech) ", null, 3, AGORA);
        assertThat(item.getCodigo()).isEqualTo("SAUDE_HEALTHTECH");
        assertThat(item.getRotulo()).isEqualTo("Saúde (healthtech)");
        assertThat(item.isAtivo()).isTrue();
        assertThat(item.renomear("Saúde e bem-estar", AGORA)).isEqualTo("Saúde (healthtech)");
        assertThat(item.getCodigo()).isEqualTo("SAUDE_HEALTHTECH");
        assertThatThrownBy(() -> item.renomear("Saúde e bem-estar", AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThat(ItemDeLista.codigoDe("pré-seed")).isEqualTo("PRE_SEED");
        assertThat(ItemDeLista.novo(Lista.SEGMENTO, "meu código", "Rótulo", null, 1, AGORA).getCodigo())
                .isEqualTo("MEU_CODIGO");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:06
     * Cartão Jira: KAN-79
     * Título: Recusas e desativação
     * Objetivo: Conferir rótulo vazio, grupo obrigatório e a troca de situação.
     * Decisões: Desativar duas vezes é recusado.
     */
    @Test
    void recusaInvalidosEDesativa() {
        assertThatThrownBy(() -> ItemDeLista.novo(Lista.SEGMENTO, null, " ", null, 1, AGORA))
                .hasMessage("Informe o rótulo.");
        assertThatThrownBy(() -> ItemDeLista.novo(Lista.SEGMENTO, null, "!!!", null, 1, AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> ItemDeLista.novo(Lista.DOCUMENTO_VERIFICACAO, null, "Selfie", null, 1, AGORA))
                .hasMessage("Informe o grupo do item.");
        ItemDeLista doc = ItemDeLista.novo(Lista.DOCUMENTO_VERIFICACAO, null, "Selfie", "investidor pessoa física", 1, AGORA);
        assertThat(doc.getGrupo()).isEqualTo("INVESTIDOR_PESSOA_FISICA");

        doc.definirAtivo(false, AGORA);
        assertThat(doc.isAtivo()).isFalse();
        assertThatThrownBy(() -> doc.definirAtivo(false, AGORA)).hasMessage("O item já está desativado.");
        doc.definirAtivo(true, AGORA);
        assertThat(doc.isAtivo()).isTrue();
        assertThat(Lista.LOCALIZACAO.isComGrupo()).isTrue();
        assertThat(Lista.SEGMENTO.getTitulo()).isEqualTo("Segmentos econômicos");
    }
}
