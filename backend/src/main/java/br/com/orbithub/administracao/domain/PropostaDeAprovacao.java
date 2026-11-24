package br.com.orbithub.administracao.domain;

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
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:52
 * Cartão Jira: KAN-81
 * Título: Proposta de ação grave
 * Objetivo: Representar uma ação grave proposta por uma pessoa e que espera a decisão de outra.
 * Decisões: Quem propõe nunca decide a própria proposta (regra também aqui, além do serviço). Toda proposta e
 *           toda decisão têm justificativa. Os dados da ação ficam guardados na proposta, para a ação aprovada ser
 *           exatamente a que foi proposta. Versão otimista: duas pessoas decidindo ao mesmo tempo, só uma vale.
 */
@Entity
@Table(name = "proposta_aprovacao")
public class PropostaDeAprovacao {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private AcaoGrave tipo;

    @Column(name = "objeto_tipo", nullable = false, length = 60)
    private String objetoTipo;

    @Column(name = "objeto_id", nullable = false, length = 80)
    private String objetoId;

    @Column(nullable = false, length = 300)
    private String resumo;

    @Column(nullable = false, length = 1000)
    private String justificativa;

    @Column(name = "proposto_por", nullable = false)
    private UUID propostoPor;

    @Column(name = "proposto_em", nullable = false)
    private Instant propostoEm;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao;

    @Column(name = "decidido_por")
    private UUID decididoPor;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    @Column(name = "justificativa_decisao", length = 1000)
    private String justificativaDecisao;

