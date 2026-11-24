package br.com.orbithub.identidade.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import br.com.orbithub.compartilhado.parametros.ParametrosFixos;
import br.com.orbithub.RelogioAjustavel;
import br.com.orbithub.compartilhado.email.EnvioDeEmail;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.Papel;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.BloqueioDeLoginRepository;
import br.com.orbithub.identidade.repository.TokenDeContaRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:30
 * Cartão Jira: KAN-14
 * Título: Testes da recuperação de senha
 * Objetivo: Verificar o envio do link, a resposta igual para qualquer e-mail, a troca de senha, o uso único,
 *           a expiração, a senha fraca e o encerramento das sessões.
 * Decisões: Serviço de links real sobre repositório em memória, para o link do e-mail ser usado de verdade.
 */
class RecuperacaoSenhaServiceTest {

    private static final Instant AGORA = Instant.parse("2026-11-24T04:30:00Z");

    private final List<TokenDeConta> tokensGravados = new ArrayList<>();
    private UsuarioRepository usuarios;
    private BloqueioDeLoginRepository bloqueios;
    private SessaoService sessoes;
    private EnvioDeEmail email;
    private RelogioAjustavel relogio;
    private RecuperacaoSenhaService servico;
    private Usuario ana;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Preparação
     * Objetivo: Montar o serviço com uma conta ativa e repositórios simulados.
     * Decisões: Codificador de senha falso ("hash:" + senha).
     */
    @BeforeEach
    void preparar() {
        usuarios = mock(UsuarioRepository.class);
        bloqueios = mock(BloqueioDeLoginRepository.class);
        sessoes = mock(SessaoService.class);
        email = mock(EnvioDeEmail.class);
        relogio = new RelogioAjustavel(AGORA);
        TokenDeContaRepository tokens = mock(TokenDeContaRepository.class);
        when(tokens.save(any())).thenAnswer(i -> {
            tokensGravados.add(i.getArgument(0));
            return i.getArgument(0);
        });
        when(tokens.findByUsuarioIdAndFinalidade(any(), any())).thenAnswer(i -> tokensGravados.stream()
                .filter(t -> t.getUsuarioId().equals(i.getArgument(0)) && t.getFinalidade() == i.getArgument(1)).toList());
        when(tokens.findByHash(anyString())).thenAnswer(i -> tokensGravados.stream()
                .filter(t -> hashDe(t).equals(i.getArgument(0))).findFirst());
        PasswordEncoder senhas = mock(PasswordEncoder.class);
        when(senhas.encode(anyString())).thenAnswer(i -> "hash:" + i.getArgument(0));
        var propriedades = new ContaProperties("https://web.exemplo", true);
        servico = new RecuperacaoSenhaService(usuarios, bloqueios, new TokenDeContaService(tokens, relogio), sessoes,
                senhas, email, propriedades, new ParametrosFixos(), relogio);

        ana = Usuario.novo("Ana", "ana@exemplo.com", "hash:antiga-senha-longa", Set.of(Papel.INVESTIDOR), AGORA);
        ana.confirmarEmail(AGORA);
        when(usuarios.findByEmail("ana@exemplo.com")).thenReturn(Optional.of(ana));
        when(usuarios.findById(ana.getId())).thenReturn(Optional.of(ana));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Hash gravado
     * Objetivo: Ler o hash de um token gravado para a busca simulada.
     * Decisões: Reflexão, porque a entidade não expõe o hash.
     */
    private static String hashDe(TokenDeConta token) {
        try {
            var campo = TokenDeConta.class.getDeclaredField("hash");
            campo.setAccessible(true);
            return (String) campo.get(token);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Token do último e-mail
     * Objetivo: Extrair o token do link do último e-mail enviado.
     * Decisões: O token vai de "token=" até o fim da linha.
     */
    private String tokenDoEmail() {
        ArgumentCaptor<EnvioDeEmail.MensagemEmail> mensagem = ArgumentCaptor.forClass(EnvioDeEmail.MensagemEmail.class);
        verify(email, org.mockito.Mockito.atLeastOnce()).enviar(mensagem.capture());
        String texto = mensagem.getValue().texto();
        int inicio = texto.indexOf("token=") + 6;
        return texto.substring(inicio, texto.indexOf('\n', inicio));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Fluxo completo
     * Objetivo: Garantir link por e-mail, troca da senha, sessões encerradas, bloqueio zerado e uso único.
     * Decisões: Usa o link do e-mail duas vezes.
     */
    @Test
    void redefineASenhaEEncerraAsSessoes() {
        servico.solicitar(" ANA@exemplo.com ");
        String token = tokenDoEmail();

        servico.redefinir(token, "nova-senha-bem-longa");

        assertThat(ana.getSenhaHash()).isEqualTo("hash:nova-senha-bem-longa");
        verify(sessoes).encerrarTodas(ana.getId(), RecuperacaoSenhaService.MOTIVO_SENHA_REDEFINIDA);
        verify(bloqueios).deleteById(SessaoService.hash("ana@exemplo.com"));
        assertThatThrownBy(() -> servico.redefinir(token, "outra-senha-longa"))
                .isInstanceOf(RegraDeNegocioException.class)
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("link_invalido");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Senha fraca não gasta o link
     * Objetivo: Garantir que a senha recusada mantém o link válido para nova tentativa.
     * Decisões: Primeiro senha comum, depois uma forte com o mesmo link.
     */
    @Test
    void senhaFracaNaoGastaOLink() {
        servico.solicitar("ana@exemplo.com");
        String token = tokenDoEmail();

        assertThatThrownBy(() -> servico.redefinir(token, "1234567890"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("senha_fraca");
        servico.redefinir(token, "nova-senha-bem-longa");
        assertThat(ana.getSenhaHash()).isEqualTo("hash:nova-senha-bem-longa");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Link expira
     * Objetivo: Garantir que, passados 30 minutos, o link não troca a senha.
     * Decisões: Avança o relógio até a validade.
     */
    @Test
    void linkExpiraEmTrintaMinutos() {
        servico.solicitar("ana@exemplo.com");
        relogio.avancar(Duration.ofMinutes(30));
        assertThatThrownBy(() -> servico.redefinir(tokenDoEmail(), "nova-senha-bem-longa"))
                .isInstanceOf(RegraDeNegocioException.class);
        assertThat(ana.getSenhaHash()).isEqualTo("hash:antiga-senha-longa");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Nada é enviado sem conta ativa
     * Objetivo: Garantir que e-mail inexistente ou conta pendente não recebem link, sem erro para quem pediu.
     * Decisões: Os dois casos terminam sem exceção.
     */
    @Test
    void semContaAtivaNadaEhEnviado() {
        servico.solicitar("ninguem@exemplo.com");
        Usuario pendente = Usuario.novo("Bia", "bia@exemplo.com", "h", Set.of(Papel.EMPREENDEDOR), AGORA);
        when(usuarios.findByEmail("bia@exemplo.com")).thenReturn(Optional.of(pendente));
        servico.solicitar("bia@exemplo.com");
        verify(email, never()).enviar(any());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Limite de pedidos
     * Objetivo: Garantir no máximo um e-mail por minuto e que o link novo invalida o anterior.
     * Decisões: Três pedidos: o segundo é ignorado; o terceiro, depois de um minuto, envia.
     */
    @Test
    void limitaPedidosEUltimoLinkVale() {
        servico.solicitar("ana@exemplo.com");
        String primeiro = tokenDoEmail();
        servico.solicitar("ana@exemplo.com");
        verify(email, times(1)).enviar(any());

        relogio.avancar(Duration.ofMinutes(1).plusSeconds(1));
        servico.solicitar("ana@exemplo.com");
        verify(email, times(2)).enviar(any());
        assertThatThrownBy(() -> servico.redefinir(primeiro, "nova-senha-bem-longa"))
                .isInstanceOf(RegraDeNegocioException.class);
        servico.redefinir(tokenDoEmail(), "nova-senha-bem-longa");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:30
     * Cartão Jira: KAN-14
     * Título: Conta removida
     * Objetivo: Garantir que um link de conta que não existe mais é tratado como inválido.
     * Decisões: O repositório não encontra a conta.
     */
    @Test
    void contaRemovidaInvalidaOLink() {
        servico.solicitar("ana@exemplo.com");
        String token = tokenDoEmail();
        when(usuarios.findById(ana.getId())).thenReturn(Optional.empty());
        assertThatThrownBy(() -> servico.redefinir(token, "nova-senha-bem-longa"))
                .extracting(e -> ((RegraDeNegocioException) e).getCodigo()).isEqualTo("link_invalido");
    }
}
