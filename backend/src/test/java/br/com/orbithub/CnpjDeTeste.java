package br.com.orbithub;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: CNPJ de teste
 * Objetivo: Gerar CNPJs válidos e diferentes para os testes de qualquer módulo.
 * Decisões: Oito dígitos sequenciais, filial 0001 e os dígitos verificadores calculados.
 */
public final class CnpjDeTeste {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger((int) (System.nanoTime() % 50_000_000L) + 10_000_000);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Sem instâncias
     * Objetivo: Impedir a criação de objetos desta classe utilitária.
     * Decisões: Construtor privado.
     */
    private CnpjDeTeste() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Novo CNPJ
     * Objetivo: Devolver um CNPJ válido ainda não usado nesta execução.
     * Decisões: Só dígitos.
     */
    public static String novo() {
        String base = String.format("%08d", SEQUENCIA.incrementAndGet() % 100_000_000) + "0001";
        String com1 = base + dv(base, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return com1 + dv(com1, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Dígito verificador
     * Objetivo: Calcular um dígito do CNPJ.
     * Decisões: Módulo 11.
     */
    private static int dv(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }
}
