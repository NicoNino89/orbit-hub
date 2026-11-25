package br.com.orbithub.empreendimentos.domain;

import java.math.BigDecimal;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Indicadores de tração
 * Objetivo: Enumerar os indicadores numéricos de tração e resultados, com o formato de cada um.
 * Decisões: A evolução do faturamento é o histórico do faturamento mensal. "Outros indicadores de desempenho" é texto
 *           livre na seção de tração (catálogo de campos). Participação de mercado é opcional, como os demais.
 */
public enum IndicadorDaStartup {
    CLIENTES("Quantidade de clientes", Formato.QUANTIDADE),
    FATURAMENTO_MENSAL("Faturamento mensal (R$)", Formato.VALOR),
    CRESCIMENTO_MENSAL("Taxa de crescimento mensal (%)", Formato.VARIACAO),
    CONTRATOS("Contratos firmados", Formato.QUANTIDADE),
    RETENCAO("Retenção de clientes (%)", Formato.PERCENTUAL),
    PARTICIPACAO_MERCADO("Participação de mercado (%)", Formato.PERCENTUAL);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Formato do indicador
     * Objetivo: Dizer que números o indicador aceita.
     * Decisões: Quantidade inteira a partir de zero; valor em reais a partir de zero, com centavos; percentual de 0 a 100;
     *           variação de -100% a 1000% (crescimento pode ser negativo ou passar de 100%).
     */
    public enum Formato {
        QUANTIDADE(BigDecimal.ZERO, null, 0),
        VALOR(BigDecimal.ZERO, null, 2),
        PERCENTUAL(BigDecimal.ZERO, new BigDecimal("100"), 2),
        VARIACAO(new BigDecimal("-100"), new BigDecimal("1000"), 2);

        private final BigDecimal minimo;
        private final BigDecimal maximo;
        private final int casas;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:11
         * Cartão Jira: KAN-24
         * Título: Criação do formato
         * Objetivo: Guardar a faixa e as casas decimais aceitas.
         * Decisões: Máximo nulo = sem teto.
         */
        Formato(BigDecimal minimo, BigDecimal maximo, int casas) {
            this.minimo = minimo;
            this.maximo = maximo;
            this.casas = casas;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:11
         * Cartão Jira: KAN-24
         * Título: Aceita o valor
         * Objetivo: Conferir a faixa e as casas decimais.
         * Decisões: Zeros à direita não contam como casas.
         */
        public boolean aceita(BigDecimal valor) {
            return valor.compareTo(minimo) >= 0 && (maximo == null || valor.compareTo(maximo) <= 0)
                    && Math.max(valor.stripTrailingZeros().scale(), 0) <= casas;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 23:11
         * Cartão Jira: KAN-24
         * Título: Explicação
         * Objetivo: Dizer, na mensagem de erro, o que o formato aceita.
         * Decisões: Em português.
         */
        public String explicacao() {
            return switch (this) {
                case QUANTIDADE -> "um número inteiro a partir de zero";
                case VALOR -> "um valor em reais a partir de zero, com até dois centavos";
                case PERCENTUAL -> "um percentual de 0 a 100";
                case VARIACAO -> "um percentual de -100 a 1000";
            };
        }
    }

    private final String rotulo;
    private final Formato formato;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Criação do indicador
     * Objetivo: Guardar o nome e o formato.
     * Decisões: Nome com a unidade.
     */
    IndicadorDaStartup(String rotulo, Formato formato) {
        this.rotulo = rotulo;
        this.formato = formato;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso ao rótulo
     * Objetivo: Expor o nome do indicador.
     * Decisões: Somente leitura.
     */
    public String getRotulo() {
        return rotulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso ao formato
     * Objetivo: Expor o formato do indicador.
     * Decisões: Somente leitura.
     */
    public Formato getFormato() {
        return formato;
    }
}
