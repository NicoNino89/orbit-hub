package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Convite para a equipe da startup
 * Objetivo: Representar o convite por e-mail para alguém entrar na equipe com um papel (e, colaborador, com seções).
 * Decisões: Guarda só o hash do token. Aceito uma vez, pela conta com o mesmo e-mail, enquanto não vence nem é cancelado.
 */
@Entity
@Table(name = "startup_convite")
public class ConviteDaStartup {

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Column(nullable = false, length = 254)
    private String email;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PapelNaStartup papel;

    @Column(length = 500)
    private String secoes;

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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o convite ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected ConviteDaStartup() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Novo convite
     * Objetivo: Criar o convite pendente.
     * Decisões: E-mail em minúsculas; seções só para colaborador.
     */
    public static ConviteDaStartup novo(UUID startupId, String email, PapelNaStartup papel, Set<SecaoDaStartup> secoes,
                                        String tokenHash, UUID convidadoPor, Instant agora, Instant expiraEm) {
        ConviteDaStartup c = new ConviteDaStartup();
        c.id = UUID.randomUUID();
        c.startupId = startupId;
        c.email = email.strip().toLowerCase(Locale.ROOT);
        c.papel = papel;
        c.secoes = papel == PapelNaStartup.COLABORADOR && secoes != null && !secoes.isEmpty()
                ? secoes.stream().map(Enum::name).sorted().collect(Collectors.joining(",")) : null;
        c.tokenHash = tokenHash;
        c.convidadoPor = convidadoPor;
        c.criadoEm = agora;
        c.expiraEm = expiraEm;
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Aceitar
     * Objetivo: Registrar que a conta aceitou.
     * Decisões: A conferência fica no serviço.
     */
    public void aceitar(UUID conta, Instant agora) {
        this.aceitoEm = agora;
        this.aceitoPor = conta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Seções do convite
     * Objetivo: Devolver as seções oferecidas ao colaborador.
     * Decisões: Vazio para fundadores.
     */
    public Set<SecaoDaStartup> getSecoes() {
        if (secoes == null || secoes.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(secoes.split(",")).map(SecaoDaStartup::valueOf)
                .collect(Collectors.toCollection(() -> EnumSet.noneOf(SecaoDaStartup.class)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Acesso à startup
     * Objetivo: Expor a startup do convite.
     * Decisões: Somente leitura.
     */
    public UUID getStartupId() {
        return startupId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Acesso ao papel
     * Objetivo: Expor o papel oferecido.
     * Decisões: Somente leitura.
     */
    public PapelNaStartup getPapel() {
        return papel;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
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
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Acesso à criação
     * Objetivo: Expor quando o convite foi enviado.
     * Decisões: Somente leitura.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }
}
