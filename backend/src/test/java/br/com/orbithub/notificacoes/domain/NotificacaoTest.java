package br.com.orbithub.notificacoes.domain;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Instant;
import java.util.Arrays;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:36
 * Cartão Jira: KAN-146
 * Título: Testes da notificação
 * Objetivo: Conferir que o link do aviso só aceita caminho interno, que os textos são cortados no tamanho das colunas
 *           e que a leitura guarda a primeira data.
 * Decisões: Sem banco.
 */
class NotificacaoTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Link só interno
     * Objetivo: Garantir que um link para outro site nunca vai para o aviso (o e-mail e o sino o abririam).
     * Decisões: "//site" também é externo.
     */
    @Test
    void linkSoInterno() {
        assertThat(nova("/investidor").getLink()).isEqualTo("/investidor");
        assertThat(nova("https://exemplo.com").getLink()).isNull();
        assertThat(nova("//exemplo.com").getLink()).isNull();
        assertThat(nova(null).getLink()).isNull();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Textos cortados e leitura
     * Objetivo: Conferir o corte do título e da mensagem e que marcar duas vezes mantém a primeira leitura.
     * Decisões: 160 e 1000 caracteres.
     */
    @Test
    void textosCortadosELeituraIdempotente() {
        Notificacao n = Notificacao.nova(UUID.randomUUID(), TipoDeNotificacao.MATCH, "t".repeat(300), "m".repeat(2000),
                null, true, AGORA);
        assertThat(n.getTitulo()).hasSize(160);
        assertThat(n.getMensagem()).hasSize(1000);
        n.marcarLida(AGORA.plusSeconds(10));
        n.marcarLida(AGORA.plusSeconds(99));
        assertThat(n.getLidaEm()).isEqualTo(AGORA.plusSeconds(10));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Obrigatórios
     * Objetivo: Conferir que os avisos de segurança, termos, prazo de conexão e da equipe são obrigatórios e chegam
     *           pelos dois canais.
     * Decisões: Lista fechada.
     */
    @Test
    void obrigatorios() {
        assertThat(Arrays.stream(TipoDeNotificacao.values()).filter(TipoDeNotificacao::isObrigatoria))
                .containsExactlyInAnyOrder(TipoDeNotificacao.PRAZO_CONEXAO, TipoDeNotificacao.TERMOS_ATUALIZADOS,
                        TipoDeNotificacao.SEGURANCA_CONTA, TipoDeNotificacao.EQUIPE_APROVACAO,
                        TipoDeNotificacao.EQUIPE_REVISAO_ACESSOS)
                .allSatisfy(t -> assertThat(t.getPadrao()).isEqualTo(TipoDeNotificacao.Canal.AMBOS));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:36
     * Cartão Jira: KAN-146
     * Título: Aviso de teste
     * Objetivo: Criar um aviso com o link informado.
     * Decisões: Tipo qualquer.
     */
    private static Notificacao nova(String link) {
        return Notificacao.nova(UUID.randomUUID(), TipoDeNotificacao.MATCH, "Título", "Mensagem", link, true, AGORA);
    }
}
