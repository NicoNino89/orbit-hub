package br.com.orbithub.conexoes.domain;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 23:51
 * Cartão Jira: KAN-59
 * Título: Rodada de investimento
 * Objetivo: Representar a rodada da startup com os termos de captação e o ciclo rascunho → aberta → em negociação →
 *           encerrada.
 * Decisões: Fonte única dos termos (estágio, instrumento, valores, prazo e termos específicos). A validação dos valores
 *           fica no serviço; aqui ficam as transições.
 */
@Entity
@Table(name = "rodada")
public class Rodada {

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private SituacaoDaRodada situacao;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private ResultadoDaRodada resultado;

    @Column(length = 60)
    private String estagio;

    @Column(length = 60)
    private String instrumento;

    @Column(name = "valor_alvo", precision = 18, scale = 2)
    private BigDecimal valorAlvo;

    @Column(name = "valor_minimo", precision = 18, scale = 2)
    private BigDecimal valorMinimo;

    @Column(name = "valor_maximo", precision = 18, scale = 2)
    private BigDecimal valorMaximo;

    private LocalDate prazo;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "rodada_termo", joinColumns = @JoinColumn(name = "rodada_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "termo", length = 40)
    @Column(name = "valor", nullable = false, columnDefinition = "TEXT")
    private Map<TermoDaRodada, String> termos = new EnumMap<>(TermoDaRodada.class);

    @Column(name = "criada_por", nullable = false)
    private UUID criadaPor;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @Column(name = "aberta_em")
    private Instant abertaEm;

    @Column(name = "encerrada_em")
    private Instant encerradaEm;

    @Column(name = "lider_chave", insertable = false, updatable = false)
    private UUID liderChave;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a rodada ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #nova}.
     */
    protected Rodada() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Nova rodada
     * Objetivo: Criar a rodada em rascunho com os termos informados.
     * Decisões: Os termos já vêm conferidos pelo serviço.
     */
    public static Rodada nova(UUID startupId, Termos termos, UUID autor, Instant agora) {
        Rodada r = new Rodada();
        r.id = UUID.randomUUID();
        r.startupId = startupId;
        r.situacao = SituacaoDaRodada.RASCUNHO;
        r.criadaPor = autor;
        r.criadaEm = agora;
        r.atualizar(termos, agora);
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Atualizar termos
     * Objetivo: Trocar os termos de captação da rodada.
     * Decisões: Só em rascunho ou aberta; os termos específicos são substituídos.
     */
    public void atualizar(Termos t, Instant agora) {
        exigir(EnumSet.of(SituacaoDaRodada.RASCUNHO, SituacaoDaRodada.ABERTA));
        this.estagio = t.estagio();
        this.instrumento = t.instrumento();
        this.valorAlvo = t.valorAlvo();
        this.valorMinimo = t.valorMinimo();
        this.valorMaximo = t.valorMaximo();
        this.prazo = t.prazo();
        this.termos.clear();
        this.termos.putAll(t.especificos());
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Abrir
     * Objetivo: Passar a rodada de rascunho para aberta.
     * Decisões: Os obrigatórios para abrir são conferidos no serviço.
     */
    public SituacaoDaRodada abrir(Instant agora) {
        SituacaoDaRodada anterior = mudar(EnumSet.of(SituacaoDaRodada.RASCUNHO), SituacaoDaRodada.ABERTA);
        this.abertaEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Iniciar negociação
     * Objetivo: Passar a rodada aberta para em negociação.
     * Decisões: Com os participantes (S7), acontece quando o primeiro investidor entra.
     */
    public SituacaoDaRodada iniciarNegociacao() {
        return mudar(EnumSet.of(SituacaoDaRodada.ABERTA), SituacaoDaRodada.EM_NEGOCIACAO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Encerrar
     * Objetivo: Encerrar a rodada como concluída ou cancelada.
     * Decisões: Rascunho só pode ser cancelado (não houve captação).
     */
    public SituacaoDaRodada encerrar(ResultadoDaRodada comResultado, Instant agora) {
        if (situacao == SituacaoDaRodada.RASCUNHO && comResultado == ResultadoDaRodada.CONCLUIDA) {
            throw new RegraDeNegocioException("rodada_nao_aberta", "Uma rodada em rascunho só pode ser cancelada.");
        }
        SituacaoDaRodada anterior = mudar(EnumSet.of(SituacaoDaRodada.RASCUNHO, SituacaoDaRodada.ABERTA, SituacaoDaRodada.EM_NEGOCIACAO,
                SituacaoDaRodada.AGUARDANDO_RESULTADO), SituacaoDaRodada.ENCERRADA);
        this.resultado = comResultado;
        this.encerradaEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-106
     * Título: Aguardar resultado
     * Objetivo: Passar a rodada aberta ou em negociação com o prazo vencido para "aguardando resultado".
     * Decisões: Usado pela R6; novas entradas ficam bloqueadas.
     */
    public SituacaoDaRodada aguardarResultado() {
        return mudar(EnumSet.of(SituacaoDaRodada.ABERTA, SituacaoDaRodada.EM_NEGOCIACAO), SituacaoDaRodada.AGUARDANDO_RESULTADO);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-109
     * Título: Prorrogar
     * Objetivo: Trocar o prazo da rodada e reabrir a que aguardava o resultado.
     * Decisões: Os limites (quantas vezes e quantos dias) ficam no serviço.
     */
    public SituacaoDaRodada prorrogar(LocalDate novoPrazo, Instant agora) {
        exigir(EnumSet.of(SituacaoDaRodada.ABERTA, SituacaoDaRodada.EM_NEGOCIACAO, SituacaoDaRodada.AGUARDANDO_RESULTADO));
        SituacaoDaRodada anterior = situacao;
        if (situacao == SituacaoDaRodada.AGUARDANDO_RESULTADO) {
            situacao = SituacaoDaRodada.ABERTA;
        }
        this.prazo = novoPrazo;
        this.atualizadaEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Sem líder
     * Objetivo: Dizer se a rodada ainda não tem investidor líder.
     * Decisões: O líder é gravado pela condução da rodada (KAN-59); aqui só leitura.
     */
    public boolean semLider() {
        return liderChave == null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 14:23
     * Cartão Jira: KAN-59
     * Título: Prazo (leitura)
     * Objetivo: Expor o prazo.
     * Decisões: Ver o campo.
     */
    public LocalDate getPrazo() {
        return prazo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Mudar situação
     * Objetivo: Conferir a origem e aplicar a situação nova.
     * Decisões: Devolve a anterior, para o histórico.
     */
    private SituacaoDaRodada mudar(Set<SituacaoDaRodada> origens, SituacaoDaRodada destino) {
        exigir(origens);
        SituacaoDaRodada anterior = situacao;
        situacao = destino;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Exigir situação
     * Objetivo: Recusar a operação fora das situações permitidas.
     * Decisões: Mensagem com a situação atual.
     */
    private void exigir(Set<SituacaoDaRodada> permitidas) {
        if (!permitidas.contains(situacao)) {
            throw new RegraDeNegocioException("situacao_invalida",
                    "Não é possível fazer isso com a rodada " + situacao.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termos
     * Objetivo: Devolver os termos de captação atuais.
     * Decisões: Cópia dos termos específicos.
     */
    public Termos termos() {
        return new Termos(estagio, instrumento, valorAlvo, valorMinimo, valorMaximo, prazo,
                termos.isEmpty() ? Map.of() : new EnumMap<>(termos));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador da rodada.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso à startup
     * Objetivo: Expor a startup da rodada.
     * Decisões: Somente leitura.
     */
    public UUID getStartupId() {
        return startupId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso à situação
     * Objetivo: Expor a situação da rodada.
     * Decisões: Somente leitura.
     */
    public SituacaoDaRodada getSituacao() {
        return situacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao resultado
     * Objetivo: Expor como a rodada terminou.
     * Decisões: Nulo enquanto não encerrada.
     */
    public ResultadoDaRodada getResultado() {
        return resultado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso à abertura
     * Objetivo: Expor quando a rodada foi aberta.
     * Decisões: Nulo em rascunho.
     */
    public Instant getAbertaEm() {
        return abertaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso ao encerramento
     * Objetivo: Expor quando a rodada foi encerrada.
     * Decisões: Nulo enquanto não encerrada.
     */
    public Instant getEncerradaEm() {
        return encerradaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Acesso à atualização
     * Objetivo: Expor quando os termos foram alterados.
     * Decisões: Somente leitura.
     */
    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:51
     * Cartão Jira: KAN-59
     * Título: Termos de captação
     * Objetivo: Levar os termos comuns e os específicos do instrumento.
     * Decisões: Valores em reais; prazo é a data de encerramento da captação.
     */
    public record Termos(String estagio, String instrumento, BigDecimal valorAlvo, BigDecimal valorMinimo, BigDecimal valorMaximo,
                         LocalDate prazo, Map<TermoDaRodada, String> especificos) {
    }
}
