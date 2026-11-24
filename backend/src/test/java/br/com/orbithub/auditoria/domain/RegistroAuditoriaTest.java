package br.com.orbithub.auditoria.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Testes do registro de auditoria
 * Objetivo: Garantir os campos obrigatórios e o corte dos textos no tamanho das colunas.
 * Decisões: Teste de unidade puro, sem Spring.
 */
class RegistroAuditoriaTest {

    static final Instant AGORA = Instant.parse("2026-11-24T13:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Registro completo
     * Objetivo: Conferir que todos os dados da ação chegam ao registro.
     * Decisões: Identificador novo a cada registro.
     */
    @Test
    void montaORegistroComTodosOsDados() {
        UUID ator = UUID.randomUUID();
        RegistroAuditoria registro = RegistroAuditoria.de(new Acao(ator, "Bia Lima", "PARAMETRO_ALTERADO",
                "parametro", "conexao.expiracao_dias", Resultado.SUCESSO, 200, "30 → 45", "Pedido do time"), AGORA);

        assertThat(registro.getId()).isNotNull();
        assertThat(registro.getAtorId()).isEqualTo(ator);
        assertThat(registro.getAtorNome()).isEqualTo("Bia Lima");
        assertThat(registro.getAcao()).isEqualTo("PARAMETRO_ALTERADO");
        assertThat(registro.getObjetoTipo()).isEqualTo("parametro");
        assertThat(registro.getObjetoId()).isEqualTo("conexao.expiracao_dias");
        assertThat(registro.getResultado()).isEqualTo(Resultado.SUCESSO);
        assertThat(registro.getStatusHttp()).isEqualTo(200);
        assertThat(registro.getDetalhe()).isEqualTo("30 → 45");
        assertThat(registro.getJustificativa()).isEqualTo("Pedido do time");
        assertThat(registro.getOcorridoEm()).isEqualTo(AGORA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Ação e resultado obrigatórios
     * Objetivo: Recusar registro sem ação ou sem resultado.
     * Decisões: Autor pode faltar (ações do sistema).
     */
    @Test
    void exigeAcaoEResultado() {
        assertThatThrownBy(() -> RegistroAuditoria.de(
                new Acao(null, null, null, null, null, Resultado.SUCESSO, null, null, null), AGORA))
                .isInstanceOf(NullPointerException.class);
        assertThatThrownBy(() -> RegistroAuditoria.de(
                new Acao(null, null, "ROTINA", null, null, null, null, null, null), AGORA))
                .isInstanceOf(NullPointerException.class);
        assertThat(RegistroAuditoria.de(new Acao(null, null, "ROTINA_EXECUTADA", null, null, Resultado.SUCESSO,
                null, null, null), AGORA).getAtorId()).isNull();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Corte dos textos
     * Objetivo: Garantir que texto longo é cortado e texto em branco vira nulo.
     * Decisões: Um detalhe grande nunca impede o registro.
     */
    @Test
    void cortaTextosLongosEDescartaBrancos() {
        String longo = "x".repeat(2000);
        RegistroAuditoria registro = RegistroAuditoria.de(new Acao(null, " ", "A".repeat(300), null, null,
                Resultado.FALHA, 422, longo, longo), AGORA);

        assertThat(registro.getAtorNome()).isNull();
        assertThat(registro.getAcao()).hasSize(120);
        assertThat(registro.getDetalhe()).hasSize(500);
        assertThat(registro.getJustificativa()).hasSize(1000);
        assertThat(RegistroAuditoria.cortar("  texto  ", 10)).isEqualTo("texto");
    }
}
