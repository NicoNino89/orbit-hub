package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.parametros.Chaves;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.compartilhado.seguranca.SegurancaProperties;
import br.com.orbithub.compartilhado.seguranca.VerificadorDeSessao;
import br.com.orbithub.identidade.domain.RefreshToken;
import br.com.orbithub.identidade.domain.StatusRefreshToken;
import br.com.orbithub.identidade.domain.Usuario;
import br.com.orbithub.identidade.repository.RefreshTokenRepository;
import br.com.orbithub.identidade.repository.UsuarioRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Sessões com refresh token revogável
 * Objetivo: Abrir, renovar (com rotação) e encerrar sessões, e responder se uma sessão está ativa.
 * Decisões: Cada login abre uma família. A renovação troca o token por um novo e marca o antigo como
 *           usado; reapresentar um token já usado (fora da tolerância de abas simultâneas) é tratado como
 *           roubo e revoga todas as sessões do usuário. O valor do token só existe na resposta ao
 *           navegador; o banco guarda o hash SHA-256.
 */
@Service
public class SessaoService implements VerificadorDeSessao {

    static final String MOTIVO_LOGOUT = "logout";
    static final String MOTIVO_REUSO = "reuso_detectado";
    static final String MOTIVO_CONTA_INATIVA = "conta_inativa";
    private static final Logger LOG = LoggerFactory.getLogger(SessaoService.class);
    private static final int BYTES_DO_TOKEN = 32;

