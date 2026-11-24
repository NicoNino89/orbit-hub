package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Component;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Executor da alteração de parâmetro sensível
 * Objetivo: Aplicar a alteração de um parâmetro sensível depois que outro administrador aprova a proposta.
 * Decisões: Só administrador aprova. Os dados da proposta são "chave" e "valor"; o valor é validado na proposta e de
 *           novo na aplicação (os limites podem ter mudado no meio). O histórico registra quem propôs como autor e
 *           quem aprovou.
 */
@Component
class ExecutorDeParametroSensivel implements ExecutorDeAcaoGrave {

    static final String CHAVE = "chave";
    static final String VALOR = "valor";

    private final AplicacaoDeParametro aplicacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do executor
     * Objetivo: Receber o componente que aplica valores de parâmetro.
     * Decisões: Injeção por construtor.
     */
    ExecutorDeParametroSensivel(AplicacaoDeParametro aplicacao) {
        this.aplicacao = aplicacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Ação atendida
     * Objetivo: Atender a alteração de parâmetro sensível.
     * Decisões: Uma ação por executor.
     */
    @Override
    public AcaoGrave acao() {
        return AcaoGrave.PARAMETRO_SENSIVEL;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Quem aprova
     * Objetivo: Aceitar só administradores como segunda pessoa.
     * Decisões: P15: parâmetros são do administrador.
     */
    @Override
    public Set<Papel> papeisQueAprovam() {
        return Set.of(Papel.ADMIN);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Conferir a proposta
     * Objetivo: Recusar já na proposta um valor fora dos limites.
     * Decisões: Mesma validação da aplicação.
     */
    @Override
    public void conferir(Map<String, String> dados) {
        String chave = dados.get(CHAVE);
        if (chave == null) {
            throw new RegraDeNegocioException("proposta_invalida", "A proposta não informa o parâmetro.");
        }
        aplicacao.validar(chave, dados.get(VALOR));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Aplicar a alteração aprovada
     * Objetivo: Gravar o valor proposto e o histórico com o autor e o aprovador.
     * Decisões: A justificativa do histórico é a da proposta.
     */
    @Override
    public void executar(PropostaDeAprovacao proposta, UUID aprovador) {
        Map<String, String> dados = proposta.getDados();
        aplicacao.aplicar(dados.get(CHAVE), dados.get(VALOR), proposta.getPropostoPor(), proposta.getJustificativa(),
                aprovador, proposta.getId());
    }
}