    @Column(name = "alertada_em")
    private Instant alertadaEm;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "proposta_aprovacao_dado", joinColumns = @JoinColumn(name = "proposta_id"))
    @MapKeyColumn(name = "chave", length = 60)
    @Column(name = "valor", length = 2000)
    private Map<String, String> dados = new LinkedHashMap<>();

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a proposta ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #propor}.
     */
    protected PropostaDeAprovacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Propor uma ação grave
     * Objetivo: Criar a proposta pendente com o objeto, o resumo, os dados e a justificativa.
     * Decisões: Justificativa obrigatória; os dados são copiados, para ninguém alterá-los depois de fora.
     */
    public static PropostaDeAprovacao propor(AcaoGrave tipo, String objetoTipo, String objetoId, String resumo,
                                             Map<String, String> dados, UUID propostoPor, String justificativa,
                                             Instant agora) {
        PropostaDeAprovacao proposta = new PropostaDeAprovacao();
        proposta.id = UUID.randomUUID();
        proposta.tipo = Objects.requireNonNull(tipo, "tipo");
        proposta.objetoTipo = Objects.requireNonNull(objetoTipo, "objetoTipo");
        proposta.objetoId = Objects.requireNonNull(objetoId, "objetoId");
        proposta.resumo = Objects.requireNonNull(resumo, "resumo");
        proposta.justificativa = exigirTexto(justificativa);
        proposta.propostoPor = Objects.requireNonNull(propostoPor, "propostoPor");
        proposta.propostoEm = agora;
        proposta.situacao = Situacao.PENDENTE;
        proposta.dados = new LinkedHashMap<>(dados == null ? Map.of() : dados);
        return proposta;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Aprovar
     * Objetivo: Registrar a aprovação por uma segunda pessoa.
     * Decisões: Só proposta pendente; quem propôs não aprova; justificativa obrigatória.
     */
    public void aprovar(UUID aprovador, String justificativaDecisao, Instant agora) {
        decidir(aprovador, justificativaDecisao, agora, Situacao.APROVADA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Rejeitar
     * Objetivo: Registrar a rejeição por uma segunda pessoa.
     * Decisões: Mesmas regras da aprovação; nada da ação é executado.
     */
    public void rejeitar(UUID aprovador, String justificativaDecisao, Instant agora) {
        decidir(aprovador, justificativaDecisao, agora, Situacao.REJEITADA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Cancelar
     * Objetivo: Deixar quem propôs desistir da proposta ainda pendente.
     * Decisões: Só quem propôs cancela; a decisão fica registrada com a própria pessoa.
     */
    public void cancelar(UUID quem, Instant agora) {
        exigirPendente();
        if (!propostoPor.equals(quem)) {
            throw new IllegalStateException("Só quem propôs pode cancelar");
        }
        situacao = Situacao.CANCELADA;
        decididoPor = quem;
        decididoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Proposta vencida
     * Objetivo: Dizer se a proposta está pendente há mais do que o prazo.
     * Decisões: Só pendentes vencem.
     */
    public boolean vencida(Instant agora, Duration prazo) {
        return situacao == Situacao.PENDENTE && !propostoEm.plus(prazo).isAfter(agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Marcar alerta enviado
     * Objetivo: Registrar que o administrador já foi alertado da proposta parada.
     * Decisões: Idempotente: não muda a data do primeiro alerta.
     */
    public void marcarAlertada(Instant agora) {
        if (alertadaEm == null) {
            alertadaEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Decidir
     * Objetivo: Aplicar a decisão (aprovar ou rejeitar).
     * Decisões: Regras comuns às duas decisões.
     */
    private void decidir(UUID aprovador, String texto, Instant agora, Situacao decisao) {
        exigirPendente();
        if (propostoPor.equals(aprovador)) {
            throw new IllegalStateException("Quem propõe não decide a própria proposta");
        }
        justificativaDecisao = exigirTexto(texto);
        situacao = decisao;
        decididoPor = Objects.requireNonNull(aprovador, "aprovador");
        decididoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Exigir pendente
     * Objetivo: Recusar decisão sobre proposta já decidida ou cancelada.
     * Decisões: IllegalStateException; o serviço traduz para mensagem de negócio.
     */
    private void exigirPendente() {
        if (situacao != Situacao.PENDENTE) {
            throw new IllegalStateException("A proposta não está mais pendente");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Exigir texto
     * Objetivo: Recusar justificativa vazia.
     * Decisões: Remove espaços nas pontas.
     */
    private static String exigirTexto(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new IllegalArgumentException("Justificativa obrigatória");
        }
        return texto.strip();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador da proposta.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao tipo
     * Objetivo: Expor a ação proposta.
     * Decisões: Somente leitura.
     */
    public AcaoGrave getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao tipo do objeto
     * Objetivo: Expor o tipo do objeto afetado.
     * Decisões: Somente leitura.
     */
    public String getObjetoTipo() {
        return objetoTipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao id do objeto
     * Objetivo: Expor o identificador do objeto afetado.
     * Decisões: Texto, porque parâmetros usam a chave.
     */
    public String getObjetoId() {
        return objetoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao resumo
     * Objetivo: Expor a frase que descreve a proposta para a tela.
     * Decisões: Escrita pelo módulo que propõe.
     */
    public String getResumo() {
        return resumo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso à justificativa
     * Objetivo: Expor a justificativa de quem propôs.
     * Decisões: Somente leitura.
     */
    public String getJustificativa() {
        return justificativa;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso a quem propôs
     * Objetivo: Expor a conta que fez a proposta.
     * Decisões: Somente leitura.
     */
    public UUID getPropostoPor() {
        return propostoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso à data da proposta
     * Objetivo: Expor quando a proposta foi feita.
     * Decisões: Base do prazo de análise.
     */
    public Instant getPropostoEm() {
        return propostoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso à situação
     * Objetivo: Expor a situação da proposta.
     * Decisões: Somente leitura.
     */
    public Situacao getSituacao() {
        return situacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso a quem decidiu
     * Objetivo: Expor a conta que aprovou, rejeitou ou cancelou.
     * Decisões: Nulo enquanto pendente.
     */
    public UUID getDecididoPor() {
        return decididoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso à data da decisão
     * Objetivo: Expor quando a proposta foi decidida.
     * Decisões: Nulo enquanto pendente.
     */
    public Instant getDecididoEm() {
        return decididoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso à justificativa da decisão
     * Objetivo: Expor por que a proposta foi aprovada ou rejeitada.
     * Decisões: Nula enquanto pendente e no cancelamento.
     */
    public String getJustificativaDecisao() {
        return justificativaDecisao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso ao alerta
     * Objetivo: Expor quando o administrador foi alertado da proposta parada.
     * Decisões: Nulo enquanto não houve alerta.
     */
    public Instant getAlertadaEm() {
        return alertadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Acesso aos dados da ação
     * Objetivo: Expor os dados que o executor usa ao aplicar a ação.
     * Decisões: Mapa somente leitura.
     */
    public Map<String, String> getDados() {
        return Collections.unmodifiableMap(dados);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:52
     * Cartão Jira: KAN-81
     * Título: Situação da proposta
     * Objetivo: Enumerar as situações possíveis.
     * Decisões: Pendente até alguém aprovar, rejeitar ou quem propôs cancelar.
     */
    public enum Situacao {
        PENDENTE,
        APROVADA,
        REJEITADA,
        CANCELADA
    }
}
