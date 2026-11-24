package br.com.orbithub.compartilhado.rotinas;

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
 * Hora: 12:17
 * Cartão Jira: KAN-104
 * Título: Execução de uma rotina
 * Objetivo: Ler o registro de uma execução: quando começou e terminou, itens, erros e resultado.
 * Decisões: Só leitura pelo JPA; a gravação é feita pelo executor com SQL direto, porque a inserção do ciclo é a
 *           própria trava entre instâncias (ON CONFLICT DO NOTHING).
 */
@Entity
@Immutable
@Table(name = "rotina_execucao")
public class ExecucaoDeRotina {

    @Id
    private UUID id;

    @Column(nullable = false, length = 10)
    private String rotina;

    @Column(nullable = false, length = 60)
    private String ciclo;

    @Column(nullable = false, length = 12)
    private String disparo;

    @Column(name = "disparado_por")
    private UUID disparadoPor;

    @Column(name = "iniciada_em", nullable = false)
    private Instant iniciadaEm;

    @Column(name = "terminada_em")
    private Instant terminadaEm;

    private int itens;

    private int erros;

    @Column(nullable = false, length = 15)
    private String resultado;

    @Column(length = 500)
    private String mensagem;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o registro ao ler do banco.
     * Decisões: Protegido.
     */
    protected ExecucaoDeRotina() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso à rotina
     * Objetivo: Expor o código da rotina.
     * Decisões: Somente leitura.
     */
    public String getRotina() {
        return rotina;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso ao disparo
     * Objetivo: Dizer se a execução foi automática ou manual.
     * Decisões: AUTOMATICO ou MANUAL.
     */
    public String getDisparo() {
        return disparo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso ao início
     * Objetivo: Expor quando a execução começou.
     * Decisões: Somente leitura.
     */
    public Instant getIniciadaEm() {
        return iniciadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso ao fim
     * Objetivo: Expor quando a execução terminou.
     * Decisões: Nulo enquanto roda.
     */
    public Instant getTerminadaEm() {
        return terminadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso aos itens
     * Objetivo: Expor quantos itens foram tratados.
     * Decisões: Somente leitura.
     */
    public int getItens() {
        return itens;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso aos erros
     * Objetivo: Expor quantos itens deram erro.
     * Decisões: Somente leitura.
     */
    public int getErros() {
        return erros;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso ao resultado
     * Objetivo: Expor o resultado da execução.
     * Decisões: EM_ANDAMENTO, SUCESSO ou FALHA.
     */
    public String getResultado() {
        return resultado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso à mensagem
     * Objetivo: Expor o resumo da execução ou o motivo da falha.
     * Decisões: Sem dado pessoal.
     */
    public String getMensagem() {
        return mensagem;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:17
     * Cartão Jira: KAN-104
     * Título: Acesso a quem disparou
     * Objetivo: Expor quem pediu a execução manual.
     * Decisões: Nulo nas automáticas.
     */
    public UUID getDisparadoPor() {
        return disparadoPor;
    }
}
