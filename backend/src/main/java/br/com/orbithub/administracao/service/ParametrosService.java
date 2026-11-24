package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.AlteracaoDeParametro;
import br.com.orbithub.administracao.domain.Parametro;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.repository.ParametroHistoricoRepository;
import br.com.orbithub.administracao.repository.ParametroRepository;
import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Gestão dos parâmetros
 * Objetivo: Listar os parâmetros e o histórico e alterar valores com justificativa; parâmetro sensível vira
 *           proposta de segunda aprovação (KAN-81).
 * Decisões: Justificativa sempre obrigatória. O valor é validado antes de propor, para a segunda pessoa não perder
 *           tempo com algo que não poderia valer. A alteração vale daqui para frente: cada prazo já iniciado guarda a
 *           data de fim calculada quando começou.
 */
@Service
public class ParametrosService {

    static final String ACAO = "PARAMETRO_ALTERADO";
    static final String OBJETO = "parametro";

    private final ParametroRepository parametros;
    private final ParametroHistoricoRepository historico;
    private final AplicacaoDeParametro aplicacao;
    private final SegundaAprovacaoService aprovacoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, aplicação e segunda aprovação.
     * Decisões: Injeção por construtor.
     */
    public ParametrosService(ParametroRepository parametros, ParametroHistoricoRepository historico,
                             AplicacaoDeParametro aplicacao, SegundaAprovacaoService aprovacoes) {
        this.parametros = parametros;
        this.historico = historico;
        this.aplicacao = aplicacao;
        this.aprovacoes = aprovacoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Catálogo
     * Objetivo: Listar todos os parâmetros por grupo.
     * Decisões: Ordem estável.
     */
    @Transactional(readOnly = true)
    public List<Parametro> listar() {
        return parametros.findAllByOrderByGrupoAscChaveAsc();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Histórico de um parâmetro
     * Objetivo: Listar as alterações de um parâmetro.
     * Decisões: Parâmetro desconhecido: 404.
     */
    @Transactional(readOnly = true)
    public List<AlteracaoDeParametro> historico(String chave) {
        aplicacao.buscar(chave);
        return historico.findByChaveOrderByAlteradoEmDesc(chave);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Alterar parâmetro
     * Objetivo: Aplicar o valor novo ou, se o parâmetro for sensível, propô-lo para segunda aprovação.
     * Decisões: Devolve o que aconteceu (aplicado ou proposto). O resumo da proposta mostra o antes e o depois.
     */
    @Transactional
    public Resultado alterar(String chave, String valor, UUID autor, String justificativa) {
        ContextoDeAuditoria.descrever(ACAO, OBJETO, chave, "Pedido: " + valor);
        ContextoDeAuditoria.justificar(justificativa);
        if (justificativa == null || justificativa.isBlank()) {
            throw new RegraDeNegocioException("justificativa_obrigatoria", "Explique o motivo da alteração.");
        }
        Parametro parametro = aplicacao.buscar(chave);
        String normalizado = aplicacao.validar(chave, valor);
        if (normalizado.equals(parametro.getValor())) {
            throw new RegraDeNegocioException("valor_invalido", "O valor informado é igual ao atual.");
        }
        String mudanca = parametro.getValor() + " → " + normalizado;
        if (parametro.isSensivel()) {
            PropostaDeAprovacao proposta = aprovacoes.propor(AcaoGrave.PARAMETRO_SENSIVEL, OBJETO, chave,
                    chave + ": " + mudanca, Map.of(ExecutorDeParametroSensivel.CHAVE, chave,
                            ExecutorDeParametroSensivel.VALOR, normalizado), autor, justificativa);
            ContextoDeAuditoria.descrever(ACAO, OBJETO, chave, "Proposto para segunda aprovação: " + mudanca);
            return new Resultado(false, proposta.getId(), parametro);
        }
        aplicacao.aplicar(chave, normalizado, autor, justificativa, null, null);
        ContextoDeAuditoria.descrever(ACAO, OBJETO, chave, mudanca);
        return new Resultado(true, null, aplicacao.buscar(chave));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Resultado da alteração
     * Objetivo: Dizer se o valor já vale ou se foi para segunda aprovação.
     * Decisões: Com proposta, o parâmetro continua com o valor antigo.
     */
    public record Resultado(boolean aplicado, UUID propostaId, Parametro parametro) {
    }
}
