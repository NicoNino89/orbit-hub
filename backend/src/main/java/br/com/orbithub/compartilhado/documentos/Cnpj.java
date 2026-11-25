package br.com.orbithub.compartilhado.documentos;

import java.util.Optional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: CNPJ
 * Objetivo: Normalizar e conferir os dígitos verificadores de um CNPJ, e formatá-lo para exibição.
 * Decisões: Aceita com ou sem pontuação e guarda só os 14 dígitos. Sequências repetidas (00.000.000/0000-00 etc.)
 *           são recusadas, embora passem no cálculo. Não consulta a Receita: a conferência da situação cadastral é
 *           da verificação (KAN-62). O CNPJ alfanumérico (a partir de 2026) entra quando a Receita emitir o primeiro.
 */
public final class Cnpj {

    private static final int[] PESOS_1 = {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};
    private static final int[] PESOS_2 = {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2};

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Sem instâncias
     * Objetivo: Impedir a criação de objetos desta classe utilitária.
     * Decisões: Construtor privado.
     */
    private Cnpj() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Normalizar e validar
     * Objetivo: Devolver os 14 dígitos de um CNPJ válido, ou vazio se for inválido.
     * Decisões: Ignora pontos, barra, hífen e espaços.
     */
    public static Optional<String> validar(String texto) {
        if (texto == null) {
            return Optional.empty();
        }
        String digitos = texto.replaceAll("[\\s./-]", "");
        if (!digitos.matches("\\d{14}") || digitos.chars().distinct().count() == 1) {
            return Optional.empty();
        }
        int dv1 = digito(digitos, PESOS_1);
        int dv2 = digito(digitos, PESOS_2);
        boolean confere = dv1 == digitos.charAt(12) - '0' && dv2 == digitos.charAt(13) - '0';
        return confere ? Optional.of(digitos) : Optional.empty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Formatar
     * Objetivo: Exibir os 14 dígitos como 00.000.000/0000-00.
     * Decisões: Recebe o valor já normalizado.
     */
    public static String formatar(String digitos) {
        return digitos.substring(0, 2) + "." + digitos.substring(2, 5) + "." + digitos.substring(5, 8) + "/"
                + digitos.substring(8, 12) + "-" + digitos.substring(12);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Dígito verificador
     * Objetivo: Calcular um dígito verificador pelo módulo 11.
     * Decisões: Resto menor que 2 dá zero.
     */
    private static int digito(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
