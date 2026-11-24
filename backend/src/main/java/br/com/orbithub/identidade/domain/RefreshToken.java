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
 * Hora: 00:20
 * Cartão Jira: KAN-57
 * Título: Refresh token revogável
 * Objetivo: Guardar o hash de um refresh token, a família (sessão) a que pertence e o seu ciclo de vida.
 * Decisões: A validade é deslizante: cada token vale pela janela de inatividade (KAN-15), mas nunca passa
 *           do limite absoluto da família, para uma sessão não durar para sempre só por estar em uso.
 */
@Entity
@Table(name = "refresh_token")
public class RefreshToken {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "familia_id", nullable = false)
    private UUID familiaId;

    @Column(nullable = false, length = 64)
    private String hash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatusRefreshToken status;

    @Column(name = "emitido_em", nullable = false)
    private Instant emitidoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "familia_expira_em", nullable = false)
    private Instant familiaExpiraEm;

    @Column(name = "usado_em")
    private Instant usadoEm;

    @Column(name = "revogado_em")
    private Instant revogadoEm;

    @Column(name = "motivo_revogacao", length = 40)
    private String motivoRevogacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #emitir}.
     */
    protected RefreshToken() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Emissão de um refresh token
     * Objetivo: Criar o registro de um token novo dentro de uma família.
     * Decisões: A expiração é o menor entre "agora + inatividade" e o limite absoluto da família.
     */
    public static RefreshToken emitir(UUID usuarioId, UUID familiaId, String hash, Instant agora,
                                      Duration inatividade, Instant familiaExpiraEm) {
        RefreshToken token = new RefreshToken();
        token.id = UUID.randomUUID();
        token.usuarioId = usuarioId;
        token.familiaId = familiaId;
        token.hash = hash;
        token.status = StatusRefreshToken.ATIVO;
        token.emitidoEm = agora;
        Instant porInatividade = agora.plus(inatividade);
        token.expiraEm = porInatividade.isBefore(familiaExpiraEm) ? porInatividade : familiaExpiraEm;
        token.familiaExpiraEm = familiaExpiraEm;
        return token;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Token ainda vale
     * Objetivo: Dizer se o token pode ser trocado por um novo.
     * Decisões: Ativo e dentro da validade; o instante exato da expiração já conta como expirado.
     */
    public boolean valido(Instant agora) {
        return status == StatusRefreshToken.ATIVO && agora.isBefore(expiraEm);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Marcar como trocado
     * Objetivo: Registrar que o token foi usado na rotação.
     * Decisões: Fica guardado como USADO (e não apagado) para detectar uma reapresentação.
     */
    public void marcarUsado(Instant agora) {
        status = StatusRefreshToken.USADO;
        usadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Revogar o token
     * Objetivo: Encerrar o token de forma definitiva.
     * Decisões: Idempotente: revogar de novo mantém a primeira data e o primeiro motivo.
     */
    public void revogar(Instant agora, String motivo) {
        if (status != StatusRefreshToken.REVOGADO) {
            status = StatusRefreshToken.REVOGADO;
            revogadoEm = agora;
            motivoRevogacao = motivo;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Troca recente
     * Objetivo: Dizer se o token foi trocado há menos que a tolerância informada.
     * Decisões: Duas abas renovando ao mesmo tempo reapresentam o mesmo token por milissegundos; dentro
     *           da tolerância isso não é tratado como roubo.
     */
    public boolean usadoHaMenosDe(Duration tolerancia, Instant agora) {
        return status == StatusRefreshToken.USADO && usadoEm != null
                && agora.isBefore(usadoEm.plus(tolerancia));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador do registro.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
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
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à família
     * Objetivo: Expor a sessão a que o token pertence.
     * Decisões: Somente leitura.
     */
    public UUID getFamiliaId() {
        return familiaId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à situação
     * Objetivo: Expor a situação do token.
     * Decisões: Somente leitura.
     */
    public StatusRefreshToken getStatus() {
        return status;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso à expiração
     * Objetivo: Expor até quando o token vale.
     * Decisões: Usada também como Max-Age do cookie.
     */
    public Instant getExpiraEm() {
        return expiraEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao limite da família
     * Objetivo: Expor o fim absoluto da sessão.
     * Decisões: Herdado pelos tokens da rotação.
     */
    public Instant getFamiliaExpiraEm() {
        return familiaExpiraEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:20
     * Cartão Jira: KAN-57
     * Título: Acesso ao motivo da revogação
     * Objetivo: Expor por que o token foi revogado.
     * Decisões: Nulo enquanto não revogado.
     */
    public String getMotivoRevogacao() {
        return motivoRevogacao;
    }
}
