package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:40
 * Cartão Jira: KAN-27
 * Título: Mudança de situação da startup
 * Objetivo: Registrar cada passagem da startup entre rascunho, em revisão, publicada, devolvida e despublicada.
 * Decisões: Imutável. Autor nulo quando a mudança foi feita por rotina (R8).
 */
@Entity
@Table(name = "startup_situacao_historico")
public class MudancaDeSituacao {

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoDaStartup de;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoDaStartup para;

    private UUID por;

    @Column(length = 300)
    private String motivo;

    @Column(nullable = false)
    private Instant em;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o registro ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #nova}.
     */
    protected MudancaDeSituacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Nova mudança
     * Objetivo: Registrar a passagem de uma situação para outra.
     * Decisões: Motivo opcional.
     */
    public static MudancaDeSituacao nova(UUID startupId, SituacaoDaStartup de, SituacaoDaStartup para, UUID por, String motivo,
                                         Instant em) {
        MudancaDeSituacao m = new MudancaDeSituacao();
        m.id = UUID.randomUUID();
        m.startupId = startupId;
        m.de = de;
        m.para = para;
        m.por = por;
        m.motivo = motivo;
        m.em = em;
        return m;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso à situação anterior
     * Objetivo: Expor de onde a startup saiu.
     * Decisões: Somente leitura.
     */
    public SituacaoDaStartup getDe() {
        return de;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso à situação nova
     * Objetivo: Expor para onde a startup foi.
     * Decisões: Somente leitura.
     */
    public SituacaoDaStartup getPara() {
        return para;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso ao autor
     * Objetivo: Expor quem fez a mudança.
     * Decisões: Nulo para rotina.
     */
    public UUID getPor() {
        return por;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso ao motivo
     * Objetivo: Expor o motivo da mudança.
     * Decisões: Pode ser nulo.
     */
    public String getMotivo() {
        return motivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso à data
     * Objetivo: Expor quando a mudança aconteceu.
     * Decisões: Somente leitura.
     */
    public Instant getEm() {
        return em;
    }
}
