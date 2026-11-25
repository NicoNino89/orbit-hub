package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Membro da startup
 * Objetivo: Representar o vínculo de uma pessoa com a equipe da startup, com o papel.
 * Decisões: Sair grava a data e mantém o registro (histórico de quem agiu pela startup). Convites e troca de papel
 *           chegam com o KAN-65.
 */
@Entity
@Table(name = "startup_membro")
public class MembroDaStartup {

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private PapelNaStartup papel;

    @Column(name = "entrou_em", nullable = false)
    private Instant entrouEm;

    @Column(name = "saiu_em")
    private Instant saiuEm;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o vínculo ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected MembroDaStartup() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Novo membro
     * Objetivo: Ligar a pessoa à startup com um papel.
     * Decisões: Começa ativo.
     */
    public static MembroDaStartup novo(UUID startupId, UUID usuarioId, PapelNaStartup papel, Instant agora) {
        MembroDaStartup m = new MembroDaStartup();
        m.id = UUID.randomUUID();
        m.startupId = startupId;
        m.usuarioId = usuarioId;
        m.papel = papel;
        m.entrouEm = agora;
        return m;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso à startup
     * Objetivo: Expor a startup do vínculo.
     * Decisões: Somente leitura.
     */
    public UUID getStartupId() {
        return startupId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso à pessoa
     * Objetivo: Expor a conta do membro.
     * Decisões: Somente leitura.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso ao papel
     * Objetivo: Expor o papel do membro.
     * Decisões: Somente leitura.
     */
    public PapelNaStartup getPapel() {
        return papel;
    }
}
