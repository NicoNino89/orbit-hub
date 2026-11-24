package br.com.orbithub.investidores.api;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import br.com.orbithub.TestcontainersConfiguration;
import br.com.orbithub.administracao.domain.DocumentoLegal;
import br.com.orbithub.administracao.service.AceiteService;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import br.com.orbithub.identidade.service.SessaoService;
import com.jayway.jsonpath.JsonPath;
import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Testes de integração das organizações
 * Objetivo: Conferir, com PostgreSQL real, criação, convite e aceite, as permissões de cada papel, a regra do último
 *           administrador e que a saída tira o acesso na hora (inclusive a atuação em nome da organização).
 * Decisões: E-mail simulado, para capturar o link do convite. Cada teste usa CNPJs próprios, gerados com dígitos
 *           verificadores corretos.
 */
@SpringBootTest
@AutoConfigureMockMvc
@Import(TestcontainersConfiguration.class)
class OrganizacoesIT {

    private static final AtomicInteger SEQUENCIA = new AtomicInteger((int) (System.nanoTime() % 90_000_000L));

    @Autowired
    MockMvc mvc;

    @Autowired
    UsuarioRepository usuarios;

    @Autowired
    SessaoService sessoes;

    @Autowired
    AceiteService aceites;

    @Autowired
    JdbcTemplate jdbc;