    private final RefreshTokenRepository tokens;
    private final UsuarioRepository usuarios;
    private final TokenAcessoService tokenAcesso;
    private final SegurancaProperties propriedades;
    private final LeituraDeParametros parametros;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, emissor de access token, parâmetros e relógio.
     * Decisões: Injeção pelo construtor.
     */
    public SessaoService(RefreshTokenRepository tokens, UsuarioRepository usuarios, TokenAcessoService tokenAcesso,
                         SegurancaProperties propriedades, LeituraDeParametros parametros, Clock relogio) {
        this.tokens = tokens;
        this.usuarios = usuarios;
        this.tokenAcesso = tokenAcesso;
        this.propriedades = propriedades;
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Abrir sessão
     * Objetivo: Criar uma família nova com o primeiro refresh token e o access token da conta.
     * Decisões: Chamado depois que a senha foi conferida (login, KAN-13); a família recebe o limite
     *           absoluto da duração máxima da sessão.
     */
    @Transactional
    public SessaoEmitida abrir(Usuario usuario) {
        if (!usuario.podeTerSessao()) {
            throw new SessaoInvalidaException(MOTIVO_CONTA_INATIVA);
        }
        Instant agora = relogio.instant();
        UUID familia = UUID.randomUUID();
        Instant limite = agora.plus(parametros.duracao(Chaves.SESSAO_DURACAO_MAXIMA));
        return emitir(usuario, familia, agora, limite);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Renovar sessão
     * Objetivo: Trocar um refresh token válido por um novo e emitir um access token novo.
     * Decisões: Token desconhecido, expirado ou revogado: 401 genérico. Token já usado dentro da tolerância:
     *           corrida entre abas, sem punição. Fora da tolerância: reuso, revoga todas as sessões. As
     *           revogações ficam gravadas mesmo com a exceção (noRollbackFor).
     */
    @Transactional(noRollbackFor = SessaoInvalidaException.class)
    public SessaoEmitida renovar(String refreshBruto) {
        Instant agora = relogio.instant();
        RefreshToken atual = tokens.findByHash(hash(refreshBruto))
                .orElseThrow(() -> new SessaoInvalidaException("token_desconhecido"));

        if (atual.getStatus() == StatusRefreshToken.USADO) {
            if (atual.usadoHaMenosDe(propriedades.sessao().toleranciaReuso(), agora)) {
                throw new RenovacaoConcorrenteException();
            }
            int revogados = revogarTodasDoUsuario(atual.getUsuarioId(), MOTIVO_REUSO, agora);
            LOG.warn("Reuso de refresh token detectado: {} sessões revogadas (usuario={})",
                    revogados, atual.getUsuarioId());
            throw new SessaoInvalidaException(MOTIVO_REUSO);
        }
        if (!atual.valido(agora)) {
            throw new SessaoInvalidaException("token_expirado_ou_revogado");
        }
        Usuario usuario = usuarios.findById(atual.getUsuarioId())
                .orElseThrow(() -> new SessaoInvalidaException(MOTIVO_CONTA_INATIVA));
        if (!usuario.podeTerSessao()) {
            atual.revogar(agora, MOTIVO_CONTA_INATIVA);
            throw new SessaoInvalidaException(MOTIVO_CONTA_INATIVA);
        }
        atual.marcarUsado(agora);
        return emitir(usuario, atual.getFamiliaId(), agora, atual.getFamiliaExpiraEm());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Encerrar sessão
     * Objetivo: Revogar a família do refresh token apresentado (logout).
     * Decisões: Silencioso para token desconhecido: sair sempre "dá certo" para quem pediu, sem revelar nada.
     */
    @Transactional
    public void encerrar(String refreshBruto) {
        if (refreshBruto == null || refreshBruto.isBlank()) {
            return;
        }
        Instant agora = relogio.instant();
        tokens.findByHash(hash(refreshBruto)).ifPresent(token ->
                tokens.findByFamiliaIdAndStatus(token.getFamiliaId(), StatusRefreshToken.ATIVO)
                        .forEach(t -> t.revogar(agora, MOTIVO_LOGOUT)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Encerrar todas as sessões
     * Objetivo: Revogar todos os refresh tokens ativos de um usuário.
     * Decisões: Usado na redefinição de senha (KAN-14), na troca de senha (KAN-16) e na exclusão de conta
     *           (KAN-17); devolve quantas sessões caíram.
     */
    @Transactional
    public int encerrarTodas(UUID usuarioId, String motivo) {
        return revogarTodasDoUsuario(usuarioId, motivo, relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Encerrar as outras sessões
     * Objetivo: Revogar todas as sessões do usuário, menos a informada.
     * Decisões: Na troca de senha pelo perfil, a pessoa continua logada no aparelho em que trocou e os demais
     *           aparelhos são desconectados.
     */
    @Transactional
    public int encerrarOutras(UUID usuarioId, UUID sessaoMantida, String motivo) {
        Instant agora = relogio.instant();
        var outras = tokens.findByUsuarioIdAndStatus(usuarioId, StatusRefreshToken.ATIVO).stream()
                .filter(t -> !t.getFamiliaId().equals(sessaoMantida))
                .toList();
        outras.forEach(t -> t.revogar(agora, motivo));
        return outras.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão ativa
     * Objetivo: Responder, a cada requisição autenticada, se a sessão do access token continua aberta.
     * Decisões: Ativa = há token ATIVO na família ainda dentro da validade; assim logout, troca de senha e
     *           inatividade derrubam também o access token já emitido.
     */
    @Override
    @Transactional(readOnly = true)
    public boolean sessaoAtiva(UUID sessaoId) {
        return tokens.existsByFamiliaIdAndStatusAndExpiraEmAfter(sessaoId, StatusRefreshToken.ATIVO,
                relogio.instant());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Emitir par de tokens
     * Objetivo: Gerar e gravar o refresh token da família e assinar o access token.
     * Decisões: 256 bits aleatórios em Base64 URL-safe, sem padding, para caber no cookie.
     */
    private SessaoEmitida emitir(Usuario usuario, UUID familia, Instant agora, Instant limiteDaFamilia) {
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        aleatorio.nextBytes(bytes);
        String refreshBruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        RefreshToken novo = RefreshToken.emitir(usuario.getId(), familia, hash(refreshBruto), agora,
                parametros.duracao(Chaves.SESSAO_INATIVIDADE), limiteDaFamilia);
        tokens.save(novo);
        usuarios.registrarAcesso(usuario.getId(), agora);
        TokenAcessoService.TokenAcesso acesso = tokenAcesso.gerar(usuario, familia);
        return new SessaoEmitida(acesso.valor(), acesso.expiraEm(), refreshBruto, novo.getExpiraEm(), usuario,
                acesso.termosPendentes());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Revogar todas do usuário
     * Objetivo: Revogar os tokens ativos de um usuário com o motivo informado.
     * Decisões: Tokens já usados ou revogados não mudam; só os ativos abrem sessão.
     */
    private int revogarTodasDoUsuario(UUID usuarioId, String motivo, Instant agora) {
        var ativos = tokens.findByUsuarioIdAndStatus(usuarioId, StatusRefreshToken.ATIVO);
        ativos.forEach(t -> t.revogar(agora, motivo));
        return ativos.size();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Hash do refresh token
     * Objetivo: Calcular o SHA-256 (hex) do valor do token.
     * Decisões: SHA-256 sem sal basta: o token já tem 256 bits aleatórios, não é adivinhável como senha.
     */
    static String hash(String refreshBruto) {
        if (refreshBruto == null) {
            return "";
        }
        try {
            MessageDigest sha = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(sha.digest(refreshBruto.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException impossivel) {
            throw new IllegalStateException("SHA-256 indisponível", impossivel);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Sessão emitida
     * Objetivo: Devolver access token, refresh token (valor bruto, só para o cookie) e a conta.
     * Decisões: O valor bruto do refresh token nunca é registrado em log nem gravado.
     */
    public record SessaoEmitida(String tokenAcesso, Instant acessoExpiraEm, String refreshToken,
                                Instant refreshExpiraEm, Usuario usuario, boolean termosPendentes) {

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 24/11/2026
         * Hora: 00:20
         * Cartão Jira: KAN-57
         * Título: Representação textual segura
         * Objetivo: Evitar que os tokens apareçam em logs por acidente.
         * Decisões: Mostra só as expirações.
         */
        @Override
        public String toString() {
            return "SessaoEmitida[acessoExpiraEm=" + acessoExpiraEm + ", refreshExpiraEm=" + refreshExpiraEm + "]";
        }
    }
}
