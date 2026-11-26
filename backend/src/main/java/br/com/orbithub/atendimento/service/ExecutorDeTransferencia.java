package br.com.orbithub.atendimento.service;

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
 * Cartão Jira: KAN-77, KAN-81
 * Título: Transferência de titularidade com segunda aprovação
 * Objetivo: Aplicar a transferência proposta pelo atendimento quando outra pessoa aprova.
 * Decisões: Aprovam administradores e o atendimento N2 (nunca quem propôs).
 */
@Component("executorDeTransferencia")
class ExecutorDeTransferencia implements ExecutorDeAcaoGrave {

    private final ObjectProvider<TitularidadeService> titularidade;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Criação
     * Objetivo: Receber o serviço sem criar ciclo.
     * Decisões: ObjectProvider.
     */
    ExecutorDeTransferencia(ObjectProvider<TitularidadeService> titularidade) {
        this.titularidade = titularidade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Ação
     * Objetivo: Indicar a ação grave tratada.
     * Decisões: Transferência de titularidade.
     */
    @Override
    public AcaoGrave acao() {
        return AcaoGrave.TRANSFERENCIA_TITULARIDADE;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Quem aprova
     * Objetivo: Indicar os papéis da segunda aprovação.
     * Decisões: Administrador ou suporte N2.
     */
    @Override
    public Set<Papel> papeisQueAprovam() {
        return Set.of(Papel.ADMIN, Papel.SUPORTE_N2);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Conferir
     * Objetivo: Recusar propostas sem alvo ou novo titular.
     * Decisões: Confere já na proposta.
     */
    @Override
    public void conferir(Map<String, String> dados) {
        try {
            TitularidadeService.Alvo.valueOf(dados.get("alvo"));
            UUID.fromString(dados.get("novoTitular"));
        } catch (RuntimeException e) {
            throw new RegraDeNegocioException("proposta_invalida", "A proposta precisa do alvo e do novo titular.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 20:54
     * Cartão Jira: KAN-77
     * Título: Executar
     * Objetivo: Aplicar a transferência.
     * Decisões: Na transação da aprovação.
     */
    @Override
    public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
        titularidade.getObject().concluir(proposta);
    }
}
