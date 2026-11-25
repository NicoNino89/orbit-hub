package br.com.orbithub.empreendimentos.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:11
 * Cartão Jira: KAN-24
 * Título: Valor declarado de um indicador
 * Objetivo: Representar um número de tração com o mês de referência, quem declarou e quando.
 * Decisões: Imutável: corrigir é declarar de novo, e o histórico fica. A referência é guardada no primeiro dia do mês.
 */
@Entity
@Table(name = "startup_indicador")
public class ValorDeIndicador {

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private IndicadorDaStartup indicador;

    @Column(nullable = false, precision = 18, scale = 2)
    private BigDecimal valor;

    @Column(nullable = false)
    private LocalDate referencia;

    @Column(name = "registrado_por", nullable = false)
    private UUID registradoPor;

    @Column(name = "registrado_em", nullable = false)
    private Instant registradoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o valor ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected ValorDeIndicador() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Novo valor
     * Objetivo: Registrar um valor declarado.
     * Decisões: A validação fica no serviço.
     */
    public static ValorDeIndicador novo(UUID startupId, IndicadorDaStartup indicador, BigDecimal valor, YearMonth referencia,
                                        UUID autor, Instant agora) {
        ValorDeIndicador v = new ValorDeIndicador();
        v.id = UUID.randomUUID();
        v.startupId = startupId;
        v.indicador = indicador;
        v.valor = valor;
        v.referencia = referencia.atDay(1);
        v.registradoPor = autor;
        v.registradoEm = agora;
        return v;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso ao indicador
     * Objetivo: Expor qual indicador é.
     * Decisões: Somente leitura.
     */
    public IndicadorDaStartup getIndicador() {
        return indicador;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso ao valor
     * Objetivo: Expor o número declarado.
     * Decisões: Somente leitura.
     */
    public BigDecimal getValor() {
        return valor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso à referência
     * Objetivo: Expor o mês a que o número se refere.
     * Decisões: Mês e ano.
     */
    public YearMonth getReferencia() {
        return YearMonth.from(referencia);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Acesso ao registro
     * Objetivo: Expor quando o valor foi declarado.
     * Decisões: Somente leitura.
     */
    public Instant getRegistradoEm() {
        return registradoEm;
    }
}
