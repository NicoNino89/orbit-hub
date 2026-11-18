package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

class ChaveArquivoTest {

    @ParameterizedTest
    @ValueSource(strings = {"logo.png", "empreendimentos/42/logo.png", "data-room/7/v2_contrato.pdf"})
    void aceitaChavesValidas(String chave) {
        assertThat(ChaveArquivo.validar(chave)).isEqualTo(chave);
    }

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "../segredo", "a/../../b", "/absoluto", "a//b", "a/", "a\\b", "c:/x", "nome com espaco", "a/./b"})
    void recusaChavesInvalidas(String chave) {
        assertThatThrownBy(() -> ChaveArquivo.validar(chave)).isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void recusaChaveLongaDemais() {
        assertThatThrownBy(() -> ChaveArquivo.validar("a".repeat(513)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("512");
    }
}
