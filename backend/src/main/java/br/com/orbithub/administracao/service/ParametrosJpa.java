package br.com.orbithub.administracao.service;

import br.com.orbithub.administracao.domain.Parametro;
import br.com.orbithub.administracao.repository.ParametroRepository;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import java.time.Duration;
import java.util.Arrays;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Leitura dos parâmetros no banco
 * Objetivo: Implementar a leitura dos parâmetros de negócio para todos os módulos.
 * Decisões: Lê o banco a cada chamada (uma consulta por chave, pela chave primária): a alteração vale na hora em
 *           todas as instâncias, sem cache para invalidar. Depende só do repositório, para qualquer serviço poder
 *           usá-la sem ciclo de dependências.
 */
@Service
public class ParametrosJpa implements LeituraDeParametros {

    private final ParametroRepository parametros;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação da leitura
     * Objetivo: Receber o repositório.
     * Decisões: Injeção por construtor.
     */
    public ParametrosJpa(ParametroRepository parametros) {
        this.parametros = parametros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor inteiro
     * Objetivo: Ler um parâmetro inteiro.
     * Decisões: Recusa parâmetro de outro tipo.
     */
    @Override
    @Transactional(readOnly = true)
    public long inteiro(String chave) {
        return Long.parseLong(doTipo(chave, Parametro.Tipo.INTEIRO).getValor());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor como duração
     * Objetivo: Ler um prazo em minutos, horas ou dias.
     * Decisões: A conversão fica na entidade.
     */
    @Override
    @Transactional(readOnly = true)
    public Duration duracao(String chave) {
        return doTipo(chave, Parametro.Tipo.INTEIRO).duracao();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Lista de inteiros
     * Objetivo: Ler um parâmetro com vários valores.
     * Decisões: O valor já está normalizado em ordem crescente.
     */
    @Override
    @Transactional(readOnly = true)
    public List<Long> listaDeInteiros(String chave) {
        return Arrays.stream(doTipo(chave, Parametro.Tipo.LISTA_INTEIROS).getValor().split(","))
                .map(Long::parseLong).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Opção escolhida
     * Objetivo: Ler um parâmetro de opção.
     * Decisões: Valor já validado contra as opções.
     */
    @Override
    @Transactional(readOnly = true)
    public String opcao(String chave) {
        return doTipo(chave, Parametro.Tipo.OPCAO).getValor();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Parâmetro do tipo esperado
     * Objetivo: Buscar o parâmetro e conferir o tipo.
     * Decisões: Chave inexistente ou tipo errado é erro de programação (IllegalStateException).
     */
    private Parametro doTipo(String chave, Parametro.Tipo tipo) {
        Parametro p = parametros.findById(chave)
                .orElseThrow(() -> new IllegalStateException("Parâmetro inexistente: " + chave));
        if (p.getTipo() != tipo) {
            throw new IllegalStateException("O parâmetro " + chave + " é do tipo " + p.getTipo() + ", não " + tipo);
        }
        return p;
    }
}
