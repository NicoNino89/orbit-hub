package br.com.orbithub.identidade.service;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.identidade.domain.TokenDeConta;
import br.com.orbithub.identidade.domain.TokenDeConta.Finalidade;
import br.com.orbithub.identidade.repository.TokenDeContaRepository;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Links de uso único da conta
 * Objetivo: Gerar e consumir os tokens dos links enviados por e-mail.
 * Decisões: Gerar um token novo invalida os anteriores da mesma finalidade (só o último link vale).
 *           Consumir um token inválido, usado ou expirado dá sempre a mesma resposta.
 */
@Service
public class TokenDeContaService {

    private static final int BYTES_DO_TOKEN = 32;

    private final TokenDeContaRepository tokens;
    private final Clock relogio;
    private final SecureRandom aleatorio = new SecureRandom();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Criação do serviço
     * Objetivo: Receber o repositório e o relógio.
     * Decisões: Injeção pelo construtor.
     */
    public TokenDeContaService(TokenDeContaRepository tokens, Clock relogio) {
        this.tokens = tokens;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Gerar token
     * Objetivo: Criar um token novo para a finalidade e devolver o valor bruto (vai só no link do e-mail).
     * Decisões: Tokens anteriores da mesma finalidade são marcados como usados.
     */
    @Transactional
    public String gerar(UUID usuarioId, Finalidade finalidade, Duration validade, String dado) {
        Instant agora = relogio.instant();
        tokens.findByUsuarioIdAndFinalidade(usuarioId, finalidade).forEach(t -> t.marcarUsado(agora));
        byte[] bytes = new byte[BYTES_DO_TOKEN];
        aleatorio.nextBytes(bytes);
        String bruto = Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
        tokens.save(TokenDeConta.emitir(usuarioId, finalidade, SessaoService.hash(bruto), dado, agora, validade));
        return bruto;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Consumir token
     * Objetivo: Validar o token do link e marcá-lo como usado.
     * Decisões: 422 "link_invalido" para qualquer problema, sem dizer se expirou, foi usado ou não existe.
     */
    @Transactional
    public TokenDeConta consumir(String bruto, Finalidade finalidade) {
        Instant agora = relogio.instant();
        TokenDeConta token = tokens.findByHash(SessaoService.hash(bruto))
                .filter(t -> t.utilizavel(finalidade, agora))
                .orElseThrow(() -> new RegraDeNegocioException("link_invalido",
                        "Este link é inválido ou expirou. Peça um novo."));
        token.marcarUsado(agora);
        return token;
    }
}
