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
import br.com.orbithub.identidade.domain.BloqueioDeLogin;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.BloqueioDeLoginRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:10
 * Cartão Jira: KAN-13
 * Título: Testes do login
 * Objetivo: Verificar login correto, recusas genéricas, conta pendente, bloqueio temporário e logout.
 * Decisões: Bloqueios guardados num mapa em memória; codificador de senha que compara texto simples.
 */
class LoginServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T04:10:00Z");

    private final Map<String, BloqueioDeLogin> bloqueiosGravados = new HashMap<>();
    private UsuarioRepository usuarios;
    private SessaoService sessoes;
    private PasswordEncoder senhas;
    private RelogioAjustavel relogio;
    private LoginService servico;
    private Usuario ana;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Preparação
     * Objetivo: Montar o serviço com uma conta ativa e limite de 3 falhas por 15 minutos.
     * Decisões: Limite menor que o padrão para o teste ficar curto.
     */
    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        sessoes = mock(SessaoService.class);
        senhas = mock(PasswordEncoder.class);
        when(senhas.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        when(senhas.matches(anyString(), anyString())).thenAnswer(i ->
                ("hash:" + i.getArgument(0)).equals(i.getArgument(1)));
        BloqueioDeLoginRepository bloqueios = mock(BloqueioDeLoginRepository.class);
        when(bloqueios.findById(anyString())).thenAnswer(i -> Optional.ofNullable(bloqueiosGravados.get(i.getArgument(0))));
        when(bloqueios.save(any())).thenAnswer(i -> {
            BloqueioDeLogin b = i.getArgument(0);
            bloqueiosGravados.put(SessaoService.hash("ana@exemplo.com"), b);
            return b;
        });
        org.mockito.Mockito.doAnswer(i -> {
            bloqueiosGravados.clear();
            return null;
        }).when(bloqueios).delete(any());
        relogio = new RelogioAjustavel(AGORA);
        servico = new LoginService(usuarios, bloqueios, senhas, sessoes,
                new LoginService.Parametros(3, Duration.ofMinutes(15)), relogio);

        ana = Usuario.novo("Ana", "ana@exemplo.com", "hash:senha-correta-1", Set.of(Papel.INVESTIDOR), AGORA);
        ana.confirmarEmail(AGORA);
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Login correto
     * Objetivo: Garantir que e-mail (com maiúsculas e espaços) e senha corretos abrem a sessão.
     * Decisões: Confere a chamada ao serviço de sessão.
     */
    @Test
    void loginCorretoAbreSessao() {
        servico.entrar("  ANA@exemplo.com ", "senha-correta-1");
        verify(sessoes).abrir(ana);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Mensagem genérica
     * Objetivo: Garantir a mesma resposta para senha errada e e-mail inexistente, e a senha conferida nos dois.
     * Decisões: O e-mail inexistente também passa pelo codificador (tempo constante).
     */
    @Test
    void senhaErradaEEmailInexistenteRespondemIgual() {
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "errada"))
                .isInstanceOf(LoginService.CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha incorretos.");
        assertThatThrownBy(() -> servico.entrar("ninguem@exemplo.com", "errada"))
                .isInstanceOf(LoginService.CredenciaisInvalidasException.class)
                .hasMessage("E-mail ou senha incorretos.");
        verify(senhas).matches("errada", "hash:senha-ficticia-para-tempo-constante");
        verify(sessoes, never()).abrir(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Bloqueio temporário
     * Objetivo: Garantir que, depois de 3 falhas, nem a senha certa entra; passados 15 minutos, entra.
     * Decisões: A terceira falha já responde "bloqueado".
     */
    @Test
    void bloqueiaDepoisDeFalhasSeguidas() {
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x1")).isInstanceOf(LoginService.CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x2")).isInstanceOf(LoginService.CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x3")).isInstanceOf(LoginService.LoginBloqueadoException.class);
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "senha-correta-1"))
                .isInstanceOf(LoginService.LoginBloqueadoException.class);

        relogio.avancar(Duration.ofMinutes(15));
        servico.entrar("ana@exemplo.com", "senha-correta-1");
        verify(sessoes).abrir(ana);
        assertThat(bloqueiosGravados).isEmpty();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Acerto zera a contagem
     * Objetivo: Garantir que um login correto zera as falhas anteriores.
     * Decisões: Duas falhas, um acerto e mais duas falhas não bloqueiam.
     */
    @Test
    void acertoZeraAContagem() {
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x1")).isInstanceOf(RuntimeException.class);
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x2")).isInstanceOf(RuntimeException.class);
        servico.entrar("ana@exemplo.com", "senha-correta-1");
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x3"))
                .isInstanceOf(LoginService.CredenciaisInvalidasException.class);
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "x4"))
                .isInstanceOf(LoginService.CredenciaisInvalidasException.class);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Conta pendente e logout
     * Objetivo: Garantir o aviso de conta pendente com senha certa e o logout delegado à sessão.
     * Decisões: Conta recém-criada sem confirmar o e-mail.
     */
    @Test
    void contaPendenteELogout() {
        Usuario pendente = Usuario.novo("Ana", "ana@exemplo.com", "hash:senha-correta-1", Set.of(Papel.INVESTIDOR), AGORA);
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(pendente));
        assertThatThrownBy(() -> servico.entrar("ana@exemplo.com", "senha-correta-1"))
                .isInstanceOf(LoginService.ContaPendenteException.class);

        servico.sair("refresh-qualquer");
        verify(sessoes).encerrar("refresh-qualquer");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Ciclo do bloqueio
     * Objetivo: Garantir que, vencido o bloqueio, a contagem recomeça do zero.
     * Decisões: Teste direto da entidade.
     */
    @Test
    void cicloDoBloqueio() {
        BloqueioDeLogin bloqueio = BloqueioDeLogin.novo("h", AGORA);
        bloqueio.registrarFalha(AGORA, 2, Duration.ofMinutes(15));
        assertThat(bloqueio.getFalhas()).isEqualTo(1);
        bloqueio.registrarFalha(AGORA, 2, Duration.ofMinutes(15));
        assertThat(bloqueio.bloqueado(AGORA)).isTrue();
        assertThat(bloqueio.getBloqueadoAte()).isEqualTo(AGORA.plus(Duration.ofMinutes(15)));

        Instant depois = AGORA.plus(Duration.ofMinutes(16));
        assertThat(bloqueio.bloqueado(depois)).isFalse();
        bloqueio.registrarFalha(depois, 2, Duration.ofMinutes(15));
        assertThat(bloqueio.getFalhas()).isEqualTo(1);
        assertThat(bloqueio.getBloqueadoAte()).isNull();
    }
}
