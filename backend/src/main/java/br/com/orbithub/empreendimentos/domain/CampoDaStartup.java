package br.com.orbithub.empreendimentos.domain;

import br.com.orbithub.administracao.domain.Lista;
import java.util.Arrays;
import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:56
 * Cartão Jira: KAN-22
 * Título: Campos das seções da startup
 * Objetivo: Catalogar os campos de cada seção do cadastro (rótulo, tipo, limite de caracteres e ajuda), para o
 *           servidor validar e a tela montar os formulários sem repetir as regras.
 * Decisões: Catálogo no código, guardado em startup_campo (campo, valor), sem JSONB: cada história do cadastro acrescenta
 *           os seus campos aqui. Limites pensados para leitura no catálogo: 1000 caracteres para respostas diretas e
 *           2000 para descrições.
 */
public enum CampoDaStartup {
    PROBLEMA(SecaoDaStartup.PROBLEMA_E_SOLUCAO, "Qual problema a startup resolve", Tipo.TEXTO, 1000, null,
            "Descreva a dor ou a necessidade, com exemplos concretos."),
    QUEM_ENFRENTA(SecaoDaStartup.PROBLEMA_E_SOLUCAO, "Quem enfrenta esse problema", Tipo.TEXTO, 1000, null,
            "Pessoas ou empresas afetadas e quantas são, se souber."),
    SOLUCAO(SecaoDaStartup.PROBLEMA_E_SOLUCAO, "Solução proposta", Tipo.TEXTO, 1000, null, null),
    COMO_FUNCIONA(SecaoDaStartup.PROBLEMA_E_SOLUCAO, "Como funciona", Tipo.TEXTO, 2000, null,
            "O caminho do cliente, do primeiro contato ao resultado."),
    DIFERENCIAL(SecaoDaStartup.PROBLEMA_E_SOLUCAO, "Diferencial", Tipo.TEXTO, 1000, null,
            "Por que a sua solução é melhor do que as alternativas de hoje."),

    DESCRICAO_PRODUTO(SecaoDaStartup.PRODUTO, "Descrição do produto ou serviço", Tipo.TEXTO, 2000, null, null),
    TEM_MVP(SecaoDaStartup.PRODUTO, "Já existe um MVP (versão mínima em uso)?", Tipo.SIM_NAO, 0, null, null),
    FUNCIONALIDADES(SecaoDaStartup.PRODUTO, "Principais funcionalidades", Tipo.TEXTO, 2000, null, null),
    TECNOLOGIA(SecaoDaStartup.PRODUTO, "Tecnologia utilizada", Tipo.TEXTO, 1000, null, null),
    PROPRIEDADE_INTELECTUAL(SecaoDaStartup.PRODUTO, "Propriedade intelectual", Tipo.TEXTO, 1000, null,
            "Patentes, registros de marca ou de software e pedidos em andamento."),
    DIFERENCIAIS_COMPETITIVOS(SecaoDaStartup.PRODUTO, "Diferenciais competitivos", Tipo.TEXTO, 1000, null, null);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Tipo do campo
     * Objetivo: Dizer como o valor é escrito e conferido.
     * Decisões: TEXTO com limite; SIM_NAO "SIM" ou "NAO". Os próximos tipos chegam com as histórias que os usam.
     */
    public enum Tipo { TEXTO, SIM_NAO }

    private final SecaoDaStartup secao;
    private final String rotulo;
    private final Tipo tipo;
    private final int limite;
    private final Lista lista;
    private final String ajuda;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Criação do campo
     * Objetivo: Guardar a seção, o rótulo, o tipo, o limite, a lista e a ajuda.
     * Decisões: Limite só vale para texto; lista só para o tipo LISTA.
     */
    CampoDaStartup(SecaoDaStartup secao, String rotulo, Tipo tipo, int limite, Lista lista, String ajuda) {
        this.secao = secao;
        this.rotulo = rotulo;
        this.tipo = tipo;
        this.limite = limite;
        this.lista = lista;
        this.ajuda = ajuda;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Campos da seção
     * Objetivo: Listar os campos de uma seção, na ordem do formulário.
     * Decisões: Ordem da declaração.
     */
    public static List<CampoDaStartup> da(SecaoDaStartup secao) {
        return Arrays.stream(values()).filter(c -> c.secao == secao).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso à seção
     * Objetivo: Expor a seção do campo.
     * Decisões: Somente leitura.
     */
    public SecaoDaStartup getSecao() {
        return secao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso ao rótulo
     * Objetivo: Expor a pergunta do campo.
     * Decisões: Somente leitura.
     */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do campo.
     * Decisões: Somente leitura.
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso ao limite
     * Objetivo: Expor o limite de caracteres do texto.
     * Decisões: Zero para quem não é texto.
     */
    public int getLimite() {
        return limite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso à lista
     * Objetivo: Expor a lista gerenciável das opções.
     * Decisões: Nula para quem não é lista.
     */
    public Lista getLista() {
        return lista;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Acesso à ajuda
     * Objetivo: Expor a dica que aparece abaixo do campo.
     * Decisões: Pode ser nula.
     */
    public String getAjuda() {
        return ajuda;
    }
}
