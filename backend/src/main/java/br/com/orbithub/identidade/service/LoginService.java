package br.com.orbithub.identidade.service;

import br.com.orbithub.identidade.domain.BloqueioDeLogin;
import br.com.orbithub.identidade.domain.StatusUsuario;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.BloqueioDeLoginRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:55
 * Cartão Jira: KAN-75
 * Título: Login e logout
 * Objetivo: Conferir e-mail e senha, abrir a sessão (ou pedir o segundo fator, nas contas da equipe),
 *           bloquear temporariamente após falhas seguidas e encerrar a sessão no logout.
 * Decisões: Credenciais erradas, e-mail inexistente e conta desativada dão a mesma resposta. Quando o
 *           e-mail não existe, a senha é conferida contra um hash fictício, para o tempo de resposta não
 *           revelar se a conta existe. O bloqueio é por e-mail (hash) e vale também para e-mails sem conta.
 *           Conta interna nunca recebe sessão só com a senha: recebe um desafio de segundo fator (KAN-75).
 */
@Service
public class LoginService {

    private static final Logger LOG = LoggerFactory.getLogger(LoginService.class);

    private final UsuarioRepository usuarios;
    private final BloqueioDeLoginRepository bloqueios;
    private final PasswordEncoder senhas;
    private final SessaoService sessoes;
    private final SegundoFatorService segundoFator;
    private final Parametros parametros;
    private final Clock relogio;
    private final String hashFicticio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:55
     * Cartão Jira: KAN-75
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, codificador de senhas, sessões, segundo fator, parâmetros e relógio.
     * Decisões: O hash fictício é calculado uma vez, com o mesmo algoritmo das senhas reais.
     */
    public LoginService(UsuarioRepository usuarios, BloqueioDeLoginRepository bloqueios, PasswordEncoder senhas,
                        SessaoService sessoes, SegundoFatorService segundoFator, Parametros parametros,
                        Clock relogio) {
        this.usuarios = usuarios;
        this.bloqueios = bloqueios;
        this.senhas = senhas;
        this.sessoes = sessoes;
        this.segundoFator = segundoFator;
        this.parametros = parametros;
        this.relogio = relogio;
        this.hashFicticio = senhas.encode("senha-ficticia-para-tempo-constante");
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:55
     * Cartão Jira: KAN-75
     * Título: Entrar
     * Objetivo: Abrir a sessão para e-mail e senha corretos de uma conta ativa, ou devolver o desafio do
     *           segundo fator quando a conta é da equipe.
     * Decisões: Bloqueado: 429 antes de conferir a senha. Senha errada: soma falha (gravada mesmo com a
     *           exceção) e responde 401 genérico. Senha certa de conta pendente: 403 "conta_pendente", para a
     *           pessoa saber que falta confirmar o e-mail (só quem sabe a senha vê essa mensagem). Na conta
     *           interna, as falhas só são zeradas depois do código correto, para quem tem a senha não ganhar
     *           tentativas novas de código a cada login (KAN-75).
     */
    @Transactional(noRollbackFor = {CredenciaisInvalidasException.class, LoginBloqueadoException.class,
        ContaPendenteException.class})
    public ResultadoLogin entrar(String email, String senha) {
        Instant agora = relogio.instant();
        String emailNormalizado = Usuario.normalizarEmail(email);
        String chave = SessaoService.hash(emailNormalizado);
        Optional<BloqueioDeLogin> bloqueio = bloqueios.findById(chave);
        if (bloqueio.filter(b -> b.bloqueado(agora)).isPresent()) {
            throw new LoginBloqueadoException();
        }

        Optional<Usuario> usuario = usuarios.findByEmail(emailNormalizado);
        boolean senhaConfere = usuario
                .map(u -> senhas.matches(senha, u.getSenhaHash()))
                .orElseGet(() -> {
                    senhas.matches(senha, hashFicticio);
                    return false;
                });
        if (!senhaConfere || usuario.get().getStatus() == StatusUsuario.DESATIVADO) {
            BloqueioDeLogin contagem = bloqueio.orElseGet(() -> BloqueioDeLogin.novo(chave, agora));
            contagem.registrarFalha(agora, parametros.maxTentativas(), parametros.bloqueio());
            bloqueios.save(contagem);
            LOG.info("Login recusado (falha {} de {})", contagem.getFalhas(), parametros.maxTentativas());
            throw contagem.bloqueado(agora) ? new LoginBloqueadoException() : new CredenciaisInvalidasException();
        }

        Usuario conta = usuario.get();
        if (conta.getStatus() == StatusUsuario.PENDENTE_CONFIRMACAO) {
            throw new ContaPendenteException();
        }
        if (conta.isInterna()) {
            return new ResultadoLogin(null, segundoFator.emitir(conta));
        }
        bloqueio.ifPresent(bloqueios::delete);
        return new ResultadoLogin(sessoes.abrir(conta), null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:55
     * Cartão Jira: KAN-75
     * Título: Resultado do login
     * Objetivo: Levar a sessão aberta ou o desafio do segundo fator.
     * Decisões: Exatamente um dos dois vem preenchido.
     */
    public record ResultadoLogin(SessaoService.SessaoEmitida sessao, SegundoFatorService.Desafio desafio) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Sair
     * Objetivo: Encerrar a sessão do refresh token apresentado.
     * Decisões: Delegado ao serviço de sessão; o access token cai na hora (KAN-57).
     */
    public void sair(String refreshToken) {
        sessoes.encerrar(refreshToken);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Parâmetros do login
     * Objetivo: Número de falhas seguidas que bloqueia e a duração do bloqueio.
     * Decisões: Padrão de 5 falhas e 15 minutos (OWASP); configuráveis e, no KAN-75, editáveis no painel.
     */
    @ConfigurationProperties("orbit.seguranca.login")
    public record Parametros(int maxTentativas, Duration bloqueio) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Credenciais inválidas
     * Objetivo: Sinalizar e-mail ou senha incorretos.
     * Decisões: Mensagem única, sem dizer qual dos dois está errado.
     */
    public static class CredenciaisInvalidasException extends RuntimeException {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:10
         * Cartão Jira: KAN-13
         * Título: Criação da exceção
         * Objetivo: Montar a mensagem exibida.
         * Decisões: Sem stack trace.
         */
        public CredenciaisInvalidasException() {
            super("E-mail ou senha incorretos.", null, false, false);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Login bloqueado
     * Objetivo: Sinalizar o bloqueio temporário por excesso de tentativas.
     * Decisões: A mesma mensagem para e-mail com ou sem conta.
     */
    public static class LoginBloqueadoException extends RuntimeException {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:10
         * Cartão Jira: KAN-13
         * Título: Criação da exceção
         * Objetivo: Montar a mensagem exibida.
         * Decisões: Sem stack trace.
         */
        public LoginBloqueadoException() {
            super("Muitas tentativas seguidas. Por segurança, aguarde alguns minutos e tente de novo.", null, false, false);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:10
     * Cartão Jira: KAN-13
     * Título: Conta pendente
     * Objetivo: Sinalizar que a senha está certa, mas o e-mail ainda não foi confirmado.
     * Decisões: A tela oferece o reenvio do link.
     */
    public static class ContaPendenteException extends RuntimeException {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 01:10
         * Cartão Jira: KAN-13
         * Título: Criação da exceção
         * Objetivo: Montar a mensagem exibida.
         * Decisões: Sem stack trace.
         */
        public ContaPendenteException() {
            super("Confirme seu e-mail para entrar. Se o link expirou, peça um novo.", null, false, false);
        }
    }
}
