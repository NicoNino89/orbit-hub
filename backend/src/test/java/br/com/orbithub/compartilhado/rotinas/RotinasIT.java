package br.com.orbithub.compartilhado.rotinas;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.AcaoGrave;
import br.com.orbithub.administracao.domain.PropostaDeAprovacao;
import br.com.orbithub.administracao.repository.PropostaDeAprovacaoRepository;
import br.com.orbithub.auditoria.domain.RegistroAuditoria;
import br.com.orbithub.auditoria.repository.RegistroAuditoriaRepository;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.annotation.Primary;
import org.springframework.http.HttpHeaders;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Testes de integração das rotinas
 * Objetivo: Conferir, com PostgreSQL real, a trava por ciclo, o registro de sucesso e de falha, a pausa, a execução
 *           manual pelo painel (com trilha) e as rotinas R12 (limpeza) e R15 (propostas paradas).
 * Decisões: Relógio parado neste contexto, para o ciclo ser o mesmo nas duas tentativas; duas rotinas de teste (uma
 *           que conta e outra que falha) registradas só aqui.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, RotinasIT.RotinasDeTeste.class})
class RotinasIT {

    static final AtomicInteger EXECUCOES = new AtomicInteger();

    @Autowired
    MockMvc mvc;

    @Autowired
    ExecutorDeRotinas executor;

    @Autowired
    ExecucaoDeRotinaRepository execucoes;

    @Autowired
    JdbcTemplate jdbc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    RegistroAuditoriaRepository registros;

