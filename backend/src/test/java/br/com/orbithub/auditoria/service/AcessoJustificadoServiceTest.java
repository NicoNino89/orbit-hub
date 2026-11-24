package br.com.orbithub.auditoria.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Acao;
import br.com.orbithub.auditoria.domain.RegistroAuditoria.Resultado;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.auditoria.service.AcessoJustificadoService.CasoAberto;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:30
 * Cartão Jira: KAN-80
 * Título: Testes do acesso justificado
 * Objetivo: Garantir que a equipe só vê dados de Nível 2 dentro de um caso aberto, com justificativa, e que o
 *           acesso fica registrado e aparece para a startup como "equipe Orbit Hub".
 * Decisões: Mockito na trilha e no repositório; uma regra de caso de teste com um caso aberto conhecido.
 */
class AcessoJustificadoServiceTest {

    static final UUID STARTUP = UUID.randomUUID();
    static final UUID CASO_ABERTO = UUID.randomUUID();
    static final String JUSTIFICATIVA = "Denúncia de dados falsos no pitch, conferir os documentos enviados";

    TrilhaDeAuditoria trilha;
    RegistroAuditoriaRepository registros;
    AcessoJustificadoService servico;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Preparação
     * Objetivo: Montar o serviço com uma regra de denúncia que conhece um caso aberto.
     * Decisões: A trilha devolve o registro que recebeu.
     */
    @BeforeEach
    void preparar() {
        trilha = mock(TrilhaDeAuditoria.class);
        registros = mock(RegistroAuditoriaRepository.class);
        when(trilha.registrar(any())).thenAnswer(i -> RegistroAuditoria.de(i.getArgument(0), Instant.now()));
        CasoAberto denuncias = new CasoAberto() {
            @Override
            public String tipo() {
                return "denuncia";
            }

            @Override
            public boolean abertoEnvolvendo(UUID casoId, UUID startupId) {
                return casoId.equals(CASO_ABERTO) && startupId.equals(STARTUP);
            }
        };
        servico = new AcessoJustificadoService(List.of(denuncias), trilha, registros);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Acesso dentro do caso
     * Objetivo: Conferir que o acesso válido é gravado com caso, startup e justificativa.
     * Decisões: O autor real fica no registro.
     */
    @Test
    void registraOAcessoDentroDeUmCasoAberto() {
        UUID analista = UUID.randomUUID();
        servico.exigir(analista, "denuncia", CASO_ABERTO, STARTUP, JUSTIFICATIVA);

        ArgumentCaptor<Acao> acao = ArgumentCaptor.forClass(Acao.class);
        verify(trilha).registrar(acao.capture());
        assertThat(acao.getValue().atorId()).isEqualTo(analista);
        assertThat(acao.getValue().acao()).isEqualTo(AcessoJustificadoService.ACAO);
        assertThat(acao.getValue().objetoId()).isEqualTo(STARTUP.toString());
        assertThat(acao.getValue().justificativa()).isEqualTo(JUSTIFICATIVA);
        assertThat(acao.getValue().detalhe()).contains("denuncia").contains(CASO_ABERTO.toString());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Justificativa obrigatória
     * Objetivo: Recusar justificativa vazia ou curta demais.
     * Decisões: Nada é gravado quando o acesso é recusado.
     */
    @Test
    void recusaSemJustificativaSuficiente() {
        assertThatThrownBy(() -> servico.exigir(UUID.randomUUID(), "denuncia", CASO_ABERTO, STARTUP, "ok"))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting("codigo").isEqualTo("justificativa_obrigatoria");
        assertThatThrownBy(() -> servico.exigir(UUID.randomUUID(), "denuncia", CASO_ABERTO, STARTUP, null))
                .isInstanceOf(RegraDeNegocioException.class);
        verify(trilha, never()).registrar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Só dentro de caso aberto
     * Objetivo: Recusar tipo de caso desconhecido, caso inexistente ou caso sobre outra startup.
     * Decisões: Negar por padrão.
     */
    @Test
    void recusaForaDeUmCasoAberto() {
        UUID ator = UUID.randomUUID();
        assertThatThrownBy(() -> servico.exigir(ator, "disputa", CASO_ABERTO, STARTUP, JUSTIFICATIVA))
                .extracting("codigo").isEqualTo("caso_nao_aberto");
        assertThatThrownBy(() -> servico.exigir(ator, "denuncia", UUID.randomUUID(), STARTUP, JUSTIFICATIVA))
                .extracting("codigo").isEqualTo("caso_nao_aberto");
        assertThatThrownBy(() -> servico.exigir(ator, "denuncia", CASO_ABERTO, UUID.randomUUID(), JUSTIFICATIVA))
                .extracting("codigo").isEqualTo("caso_nao_aberto");
        assertThatThrownBy(() -> servico.exigir(ator, "denuncia", null, STARTUP, JUSTIFICATIVA))
                .extracting("codigo").isEqualTo("caso_nao_aberto");
        verify(trilha, never()).registrar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:30
     * Cartão Jira: KAN-80
     * Título: Histórico da startup sem nome da equipe
     * Objetivo: Conferir que os acessos internos aparecem como "equipe Orbit Hub".
     * Decisões: O motivo (caso) aparece; o nome e o id da pessoa, não.
     */
    @Test
    void mostraOsAcessosComoEquipeOrbitHub() {
        RegistroAuditoria registro = RegistroAuditoria.de(new Acao(UUID.randomUUID(), "Carla Analista",
                AcessoJustificadoService.ACAO, "startup", STARTUP.toString(), Resultado.SUCESSO, null,
                "Caso denuncia 1", JUSTIFICATIVA), Instant.parse("2026-11-24T13:00:00Z"));
        when(registros.findByAcaoAndObjetoTipoAndObjetoIdAndResultadoOrderByOcorridoEmDesc(
                AcessoJustificadoService.ACAO, "startup", STARTUP.toString(), Resultado.SUCESSO))
                .thenReturn(List.of(registro));

        assertThat(servico.acessosInternos(STARTUP)).singleElement().satisfies(a -> {
            assertThat(a.autor()).isEqualTo("equipe Orbit Hub");
            assertThat(a.motivo()).isEqualTo("Caso denuncia 1");
            assertThat(a.toString()).doesNotContain("Carla");
        });
    }
}
