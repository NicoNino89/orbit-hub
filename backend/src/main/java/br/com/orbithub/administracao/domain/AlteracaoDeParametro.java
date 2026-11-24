package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 11:08
 * Cartão Jira: KAN-78
 * Título: Alteração de um parâmetro
 * Objetivo: Registrar no histórico o valor anterior, o novo, o autor, a data, a justificativa e, nos sensíveis,
 *           quem aprovou e a proposta.
 * Decisões: Imutável: o histórico só recebe inserções.
 */
@Entity
@Immutable
@Table(name = "parametro_historico")
public class AlteracaoDeParametro {

    @Id
    private UUID id;

    @Column(nullable = false, length = 80)
    private String chave;

    @Column(name = "valor_anterior", nullable = false, length = 200)
    private String valorAnterior;

    @Column(name = "valor_novo", nullable = false, length = 200)
    private String valorNovo;

    @Column(name = "autor_id", nullable = false)
    private UUID autorId;

    @Column(name = "aprovado_por")
    private UUID aprovadoPor;

    @Column(name = "proposta_id")
    private UUID propostaId;

    @Column(nullable = false, length = 1000)
    private String justificativa;

    @Column(name = "alterado_em", nullable = false)
    private Instant alteradoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o registro ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #registrar}.
     */
    protected AlteracaoDeParametro() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Registrar alteração
     * Objetivo: Criar o registro do histórico.
     * Decisões: Aprovador e proposta só nos sensíveis.
     */
    public static AlteracaoDeParametro registrar(String chave, String anterior, String novo, UUID autor,
                                                 UUID aprovadoPor, UUID propostaId, String justificativa,
                                                 Instant agora) {
        AlteracaoDeParametro a = new AlteracaoDeParametro();
        a.id = UUID.randomUUID();
        a.chave = chave;
        a.valorAnterior = anterior;
        a.valorNovo = novo;
        a.autorId = autor;
        a.aprovadoPor = aprovadoPor;
        a.propostaId = propostaId;
        a.justificativa = justificativa;
        a.alteradoEm = agora;
        return a;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à chave
     * Objetivo: Expor o parâmetro alterado.
     * Decisões: Somente leitura.
     */
    public String getChave() {
        return chave;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao valor anterior
     * Objetivo: Expor o valor antes da alteração.
     * Decisões: Somente leitura.
     */
    public String getValorAnterior() {
        return valorAnterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao valor novo
     * Objetivo: Expor o valor depois da alteração.
     * Decisões: Somente leitura.
     */
    public String getValorNovo() {
        return valorNovo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao autor
     * Objetivo: Expor quem pediu a alteração.
     * Decisões: Nos sensíveis, é quem propôs.
     */
    public UUID getAutorId() {
        return autorId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso ao aprovador
     * Objetivo: Expor quem aprovou a alteração sensível.
     * Decisões: Nulo nos não sensíveis.
     */
    public UUID getAprovadoPor() {
        return aprovadoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à justificativa
     * Objetivo: Expor o motivo da alteração.
     * Decisões: Somente leitura.
     */
    public String getJustificativa() {
        return justificativa;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 11:08
     * Cartão Jira: KAN-78
     * Título: Acesso à data
     * Objetivo: Expor quando a alteração passou a valer.
     * Decisões: Somente leitura.
     */
    public Instant getAlteradoEm() {
        return alteradoEm;
    }
}
