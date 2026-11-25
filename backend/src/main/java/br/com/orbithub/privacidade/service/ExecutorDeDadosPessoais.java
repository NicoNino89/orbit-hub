package br.com.orbithub.privacidade.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.service.ExecutorDeAcaoGrave;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 20:54
 * Cartão Jira: KAN-90, KAN-81
 * Título: Dados pessoais fora do fluxo com segunda aprovação
 * Objetivo: Executar a exclusão da conta proposta pelo encarregado quando outra pessoa aprova, ou devolver o pedido à
 *           análise quando a proposta é rejeitada ou cancelada.
 * Decisões: Aprovam administradores e encarregados (nunca quem propôs). No MVP só a exclusão usa esta ação; exportação
 *           fora do fluxo entra quando houver o caso de uso.
 */
@Component
class ExecutorDeDadosPessoais implements ExecutorDeAcaoGrave {

    private final ObjectProvider<PrivacidadeService> privacidade;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Criação
     * Objetivo: Receber a privacidade sem criar ciclo entre os serviços.
     * Decisões: ObjectProvider, resolvido só na execução.
     */
    ExecutorDeDadosPessoais(ObjectProvider<PrivacidadeService> privacidade) {
        this.privacidade = privacidade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Ação
     * Objetivo: Indicar a ação grave tratada.
     * Decisões: Dados pessoais fora do fluxo normal.
     */
    @Override
    public AcaoGrave acao() {
        return AcaoGrave.DADOS_PESSOAIS_FORA_DO_FLUXO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Quem aprova
     * Objetivo: Indicar os papéis que podem dar a segunda aprovação.
     * Decisões: Administrador ou encarregado de dados.
     */
    @Override
    public Set<Papel> papeisQueAprovam() {
        return Set.of(Papel.ADMIN, Papel.ENCARREGADO_DADOS);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Conferir
     * Objetivo: Recusar propostas sem a ação de exclusão.
     * Decisões: Única ação do MVP.
     */
    @Override
    public void conferir(Map<String, String> dados) {
        if (!"EXCLUSAO".equals(dados.get("acao"))) {
            throw new RegraDeNegocioException("acao_invalida", "Só a exclusão da conta está disponível fora do fluxo.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Executar
     * Objetivo: Excluir a conta e concluir o pedido.
     * Decisões: Na mesma transação da aprovação.
     */
    @Override
    public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
        privacidade.getObject().concluirExclusao(pedido(proposta), aprovador);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Ao desistir
     * Objetivo: Devolver o pedido à análise.
     * Decisões: Vale para rejeição e cancelamento.
     */
    @Override
    public void aoDesistir(PropostaDeAprovacao proposta) {
        privacidade.getObject().desistirDaExclusao(pedido(proposta));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-90
     * Título: Pedido da proposta
     * Objetivo: Ler o pedido do titular a que a proposta se refere.
     * Decisões: Proposta sem pedido válido é recusada.
     */
    private static UUID pedido(PropostaDeAprovacao proposta) {
        try {
            return UUID.fromString(proposta.getObjetoId());
        } catch (IllegalArgumentException e) {
            throw new RegraDeNegocioException("proposta_invalida", "A proposta não identifica o pedido do titular.");
        }
    }
}
