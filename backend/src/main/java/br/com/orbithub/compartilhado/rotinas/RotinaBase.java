package br.com.orbithub.compartilhado.rotinas;

import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Base das rotinas
 * Objetivo: Guardar a descrição de uma rotina (código, descrição, frequência, módulo e parâmetros), para cada rotina
 *           concreta só escrever o trabalho.
 * Decisões: Classe abstrata com os dados no construtor, em vez de cinco métodos repetidos em cada rotina.
 */
public abstract class RotinaBase implements RotinaAgendada {

    private final String codigo;
    private final String descricao;
    private final String cron;
    private final String modulo;
    private final List<String> parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Criação da rotina
     * Objetivo: Guardar a descrição da rotina.
     * Decisões: Lista de parâmetros copiada e imutável.
     */
    protected RotinaBase(String codigo, String descricao, String cron, String modulo, List<String> parametros) {
        this.codigo = codigo;
        this.descricao = descricao;
        this.cron = cron;
        this.modulo = modulo;
        this.parametros = List.copyOf(parametros);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Código
     * Objetivo: Devolver o identificador da rotina.
     * Decisões: R1 a R16.
     */
    @Override
    public String codigo() {
        return codigo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Descrição
     * Objetivo: Devolver o que a rotina faz.
     * Decisões: Para o painel.
     */
    @Override
    public String descricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Frequência
     * Objetivo: Devolver o cron da rotina.
     * Decisões: Horário de Brasília.
     */
    @Override
    public String cron() {
        return cron;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Módulo dono
     * Objetivo: Devolver o módulo responsável.
     * Decisões: Nome do pacote de topo.
     */
    @Override
    public String modulo() {
        return modulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Parâmetros usados
     * Objetivo: Devolver as chaves de parâmetro lidas pela rotina.
     * Decisões: Imutável.
     */
    @Override
    public List<String> parametros() {
        return parametros;
    }
}
