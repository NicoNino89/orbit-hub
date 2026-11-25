package br.com.orbithub.compartilhado.documentos;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Testes do CNPJ
 * Objetivo: Conferir a normalização, os dígitos verificadores, as sequências repetidas e a formatação.
 * Decisões: CNPJs de exemplo com dígitos conhecidos.
 */
class CnpjTest {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Válidos e inválidos
     * Objetivo: Aceitar com e sem pontuação; recusar dígito errado, tamanho errado, letras, repetidos e nulo.
     * Decisões: Devolve só os dígitos.
     */
    @Test
    void validaDigitos() {
        assertThat(Cnpj.validar("11.222.333/0001-81")).contains("11222333000181");
        assertThat(Cnpj.validar(" 11444777000161 ")).contains("11444777000161");
        assertThat(Cnpj.validar("11.222.333/0001-82")).isEmpty();
        assertThat(Cnpj.validar("1122233300018")).isEmpty();
        assertThat(Cnpj.validar("11a22333000181")).isEmpty();
        assertThat(Cnpj.validar("00000000000000")).isEmpty();
        assertThat(Cnpj.validar("11111111111111")).isEmpty();
        assertThat(Cnpj.validar(null)).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Formatação
     * Objetivo: Exibir no formato 00.000.000/0000-00.
     * Decisões: A partir dos 14 dígitos.
     */
    @Test
    void formata() {
        assertThat(Cnpj.formatar("11222333000181")).isEqualTo("11.222.333/0001-81");
    }
}