    @MockitoBean
    EnvioDeEmail email;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criar, convidar e aceitar
     * Objetivo: Conferir o cadastro com as recusas (CNPJ inválido ou repetido, tipo pessoa física), o convite por e-mail
     *           e no sino, o aceite só pela conta certa, a visão de quem é e de quem não é membro e a atuação em nome da
     *           organização.
     * Decisões: Ana cria; Bia é convidada como membro; Caio é de fora.
     */
    @Test
    void criarConvidarEAceitar() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR);
        Usuario bia = conta(Papel.INVESTIDOR);
        Usuario caio = conta(Papel.INVESTIDOR);
        String cnpj = novoCnpj();

        criar(ana, "Fundo Aurora", "FUNDO_VC", "12.345.678/0001-00").andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("cnpj_invalido"));
        criar(ana, "Ana Anjo", "ANJO", cnpj).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("tipo_pessoa_fisica"));
        String corpo = criar(ana, "Fundo Aurora", "FUNDO_VC", cnpj).andExpect(status().isCreated())
                .andExpect(jsonPath("$.meuPapel").value("ADMINISTRADOR"))
                .andExpect(jsonPath("$.cnpj").value(formatado(cnpj)))
                .andReturn().getResponse().getContentAsString();
        String org = JsonPath.read(corpo, "$.id");
        criar(caio, "Outro nome", "FUNDO_VC", cnpj).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("cnpj_em_uso"));

        String token = convidar(ana, org, bia.getEmail().toUpperCase(), "MEMBRO");
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND tipo = 'ORGANIZACAO'",
                Integer.class, bia.getId())).isEqualTo(1);

        mvc.perform(post("/api/v1/organizacoes/convites/aceite").header(HttpHeaders.AUTHORIZATION, bearer(caio))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("convite_outro_email"));
        mvc.perform(post("/api/v1/organizacoes/convites/consulta").header(HttpHeaders.AUTHORIZATION, bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.organizacao").value("Fundo Aurora"))
                .andExpect(jsonPath("$.papel").value("MEMBRO"));
        aceitar(bia, token).andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Fundo Aurora"));
        aceitar(bia, token).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("convite_invalido"));
        assertThat(jdbc.queryForObject("SELECT count(*) FROM notificacao WHERE usuario_id = ? AND tipo = 'ORGANIZACAO'",
                Integer.class, ana.getId())).isEqualTo(1);

        mvc.perform(get("/api/v1/organizacoes/minhas").header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(jsonPath("$[0].nome").value("Fundo Aurora")).andExpect(jsonPath("$[0].meuPapel").value("MEMBRO"));
        mvc.perform(get("/api/v1/organizacoes/" + org).header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.membros.length()").value(2))
                .andExpect(jsonPath("$.membros[0].email").doesNotExist());
        mvc.perform(get("/api/v1/organizacoes/" + org).header(HttpHeaders.AUTHORIZATION, bearer(caio)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/organizacoes/" + org + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(bia))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"x@exemplo.com\",\"papel\":\"MEMBRO\"}"))
                .andExpect(status().isForbidden());

        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(bia))
                        .header("X-Orbit-Organizacao", org))
                .andExpect(status().isOk()).andExpect(jsonPath("$.atuandoPor.nome").value("Fundo Aurora"))
                .andExpect(jsonPath("$.atuandoPor.papel").value("MEMBRO"));
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(caio))
                        .header("X-Orbit-Organizacao", org))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(caio)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.atuandoPor").doesNotExist());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Último administrador e saída
     * Objetivo: Conferir que o último administrador não sai nem perde o papel, que a troca de papel funciona e que,
     *           depois de sair, a pessoa não vê a organização nem atua por ela.
     * Decisões: Bia vira administradora para Ana poder sair; depois Bia remove Caio.
     */
    @Test
    void ultimoAdministradorESaida() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR);
        Usuario bia = conta(Papel.INVESTIDOR);
        Usuario caio = conta(Papel.INVESTIDOR);
        String org = JsonPath.read(criar(ana, "Rede Sol", "REDE_DE_ANJOS", novoCnpj()).andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString(), "$.id");
        aceitar(bia, convidar(ana, org, bia.getEmail(), "OBSERVADOR")).andExpect(status().isOk());
        aceitar(caio, convidar(ana, org, caio.getEmail(), "MEMBRO")).andExpect(status().isOk());

        mvc.perform(delete("/api/v1/organizacoes/" + org + "/membros/" + ana.getId()).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("ultimo_administrador"));
        papel(ana, org, ana.getId(), "MEMBRO").andExpect(status().isUnprocessableContent());

        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(bia)).header("X-Orbit-Organizacao", org))
                .andExpect(status().isOk()).andExpect(jsonPath("$.atuandoPor.papel").value("OBSERVADOR"));
        papel(ana, org, bia.getId(), "ADMINISTRADOR").andExpect(status().isNoContent());
        mvc.perform(delete("/api/v1/organizacoes/" + org + "/membros/" + ana.getId()).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNoContent());
        mvc.perform(get("/api/v1/organizacoes/" + org).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/v1/investidores/eu").header(HttpHeaders.AUTHORIZATION, bearer(ana)).header("X-Orbit-Organizacao", org))
                .andExpect(status().isForbidden());

        mvc.perform(delete("/api/v1/organizacoes/" + org + "/membros/" + caio.getId()).header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(status().isNoContent());
        assertThat(jdbc.queryForObject("SELECT motivo_saida FROM organizacao_membro WHERE usuario_id = ? AND organizacao_id = ?",
                String.class, caio.getId(), UUID.fromString(org))).isEqualTo("REMOVIDO");
        mvc.perform(get("/api/v1/organizacoes/" + org).header(HttpHeaders.AUTHORIZATION, bearer(bia)))
                .andExpect(jsonPath("$.membros.length()").value(1));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Quem não é investidor
     * Objetivo: Conferir que conta só de empreendedor não cria organização e, ao abrir um convite, recebe a explicação.
     * Decisões: E-mail do convite igual ao da conta.
     */
    @Test
    void quemNaoEInvestidor() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR);
        Usuario eva = conta(Papel.EMPREENDEDOR);
        criar(eva, "Startup", "FUNDO_VC", novoCnpj()).andExpect(status().isForbidden());
        String org = JsonPath.read(criar(ana, "CVC Norte", "CVC", novoCnpj()).andReturn().getResponse().getContentAsString(),
                "$.id");
        aceitar(eva, convidar(ana, org, eva.getEmail(), "MEMBRO")).andExpect(status().isUnprocessableContent())
                .andExpect(jsonPath("$.codigo").value("sem_papel_investidor"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Editar e cancelar convite
     * Objetivo: Conferir a edição dos dados (o CNPJ não muda), a lista de convites pendentes, o convite repetido que
     *           substitui o anterior e o cancelamento, que invalida o link.
     * Decisões: Convidado sem conta (e-mail novo), para conferir que o convite vai mesmo assim.
     */
    @Test
    void editarEConvites() throws Exception {
        Usuario ana = conta(Papel.INVESTIDOR);
        String cnpj = novoCnpj();
        String org = JsonPath.read(criar(ana, "Family Sul", "FAMILY_OFFICE", cnpj).andReturn().getResponse()
                .getContentAsString(), "$.id");
        mvc.perform(put("/api/v1/organizacoes/" + org).header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"nome\":\"Family Sul Gestão\",\"tipo\":\"FAMILY_OFFICE\",\"cnpj\":\"" + novoCnpj()
                                + "\",\"site\":\" \",\"descricao\":\"Nova descrição\"}"))
                .andExpect(status().isOk()).andExpect(jsonPath("$.nome").value("Family Sul Gestão"))
                .andExpect(jsonPath("$.cnpj").value(formatado(cnpj))).andExpect(jsonPath("$.site").doesNotExist())
                .andExpect(jsonPath("$.descricao").value("Nova descrição"));

        String destino = "novo-" + UUID.randomUUID() + "@exemplo.com";
        String primeiro = convidar(ana, org, destino, "MEMBRO");
        String segundo = convidar(ana, org, destino, "OBSERVADOR");
        mvc.perform(post("/api/v1/organizacoes/convites/consulta").header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + primeiro + "\"}"))
                .andExpect(status().isUnprocessableContent());
        String pendentes = mvc.perform(get("/api/v1/organizacoes/" + org + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isOk()).andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].papel").value("OBSERVADOR")).andReturn().getResponse().getContentAsString();
        String conviteId = JsonPath.read(pendentes, "$[0].id");
        mvc.perform(delete("/api/v1/organizacoes/" + org + "/convites/" + conviteId).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNoContent());
        mvc.perform(post("/api/v1/organizacoes/convites/consulta").header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + segundo + "\"}"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("convite_invalido"));
        mvc.perform(get("/api/v1/organizacoes/" + org + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(jsonPath("$.length()").value(0));
        mvc.perform(delete("/api/v1/organizacoes/" + org + "/convites/" + UUID.randomUUID()).header(HttpHeaders.AUTHORIZATION, bearer(ana)))
                .andExpect(status().isNotFound());
        mvc.perform(post("/api/v1/organizacoes/" + org + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(ana))
                        .contentType(MediaType.APPLICATION_JSON).content("{\"email\":\"sem-arroba\",\"papel\":\"MEMBRO\"}"))
                .andExpect(status().isUnprocessableContent()).andExpect(jsonPath("$.codigo").value("email_invalido"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Criar pela API
     * Objetivo: Enviar o cadastro de uma organização.
     * Decisões: Site e descrição fixos.
     */
    private ResultActions criar(Usuario quem, String nome, String tipo, String cnpj)
            throws Exception {
        return mvc.perform(post("/api/v1/organizacoes").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON)
                .content("{\"nome\":\"" + nome + "\",\"tipo\":\"" + tipo + "\",\"cnpj\":\"" + cnpj
                        + "\",\"site\":\"https://exemplo.com\",\"descricao\":\"Teste\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Convidar pela API
     * Objetivo: Enviar o convite e devolver o token tirado do e-mail.
     * Decisões: Último e-mail capturado.
     */
    private String convidar(Usuario admin, String org, String destino, String papel) throws Exception {
        mvc.perform(post("/api/v1/organizacoes/" + org + "/convites").header(HttpHeaders.AUTHORIZATION, bearer(admin))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"" + destino + "\",\"papel\":\"" + papel + "\"}"))
                .andExpect(status().isCreated());
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, atLeastOnce()).enviar(mensagem.capture());
        List<EnvioDeEmail.MensagemEmail> enviadas = mensagem.getAllValues();
        String texto = enviadas.get(enviadas.size() - 1).texto();
        return texto.substring(texto.indexOf("token=") + 6).split("\\s")[0];
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Aceitar pela API
     * Objetivo: Enviar o aceite de um convite.
     * Decisões: Conta informada.
     */
    private ResultActions aceitar(Usuario quem, String token) throws Exception {
        return mvc.perform(post("/api/v1/organizacoes/convites/aceite").header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"token\":\"" + token + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Trocar papel pela API
     * Objetivo: Enviar a troca de papel de um membro.
     * Decisões: Quem pede é informado.
     */
    private ResultActions papel(Usuario quem, String org, UUID alvo, String papel)
            throws Exception {
        return mvc.perform(put("/api/v1/organizacoes/" + org + "/membros/" + alvo).header(HttpHeaders.AUTHORIZATION, bearer(quem))
                .contentType(MediaType.APPLICATION_JSON).content("{\"papel\":\"" + papel + "\"}"));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Conta de teste
     * Objetivo: Criar uma conta ativa com o papel informado e os termos aceitos.
     * Decisões: E-mail único.
     */
    private Usuario conta(Papel papel) {
        Usuario u = Usuario.novo("Pessoa " + papel, "o-" + UUID.randomUUID() + "@exemplo.com", "{noop}x", Set.of(papel),
                Instant.now());
        u.confirmarEmail(Instant.now());
        usuarios.save(u);
        aceites.aceitar(u.getId(), aceites.pendentes(u.getId(), true).stream().map(DocumentoLegal::getId).toList());
        return u;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
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
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: CNPJ novo
     * Objetivo: Gerar um CNPJ válido diferente a cada chamada.
     * Decisões: Oito dígitos sequenciais, filial 0001 e os dois dígitos verificadores calculados.
     */
    static String novoCnpj() {
        String base = String.format("%08d", SEQUENCIA.incrementAndGet() % 100_000_000) + "0001";
        String com1 = base + dv(base, new int[] {5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
        return com1 + dv(com1, new int[] {6, 5, 4, 3, 2, 9, 8, 7, 6, 5, 4, 3, 2});
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Dígito verificador
     * Objetivo: Calcular um dígito do CNPJ para gerar números válidos.
     * Decisões: Módulo 11.
     */
    private static int dv(String digitos, int[] pesos) {
        int soma = 0;
        for (int i = 0; i < pesos.length; i++) {
            soma += (digitos.charAt(i) - '0') * pesos[i];
        }
        int resto = soma % 11;
        return resto < 2 ? 0 : 11 - resto;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: CNPJ formatado
     * Objetivo: Escrever o CNPJ como a API devolve.
     * Decisões: 00.000.000/0000-00.
     */
    private static String formatado(String d) {
        return d.substring(0, 2) + "." + d.substring(2, 5) + "." + d.substring(5, 8) + "/" + d.substring(8, 12) + "-"
                + d.substring(12);
    }
}
