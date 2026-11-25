package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Set;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:48
 * Cartão Jira: KAN-65
 * Título: Membro da startup
 * Objetivo: Representar o vínculo de uma pessoa com a equipe da startup, com o papel.
 * Decisões: Sair grava a data e o motivo e mantém o registro (histórico de quem agiu pela startup). Colaborador edita só
 *           as seções autorizadas (KAN-65).
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

    @Column(name = "motivo_saida", length = 20)
    private String motivoSaida;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "startup_membro_secao", joinColumns = @JoinColumn(name = "membro_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "secao", length = 40)
    private Set<SecaoDaStartup> secoes = EnumSet.noneOf(SecaoDaStartup.class);

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Trocar papel
     * Objetivo: Mudar o papel do membro e, para colaborador, as seções que ele edita.
     * Decisões: Fundadores não guardam seções (editam tudo).
     */
    public void trocarPapel(PapelNaStartup novo, Set<SecaoDaStartup> novasSecoes) {
        this.papel = novo;
        this.secoes.clear();
        if (novo == PapelNaStartup.COLABORADOR && novasSecoes != null) {
            this.secoes.addAll(novasSecoes);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Encerrar vínculo
     * Objetivo: Registrar a saída do membro.
     * Decisões: SAIU ou REMOVIDO; a partir daqui não dá acesso à startup.
     */
    public void encerrar(String motivo, Instant agora) {
        this.saiuEm = agora;
        this.motivoSaida = motivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Pode editar a seção
     * Objetivo: Dizer se o membro edita uma seção do cadastro.
     * Decisões: Fundadores editam todas; colaborador, só as autorizadas.
     */
    public boolean podeEditar(SecaoDaStartup secao) {
        return papel != PapelNaStartup.COLABORADOR || secoes.contains(secao);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Acesso às seções
     * Objetivo: Expor as seções que o colaborador edita.
     * Decisões: Cópia; vazia para fundadores.
     */
    public Set<SecaoDaStartup> getSecoes() {
        return secoes.isEmpty() ? Set.of() : EnumSet.copyOf(secoes);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:48
     * Cartão Jira: KAN-65
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador do vínculo.
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
     * Título: Acesso à entrada
     * Objetivo: Expor quando a pessoa entrou na equipe.
     * Decisões: Somente leitura.
     */
    public Instant getEntrouEm() {
        return entrouEm;
    }
}
