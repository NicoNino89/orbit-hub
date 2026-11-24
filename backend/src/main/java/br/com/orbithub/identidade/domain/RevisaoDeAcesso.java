package br.com.orbithub.identidade.domain;

import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:39
 * Cartão Jira: KAN-75
 * Título: Revisão periódica dos acessos da equipe
 * Objetivo: Registrar que o administrador conferiu quem continua na equipe e com quais papéis.
 * Decisões: Guarda uma fotografia de cada membro ativo (id, nome e papéis no momento), para a revisão continuar
 *           legível depois de mudanças. Não é alterada depois de gravada.
 */
@Entity
@Table(name = "revisao_acesso")
public class RevisaoDeAcesso {

    @Id
    private UUID id;

    @Column(name = "revisor_id", nullable = false)
    private UUID revisorId;

    @Column(name = "realizada_em", nullable = false)
    private Instant realizadaEm;

    @Column(length = 1000)
    private String observacao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "revisao_acesso_item", joinColumns = @JoinColumn(name = "revisao_id"))
    private List<Item> itens = new ArrayList<>();

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a revisão ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #registrar}.
     */
    protected RevisaoDeAcesso() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Registrar revisão
     * Objetivo: Criar a revisão com a fotografia dos membros confirmados.
     * Decisões: Observação opcional; vazia vira nula.
     */
    public static RevisaoDeAcesso registrar(UUID revisorId, String observacao, List<Item> itens, Instant agora) {
        RevisaoDeAcesso revisao = new RevisaoDeAcesso();
        revisao.id = UUID.randomUUID();
        revisao.revisorId = revisorId;
        revisao.realizadaEm = agora;
        revisao.observacao = observacao == null || observacao.isBlank() ? null : observacao.strip();
        revisao.itens = new ArrayList<>(itens);
        return revisao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador da revisão.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso ao revisor
     * Objetivo: Expor quem fez a revisão.
     * Decisões: Somente leitura.
     */
    public UUID getRevisorId() {
        return revisorId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso à data
     * Objetivo: Expor quando a revisão foi feita.
     * Decisões: Base para calcular a próxima.
     */
    public Instant getRealizadaEm() {
        return realizadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso à observação
     * Objetivo: Expor a observação do revisor.
     * Decisões: Pode ser nula.
     */
    public String getObservacao() {
        return observacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Acesso aos membros confirmados
     * Objetivo: Expor a fotografia dos membros da revisão.
     * Decisões: Lista somente leitura.
     */
    public List<Item> getItens() {
        return Collections.unmodifiableList(itens);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:39
     * Cartão Jira: KAN-75
     * Título: Membro confirmado na revisão
     * Objetivo: Guardar id, nome e papéis de um membro no momento da revisão.
     * Decisões: Papéis como texto separado por vírgula, em ordem alfabética.
     */
    @Embeddable
    public record Item(
            @Column(name = "membro_id", nullable = false) UUID membroId,
            @Column(nullable = false, length = 120) String nome,
            @Column(nullable = false, length = 300) String papeis) {
    }
}
