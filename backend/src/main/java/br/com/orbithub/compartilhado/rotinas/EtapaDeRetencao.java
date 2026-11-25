package br.com.orbithub.compartilhado.rotinas;

import java.time.Instant;
import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-93, KAN-107
 * Título: Etapa de retenção
 * Objetivo: Deixar cada módulo declarar um tipo de dado com prazo de guarda, que a rotina R12 aplica (exclusão ou
 *           anonimização).
 * Decisões: Strategy: a R12 roda todas as etapas e registra só a quantidade por tipo, nunca os dados.
 */
public interface EtapaDeRetencao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93
     * Título: Tipo de dado
     * Objetivo: Nomear o tipo de dado tratado, para o registro da execução.
     * Decisões: Texto curto em português.
     */
    String tipo();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93
     * Título: Parâmetros
     * Objetivo: Listar os parâmetros de prazo que a etapa usa.
     * Decisões: Aparecem na tela das rotinas.
     */
    List<String> parametros();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-93
     * Título: Aplicar
     * Objetivo: Excluir ou anonimizar o que passou do prazo e devolver a quantidade.
     * Decisões: Roda na transação da R12.
     */
    int aplicar(Instant agora);
}
