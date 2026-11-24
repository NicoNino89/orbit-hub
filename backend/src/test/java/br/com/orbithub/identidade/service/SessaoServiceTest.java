package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.compartilhado.seguranca.SegurancaProperties;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.RefreshToken;
import br.com.orbithub.identidade.domain.StatusRefreshToken;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.RefreshTokenRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
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
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Testes do serviço de sessão
 * Objetivo: Verificar abertura, rotação, detecção de reuso, expiração, logout e revogação geral.
 * Decisões: Repositório de tokens simulado em memória (lista), para o teste enxergar o estado gravado;
 *           relógio ajustável para avançar o tempo sem esperar.
 */
class SessaoServiceTest {

    private static final Instant INICIO = Instant.parse("2026-11-24T03:00:00Z");

    private final List<RefreshToken> gravados = new ArrayList<>();
    private RefreshTokenRepository tokens;
    private UsuarioRepository usuarios;
    private TokenAcessoService tokenAcesso;
    private RelogioAjustavel relogio;
    private SessaoService servico;
    private Usuario ana;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Preparação
     * Objetivo: Montar o serviço com repositórios simulados e uma conta ativa.
     * Decisões: Parâmetros iguais aos padrões da aplicação (30 min, 12 h, 10 s).
     */
    @BeforeEach
    void preparar() {
        tokens = mock(RefreshTokenRepository.class);
        usuarios = mock(UsuarioRepository.class);
        tokenAcesso = mock(TokenAcessoService.class);
        relogio = new RelogioAjustavel(INICIO);
        var propriedades = new SegurancaProperties(
                new SegurancaProperties.Jwt("x", "orbit-hub", Duration.ofMinutes(15)),
                new SegurancaProperties.Sessao(Duration.ofMinutes(30), Duration.ofHours(12), Duration.ofSeconds(10)));
        servico = new SessaoService(tokens, usuarios, tokenAcesso, propriedades, relogio);

        ana = Usuario.novo("Ana", "ana@exemplo.com", "h", Set.of(Papel.INVESTIDOR), INICIO);
        ana.confirmarEmail(INICIO);
        when(usuarios.findById(ana.getId())).thenReturn(Optional.of(ana));
        when(tokenAcesso.gerar(any(), any())).thenAnswer(i ->
                new TokenAcessoService.TokenAcesso("jwt-" + i.getArgument(1), relogio.instant().plusSeconds(900)));
        when(tokens.save(any())).thenAnswer(i -> {
            gravados.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(tokens.findByHash(anyString())).thenAnswer(i -> porHash(i.getArgument(0)));
        when(tokens.findByFamiliaIdAndStatus(any(), any())).thenAnswer(i -> gravados.stream()
                .filter(t -> t.getFamiliaId().equals(i.getArgument(0)) && t.getStatus() == i.getArgument(1))
                .toList());
        when(tokens.findByUsuarioIdAndStatus(any(), any())).thenAnswer(i -> gravados.stream()
                .filter(t -> t.getUsuarioId().equals(i.getArgument(0)) && t.getStatus() == i.getArgument(1))
                .toList());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Busca simulada pelo hash
     * Objetivo: Encontrar o token gravado cujo hash bate com o pedido.
     * Decisões: A entidade não expõe o hash; o teste guarda, para cada sessão emitida, o hash do valor bruto
     *           devolvido e o registro gravado correspondente.
     */
    private Optional<RefreshToken> porHash(String hash) {
        return Optional.ofNullable(emitidos.get(hash));
    }

    private final Map<String, RefreshToken> emitidos = new HashMap<>();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Registrar sessão emitida
     * Objetivo: Associar o valor bruto devolvido ao token gravado, para a busca por hash funcionar.
     * Decisões: O último gravado é sempre o da sessão recém-emitida.
     */
    private SessaoService.SessaoEmitida registrar(SessaoService.SessaoEmitida sessao) {
        emitidos.put(SessaoService.hash(sessao.refreshToken()), gravados.get(gravados.size() - 1));
        return sessao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Abrir sessão
     * Objetivo: Garantir que o login cria família, grava só o hash e devolve os dois tokens.
     * Decisões: Confere que o valor bruto não está no registro gravado.
     */
    @Test
    void abrirCriaFamiliaEGravaSoOHash() {
        var sessao = registrar(servico.abrir(ana));

        assertThat(gravados).hasSize(1);
        RefreshToken gravado = gravados.get(0);
        assertThat(gravado.getUsuarioId()).isEqualTo(ana.getId());
        assertThat(gravado.getExpiraEm()).isEqualTo(INICIO.plus(Duration.ofMinutes(30)));
        assertThat(gravado.getFamiliaExpiraEm()).isEqualTo(INICIO.plus(Duration.ofHours(12)));
        assertThat(sessao.refreshToken()).hasSizeGreaterThanOrEqualTo(43);
        assertThat(sessao.tokenAcesso()).isEqualTo("jwt-" + gravado.getFamiliaId());
        assertThat(sessao.refreshExpiraEm()).isEqualTo(gravado.getExpiraEm());
        assertThat(sessao.usuario()).isSameAs(ana);
        assertThat(sessao.toString()).doesNotContain(sessao.refreshToken()).doesNotContain(sessao.tokenAcesso());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conta inativa não abre sessão
     * Objetivo: Garantir que conta aguardando confirmação não recebe tokens.
     * Decisões: Conta recém-criada, sem confirmar o e-mail.
     */
    @Test
    void contaInativaNaoAbreSessao() {
        Usuario pendente = Usuario.novo("Bia", "bia@exemplo.com", "h", Set.of(Papel.EMPREENDEDOR), INICIO);
        assertThatThrownBy(() -> servico.abrir(pendente)).isInstanceOf(SessaoInvalidaException.class);
        verify(tokens, never()).save(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Rotação na renovação
     * Objetivo: Garantir que renovar marca o token como usado e emite outro na mesma família.
     * Decisões: Renova 20 minutos depois; o novo vale mais 30 minutos a partir dali.
     */
    @Test
    void renovarFazRotacaoNaMesmaFamilia() {
        var primeira = registrar(servico.abrir(ana));
        relogio.avancar(Duration.ofMinutes(20));

        var segunda = registrar(servico.renovar(primeira.refreshToken()));

        assertThat(gravados).hasSize(2);
        assertThat(gravados.get(0).getStatus()).isEqualTo(StatusRefreshToken.USADO);
        assertThat(gravados.get(1).getStatus()).isEqualTo(StatusRefreshToken.ATIVO);
        assertThat(gravados.get(1).getFamiliaId()).isEqualTo(gravados.get(0).getFamiliaId());
        assertThat(gravados.get(1).getExpiraEm()).isEqualTo(INICIO.plus(Duration.ofMinutes(50)));
        assertThat(segunda.refreshToken()).isNotEqualTo(primeira.refreshToken());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Token desconhecido
     * Objetivo: Garantir que um valor qualquer não renova nada.
     * Decisões: Valor inventado e valor nulo.
     */
    @Test
    void tokenDesconhecidoEhRecusado() {
        assertThatThrownBy(() -> servico.renovar("inventado")).isInstanceOf(SessaoInvalidaException.class);
        assertThatThrownBy(() -> servico.renovar(null)).isInstanceOf(SessaoInvalidaException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Expiração por inatividade
     * Objetivo: Garantir que, passados 30 minutos sem renovar, a sessão não renova mais.
     * Decisões: Exatamente na fronteira da validade.
     */
    @Test
    void sessaoExpiraPorInatividade() {
        var sessao = registrar(servico.abrir(ana));
        relogio.avancar(Duration.ofMinutes(30));

        assertThatThrownBy(() -> servico.renovar(sessao.refreshToken())).isInstanceOf(SessaoInvalidaException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Corrida entre abas
     * Objetivo: Garantir que reapresentar o token logo após a troca não derruba as sessões.
     * Decisões: 3 segundos depois, dentro da tolerância de 10 s.
     */
    @Test
    void reapresentacaoDentroDaToleranciaEhCorrida() {
        var primeira = registrar(servico.abrir(ana));
        registrar(servico.renovar(primeira.refreshToken()));
        relogio.avancar(Duration.ofSeconds(3));

        assertThatThrownBy(() -> servico.renovar(primeira.refreshToken()))
                .isInstanceOf(RenovacaoConcorrenteException.class);
        assertThat(gravados.get(1).getStatus()).isEqualTo(StatusRefreshToken.ATIVO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Reuso revoga tudo
     * Objetivo: Garantir que reapresentar um token já trocado revoga todas as sessões do usuário.
     * Decisões: Duas sessões abertas (dois aparelhos); o reuso em uma derruba as duas.
     */
    @Test
    void reusoForaDaToleranciaRevogaTodasAsSessoes() {
        var celular = registrar(servico.abrir(ana));
        registrar(servico.abrir(ana));
        registrar(servico.renovar(celular.refreshToken()));
        relogio.avancar(Duration.ofMinutes(1));

        assertThatThrownBy(() -> servico.renovar(celular.refreshToken()))
                .isInstanceOf(SessaoInvalidaException.class)
                .hasMessage(SessaoService.MOTIVO_REUSO);
        assertThat(gravados).filteredOn(t -> t.getStatus() == StatusRefreshToken.ATIVO).isEmpty();
        assertThat(gravados).filteredOn(t -> t.getStatus() == StatusRefreshToken.REVOGADO)
                .allMatch(t -> SessaoService.MOTIVO_REUSO.equals(t.getMotivoRevogacao()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Conta desativada não renova
     * Objetivo: Garantir que uma conta que deixou de estar ativa perde a sessão na próxima renovação.
     * Decisões: Simula a conta sumindo do banco (exclusão) e a conta pendente.
     */
    @Test
    void contaQueNaoPodeTerSessaoNaoRenova() {
        var sessao = registrar(servico.abrir(ana));
        when(usuarios.findById(ana.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.renovar(sessao.refreshToken())).isInstanceOf(SessaoInvalidaException.class);

        Usuario pendente = Usuario.novo("Bia", "bia@exemplo.com", "h", Set.of(Papel.EMPREENDEDOR), INICIO);
        when(usuarios.findById(ana.getId())).thenReturn(Optional.of(pendente));
        assertThatThrownBy(() -> servico.renovar(sessao.refreshToken())).isInstanceOf(SessaoInvalidaException.class);
        assertThat(gravados.get(0).getStatus()).isEqualTo(StatusRefreshToken.REVOGADO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Logout revoga a família
     * Objetivo: Garantir que sair revoga a sessão daquele aparelho e não a dos outros.
     * Decisões: Duas sessões; o logout de uma mantém a outra ativa.
     */
    @Test
    void encerrarRevogaSoAFamiliaDoToken() {
        var notebook = registrar(servico.abrir(ana));
        registrar(servico.abrir(ana));

        servico.encerrar(notebook.refreshToken());

        assertThat(gravados.get(0).getStatus()).isEqualTo(StatusRefreshToken.REVOGADO);
        assertThat(gravados.get(0).getMotivoRevogacao()).isEqualTo(SessaoService.MOTIVO_LOGOUT);
        assertThat(gravados.get(1).getStatus()).isEqualTo(StatusRefreshToken.ATIVO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Logout sem token
     * Objetivo: Garantir que sair sem cookie ou com token desconhecido não dá erro.
     * Decisões: Nulo, vazio e inventado.
     */
    @Test
    void encerrarSemTokenValidoNaoFalha() {
        servico.encerrar(null);
        servico.encerrar(" ");
        servico.encerrar("inventado");
        verify(tokens, never()).findByFamiliaIdAndStatus(any(), any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Encerrar todas as sessões
     * Objetivo: Garantir que a revogação geral derruba todas as sessões ativas e informa quantas.
     * Decisões: Duas sessões abertas.
     */
    @Test
    void encerrarTodasRevogaTodasAsAtivas() {
        registrar(servico.abrir(ana));
        registrar(servico.abrir(ana));

        int revogadas = servico.encerrarTodas(ana.getId(), "senha_redefinida");

        assertThat(revogadas).isEqualTo(2);
        assertThat(gravados).allMatch(t -> t.getStatus() == StatusRefreshToken.REVOGADO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão ativa consulta o banco
     * Objetivo: Garantir que a checagem por requisição pergunta por token ativo e ainda válido.
     * Decisões: Confere os argumentos passados ao repositório.
     */
    @Test
    void sessaoAtivaConsultaTokenAtivoNaoExpirado() {
        UUID familia = UUID.randomUUID();
        when(tokens.existsByFamiliaIdAndStatusAndExpiraEmAfter(familia, StatusRefreshToken.ATIVO, INICIO))
                .thenReturn(true);
        assertThat(servico.sessaoAtiva(familia)).isTrue();
        assertThat(servico.sessaoAtiva(UUID.randomUUID())).isFalse();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Hash do token
     * Objetivo: Garantir que o hash é SHA-256 em hexadecimal e que nulo vira vazio.
     * Decisões: Valor conhecido de SHA-256("abc").
     */
    @Test
    void hashEhSha256Hexadecimal() {
        assertThat(SessaoService.hash("abc"))
                .isEqualTo("ba7816bf8f01cfea414140de5dae2223b00361a396177a9cb410ff61f20015ad");
        assertThat(SessaoService.hash(null)).isEmpty();
    }
}
