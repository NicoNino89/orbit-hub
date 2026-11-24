package br.com.orbithub.compartilhado.parametros;

import java.time.Duration;
import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Leitura dos parâmetros de negócio
 * Objetivo: Dar a qualquer módulo o valor atual de um prazo ou limite, para nenhuma regra de negócio ter valor
 *           fixo no código (princípio 1 de processos-e-operacao.md).
 * Decisões: Interface em compartilhado, para os módulos não dependerem do módulo de administração; a implementação
 *           lê o banco a cada chamada (a alteração vale na hora, inclusive com várias instâncias da API). Chave
 *           inexistente ou de tipo errado é erro de programação e lança IllegalStateException. Os prazos já
 *           iniciados não usam esta leitura: cada um guarda a sua data de fim quando começa ("vale daqui para
 *           frente", KAN-78).
 */
public interface LeituraDeParametros {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor inteiro
     * Objetivo: Devolver o valor de um parâmetro inteiro (quantidade, percentual, dias, meses...).
     * Decisões: A unidade fica a cargo de quem chama.
     */
    long inteiro(String chave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor como duração
     * Objetivo: Devolver um prazo em minutos, horas ou dias como Duration.
     * Decisões: Meses e dias ou horas úteis não viram Duration (dependem de calendário): use {@link #inteiro}.
     */
    Duration duracao(String chave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Lista de inteiros
     * Objetivo: Devolver um parâmetro com vários valores (por exemplo, lembretes aos 7 e 21 dias).
     * Decisões: Em ordem crescente.
     */
    List<Long> listaDeInteiros(String chave);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Opção escolhida
     * Objetivo: Devolver o valor de um parâmetro de opção (por exemplo, MENSAL).
     * Decisões: Sempre uma das opções aceitas.
     */
    String opcao(String chave);
}
