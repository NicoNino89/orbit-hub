package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.identidade.domain.Papel;
import java.util.Map;
import java.util.Set;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Executor de uma ação grave
 * Objetivo: Contrato que o módulo dono de cada ação grave implementa: quem pode aprovar, como conferir os dados
 *           na proposta e como aplicar a ação depois da aprovação.
 * Decisões: Padrão Command/Strategy: a proposta guarda os dados, e o executor só roda quando a segunda pessoa
 *           aprova. Ação sem executor registrado não pode ser proposta (a funcionalidade ainda não existe).
 */
public interface ExecutorDeAcaoGrave {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Ação atendida
     * Objetivo: Dizer qual ação grave este executor aplica.
     * Decisões: Um executor por ação.
     */
    AcaoGrave acao();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Quem pode aprovar
     * Objetivo: Dizer quais papéis da equipe podem aprovar ou rejeitar esta ação.
     * Decisões: Por exemplo, parâmetro sensível: administrador; dados pessoais: encarregado de dados.
     */
    Set<Papel> papeisQueAprovam();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Conferir a proposta
     * Objetivo: Recusar já na proposta os dados que a ação não aceitaria.
     * Decisões: Padrão sem conferência; o executor lança RegraDeNegocioException quando os dados não valem.
     */
    default void conferir(Map<String, String> dados) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Aplicar a ação
     * Objetivo: Executar a ação aprovada, com os dados guardados na proposta.
     * Decisões: Roda na mesma transação da aprovação: se falhar, a proposta continua pendente e nada muda.
     */
    void executar(PropostaDeAprovacao proposta);
}
