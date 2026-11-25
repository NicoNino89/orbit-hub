package br.com.orbithub.compartilhado.seguranca;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import java.time.Instant;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Teste de integração das permissões por perfil
 * Objetivo: Verificar, com API e banco reais, que investidor não acessa função de empreendedor e
 *           vice-versa, que só o dono acessa o próprio recurso e que as tentativas recusadas ficam registradas.
 * Decisões: Um controller e uma regra de dono só de teste simulam o "editar a própria startup" da S4.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import({TestcontainersConfiguration.class, PermissoesIT.RecursoDeTeste.class})
class PermissoesIT {

    static final UUID STARTUP_DA_ANA = UUID.randomUUID();

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    AcessoNegadoRepository negados;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Papéis separados
     * Objetivo: Garantir que cada papel acessa só a própria área e que a recusa fica registrada.
     * Decisões: Duas contas, uma de cada papel. Sem tese, o próximo passo do investidor é o onboarding (KAN-18).
     */
    @Test
    void investidorEEmpreendedorNaoAcessamAAreaDoOutro() throws Exception {
        Usuario investidora = conta(Papel.INVESTIDOR);
        Usuario empreendedor = conta(Papel.EMPREENDEDOR);
        String tokenInvestidora = bearer(investidora);
        String tokenEmpreendedor = bearer(empreendedor);

        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, tokenInvestidora))
                .andExpect(status().isOk()).andExpect(jsonPath("$.proximoPasso").value("/investidor/onboarding"));
        mvc.perform(get("/api/v1/empreendimentos/meus").header(HttpHeaders.AUTHORIZATION, tokenEmpreendedor))
                .andExpect(status().isOk()).andExpect(jsonPath("$.startups").isEmpty());

        mvc.perform(get("/api/v1/empreendimentos/meus").header(HttpHeaders.AUTHORIZATION, tokenInvestidora))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.codigo").value("acesso_negado"));
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, tokenEmpreendedor))
                .andExpect(status().isForbidden());

        assertThat(negados.findByUsuarioId(investidora.getId()))
                .extracting(AcessoNegado::getCaminho).containsExactly("/api/v1/empreendimentos/meus");
        assertThat(negados.findByUsuarioId(empreendedor.getId()))
                .extracting(AcessoNegado::getStatus).containsExactly(403);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Só o dono
     * Objetivo: Garantir que um empreendedor só acessa a startup dele.
     * Decisões: Regra de dono de teste que reconhece a Ana como dona de STARTUP_DA_ANA.
     */
    @Test
    void soODonoAcessaOProprioRecurso() throws Exception {
        Usuario ana = conta(Papel.EMPREENDEDOR);
        RecursoDeTeste.dona = ana.getId();
        Usuario outro = conta(Papel.EMPREENDEDOR);

        mvc.perform(get("/api/v1/teste/startups/" + STARTUP_DA_ANA).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/teste/startups/" + STARTUP_DA_ANA).header(HttpHeaders.AUTHORIZATION, bearer(outro)))
                .andExpect(status().isForbidden());
        assertThat(negados.findByUsuarioId(outro.getId())).hasSize(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Sem login, token falso e rota de administrador
     * Objetivo: Garantir 401 sem token e com token adulterado (este registrado) e 403 na rota de administrador.
     * Decisões: O 401 sem token não vai para a tabela.
     */
    @Test
    void semLoginTokenFalsoERotaDeAdministrador() throws Exception {
        long antes = negados.count();
        mvc.perform(get("/api/v1/investidores/eu"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.codigo").value("nao_autenticado"));
        assertThat(negados.count()).isEqualTo(antes);

        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, "Bearer adulterado"))
                .andExpect(status().isUnauthorized());
        assertThat(negados.count()).isEqualTo(antes + 1);

        Usuario comum = conta(Papel.INVESTIDOR);
        mvc.perform(post("/api/v1/admin/documentos-legais").header(HttpHeaders.AUTHORIZATION, bearer(comum))
                        .contentType(MediaType.APPLICATION_JSON).content("{}"))
                .andExpect(status().isForbidden());
        assertThat(negados.findByUsuarioId(comum.getId())).hasSize(1);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Conta de apoio
     * Objetivo: Gravar uma conta ativa com o papel informado e os termos aceitos.
     * Decisões: Sem pendência de termos, para o teste medir só a permissão por papel.
     */
    private Usuario conta(Papel papel) {
        Usuario usuario = Usuario.novo("Pessoa", "p-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel),
                Instant.now());
        usuario.confirmarEmail(Instant.now());
        usuarios.save(usuario);
        aceites.aceitar(usuario.getId(), aceites.pendentes(usuario.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return usuario;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Cabeçalho de autorização
     * Objetivo: Abrir uma sessão e devolver o cabeçalho Bearer.
     * Decisões: Pelo serviço de sessão.
     */
    private String bearer(Usuario usuario) {
        return "Bearer " + sessoes.abrir(usuario).tokenAcesso();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Recurso de teste com regra de dono
     * Objetivo: Simular uma rota de startup protegida por papel e por dono.
     * Decisões: Registrado só neste teste.
     */
    @TestConfiguration(proxyBeanMethods = false)
    static class RecursoDeTeste {

        static volatile UUID dona;

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:50
         * Cartão Jira: KAN-34
         * Título: Regra de dono de teste
         * Objetivo: Reconhecer a dona da startup de teste.
         * Decisões: Tipo "startup".
         */
        @Bean
        ControleDeAcesso.DonoDeRecurso donoDeStartupDeTeste() {
            return new ControleDeAcesso.DonoDeRecurso() {
                @Override
                public String tipo() {
                    return "startup";
                }

                @Override
                public boolean ehDono(UUID contaId, UUID recursoId) {
                    return contaId.equals(dona) && recursoId.equals(STARTUP_DA_ANA);
                }
            };
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:50
         * Cartão Jira: KAN-34
         * Título: Controller de teste
         * Objetivo: Expor a rota protegida pela regra de dono.
         * Decisões: Mesmas anotações que a S4 vai usar.
         */
        @Bean
        StartupDeTesteController startupDeTesteController() {
            return new StartupDeTesteController();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Controller de startup de teste
     * Objetivo: Rota que só o empreendedor dono acessa.
     * Decisões: Papel na classe e dono no método.
     */
    @RestController
    @Permissoes.SomenteEmpreendedor
    static class StartupDeTesteController {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:50
         * Cartão Jira: KAN-34
         * Título: Ver startup
         * Objetivo: Responder 200 só para a dona.
         * Decisões: Regra de dono pelo bean "acesso".
         */
        @GetMapping("/api/v1/teste/startups/{id}")
        @PreAuthorize("@acesso.ehDono('startup', #id)")
        String ver(@PathVariable UUID id) {
            return "ok";
        }
    }
}
