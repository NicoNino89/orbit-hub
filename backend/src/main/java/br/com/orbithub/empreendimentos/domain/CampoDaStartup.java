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
    CANAIS_DISTRIBUICAO(SecaoDaStartup.MODELO_DE_NEGOCIO, "Canais de distribuição", Tipo.TEXTO, 1000, null, null),

    OUTROS_INDICADORES(SecaoDaStartup.TRACAO, "Outros indicadores de desempenho", Tipo.TEXTO, 1000, null,
            "Métricas próprias do negócio (CAC, LTV, ticket médio...), com o mês de referência."),

    EXPERIENCIA(SecaoDaStartup.EQUIPE_E_ESTRUTURA, "Experiência dos fundadores", Tipo.TEXTO, 2000, null,
            "Os fundadores e a participação de cada um ficam nos dados cadastrais; aqui, a trajetória."),
    FORMACAO(SecaoDaStartup.EQUIPE_E_ESTRUTURA, "Formação", Tipo.TEXTO, 1000, null, null),
    COMPOSICAO_EQUIPE(SecaoDaStartup.EQUIPE_E_ESTRUTURA, "Composição da equipe", Tipo.TEXTO, 1000, null,
            "Quantas pessoas, em quais áreas, dedicação integral ou parcial."),
    ESTRUTURA_OPERACIONAL(SecaoDaStartup.EQUIPE_E_ESTRUTURA, "Estrutura operacional", Tipo.TEXTO, 1000, null, null),
    PARCEIROS_ESTRATEGICOS(SecaoDaStartup.EQUIPE_E_ESTRUTURA, "Parceiros estratégicos", Tipo.TEXTO, 1000, null, null),

    OBJETIVOS_CURTO_PRAZO(SecaoDaStartup.PLANEJAMENTO, "Objetivos de curto prazo (até 1 ano)", Tipo.TEXTO, 1000, null, null),
    OBJETIVOS_MEDIO_PRAZO(SecaoDaStartup.PLANEJAMENTO, "Objetivos de médio prazo (1 a 3 anos)", Tipo.TEXTO, 1000, null, null),
    OBJETIVOS_LONGO_PRAZO(SecaoDaStartup.PLANEJAMENTO, "Objetivos de longo prazo (mais de 3 anos)", Tipo.TEXTO, 1000, null, null),
    ESTRATEGIA_EXPANSAO(SecaoDaStartup.PLANEJAMENTO, "Estratégia de expansão", Tipo.TEXTO, 1000, null,
            "Novos mercados, regiões, produtos ou canais."),
    METAS(SecaoDaStartup.PLANEJAMENTO, "Metas", Tipo.TEXTO, 1000, null, "De preferência mensuráveis e com prazo."),
    RISCOS(SecaoDaStartup.PLANEJAMENTO, "Riscos identificados", Tipo.TEXTO, 1000, null, null),
    MITIGACAO(SecaoDaStartup.PLANEJAMENTO, "Estratégias de mitigação", Tipo.TEXTO, 1000, null, null),

    DESTINO_PRODUTO(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Destinação para produto e tecnologia (%)", Tipo.PERCENTUAL, 0, null,
            "Valores, instrumento, valuation e participação oferecida ficam só na rodada."),
    DESTINO_COMERCIAL(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Destinação para comercial e marketing (%)", Tipo.PERCENTUAL, 0, null, null),
    DESTINO_EQUIPE(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Destinação para equipe (%)", Tipo.PERCENTUAL, 0, null, null),
    DESTINO_OPERACAO(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Destinação para operação e infraestrutura (%)", Tipo.PERCENTUAL, 0, null, null),
    DESTINO_OUTROS(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Outras destinações (%)", Tipo.PERCENTUAL, 0, null, null),
    PRAZO_USO_MESES(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Prazo estimado de uso dos recursos (meses)", Tipo.INTEIRO, 120, null, null),
    METAS_DO_INVESTIMENTO(SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO, "Metas que o investimento deve viabilizar", Tipo.TEXTO, 1000, null, null),

    ESTAGIO(SecaoDaStartup.MATURIDADE, "Estágio do negócio", Tipo.ESCALA, 0, null,
            "Obrigatório para publicar. Escolha o que descreve a startup hoje, não onde ela quer chegar.");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:30
     * Cartão Jira: KAN-26
     * Título: Tipo do campo
     * Objetivo: Dizer como o valor é escrito e conferido.
     * Decisões: TEXTO com limite; SIM_NAO "SIM" ou "NAO"; LISTA com o código de uma opção ativa da lista gerenciável
     *           (KAN-23); PERCENTUAL de 0 a 100, com até duas casas; INTEIRO de 1 até o limite (KAN-25); ESCALA com um nível
     *           da maturidade do negócio (KAN-26).
     */
    public enum Tipo { TEXTO, SIM_NAO, LISTA, PERCENTUAL, INTEIRO, ESCALA }

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
     * Hora: 23:26
     * Cartão Jira: KAN-25
     * Título: Acesso ao limite
     * Objetivo: Expor o limite de caracteres do texto ou o máximo do inteiro.
     * Decisões: Zero para os outros tipos.
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
