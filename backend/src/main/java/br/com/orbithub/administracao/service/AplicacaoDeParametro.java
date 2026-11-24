package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.AlteracaoDeParametro;
import br.com.orbithub.administracao.domain.Parametro;
import br.com.orbithub.administracao.repository.ParametroHistoricoRepository;
import br.com.orbithub.administracao.repository.ParametroRepository;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Aplicação de um valor de parâmetro
 * Objetivo: Validar e gravar o valor novo de um parâmetro e registrar a alteração no histórico, num lugar só para a
 *           alteração direta e para a aprovada por segunda pessoa.
 * Decisões: Depende só dos repositórios, para o executor de parâmetro sensível usá-la sem ciclo de dependências com
 *           o serviço de aprovação. Erro de validação vira RegraDeNegocioException "valor_invalido" com a mensagem
 *           da entidade.
 */
@Component
public class AplicacaoDeParametro {

    private final ParametroRepository parametros;
    private final ParametroHistoricoRepository historico;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do componente
     * Objetivo: Receber os repositórios e o relógio.
     * Decisões: Injeção por construtor.
     */
    public AplicacaoDeParametro(ParametroRepository parametros, ParametroHistoricoRepository historico, Clock relogio) {
        this.parametros = parametros;
        this.historico = historico;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Buscar parâmetro
     * Objetivo: Ler o parâmetro pela chave.
     * Decisões: Chave desconhecida: 404.
     */
    @Transactional(readOnly = true)
    public Parametro buscar(String chave) {
        return parametros.findById(chave)
                .orElseThrow(() -> new RecursoNaoEncontradoException("Parâmetro não encontrado: " + chave));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Validar valor
     * Objetivo: Conferir o valor e devolvê-lo normalizado, sem gravar.
     * Decisões: Usado antes de propor uma alteração sensível.
     */
    @Transactional(readOnly = true)
    public String validar(String chave, String valor) {
        try {
            return buscar(chave).validar(valor);
        } catch (IllegalArgumentException invalido) {
            throw new RegraDeNegocioException("valor_invalido", invalido.getMessage());
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Aplicar valor
     * Objetivo: Gravar o valor novo e registrar a alteração.
     * Decisões: Aprovador e proposta só quando veio de segunda aprovação.
     */
    @Transactional
    public AlteracaoDeParametro aplicar(String chave, String valor, UUID autor, String justificativa, UUID aprovadoPor,
                                        UUID propostaId) {
        Parametro parametro = buscar(chave);
        String anterior;
        try {
            anterior = parametro.aplicar(valor, autor, relogio.instant());
        } catch (IllegalArgumentException invalido) {
            throw new RegraDeNegocioException("valor_invalido", invalido.getMessage());
        }
        return historico.save(AlteracaoDeParametro.registrar(chave, anterior, parametro.getValor(), autor, aprovadoPor,
                propostaId, justificativa, relogio.instant()));
    }
}
