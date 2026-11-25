package br.com.orbithub.catalogo.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Histórico da edição
 * Objetivo: Registrar cada mudança na edição (dados, composição, ordem, capa, publicação e arquivamento) com quem fez.
 * Decisões: Sem autor quando é a rotina R10. Só inclusão.
 */
@Entity
@Table(name = "edicao_historico")
public class MudancaNaEdicao {

    @Id
    private UUID id;

    @Column(name = "edicao_id", nullable = false)
    private UUID edicaoId;

    @Column(nullable = false, length = 40)
    private String acao;

    @Column(length = 2000)
    private String detalhe;

    private UUID por;

    @Column(nullable = false)
    private Instant em;

    protected MudancaNaEdicao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Nova mudança
     * Objetivo: Criar o registro.
     * Decisões: Detalhe cortado em 2.000 caracteres.
     */
    public static MudancaNaEdicao nova(UUID edicaoId, String acao, String detalhe, UUID por, Instant em) {
        MudancaNaEdicao m = new MudancaNaEdicao();
        m.id = UUID.randomUUID();
        m.edicaoId = edicaoId;
        m.acao = acao;
        m.detalhe = detalhe == null || detalhe.length() <= 2000 ? detalhe : detalhe.substring(0, 2000);
        m.por = por;
        m.em = em;
        return m;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Acao
     * Objetivo: Expor Acao.
     * Decisões: Somente leitura.
     */
    public String getAcao() {
        return acao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Detalhe
     * Objetivo: Expor Detalhe.
     * Decisões: Somente leitura.
     */
    public String getDetalhe() {
        return detalhe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Por
     * Objetivo: Expor Por.
     * Decisões: Somente leitura.
     */
    public UUID getPor() {
        return por;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Em
     * Objetivo: Expor Em.
     * Decisões: Somente leitura.
     */
    public Instant getEm() {
        return em;
    }
}
