package br.com.orbithub.empreendimentos.domain;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-28
 * Título: Itens de visibilidade da startup
 * Objetivo: Listar o que a equipe pode marcar como público (Nível 1) ou confidencial (Nível 2) e o padrão de cada item.
 * Decisões: Nome, segmento, logotipo e estágio são sempre públicos: sem eles a startup não tem identidade no catálogo.
 *           A participação societária dos fundadores é sempre confidencial (não é item). Começam confidenciais os campos
 *           financeiros (faturamento, precificação, receitas recorrentes, custos, destinação e prazo dos recursos),
 *           societários e estratégicos (propriedade intelectual, concorrentes, barreiras, parceiros, objetivos, metas,
 *           riscos), a localização, os indicadores e os dados da rodada. Os dados da rodada só aparecem para investidor
 *           verificado, mesmo marcados como públicos; valuation e termos do instrumento são sempre Nível 2.
 */
public final class VisibilidadeDaStartup {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Itens fora das seções
     * Objetivo: Nomear os itens de visibilidade que não são campos das seções.
     * Decisões: Os demais itens têm o nome do campo (CampoDaStartup).
     */
    public enum Extra {
        LOCALIZACAO("Localização", SecaoDaStartup.CADASTRO),
        SITE("Site", SecaoDaStartup.CADASTRO),
        REDES_SOCIAIS("Redes sociais", SecaoDaStartup.CADASTRO),
        FUNDADORES("Nomes e cargos dos fundadores", SecaoDaStartup.CADASTRO),
        INDICADORES("Indicadores de tração", SecaoDaStartup.TRACAO),
        DADOS_DA_RODADA("Dados da rodada aberta (estágio, instrumento e valores)", SecaoDaStartup.NECESSIDADE_DE_INVESTIMENTO);

        private final String rotulo;
        private final SecaoDaStartup secao;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 10:40
         * Cartão Jira: KAN-28
         * Título: Criação do item
         * Objetivo: Guardar o rótulo e a seção do item.
         * Decisões: A seção agrupa o item na tela.
         */
        Extra(String rotulo, SecaoDaStartup secao) {
            this.rotulo = rotulo;
            this.secao = secao;
        }
    }

    private static final Set<Extra> EXTRAS_PUBLICOS = Collections.unmodifiableSet(EnumSet.of(Extra.SITE, Extra.REDES_SOCIAIS,
            Extra.FUNDADORES));

    private static final Set<CampoDaStartup> CONFIDENCIAIS = Collections.unmodifiableSet(EnumSet.of(
            CampoDaStartup.PROPRIEDADE_INTELECTUAL, CampoDaStartup.CONCORRENTES, CampoDaStartup.BARREIRAS_ENTRADA,
            CampoDaStartup.FONTES_FATURAMENTO, CampoDaStartup.PRECIFICACAO, CampoDaStartup.RECEITAS_RECORRENTES,
            CampoDaStartup.ESTRUTURA_CUSTOS, CampoDaStartup.OUTROS_INDICADORES, CampoDaStartup.PARCEIROS_ESTRATEGICOS,
            CampoDaStartup.OBJETIVOS_CURTO_PRAZO, CampoDaStartup.OBJETIVOS_MEDIO_PRAZO, CampoDaStartup.OBJETIVOS_LONGO_PRAZO,
            CampoDaStartup.METAS, CampoDaStartup.RISCOS, CampoDaStartup.MITIGACAO, CampoDaStartup.DESTINO_PRODUTO,
            CampoDaStartup.DESTINO_COMERCIAL, CampoDaStartup.DESTINO_EQUIPE, CampoDaStartup.DESTINO_OPERACAO,
            CampoDaStartup.DESTINO_OUTROS, CampoDaStartup.PRAZO_USO_MESES));

    private VisibilidadeDaStartup() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Item de visibilidade
     * Objetivo: Descrever um item: código, rótulo, seção e padrão.
     * Decisões: O código é o nome do campo ou do extra.
     */
    public record Item(String codigo, String rotulo, SecaoDaStartup secao, boolean padraoPublico) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Itens configuráveis
     * Objetivo: Listar todos os itens, na ordem das seções do cadastro.
     * Decisões: O estágio fica fora: é sempre público.
     */
    public static List<Item> itens() {
        List<Item> itens = new ArrayList<>();
        for (SecaoDaStartup secao : SecaoDaStartup.values()) {
            for (Extra e : Extra.values()) {
                if (e.secao == secao) {
                    itens.add(new Item(e.name(), e.rotulo, secao, EXTRAS_PUBLICOS.contains(e)));
                }
            }
            for (CampoDaStartup c : CampoDaStartup.da(secao)) {
                if (c != CampoDaStartup.ESTAGIO) {
                    itens.add(new Item(c.name(), c.getRotulo(), secao, !CONFIDENCIAIS.contains(c)));
                }
            }
        }
        return itens;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Item pelo código
     * Objetivo: Achar um item configurável pelo código.
     * Decisões: Vazio para código desconhecido.
     */
    public static Optional<Item> item(String codigo) {
        return itens().stream().filter(i -> i.codigo().equals(codigo)).findFirst();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: É público
     * Objetivo: Dizer se um item aparece no catálogo, pela escolha da equipe ou pelo padrão.
     * Decisões: O estágio é sempre público; código desconhecido nunca é.
     */
    public static boolean publico(String codigo, Map<String, Boolean> escolhas) {
        if (CampoDaStartup.ESTAGIO.name().equals(codigo)) {
            return true;
        }
        return item(codigo).map(i -> escolhas.getOrDefault(codigo, i.padraoPublico())).orElse(false);
    }
}
