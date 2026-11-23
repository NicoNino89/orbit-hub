package br.com.orbithub.compartilhado.armazenamento;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 18/11/2026
 * Hora: 20:47
 * Cartão Jira: KAN-52
 * Título: Teste da validação de chaves de arquivo
 * Objetivo: Verificar a regra de chave válida, a primeira barreira contra fuga de pasta.
 * Decisões: Testes parametrizados com os ataques conhecidos de travessia de caminho.
 */
class ChaveArquivoTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Chaves válidas são aceitas
     * Objetivo: Garantir que chaves simples e com subpastas passem sem alteração.
     * Decisões: Exemplos tirados dos usos reais previstos (logotipo e Data Room).
     */
    @ParameterizedTest
    @ValueSource(strings = {"logo.png", "empreendimentos/42/logo.png", "data-room/7/v2_contrato.pdf"})
    void aceitaChavesValidas(String chave) {
        assertThat(ChaveArquivo.validar(chave)).isEqualTo(chave);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Chaves inválidas são recusadas
     * Objetivo: Garantir a recusa de chave nula, vazia, com "..", absoluta, com barra dupla ou invertida,
     *           com unidade do Windows ou com espaço.
     * Decisões: Cobre também "a/./b", que casa com o padrão de caracteres mas é recusado à parte.
     */
    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = {" ", "../segredo", "a/../../b", "/absoluto", "a//b", "a/", "a\\b", "c:/x", "nome com espaco", "a/./b"})
    void recusaChavesInvalidas(String chave) {
        assertThatThrownBy(() -> ChaveArquivo.validar(chave)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 18/11/2026
     * Hora: 20:47
     * Cartão Jira: KAN-52
     * Título: Chave longa demais é recusada
     * Objetivo: Garantir o limite de 512 caracteres.
     * Decisões: Testa exatamente o primeiro valor acima do limite (513).
     */
    @Test
    void recusaChaveLongaDemais() {
        assertThatThrownBy(() -> ChaveArquivo.validar("a".repeat(513)))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("512");
    }
}
