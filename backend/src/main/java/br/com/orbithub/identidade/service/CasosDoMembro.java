package br.com.orbithub.identidade.service;

import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:15
 * Cartão Jira: KAN-75
 * Título: Casos abertos de um membro da equipe
 * Objetivo: Contrato que cada fila (verificação, curadoria, moderação, atendimento, privacidade) implementa para
 *           devolver à fila os casos de quem foi desligado, para serem redistribuídos.
 * Decisões: Padrão Strategy, uma implementação por fila, registrada pelo módulo dono da fila quando ela existir
 *           (S3 em diante). O desligamento chama todas e soma o que foi devolvido.
 */
public interface CasosDoMembro {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:15
     * Cartão Jira: KAN-75
     * Título: Nome da fila
     * Objetivo: Identificar a fila nos registros (por exemplo, "curadoria").
     * Decisões: Minúsculas, sem espaço.
     */
    String fila();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:15
     * Cartão Jira: KAN-75
     * Título: Devolver os casos
     * Objetivo: Tirar do membro os casos abertos e devolvê-los à fila.
     * Decisões: Devolve quantos casos voltaram; roda na transação do desligamento.
     */
    int devolverParaFila(UUID membroId);
}
