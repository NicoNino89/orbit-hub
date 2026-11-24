package br.com.orbithub.investidores.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Convite para a organização
 * Objetivo: Representar o convite enviado por e-mail para alguém entrar na organização com um papel.
 * Decisões: Guarda só o hash do token. Pode ser aceito uma vez, por uma conta com o mesmo e-mail, enquanto não
 *           vence nem é cancelado.
 */
@Entity
@Table(name = "organizacao_convite")
public class ConviteDeOrganizacao {

    @Id
    private UUID id;

    @Column(name = "organizacao_id", nullable = false)
    private UUID organizacaoId;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelNaOrganizacao papel;

    @Column(name = "token_hash", nullable = false, length = 64)
    private String tokenHash;

    @Column(name = "convidado_por", nullable = false)
    private UUID convidadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "expira_em", nullable = false)
    private Instant expiraEm;

    @Column(name = "aceito_em")
    private Instant aceitoEm;

    @Column(name = "aceito_por")
    private UUID aceitoPor;

    @Column(name = "cancelado_em")
    private Instant canceladoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o convite ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected ConviteDeOrganizacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Novo convite
     * Objetivo: Criar o convite pendente com a validade informada.
     * Decisões: E-mail em minúsculas, para comparar com a conta.
     */
    public static ConviteDeOrganizacao novo(UUID organizacaoId, String email, PapelNaOrganizacao papel, String tokenHash,
                                            UUID convidadoPor, Instant agora, Instant expiraEm) {
        ConviteDeOrganizacao c = new ConviteDeOrganizacao();
        c.id = UUID.randomUUID();
        c.organizacaoId = organizacaoId;
        c.email = email.strip().toLowerCase(Locale.ROOT);
        c.papel = papel;
        c.tokenHash = tokenHash;
        c.convidadoPor = convidadoPor;
        c.criadoEm = agora;
        c.expiraEm = expiraEm;
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Pendente
     * Objetivo: Dizer se o convite ainda pode ser aceito.
     * Decisões: Não aceito, não cancelado e dentro da validade.
     */
    public boolean pendente(Instant agora) {
        return aceitoEm == null && canceladoEm == null && agora.isBefore(expiraEm);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Aceitar
     * Objetivo: Registrar que a conta aceitou o convite.
     * Decisões: A conferência de validade e e-mail fica no serviço.
     */
    public void aceitar(UUID conta, Instant agora) {
        this.aceitoEm = agora;
        this.aceitoPor = conta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Cancelar
     * Objetivo: Invalidar o convite antes do aceite.
     * Decisões: Idempotente.
     */
    public void cancelar(Instant agora) {
        if (canceladoEm == null) {
            this.canceladoEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à organização
     * Objetivo: Expor a organização do convite.
     * Decisões: Somente leitura.
     */
    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao e-mail
     * Objetivo: Expor o e-mail convidado.
     * Decisões: Somente leitura.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao papel
     * Objetivo: Expor o papel oferecido.
     * Decisões: Somente leitura.
     */
    public PapelNaOrganizacao getPapel() {
        return papel;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à validade
     * Objetivo: Expor quando o convite vence.
     * Decisões: Somente leitura.
     */
    public Instant getExpiraEm() {
        return expiraEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à criação
     * Objetivo: Expor quando o convite foi enviado.
     * Decisões: Somente leitura.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }
}
