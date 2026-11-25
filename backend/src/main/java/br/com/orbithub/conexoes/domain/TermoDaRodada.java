package br.com.orbithub.conexoes.domain;

import java.util.Arrays;
import java.util.List;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Termos específicos por instrumento
 * Objetivo: Catalogar os campos próprios de cada instrumento de investimento, com o tipo de valor.
 * Decisões: Ligados ao código da lista INSTRUMENTO. Instrumento novo na lista (sem termos aqui) usa a descrição livre
 *           OUTROS_TERMOS. Todos são Nível 2 (só aparecem com acesso liberado, KAN-30).
 */
public enum TermoDaRodada {
    VALUATION_PRE_MONEY("PARTICIPACAO_SOCIETARIA", "Valuation pré-money (R$)", Tipo.VALOR),
    PARTICIPACAO_OFERECIDA("PARTICIPACAO_SOCIETARIA", "Participação oferecida (%)", Tipo.PERCENTUAL),

    VALUATION_CAP("MUTUO_CONVERSIVEL", "Valuation cap (R$)", Tipo.VALOR),
    DESCONTO_CONVERSAO("MUTUO_CONVERSIVEL", "Desconto na conversão (%)", Tipo.PERCENTUAL),
    TAXA_JUROS("MUTUO_CONVERSIVEL", "Taxa de juros (% ao ano)", Tipo.PERCENTUAL),
    VENCIMENTO_MUTUO_MESES("MUTUO_CONVERSIVEL", "Prazo de vencimento (meses)", Tipo.MESES),
    EVENTOS_CONVERSAO("MUTUO_CONVERSIVEL", "Eventos de conversão", Tipo.TEXTO),

    PRAZO_CONTRATO_MESES("CONTRATO_INVESTIMENTO_ANJO", "Prazo do contrato (meses)", Tipo.MESES),
    FORMA_REMUNERACAO("CONTRATO_INVESTIMENTO_ANJO", "Forma de remuneração", Tipo.TEXTO),
    CONDICOES_RESGATE("CONTRATO_INVESTIMENTO_ANJO", "Condições de resgate", Tipo.TEXTO),

    VALUATION_EXERCICIO("OPCAO_DE_COMPRA", "Preço ou valuation de exercício (R$)", Tipo.VALOR),
    PRAZO_EXERCICIO_MESES("OPCAO_DE_COMPRA", "Prazo para exercer (meses)", Tipo.MESES),

    VALOR_EMISSAO("DEBENTURE_CONVERSIVEL", "Valor da emissão (R$)", Tipo.VALOR),
    REMUNERACAO("DEBENTURE_CONVERSIVEL", "Remuneração", Tipo.TEXTO),
    VENCIMENTO_DEBENTURE_MESES("DEBENTURE_CONVERSIVEL", "Vencimento (meses)", Tipo.MESES),
    CONDICOES_CONVERSAO("DEBENTURE_CONVERSIVEL", "Condições de conversão", Tipo.TEXTO),

    OUTROS_TERMOS(null, "Termos do instrumento", Tipo.TEXTO);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Tipo do termo
     * Objetivo: Dizer como o valor do termo é conferido.
     * Decisões: Valor em reais maior que zero; percentual de 0 a 100; meses de 1 a 240; texto até 1000 caracteres.
     */
    public enum Tipo { VALOR, PERCENTUAL, MESES, TEXTO }

    private final String instrumento;
    private final String rotulo;
    private final Tipo tipo;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Criação do termo
     * Objetivo: Guardar o instrumento, o rótulo e o tipo.
     * Decisões: Instrumento nulo = termo genérico.
     */
    TermoDaRodada(String instrumento, String rotulo, Tipo tipo) {
        this.instrumento = instrumento;
        this.rotulo = rotulo;
        this.tipo = tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termos do instrumento
     * Objetivo: Listar os termos de um instrumento, na ordem do formulário.
     * Decisões: Instrumento sem termos próprios recebe a descrição livre.
     */
    public static List<TermoDaRodada> doInstrumento(String instrumento) {
        List<TermoDaRodada> termos = Arrays.stream(values()).filter(t -> t.instrumento != null && t.instrumento.equals(instrumento)).toList();
        return termos.isEmpty() ? List.of(OUTROS_TERMOS) : termos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao instrumento
     * Objetivo: Expor o código do instrumento do termo.
     * Decisões: Nulo no termo genérico.
     */
    public String getInstrumento() {
        return instrumento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao rótulo
     * Objetivo: Expor o nome do termo.
     * Decisões: Com a unidade.
     */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do termo.
     * Decisões: Somente leitura.
     */
    public Tipo getTipo() {
        return tipo;
    }
}
