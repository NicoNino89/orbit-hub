package br.com.orbithub.catalogo.service;

import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import java.util.Optional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-39
 * Título: Critério de compatibilidade
 * Objetivo: Definir a estratégia que compara a startup com o perfil de investimento e devolve a justificativa quando
 *           há compatibilidade.
 * Decisões: Padrão Strategy: cada critério é um componente; um critério novo é uma classe nova, sem mexer nas outras.
 *           Vazio quando não há compatibilidade ou falta o dado (critério sem dado não penaliza). Critérios da rodada só
 *           são avaliados com rodada aberta e investidor verificado.
 */
public interface CriterioDeCompatibilidade {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Depende da rodada
     * Objetivo: Dizer se o critério lê os dados da rodada aberta.
     * Decisões: Padrão: não.
     */
    default boolean daRodada() {
        return false;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Avaliar
     * Objetivo: Comparar e devolver a justificativa, se houver compatibilidade.
     * Decisões: Texto curto, sem percentual.
     */
    Optional<String> avaliar(Contexto contexto);

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Contexto da avaliação
     * Objetivo: Levar o perfil do investidor e os dados da startup que os critérios comparam.
     * Decisões: Rótulos já traduzidos para a justificativa; rodada nula quando não há aberta.
     */
    record Contexto(PerfilDeInvestimento.Dados perfil, String segmento, String segmentoRotulo, MaturidadeDoNegocio maturidade,
                    String localizacao, String modelo, String modeloRotulo, Rodada.Termos rodada, String estagioRotulo,
                    String instrumentoRotulo, boolean semLider) {
    }
}
