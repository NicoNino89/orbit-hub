package br.com.orbithub.empreendimentos.domain;

import br.com.orbithub.compartilhado.convites.ConviteBase;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Arrays;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:20
 * Cartão Jira: KAN-65
 * Título: Convite para a equipe da startup
 * Objetivo: Representar o convite por e-mail para alguém entrar na equipe com um papel (e, colaborador, com seções).
 * Decisões: O ciclo do convite (token, validade, aceite, cancelamento) vem de {@link ConviteBase}; aqui ficam a startup,
 *           o papel e as seções.
 */
@Entity
@Table(name = "startup_convite")
public class ConviteDaStartup extends ConviteBase {

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PapelNaStartup papel;

    @Column(length = 500)
    private String secoes;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
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
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Novo convite
     * Objetivo: Criar o convite pendente.
     * Decisões: Seções só para colaborador, em ordem alfabética separadas por vírgula.
     */
    public static ConviteDaStartup novo(UUID startupId, String email, PapelNaStartup papel, Set<SecaoDaStartup> secoes,
                                        String tokenHash, UUID convidadoPor, Instant agora, Instant expiraEm) {
        ConviteDaStartup c = new ConviteDaStartup();
        c.iniciar(email, tokenHash, convidadoPor, agora, expiraEm);
        c.startupId = startupId;
        c.papel = papel;
        c.secoes = papel == PapelNaStartup.COLABORADOR && secoes != null && !secoes.isEmpty()
                ? secoes.stream().map(Enum::name).sorted().collect(Collectors.joining(",")) : null;
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:20
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
     * Hora: 23:20
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
     * Hora: 23:20
     * Cartão Jira: KAN-65
     * Título: Acesso ao papel
     * Objetivo: Expor o papel oferecido.
     * Decisões: Somente leitura.
     */
    public PapelNaStartup getPapel() {
        return papel;
    }
}
