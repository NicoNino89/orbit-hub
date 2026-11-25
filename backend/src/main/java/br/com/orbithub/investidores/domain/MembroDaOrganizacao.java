package br.com.orbithub.investidores.domain;

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
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Membro da organização
 * Objetivo: Representar o vínculo de uma pessoa com uma organização, com o papel e o período.
 * Decisões: Sair não apaga o vínculo: grava a data e o motivo (saiu ou foi removida), para o histórico de quem agiu
 *           em nome da organização. Vínculo encerrado não volta: um novo convite cria outro vínculo.
 */
@Entity
@Table(name = "organizacao_membro")
public class MembroDaOrganizacao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Motivo da saída
     * Objetivo: Diferenciar quem saiu por conta própria de quem foi removido por um administrador.
     * Decisões: Gravado como texto.
     */
    public enum MotivoSaida {
        SAIU,
        REMOVIDO
    }

    @Id
    private UUID id;

    @Column(name = "organizacao_id", nullable = false)
    private UUID organizacaoId;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PapelNaOrganizacao papel;

    @Column(name = "entrou_em", nullable = false)
    private Instant entrouEm;

    @Column(name = "saiu_em")
    private Instant saiuEm;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_saida", length = 20)
    private MotivoSaida motivoSaida;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o vínculo ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected MembroDaOrganizacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Novo vínculo
     * Objetivo: Ligar a pessoa à organização com um papel.
     * Decisões: Começa ativo.
     */
    public static MembroDaOrganizacao novo(UUID organizacaoId, UUID usuarioId, PapelNaOrganizacao papel, Instant agora) {
        MembroDaOrganizacao m = new MembroDaOrganizacao();
        m.id = UUID.randomUUID();
        m.organizacaoId = organizacaoId;
        m.usuarioId = usuarioId;
        m.papel = papel;
        m.entrouEm = agora;
        return m;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Trocar papel
     * Objetivo: Mudar o papel de um membro ativo.
     * Decisões: A regra do último administrador fica no serviço.
     */
    public void trocarPapel(PapelNaOrganizacao novo) {
        this.papel = novo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Encerrar vínculo
     * Objetivo: Registrar a saída do membro.
     * Decisões: A partir daqui o vínculo não dá acesso a nada da organização.
     */
    public void encerrar(MotivoSaida motivo, Instant agora) {
        this.saiuEm = agora;
        this.motivoSaida = motivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
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
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à organização
     * Objetivo: Expor a organização do vínculo.
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
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao papel
     * Objetivo: Expor o papel do membro.
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
     * Título: Acesso à entrada
     * Objetivo: Expor quando a pessoa entrou.
     * Decisões: Somente leitura.
     */
    public Instant getEntrouEm() {
        return entrouEm;
    }
}