    @Autowired
    PropostaDeAprovacaoRepository propostas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Trava, falha e pausa
     * Objetivo: Conferir que o mesmo ciclo só roda uma vez, que a falha fica registrada e que a pausa impede o automático.
     * Decisões: Rotinas de teste R90 (conta) e R91 (falha).
     */
    @Test
    void cicloRodaUmaVezFalhaFicaRegistradaEPausaImpede() {
        int antes = EXECUCOES.get();
        assertThat(executor.executarAgendada("R90")).isPresent();
        assertThat(executor.executarAgendada("R90")).isEmpty();
        assertThat(EXECUCOES.get()).isEqualTo(antes + 1);
        ExecucaoDeRotina ultima = execucoes.findFirstByRotinaOrderByIniciadaEmDesc("R90").orElseThrow();
        assertThat(ultima.getResultado()).isEqualTo("SUCESSO");
        assertThat(ultima.getItens()).isEqualTo(3);
        assertThat(ultima.getTerminadaEm()).isNotNull();

        executor.executarAgendada("R91");
        ExecucaoDeRotina falha = execucoes.findFirstByRotinaOrderByIniciadaEmDesc("R91").orElseThrow();
        assertThat(falha.getResultado()).isEqualTo("FALHA");
        assertThat(falha.getMensagem()).isEqualTo("Erro: IllegalStateException");

        executor.definirPausa("R91", true, null);
        assertThat(executor.pausada("R91")).isTrue();
        assertThat(executor.executarAgendada("R91")).isEmpty();
        executor.definirPausa("R91", false, null);
        assertThat(executor.pausada("R91")).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Painel e execução manual
     * Objetivo: Conferir a lista do painel, a execução manual (mesmo pausada), o histórico e a trilha.
     * Decisões: Curador não acessa o painel.
     */
    @Test
    void painelExecutaManualmenteComRegistroNaTrilha() throws Exception {
        Usuario admin = membro(Papel.ADMIN);
        String bearer = bearer(admin);
        mvc.perform(get("/api/v1/admin/rotinas").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[?(@.codigo == 'R12')].modulo").value("identidade"))
                .andExpect(jsonPath("$[?(@.codigo == 'R15')].parametros[0]").value("aprovacao.prazo_horas"))
                .andExpect(jsonPath("$[?(@.codigo == 'R16')].proximaExecucao").isNotEmpty());

        mvc.perform(post("/api/v1/admin/rotinas/R90/pausa").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        int antes = EXECUCOES.get();
        mvc.perform(post("/api/v1/admin/rotinas/R90/execucao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.itens").value(3));
        assertThat(EXECUCOES.get()).isEqualTo(antes + 1);
        mvc.perform(post("/api/v1/admin/rotinas/R90/retomada").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/admin/rotinas/R90/execucoes").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(jsonPath("$[*].disparo").value(org.hamcrest.Matchers.hasItem("MANUAL")));
        mvc.perform(post("/api/v1/admin/rotinas/R99/execucao").header(HttpHeaders.AUTHORIZATION, bearer))
                .andExpect(status().isNotFound());
        assertThat(registros.findByAtorIdOrderByOcorridoEmDesc(admin.getId())).extracting(RegistroAuditoria::getAcao)
                .contains("ROTINA_PAUSADA", "ROTINA_EXECUTADA_MANUALMENTE", "ROTINA_RETOMADA");

        mvc.perform(get("/api/v1/admin/rotinas").header(HttpHeaders.AUTHORIZATION, bearer(membro(Papel.CURADOR))))
                .andExpect(status().isForbidden());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: R12 e R15 de verdade
     * Objetivo: Conferir que a R12 apaga só links vencidos além da retenção e que a R15 alerta cada proposta parada uma
     *           única vez.
     * Decisões: Dados gravados direto no banco com datas antigas.
     */
    @Test
    void limpezaEAlertaDePropostas() {
        Usuario admin = membro(Papel.ADMIN);
        UUID velho = UUID.randomUUID();
        UUID novo = UUID.randomUUID();
        Instant agora = Instant.now();
        String insert = "INSERT INTO token_conta (id, usuario_id, finalidade, hash, criado_em, expira_em) VALUES (?, ?, 'TROCA_EMAIL', ?, ?, ?)";
        jdbc.update(insert, velho, admin.getId(), "h-" + velho, Timestamp.from(agora.minus(Duration.ofDays(60))),
                Timestamp.from(agora.minus(Duration.ofDays(40))));
        jdbc.update(insert, novo, admin.getId(), "h-" + novo, Timestamp.from(agora.minus(Duration.ofDays(2))),
                Timestamp.from(agora.minus(Duration.ofDays(1))));
        executor.executarManual("R12", admin.getId());
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_conta WHERE id = ?", Integer.class, velho)).isZero();
        assertThat(jdbc.queryForObject("SELECT count(*) FROM token_conta WHERE id = ?", Integer.class, novo)).isEqualTo(1);

        Usuario outro = membro(Papel.ADMIN);
        PropostaDeAprovacao parada = propostas.save(PropostaDeAprovacao.propor(AcaoGrave.PARAMETRO_SENSIVEL, "parametro",
                "teste-" + UUID.randomUUID(), "Parada", Map.of(), outro.getId(), "Teste", agora.minus(Duration.ofDays(3))));
        RotinaAgendada.Resultado primeira = executor.executarManual("R15", admin.getId());
        assertThat(primeira.itens()).isGreaterThanOrEqualTo(1);
        assertThat(propostas.findById(parada.getId()).orElseThrow().getAlertadaEm()).isNotNull();
        assertThat(executor.executarManual("R15", admin.getId()).itens()).isZero();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Membro de teste
     * Objetivo: Criar um membro ativo da equipe.
     * Decisões: E-mail aleatório.
     */
    private Usuario membro(Papel papel) {
        Usuario membro = Usuario.novoInterno("Membro", "r-" + UUID.randomUUID() + "@orbithub.com", "{noop}x",
                Set.of(papel), Instant.now());
        membro.aceitarConvite("{noop}x", Instant.now());
        return usuarios.save(membro);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Token da conta
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Authorization.
     * Decisões: Direto pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Rotinas e relógio de teste
     * Objetivo: Registrar uma rotina que conta, uma que falha e um relógio parado.
     * Decisões: Relógio parado no instante da subida do contexto.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RotinasDeTeste {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:17
         * Cartão Jira: KAN-104
         * Título: Relógio parado
         * Objetivo: Manter o mesmo ciclo nas duas tentativas.
         * Decisões: Começa agora, para os tokens continuarem válidos.
         */
        @Bean
        @Primary
        Clock relogioDeTeste() {
            return new RelogioAjustavel(Instant.now());
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:17
         * Cartão Jira: KAN-104
         * Título: Rotina que conta
         * Objetivo: Contar as execuções e tratar 3 itens.
         * Decisões: Código R90, fora da faixa real.
         */
        @Bean
        RotinaAgendada contadora() {
            return new RotinaBase("R90", "Conta", "0 0 4 * * *", "teste", List.of()) {
                @Override
                public Resultado executar(Instant agora) {
                    EXECUCOES.incrementAndGet();
                    return new Resultado(3, 0, "ok");
                }
            };
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 12:17
         * Cartão Jira: KAN-104
         * Título: Rotina que falha
         * Objetivo: Lançar exceção para conferir o registro de falha.
         * Decisões: Código R91.
         */
        @Bean
        RotinaAgendada falhadora() {
            return new RotinaBase("R91", "Falha", "0 0 4 * * *", "teste", List.of()) {
                @Override
                public Resultado executar(Instant agora) {
                    throw new IllegalStateException("falha de teste");
                }
            };
        }
    }
}
