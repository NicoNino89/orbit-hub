package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.compartilhado.rotinas.RotinaAgendada;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.notificacoes.domain.TipoDeNotificacao;
import br.com.orbithub.notificacoes.service.NotificacoesService;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 13:28
 * Cartão Jira: KAN-146
 * Título: Testes do lembrete de revisão de acessos (R16)
 * Objetivo: Conferir que a rotina só lembra quando a revisão está vencida, e que o lembrete vai para os
 *           administradores ativos pela central de notificações.
 * Decisões: Situação da revisão, contas e notificações simuladas.
 */
class LembreteDeRevisaoDeAcessosTest {

    static final Instant AGORA = Instant.parse("2026-11-24T15:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 13:28
     * Cartão Jira: KAN-146
     * Título: Em dia e vencida
     * Objetivo: Em dia, nenhum item e nenhum aviso; vencida, um lembrete aos administradores.
     * Decisões: Sem erro nos dois casos.
     */
    @Test
    void lembraSoQuandoVencida() {
        EquipeService equipe = mock(EquipeService.class);
        ConsultaDeContas contas = mock(ConsultaDeContas.class);
        NotificacoesService notificacoes = mock(NotificacoesService.class);
        List<UUID> admins = List.of(UUID.randomUUID());
        when(contas.equipeAtivaComPapeis(Set.of(Papel.ADMIN))).thenReturn(admins);
        LembreteDeRevisaoDeAcessos rotina = new LembreteDeRevisaoDeAcessos(equipe, contas, notificacoes);
        assertThat(rotina.codigo()).isEqualTo("R16");

        when(equipe.situacaoRevisao()).thenReturn(new EquipeService.SituacaoRevisao(AGORA, "Bia", AGORA.plusSeconds(60),
                false, 3));
        RotinaAgendada.Resultado emDia = rotina.executar(AGORA);
        assertThat(emDia.itens()).isZero();
        verify(notificacoes, never()).notificarTodos(any(), any(), anyString(), anyString(), anyString());

        when(equipe.situacaoRevisao()).thenReturn(new EquipeService.SituacaoRevisao(null, null, AGORA.minusSeconds(60),
                true, 3));
        RotinaAgendada.Resultado vencida = rotina.executar(AGORA);
        assertThat(vencida.itens()).isEqualTo(1);
        assertThat(vencida.erros()).isZero();
        verify(notificacoes).notificarTodos(eq(admins), eq(TipoDeNotificacao.EQUIPE_REVISAO_ACESSOS), anyString(),
                anyString(), eq("/admin/equipe"));
    }
}
