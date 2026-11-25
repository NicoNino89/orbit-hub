package br.com.orbithub.empreendimentos.service;

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
 * Hora: 10:40
 * Cartão Jira: KAN-84
 * Título: Executor da recusa de startup por fraude
 * Objetivo: Concluir a recusa por fraude depois que uma segunda pessoa aprova a proposta do curador, ou devolver a revisão
 *           à análise quando a proposta é rejeitada ou cancelada.
 * Decisões: Aprovam administradores e curadores (a segunda aprovação já impede quem propôs). A curadoria é lida pelo
 *           provedor para não fechar ciclo entre a curadoria e a segunda aprovação.
 */
@Component
class ExecutorDeRecusaPorFraude implements ExecutorDeAcaoGrave {

    private final ObjectProvider<CuradoriaService> curadoria;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Criação
     * Objetivo: Receber o provedor da curadoria.
     * Decisões: Resolvido só na execução.
     */
    ExecutorDeRecusaPorFraude(ObjectProvider<CuradoriaService> curadoria) {
        this.curadoria = curadoria;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Ação
     * Objetivo: Dizer qual ação grave este executor trata.
     * Decisões: Remoção de startup por fraude.
     */
    @Override
    public AcaoGrave acao() {
        return AcaoGrave.REMOCAO_STARTUP_FRAUDE;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Quem aprova
     * Objetivo: Dizer quem pode dar a segunda aprovação.
     * Decisões: Administrador ou curador.
     */
    @Override
    public Set<Papel> papeisQueAprovam() {
        return Set.of(Papel.ADMIN, Papel.CURADOR);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Executar
     * Objetivo: Recusar a startup e fechar a revisão.
     * Decisões: O objeto da proposta é a startup.
     */
    @Override
    public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
        curadoria.getObject().concluirRecusa(startup(proposta), proposta.getId(), aprovador, true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Ao desistir
     * Objetivo: Devolver a revisão à análise quando a recusa não é aprovada.
     * Decisões: A startup não muda.
     */
    @Override
    public void aoDesistir(PropostaDeAprovacao proposta) {
        curadoria.getObject().concluirRecusa(startup(proposta), proposta.getId(), null, false);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Startup da proposta
     * Objetivo: Ler o identificador da startup na proposta.
     * Decisões: Proposta malformada é recusada.
     */
    private static UUID startup(PropostaDeAprovacao proposta) {
        try {
            return UUID.fromString(proposta.getObjetoId());
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("proposta_invalida", "A proposta não identifica a startup.");
        }
    }
}
