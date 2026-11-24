package br.com.orbithub.identidade.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Instant;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Testes das regras da conta da equipe
 * Objetivo: Garantir a separação entre conta interna e pública, o convite, a troca de papéis, o desligamento e
 *           o ciclo do segundo fator.
 * Decisões: Teste de unidade da entidade, sem Spring.
 */
class ContaDaEquipeTest {

    static final Instant AGORA = Instant.parse("2026-11-24T14:00:00Z");

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Papéis internos e públicos não se misturam
     * Objetivo: Conferir que a conta com papel interno nasce interna e que misturar papéis é recusado.
     * Decisões: Conta pública continua pública.
     */
    @Test
    void separaContaInternaDeContaPublica() {
        assertThat(Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA).isInterna()).isFalse();
        assertThat(Usuario.novo("Bia", "bia@orbithub.com", "h", Set.of(Papel.CURADOR), AGORA).isInterna()).isTrue();
        assertThatThrownBy(() -> Usuario.novo("Caio", "c@x.com", "h", Set.of(Papel.ADMIN, Papel.INVESTIDOR), AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> Usuario.novoInterno("Caio", "c@x.com", "h", Set.of(Papel.EMPREENDEDOR), AGORA))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(Papel.MODERADOR.interno()).isTrue();
        assertThat(Papel.INVESTIDOR.interno()).isFalse();

        Usuario bia = Usuario.novoInterno("Bia", "bia@orbithub.com", "h", Set.of(Papel.SUPORTE_N1), AGORA);
        assertThatThrownBy(() -> bia.ativarPapel(Papel.INVESTIDOR, AGORA)).isInstanceOf(IllegalStateException.class);
        Usuario ana = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA);
        assertThatThrownBy(() -> ana.definirPapeisInternos(Set.of(Papel.ADMIN), AGORA))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Convite e troca de papéis
     * Objetivo: Conferir que o convite ativa a conta uma vez e que a troca devolve os papéis anteriores.
     * Decisões: Conjunto vazio ou com papel público é recusado.
     */
    @Test
    void aceitaConviteUmaVezETrocaPapeis() {
        Usuario bia = Usuario.novoInterno("Bia", "bia@orbithub.com", "h0", Set.of(Papel.CURADOR), AGORA);
        assertThat(bia.getStatus()).isEqualTo(StatusUsuario.PENDENTE_CONFIRMACAO);
        bia.aceitarConvite("h1", AGORA);
        assertThat(bia.getStatus()).isEqualTo(StatusUsuario.ATIVO);
        assertThat(bia.getSenhaHash()).isEqualTo("h1");
        assertThatThrownBy(() -> bia.aceitarConvite("h2", AGORA)).isInstanceOf(IllegalStateException.class);

        assertThat(bia.definirPapeisInternos(Set.of(Papel.CURADOR, Papel.MODERADOR), AGORA))
                .containsExactly(Papel.CURADOR);
        assertThat(bia.getPapeis()).containsExactlyInAnyOrder(Papel.CURADOR, Papel.MODERADOR);
        assertThatThrownBy(() -> bia.definirPapeisInternos(Set.of(), AGORA)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> bia.definirPapeisInternos(Set.of(Papel.INVESTIDOR), AGORA))
                .isInstanceOf(IllegalArgumentException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Desligamento
     * Objetivo: Conferir que o desligamento impede a sessão, guarda a data, apaga o segundo fator e é idempotente.
     * Decisões: Conta pública não é "desligada".
     */
    @Test
    void desligaUmaVezERevogaOAcesso() {
        Usuario bia = Usuario.novoInterno("Bia", "bia@orbithub.com", "h", Set.of(Papel.MODERADOR), AGORA);
        bia.aceitarConvite("h1", AGORA);
        bia.iniciarSegundoFator("v1:x", AGORA);
        bia.concluirSegundoFator(10, AGORA);
        bia.desligar(AGORA);
        assertThat(bia.podeTerSessao()).isFalse();
        assertThat(bia.getDesligadoEm()).isEqualTo(AGORA);
        assertThat(bia.temSegundoFator()).isFalse();
        bia.desligar(AGORA.plusSeconds(60));
        assertThat(bia.getDesligadoEm()).isEqualTo(AGORA);
        assertThat(bia.getPapeis()).containsExactly(Papel.MODERADOR);

        Usuario ana = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), AGORA);
        assertThatThrownBy(() -> ana.desligar(AGORA)).isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Ciclo do segundo fator
     * Objetivo: Conferir configuração pendente, conclusão, bloqueio de reuso de código e redefinição.
     * Decisões: Concluir sem configuração em andamento é erro.
     */
    @Test
    void conduzOSegundoFatorEImpedeReuso() {
        Usuario bia = Usuario.novoInterno("Bia", "bia@orbithub.com", "h", Set.of(Papel.ADMIN), AGORA);
        assertThat(bia.temSegundoFator()).isFalse();
        assertThatThrownBy(() -> bia.concluirSegundoFator(1, AGORA)).isInstanceOf(IllegalStateException.class);

        bia.iniciarSegundoFator("v1:pendente", AGORA);
        assertThat(bia.temSegundoFator()).isFalse();
        assertThat(bia.getTotpPendente()).isEqualTo("v1:pendente");
        bia.concluirSegundoFator(100, AGORA);
        assertThat(bia.temSegundoFator()).isTrue();
        assertThat(bia.getTotpSegredo()).isEqualTo("v1:pendente");
        assertThat(bia.getTotpPendente()).isNull();

        assertThat(bia.usarPassoDoCodigo(100)).isFalse();
        assertThat(bia.usarPassoDoCodigo(99)).isFalse();
        assertThat(bia.usarPassoDoCodigo(101)).isTrue();
        assertThat(bia.usarPassoDoCodigo(101)).isFalse();

        bia.redefinirSegundoFator(AGORA);
        assertThat(bia.temSegundoFator()).isFalse();
        assertThat(bia.usarPassoDoCodigo(1)).isTrue();
    }
}
