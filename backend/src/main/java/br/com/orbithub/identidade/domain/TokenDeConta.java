package br.com.orbithub.identidade.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:00
 * Cartão Jira: KAN-12
 * Título: Token de uso único da conta
 * Objetivo: Representar um link enviado por e-mail (confirmação, redefinição de senha, troca de e-mail).
 * Decisões: Expira e só pode ser usado uma vez; o valor em si nunca é guardado, só o hash.
 */
@Entity
@Table(name = "token_conta")
public class TokenDeConta {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private Finalidade finalidade;

    @Column(nullable = false, length = 64)
    private String hash;

    @Column(length = 254)
    private String dado;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #emitir}.
     */
    protected TokenDeConta() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Emissão do token
     * Objetivo: Criar o registro de um token com finalidade, validade e dado opcional.
     * Decisões: O dado só é usado na troca de e-mail (novo endereço).
     */
    public static TokenDeConta emitir(UUID usuarioId, Finalidade finalidade, String hash, String dado,
                                      Instant agora, Duration validade) {
        TokenDeConta token = new TokenDeConta();
        token.id = UUID.randomUUID();
        token.usuarioId = usuarioId;
        token.finalidade = finalidade;
        token.hash = hash;
        token.dado = dado;
        token.criadoEm = agora;
        token.expiraEm = agora.plus(validade);
        return token;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Token utilizável
     * Objetivo: Dizer se o token ainda pode ser usado para a finalidade informada.
     * Decisões: Mesma finalidade, não usado e dentro da validade.
     */
    public boolean utilizavel(Finalidade esperada, Instant agora) {
        return finalidade == esperada && usadoEm == null && agora.isBefore(expiraEm);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Marcar como usado
     * Objetivo: Impedir um segundo uso do token.
     * Decisões: Também usado para invalidar tokens antigos quando um novo é pedido.
     */
    public void marcarUsado(Instant agora) {
        if (usadoEm == null) {
            usadoEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Acesso ao usuário
     * Objetivo: Expor o dono do token.
     * Decisões: Somente leitura.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Acesso ao dado
     * Objetivo: Expor o dado associado (novo e-mail na troca de e-mail).
     * Decisões: Nulo nas outras finalidades.
     */
    public String getDado() {
        return dado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Acesso à finalidade
     * Objetivo: Expor para que serve o token.
     * Decisões: Somente leitura.
     */
    public Finalidade getFinalidade() {
        return finalidade;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Acesso à expiração
     * Objetivo: Expor até quando o token vale.
     * Decisões: Somente leitura.
     */
    public Instant getExpiraEm() {
        return expiraEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:00
     * Cartão Jira: KAN-12
     * Título: Finalidades do token
     * Objetivo: Enumerar para que um token de conta pode servir.
     * Decisões: Um token de uma finalidade nunca vale para outra.
     */
    public enum Finalidade {
        CONFIRMACAO_EMAIL,
        REDEFINICAO_SENHA,
        TROCA_EMAIL
    }
}
