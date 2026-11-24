package br.com.orbithub.compartilhado.rotinas;

import java.time.Instant;
import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Rotina agendada
 * Objetivo: Contrato que cada módulo implementa para uma regra baseada em tempo (R1 a R16, seção 4 de
 *           processos-e-operacao.md).
 * Decisões: A rotina só descreve a si mesma e faz o trabalho; agendamento, trava entre instâncias, registro, pausa e
 *           alertas ficam no {@link ExecutorDeRotinas}, iguais para todas. Toda rotina precisa ser idempotente: rodar
 *           duas vezes no mesmo ciclo não pode repetir efeito (por exemplo, marca o que já tratou). Os prazos vêm dos
 *           parâmetros (KAN-78), nunca de valores fixos.
 */
public interface RotinaAgendada {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Identificador
     * Objetivo: Devolver o código da rotina (R1 a R16).
     * Decisões: Igual ao da seção 4 de processos-e-operacao.md.
     */
    String codigo();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Descrição
     * Objetivo: Dizer o que a rotina faz, para o painel.
     * Decisões: Frase curta.
     */
    String descricao();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Frequência
     * Objetivo: Devolver a expressão cron (segundo minuto hora dia mês dia-da-semana), no horário de Brasília.
     * Decisões: Cron do Spring.
     */
    String cron();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Módulo dono
     * Objetivo: Dizer qual módulo é responsável pela rotina.
     * Decisões: Nome do pacote de topo.
     */
    String modulo();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Parâmetros usados
     * Objetivo: Listar as chaves de parâmetro que a rotina lê.
     * Decisões: Mostradas no painel.
     */
    List<String> parametros();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Executar
     * Objetivo: Fazer o trabalho do ciclo e dizer quantos itens tratou e quantos deram erro.
     * Decisões: Erro de um item não deve parar os outros: a rotina conta e segue. Exceção não tratada vira falha.
     */
    Resultado executar(Instant agora);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Resultado de um ciclo
     * Objetivo: Levar itens tratados, itens com erro e uma mensagem curta.
     * Decisões: Com erros, a execução conta como falha e gera alerta.
     */
    record Resultado(int itens, int erros, String mensagem) {
    }
}
