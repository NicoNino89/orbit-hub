package br.com.orbithub.conexoes.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-49
 * Título: Testes do ciclo da conexão
 * Objetivo: Conferir as transições válidas e inválidas do diagrama, os estados finais e a chave do investidor.
 * Decisões: Sem banco.
 */
class ConexaoTest {

    static final Instant AGORA = Instant.parse("2026-11-25T14:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Caminho completo e recusas
     * Objetivo: Pendente → Match → análise → negociação → investiu; pular etapas é recusado.
     * Decisões: Investiu é final e não negativo.
     */
    @Test
    void caminhoETransicoesInvalidas() {
        UUID pessoa = UUID.randomUUID();
        UUID org = UUID.randomUUID();
        Conexao c = Conexao.nova(UUID.randomUUID(), pessoa, org, new Conexao.Interesse(null, null, null, null), AGORA);
        assertThat(c.getInvestidorChave()).isEqualTo(org);
        assertThat(c.getPedido()).isEqualTo(Conexao.Pedido.NENHUM);
        assertThatThrownBy(() -> c.mudar(Conexao.Situacao.EM_NEGOCIACAO, AGORA)).isInstanceOf(RegraDeNegocioException.class)
                .hasMessageContaining("pendente");
        c.mudar(Conexao.Situacao.MATCH, AGORA);
        c.mudar(Conexao.Situacao.EM_ANALISE, AGORA);
        c.mudar(Conexao.Situacao.EM_NEGOCIACAO, AGORA);
        assertThat(c.mudar(Conexao.Situacao.INVESTIU, AGORA)).isEqualTo(Conexao.Situacao.EM_NEGOCIACAO);
        assertThat(c.getSituacao().finalizada()).isTrue();
        assertThat(c.getSituacao().negativa()).isFalse();
        assertThatThrownBy(() -> c.encerrar("OUTRO", Conexao.Lado.INVESTIDOR, AGORA)).isInstanceOf(RegraDeNegocioException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48, KAN-49
     * Título: Recusa e encerramento
     * Objetivo: Guardar o motivo e o lado; finais negativos liberam nova tentativa.
     * Decisões: Sem organização, a chave é a pessoa.
     */
    @Test
    void recusaEEncerramento() {
        UUID pessoa = UUID.randomUUID();
        Conexao r = Conexao.nova(UUID.randomUUID(), pessoa, null, new Conexao.Interesse(null, null, Conexao.Pedido.INFORMACOES, "Oi"), AGORA);
        assertThat(r.getInvestidorChave()).isEqualTo(pessoa);
        r.recusar("TICKET_INCOMPATIVEL", AGORA);
        assertThat(r.getMotivoLado()).isEqualTo(Conexao.Lado.STARTUP);
        assertThat(r.getSituacao().negativa()).isTrue();
        Conexao e = Conexao.nova(UUID.randomUUID(), pessoa, null, new Conexao.Interesse(null, null, null, null), AGORA);
        e.mudar(Conexao.Situacao.MATCH, AGORA);
        e.encerrar("OUTRO", Conexao.Lado.INVESTIDOR, AGORA);
        assertThat(e.getMotivo()).isEqualTo("OUTRO");
        assertThat(Conexao.rotulo(Conexao.Situacao.MATCH)).isEqualTo("em Match");
        e.registrarLembrete();
        assertThat(e.getLembretes()).isEqualTo(1);
    }
}
