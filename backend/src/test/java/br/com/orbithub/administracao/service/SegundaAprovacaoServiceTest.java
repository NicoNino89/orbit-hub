package br.com.orbithub.administracao.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao.Situacao;
import br.com.orbithub.administracao.repository.PropostaDeAprovacaoRepository;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Testes da segunda aprovação
 * Objetivo: Garantir que nada muda até a segunda pessoa aprovar, que o executor só roda na aprovação, as recusas
 *           (própria proposta, papel errado, ação indisponível, proposta repetida) e o alerta de vencidas.
 * Decisões: Executor de teste que conta as execuções e pode recusar; repositório simulado.
 */
class SegundaAprovacaoServiceTest {

    static final Instant AGORA = Instant.parse("2026-11-24T14:00:00Z");
    static final UUID BIA = UUID.randomUUID();
    static final UUID CAIO = UUID.randomUUID();

    PropostaDeAprovacaoRepository repositorio;
    ExecutorDeTeste executor;
    RelogioAjustavel relogio;
    SegundaAprovacaoService servico;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Preparação
     * Objetivo: Montar o serviço com um executor de parâmetro sensível aprovado por administrador.
     * Decisões: Prazo de 48 h.
     */
    @BeforeEach
    void preparar() {
        repositorio = mock(PropostaDeAprovacaoRepository.class);
        when(repositorio.save(any())).thenAnswer(i -> i.getArgument(0));
        executor = new ExecutorDeTeste();
        relogio = new RelogioAjustavel(AGORA);
        servico = new SegundaAprovacaoService(repositorio, List.of(executor),
                new SegundaAprovacaoService.Parametros(Duration.ofHours(48)), relogio);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Nada muda até aprovar
     * Objetivo: Conferir que a proposta não executa nada, que a aprovação executa uma vez com os dados da
     *           proposta e que a rejeição não executa.
     * Decisões: O executor de teste guarda os dados recebidos.
     */
    @Test
    void executaSoNaAprovacao() {
        PropostaDeAprovacao p = propor("nivel2.validade_dias");
        assertThat(executor.executadas).isEmpty();
        when(repositorio.findById(p.getId())).thenReturn(Optional.of(p));

        servico.aprovar(p.getId(), CAIO, Set.of(Papel.ADMIN), "Conferido");
        assertThat(executor.executadas).containsExactly("60");
        assertThat(p.getSituacao()).isEqualTo(Situacao.APROVADA);

        PropostaDeAprovacao outra = propor("lgpd.prazo_resposta_dias");
        when(repositorio.findById(outra.getId())).thenReturn(Optional.of(outra));
        servico.rejeitar(outra.getId(), CAIO, Set.of(Papel.ADMIN), "Prazo legal não permite");
        assertThat(executor.executadas).hasSize(1);
        assertThat(outra.getSituacao()).isEqualTo(Situacao.REJEITADA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Recusas na decisão
     * Objetivo: Conferir as recusas: própria proposta, papel sem permissão, sem justificativa e já decidida.
     * Decisões: Nenhuma recusa executa a ação.
     */
    @Test
    void recusaDecisoesInvalidas() {
        PropostaDeAprovacao p = propor("nivel2.validade_dias");
        when(repositorio.findById(p.getId())).thenReturn(Optional.of(p));
        assertThatThrownBy(() -> servico.aprovar(p.getId(), BIA, Set.of(Papel.ADMIN), "Eu mesma"))
                .extracting("codigo").isEqualTo("autoaprovacao");
        assertThatThrownBy(() -> servico.aprovar(p.getId(), CAIO, Set.of(Papel.CURADOR), "Ok"))
                .extracting("codigo").isEqualTo("papel_insuficiente");
        assertThatThrownBy(() -> servico.aprovar(p.getId(), CAIO, Set.of(Papel.ADMIN), " "))
                .extracting("codigo").isEqualTo("justificativa_obrigatoria");
        assertThat(servico.podeDecidir(p, CAIO, Set.of(Papel.ADMIN))).isTrue();
        assertThat(servico.podeDecidir(p, BIA, Set.of(Papel.ADMIN))).isFalse();
        assertThat(executor.executadas).isEmpty();

        servico.cancelar(p.getId(), BIA);
        assertThatThrownBy(() -> servico.aprovar(p.getId(), CAIO, Set.of(Papel.ADMIN), "Ok"))
                .extracting("codigo").isEqualTo("proposta_decidida");
        assertThat(servico.podeDecidir(p, CAIO, Set.of(Papel.ADMIN))).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Executor recusa na aprovação
     * Objetivo: Conferir que, se a ação não puder ser aplicada, a proposta continua pendente.
     * Decisões: O executor de teste recusa quando pedido.
     */
    @Test
    void falhaDoExecutorMantemPendente() {
        PropostaDeAprovacao p = propor("nivel2.validade_dias");
        when(repositorio.findById(p.getId())).thenReturn(Optional.of(p));
        executor.recusar = true;
        assertThatThrownBy(() -> servico.aprovar(p.getId(), CAIO, Set.of(Papel.ADMIN), "Ok"))
                .isInstanceOf(RegraDeNegocioException.class).extracting("codigo").isEqualTo("valor_fora_dos_limites");
        assertThat(p.getSituacao()).isEqualTo(Situacao.PENDENTE);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Recusas na proposta
     * Objetivo: Conferir que ação sem executor, proposta repetida e proposta sem justificativa são recusadas.
     * Decisões: A conferência do executor também roda na proposta.
     */
    @Test
    void recusaPropostasInvalidas() {
        assertThatThrownBy(() -> servico.propor(AcaoGrave.BANIMENTO_DEFINITIVO, "usuario", "u1", "Banir", Map.of(),
                BIA, "Fraude")).extracting("codigo").isEqualTo("acao_indisponivel");
        assertThatThrownBy(() -> servico.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro", "p", "r", Map.of("valor", "1"),
                BIA, "")).extracting("codigo").isEqualTo("justificativa_obrigatoria");
        when(repositorio.existsByTipoAndObjetoTipoAndObjetoIdAndSituacao(AcaoGrave.PARAMETRO_SENSIVEL, "parametro",
                "repetido", Situacao.PENDENTE)).thenReturn(true);
        assertThatThrownBy(() -> servico.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro", "repetido", "r",
                Map.of("valor", "1"), BIA, "Motivo")).extracting("codigo").isEqualTo("proposta_pendente");
        executor.recusar = true;
        assertThatThrownBy(() -> propor("x")).extracting("codigo").isEqualTo("valor_fora_dos_limites");
        assertThat(servico.acoesDisponiveis()).containsExactly(AcaoGrave.PARAMETRO_SENSIVEL);
        assertThatThrownBy(() -> new SegundaAprovacaoService(repositorio, List.of(executor, new ExecutorDeTeste()),
                new SegundaAprovacaoService.Parametros(Duration.ofHours(48)), relogio))
                .isInstanceOf(IllegalStateException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Alerta das vencidas
     * Objetivo: Conferir que as vencidas sem alerta são marcadas e que a consulta usa agora menos o prazo.
     * Decisões: O repositório devolve a proposta como vencida.
     */
    @Test
    void marcaAsVencidasParaOAlerta() {
        PropostaDeAprovacao p = propor("nivel2.validade_dias");
        relogio.avancar(Duration.ofHours(49));
        when(repositorio.pendentesVencidasSemAlerta(AGORA.plus(Duration.ofHours(1)))).thenReturn(List.of(p));
        assertThat(servico.vencida(p)).isTrue();
        assertThat(servico.vencidasParaAlertar()).containsExactly(p);
        assertThat(p.getAlertadaEm()).isEqualTo(AGORA.plus(Duration.ofHours(49)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta de teste
     * Objetivo: Propor a troca de um parâmetro sensível para 60.
     * Decisões: A Bia propõe.
     */
    private PropostaDeAprovacao propor(String chave) {
        return servico.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro", chave, chave + ": 90 → 60",
                Map.of("valor", "60"), BIA, "Pedido do jurídico");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Executor de teste
     * Objetivo: Simular o executor de parâmetros sensíveis.
     * Decisões: Guarda o valor executado e recusa quando "recusar" está ligado.
     */
    static class ExecutorDeTeste implements ExecutorDeAcaoGrave {

        final List<String> executadas = new ArrayList<>();
        boolean recusar;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Ação do executor de teste
         * Objetivo: Atender parâmetro sensível.
         * Decisões: Igual ao executor real da KAN-78.
         */
        @Override
        public AcaoGrave acao() {
            return AcaoGrave.PARAMETRO_SENSIVEL;
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Papéis que aprovam no teste
         * Objetivo: Aceitar só administrador.
         * Decisões: Igual ao executor real da KAN-78.
         */
        @Override
        public Set<Papel> papeisQueAprovam() {
            return Set.of(Papel.ADMIN);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Conferência de teste
         * Objetivo: Recusar quando o teste pede.
         * Decisões: Simula valor fora dos limites.
         */
        @Override
        public void conferir(Map<String, String> dados) {
            if (recusar) {
                throw new RegraDeNegocioException("valor_fora_dos_limites", "Fora dos limites.");
            }
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 10:52
         * Cartão Jira: KAN-81
         * Título: Execução de teste
         * Objetivo: Guardar o valor aplicado.
         * Decisões: Confere de novo antes, como o executor real.
         */
        @Override
        public void executar(PropostaDeAprovacao proposta) {
            conferir(proposta.getDados());
            executadas.add(proposta.getDados().get("valor"));
        }
    }
}
