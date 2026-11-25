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
    DIFERENCIAIS_COMPETITIVOS(SecaoDaStartup.PRODUTO, "Diferenciais competitivos", Tipo.TEXTO, 1000, null, null),

    PUBLICO_ALVO(SecaoDaStartup.MERCADO, "Público-alvo", Tipo.TEXTO, 1000, null,
            "O segmento de atuação fica nos dados cadastrais; aqui, para quem você vende."),
    PERFIL_CLIENTES(SecaoDaStartup.MERCADO, "Perfil dos clientes", Tipo.TEXTO, 1000, null,
            "Pessoas ou empresas, porte, região, comportamento de compra."),
    DIMENSAO_MERCADO(SecaoDaStartup.MERCADO, "Dimensão do mercado", Tipo.TEXTO, 1000, null,
            "Tamanho total do mercado, com a fonte da estimativa."),
    MERCADO_ENDERECAVEL(SecaoDaStartup.MERCADO, "Mercado endereçável", Tipo.TEXTO, 1000, null,
            "A parte do mercado que a startup consegue atender hoje."),
    CONCORRENTES(SecaoDaStartup.MERCADO, "Concorrentes", Tipo.TEXTO, 1000, null, "Diretos e indiretos, e como você se compara."),
    BARREIRAS_ENTRADA(SecaoDaStartup.MERCADO, "Barreiras de entrada", Tipo.TEXTO, 1000, null, null),
    TENDENCIAS(SecaoDaStartup.MERCADO, "Tendências", Tipo.TEXTO, 1000, null, null),
    POTENCIAL_CRESCIMENTO(SecaoDaStartup.MERCADO, "Potencial de crescimento", Tipo.TEXTO, 1000, null, null),

    MODELO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Classificação do modelo de negócio", Tipo.LISTA, 0, Lista.MODELO_NEGOCIO,
            "Usada nos filtros do catálogo e na compatibilidade com investidores."),
    GERACAO_RECEITA(SecaoDaStartup.MODELO_DE_NEGOCIO, "Como a startup gera receita", Tipo.TEXTO, 1000, null, null),
    FONTES_FATURAMENTO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Fontes de faturamento", Tipo.TEXTO, 1000, null, null),
    PRECIFICACAO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Precificação", Tipo.TEXTO, 1000, null, null),
    MODELO_COMERCIALIZACAO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Modelo de comercialização", Tipo.TEXTO, 1000, null,
            "Venda direta, parceiros, autosserviço, licitações..."),
    RECEITAS_RECORRENTES(SecaoDaStartup.MODELO_DE_NEGOCIO, "Receitas recorrentes", Tipo.TEXTO, 1000, null,
            "Assinaturas, contratos ou compras repetidas, se houver."),
    ESTRUTURA_CUSTOS(SecaoDaStartup.MODELO_DE_NEGOCIO, "Estrutura de custos", Tipo.TEXTO, 1000, null, null),
    CANAIS_DISTRIBUICAO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Canais de distribuição", Tipo.TEXTO, 1000, null, null);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:02
     * Cartão Jira: KAN-23
     * Título: Tipo do campo
     * Objetivo: Dizer como o valor é escrito e conferido.
     * Decisões: TEXTO com limite; SIM_NAO "SIM" ou "NAO"; LISTA com o código de uma opção ativa da lista gerenciável
     *           (KAN-23). Os próximos tipos chegam com as histórias que os usam.
     */
    public enum Tipo { TEXTO, SIM_NAO, LISTA }

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
