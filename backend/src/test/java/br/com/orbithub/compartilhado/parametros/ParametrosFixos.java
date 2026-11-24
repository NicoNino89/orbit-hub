package br.com.orbithub.compartilhado.parametros;

import java.time.Duration;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Parâmetros fixos para testes de unidade
 * Objetivo: Dar aos testes de unidade os mesmos valores padrão do catálogo, sem banco, e deixar cada teste trocar
 *           o que precisar.
 * Decisões: Valores e unidades iguais aos da migração V11; {@link #com} troca um valor e devolve o próprio objeto.
 */
public final class ParametrosFixos implements LeituraDeParametros {

    private final Map<String, String> valores = new HashMap<>();
    private final Map<String, ChronoUnit> unidades = new HashMap<>();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valores padrão
     * Objetivo: Carregar os padrões do catálogo para as chaves usadas pelo código.
     * Decisões: Mesmos valores da migração.
     */
    public ParametrosFixos() {
        definir(Chaves.CADASTRO_LINK_CONFIRMACAO, "24", ChronoUnit.HOURS);
        definir(Chaves.CONTA_LINK_REDEFINICAO, "30", ChronoUnit.MINUTES);
        definir(Chaves.CONTA_LINK_TROCA_EMAIL, "24", ChronoUnit.HOURS);
        definir(Chaves.LOGIN_MAX_TENTATIVAS, "5", null);
        definir(Chaves.LOGIN_BLOQUEIO, "15", ChronoUnit.MINUTES);
        definir(Chaves.SESSAO_INATIVIDADE, "30", ChronoUnit.MINUTES);
        definir(Chaves.SESSAO_DURACAO_MAXIMA, "12", ChronoUnit.HOURS);
        definir(Chaves.CONVITE_VALIDADE, "14", ChronoUnit.DAYS);
        definir(Chaves.ACESSO_REVISAO, "3", null);
        definir(Chaves.APROVACAO_PRAZO, "48", ChronoUnit.HOURS);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Trocar um valor
     * Objetivo: Devolver os parâmetros com um valor trocado.
     * Decisões: Mantém a unidade da chave.
     */
    public ParametrosFixos com(String chave, String valor) {
        if (!valores.containsKey(chave)) {
            throw new IllegalArgumentException("Chave desconhecida nos testes: " + chave);
        }
        valores.put(chave, valor);
        return this;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor inteiro
     * Objetivo: Ler o valor como número.
     * Decisões: Igual à implementação real.
     */
    @Override
    public long inteiro(String chave) {
        return Long.parseLong(valor(chave));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor como duração
     * Objetivo: Ler o valor como Duration na unidade da chave.
     * Decisões: Chave sem unidade de tempo é erro.
     */
    @Override
    public Duration duracao(String chave) {
        ChronoUnit unidade = unidades.get(chave);
        if (unidade == null) {
            throw new IllegalStateException(chave + " não é uma duração");
        }
        return Duration.of(inteiro(chave), unidade);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Lista de inteiros
     * Objetivo: Ler o valor como lista separada por vírgula.
     * Decisões: Igual à implementação real.
     */
    @Override
    public List<Long> listaDeInteiros(String chave) {
        return Arrays.stream(valor(chave).split(",")).map(Long::parseLong).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Opção
     * Objetivo: Ler o valor como texto.
     * Decisões: Sem conferir as opções nos testes.
     */
    @Override
    public String opcao(String chave) {
        return valor(chave);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Definir padrão
     * Objetivo: Registrar valor e unidade de uma chave.
     * Decisões: Unidade nula para quantidades e meses.
     */
    private void definir(String chave, String valor, ChronoUnit unidade) {
        valores.put(chave, valor);
        if (unidade != null) {
            unidades.put(chave, unidade);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Valor bruto
     * Objetivo: Ler o texto de uma chave.
     * Decisões: Chave desconhecida é erro, como na implementação real.
     */
    private String valor(String chave) {
        String v = valores.get(chave);
        if (v == null) {
            throw new IllegalStateException("Parâmetro inexistente: " + chave);
        }
        return v;
    }
}
