package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:14
 * Cartão Jira: KAN-79
 * Título: Alteração de um item de lista
 * Objetivo: Registrar no histórico cada inclusão, renomeação, desativação e reativação, com autor e data.
 * Decisões: Imutável; guarda o rótulo antes e depois.
 */
@Entity
@Immutable
@Table(name = "lista_item_historico")
public class AlteracaoDeItem {

    @Id
    private UUID id;

    @Column(name = "item_id", nullable = false)
    private UUID itemId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo acao;

    @Column(name = "rotulo_anterior", length = 160)
    private String rotuloAnterior;

    @Column(name = "rotulo_novo", length = 160)
    private String rotuloNovo;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o registro ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #registrar}.
     */
    protected AlteracaoDeItem() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Registrar alteração
     * Objetivo: Criar o registro do histórico do item.
     * Decisões: Rótulos podem ser nulos conforme a ação.
     */
    public static AlteracaoDeItem registrar(UUID itemId, Tipo acao, String anterior, String novo, UUID autor,
                                            Instant agora) {
        AlteracaoDeItem a = new AlteracaoDeItem();
        a.id = UUID.randomUUID();
        a.itemId = itemId;
        a.acao = acao;
        a.rotuloAnterior = anterior;
        a.rotuloNovo = novo;
        a.autorId = autor;
        a.ocorridoEm = agora;
        return a;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso à ação
     * Objetivo: Expor o que foi feito no item.
     * Decisões: Somente leitura.
     */
    public Tipo getAcao() {
        return acao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao rótulo anterior
     * Objetivo: Expor o rótulo antes da alteração.
     * Decisões: Nulo na inclusão.
     */
    public String getRotuloAnterior() {
        return rotuloAnterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao rótulo novo
     * Objetivo: Expor o rótulo depois da alteração.
     * Decisões: Somente leitura.
     */
    public String getRotuloNovo() {
        return rotuloNovo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso ao autor
     * Objetivo: Expor quem fez a alteração.
     * Decisões: Somente leitura.
     */
    public UUID getAutorId() {
        return autorId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Acesso à data
     * Objetivo: Expor quando a alteração aconteceu.
     * Decisões: Somente leitura.
     */
    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:14
     * Cartão Jira: KAN-79
     * Título: Tipo de alteração
     * Objetivo: Enumerar o que pode acontecer com um item.
     * Decisões: Não existe exclusão.
     */
    public enum Tipo {
        INCLUIDO,
        RENOMEADO,
        DESATIVADO,
        REATIVADO
    }
}
