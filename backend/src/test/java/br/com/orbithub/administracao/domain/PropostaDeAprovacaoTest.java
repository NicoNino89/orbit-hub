package br.com.orbithub.administracao.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.administracao.domain.PropostaDeAprovacao.Situacao;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Testes da proposta de ação grave
 * Objetivo: Garantir que quem propõe não decide, que toda decisão tem justificativa, que só pendentes são
 *           decididas e o cálculo de vencimento.
 * Decisões: Teste de unidade da entidade.
 */
class PropostaDeAprovacaoTest {

    static final Instant AGORA = Instant.parse("2026-11-24T14:00:00Z");
    static final UUID BIA = UUID.randomUUID();
    static final UUID CAIO = UUID.randomUUID();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta nova
     * Objetivo: Conferir que a proposta nasce pendente, com dados copiados e justificativa obrigatória.
     * Decisões: Alterar o mapa original depois não muda a proposta.
     */
    @Test
    void nascePendenteComDadosCopiados() {
        Map<String, String> dados = new HashMap<>(Map.of("valor", "60"));
        PropostaDeAprovacao p = proposta(dados);
        dados.put("valor", "999");
        assertThat(p.getSituacao()).isEqualTo(Situacao.PENDENTE);
        assertThat(p.getDados()).containsEntry("valor", "60");
        assertThatThrownBy(() -> p.getDados().put("x", "y")).isInstanceOf(UnsupportedOperationException.class);
        assertThatThrownBy(() -> PropostaDeAprovacao.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro", "x", "r",
                Map.of(), BIA, " ", AGORA)).isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Segunda pessoa decide
     * Objetivo: Conferir que quem propõe não aprova nem rejeita, que outra pessoa decide com justificativa e que
     *           a decisão não se repete.
     * Decisões: Cancelar é só de quem propôs.
     */
    @Test
    void soOutraPessoaDecideUmaVez() {
        PropostaDeAprovacao p = proposta(Map.of());
        assertThatThrownBy(() -> p.aprovar(BIA, "Ok, eu mesma", AGORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> p.rejeitar(BIA, "Não", AGORA)).isInstanceOf(IllegalStateException.class);
        assertThatThrownBy(() -> p.aprovar(CAIO, "", AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> p.cancelar(CAIO, AGORA)).isInstanceOf(IllegalStateException.class);

        p.aprovar(CAIO, "Conferi com o jurídico", AGORA);
        assertThat(p.getSituacao()).isEqualTo(Situacao.APROVADA);
        assertThat(p.getDecididoPor()).isEqualTo(CAIO);
        assertThat(p.getJustificativaDecisao()).isEqualTo("Conferi com o jurídico");
        assertThatThrownBy(() -> p.rejeitar(CAIO, "Mudei de ideia", AGORA)).isInstanceOf(IllegalStateException.class);

        PropostaDeAprovacao outra = proposta(Map.of());
        outra.cancelar(BIA, AGORA);
        assertThat(outra.getSituacao()).isEqualTo(Situacao.CANCELADA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Vencimento e alerta
     * Objetivo: Conferir que a proposta vence no prazo, que decidida não vence e que o alerta é marcado uma vez.
     * Decisões: Prazo de 48 h, o padrão.
     */
    @Test
    void venceNoPrazoEMarcaOAlertaUmaVez() {
        PropostaDeAprovacao p = proposta(Map.of());
        Duration prazo = Duration.ofHours(48);
        assertThat(p.vencida(AGORA.plus(Duration.ofHours(47)), prazo)).isFalse();
        assertThat(p.vencida(AGORA.plus(prazo), prazo)).isTrue();
        p.marcarAlertada(AGORA.plus(prazo));
        p.marcarAlertada(AGORA.plus(Duration.ofHours(60)));
        assertThat(p.getAlertadaEm()).isEqualTo(AGORA.plus(prazo));
        p.rejeitar(CAIO, "Fora de hora", AGORA);
        assertThat(p.vencida(AGORA.plus(Duration.ofDays(10)), prazo)).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta de teste
     * Objetivo: Criar uma proposta de parâmetro sensível feita pela Bia.
     * Decisões: Dados informados pelo teste.
     */
    private static PropostaDeAprovacao proposta(Map<String, String> dados) {
        return PropostaDeAprovacao.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro", "nivel2.validade_dias",
                "nivel2.validade_dias: 90 → 60", dados, BIA, "Pedido do jurídico", AGORA);
    }
}
