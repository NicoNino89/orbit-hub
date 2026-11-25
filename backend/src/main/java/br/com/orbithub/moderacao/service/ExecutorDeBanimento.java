package br.com.orbithub.moderacao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.service.ExecutorDeAcaoGrave;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 16:19
 * Cartão Jira: KAN-101, KAN-81
 * Título: Banimento com segunda aprovação
 * Objetivo: Aplicar o banimento proposto pelo moderador quando outra pessoa aprova, ou devolver o caso para análise
 *           quando a proposta é rejeitada ou cancelada.
 * Decisões: Aprovam administradores e moderadores (nunca quem propôs, regra da segunda aprovação).
 */
@Component
class ExecutorDeBanimento implements ExecutorDeAcaoGrave {

    private final ObjectProvider<ModeracaoService> moderacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Criação
     * Objetivo: Receber a moderação sem criar ciclo entre os serviços.
     * Decisões: ObjectProvider, resolvido só na execução.
     */
    ExecutorDeBanimento(ObjectProvider<ModeracaoService> moderacao) {
        this.moderacao = moderacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Ação
     * Objetivo: Indicar a ação grave tratada.
     * Decisões: Banimento definitivo.
     */
    @Override
    public AcaoGrave acao() {
        return AcaoGrave.BANIMENTO_DEFINITIVO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Quem aprova
     * Objetivo: Indicar os papéis que podem dar a segunda aprovação.
     * Decisões: Administrador ou moderador.
     */
    @Override
    public Set<Papel> papeisQueAprovam() {
        return Set.of(Papel.ADMIN, Papel.MODERADOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Executar
     * Objetivo: Aplicar o banimento e fechar o caso.
     * Decisões: Na mesma transação da aprovação.
     */
    @Override
    public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
        moderacao.getObject().concluirBanimento(caso(proposta), proposta.getId(), aprovador);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Ao desistir
     * Objetivo: Devolver o caso para análise.
     * Decisões: Vale para rejeição e cancelamento.
     */
    @Override
    public void aoDesistir(PropostaDeAprovacao proposta) {
        moderacao.getObject().desistirDoBanimento(caso(proposta), proposta.getId());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 16:19
     * Cartão Jira: KAN-101
     * Título: Caso da proposta
     * Objetivo: Ler o caso de moderação a que a proposta se refere.
     * Decisões: Proposta sem caso válido é recusada.
     */
    private static UUID caso(PropostaDeAprovacao proposta) {
        try {
            return UUID.fromString(proposta.getObjetoId());
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("proposta_invalida", "A proposta não identifica o caso de moderação.");
        }
    }
}
