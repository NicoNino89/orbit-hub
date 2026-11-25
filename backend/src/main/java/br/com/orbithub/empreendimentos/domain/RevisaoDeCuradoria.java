package br.com.orbithub.empreendimentos.domain;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Embeddable;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-66, KAN-84
 * Título: Revisão da curadoria
 * Objetivo: Registrar cada revisão de uma startup pela curadoria: o pedido (publicação ou alteração relevante), quem
 *           assumiu, o checklist marcado, os apontamentos por seção, o parecer e a decisão.
 * Decisões: Uma revisão aberta por startup (índice único parcial). A recusa por fraude passa por "aguardando aprovação"
 *           até a segunda aprovação (KAN-81) decidir.
 */
@Entity
@Table(name = "revisao_curadoria")
public class RevisaoDeCuradoria {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Tipo da revisão
     * Objetivo: Separar a primeira publicação (ou republicação) da alteração relevante de uma startup já publicada.
     * Decisões: Na alteração, a versão aprovada continua no catálogo até a decisão.
     */
    public enum Tipo { PUBLICACAO, ALTERACAO }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Situação da revisão
     * Objetivo: Enumerar o ciclo da revisão.
     * Decisões: Cancelada quando o empreendedor cancela o envio.
     */
    public enum Situacao { EM_ANALISE, AGUARDANDO_APROVACAO, APROVADA, DEVOLVIDA, RECUSADA, CANCELADA }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Item da revisão
     * Objetivo: Guardar um item do checklist (atendido ou não, com comentário) ou um apontamento numa seção do cadastro.
     * Decisões: CHECKLIST usa o código da lista gerenciável; SECAO usa o nome da seção.
     */
    @Embeddable
    public record Item(@Column(nullable = false, length = 20) String tipo, @Column(nullable = false, length = 60) String codigo,
                       Boolean atendido, @Column(length = 1000) String comentario) {
        public static final String CHECKLIST = "CHECKLIST";
        public static final String SECAO = "SECAO";
    }

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao;

    @Column(name = "enviada_em", nullable = false)
    private Instant enviadaEm;

    @Column(name = "enviada_por")
    private UUID enviadaPor;

    @Column(name = "responsavel_id")
    private UUID responsavelId;

    @Column(name = "assumida_em")
    private Instant assumidaEm;

    @Column(name = "decidida_em")
    private Instant decididaEm;

    @Column(name = "decidida_por")
    private UUID decididaPor;

    @Column(length = 2000)
    private String parecer;

    @Column(name = "proposta_id")
    private UUID propostaId;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "revisao_curadoria_item", joinColumns = @JoinColumn(name = "revisao_id"))
    @OrderColumn(name = "ordem")
    private List<Item> itens = new ArrayList<>();

    @Version
    private long versao;

    protected RevisaoDeCuradoria() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Nova revisão
     * Objetivo: Abrir uma revisão em análise para a startup.
     * Decisões: Sem responsável: entra na fila por ordem de chegada.
     */
    public static RevisaoDeCuradoria nova(UUID startupId, Tipo tipo, UUID enviadaPor, Instant agora) {
        RevisaoDeCuradoria r = new RevisaoDeCuradoria();
        r.id = UUID.randomUUID();
        r.startupId = startupId;
        r.tipo = tipo;
        r.situacao = Situacao.EM_ANALISE;
        r.enviadaPor = enviadaPor;
        r.enviadaEm = agora;
        return r;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Assumir
     * Objetivo: Atribuir a revisão a um curador.
     * Decisões: Só em análise.
     */
    public void assumir(UUID curador, Instant agora) {
        exigirEmAnalise();
        this.responsavelId = curador;
        this.assumidaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-87
     * Título: Liberar
     * Objetivo: Devolver a revisão à fila quando o responsável se declara impedido.
     * Decisões: Mantém o restante.
     */
    public void liberar() {
        this.responsavelId = null;
        this.assumidaEm = null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Decidir
     * Objetivo: Registrar a decisão (aprovada, devolvida ou aguardando a segunda aprovação da recusa), o checklist, os
     *           apontamentos e o parecer.
     * Decisões: A proposta da segunda aprovação fica guardada para a recusa ser concluída depois.
     */
    public void decidir(Situacao resultado, UUID curador, List<Item> itens, String parecer, UUID propostaId, Instant agora) {
        exigirEmAnalise();
        this.situacao = resultado;
        this.itens.clear();
        this.itens.addAll(itens);
        this.parecer = parecer;
        this.propostaId = propostaId;
        this.decididaPor = curador;
        this.decididaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Concluir a recusa
     * Objetivo: Fechar a revisão depois da decisão da segunda aprovação.
     * Decisões: Rejeitada a proposta, a revisão volta para análise.
     */
    public void concluirRecusa(boolean aprovada, Instant agora) {
        if (situacao != Situacao.AGUARDANDO_APROVACAO) {
            throw new RegraDeNegocioException("revisao_sem_recusa", "Esta revisão não aguarda a segunda aprovação.");
        }
        this.situacao = aprovada ? Situacao.RECUSADA : Situacao.EM_ANALISE;
        this.decididaEm = aprovada ? agora : null;
        this.propostaId = aprovada ? propostaId : null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Cancelar
     * Objetivo: Encerrar a revisão quando o empreendedor cancela o envio.
     * Decisões: Só em análise.
     */
    public void cancelar(Instant agora) {
        exigirEmAnalise();
        this.situacao = Situacao.CANCELADA;
        this.decididaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Exigir em análise
     * Objetivo: Recusar ações sobre revisão já decidida.
     * Decisões: Mensagem única.
     */
    private void exigirEmAnalise() {
        if (situacao != Situacao.EM_ANALISE) {
            throw new RegraDeNegocioException("revisao_fora_da_fila", "Esta revisão não está mais na fila.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-66
     * Título: Aberta
     * Objetivo: Dizer se a revisão ainda espera decisão.
     * Decisões: Inclui a espera da segunda aprovação.
     */
    public boolean aberta() {
        return situacao == Situacao.EM_ANALISE || situacao == Situacao.AGUARDANDO_APROVACAO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a Id
     * Objetivo: Expor Id.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a StartupId
     * Objetivo: Expor StartupId.
     * Decisões: Somente leitura.
     */
    public UUID getStartupId() {
        return startupId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a Tipo
     * Objetivo: Expor Tipo.
     * Decisões: Somente leitura.
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a Situacao
     * Objetivo: Expor Situacao.
     * Decisões: Somente leitura.
     */
    public Situacao getSituacao() {
        return situacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a EnviadaEm
     * Objetivo: Expor EnviadaEm.
     * Decisões: Somente leitura.
     */
    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a ResponsavelId
     * Objetivo: Expor ResponsavelId.
     * Decisões: Somente leitura.
     */
    public UUID getResponsavelId() {
        return responsavelId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a DecididaEm
     * Objetivo: Expor DecididaEm.
     * Decisões: Somente leitura.
     */
    public Instant getDecididaEm() {
        return decididaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a Parecer
     * Objetivo: Expor Parecer.
     * Decisões: Somente leitura.
     */
    public String getParecer() {
        return parecer;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a PropostaId
     * Objetivo: Expor PropostaId.
     * Decisões: Somente leitura.
     */
    public UUID getPropostaId() {
        return propostaId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Acesso a Itens
     * Objetivo: Expor Itens.
     * Decisões: Somente leitura.
     */
    public List<Item> getItens() {
        return List.copyOf(itens);
    }
}
