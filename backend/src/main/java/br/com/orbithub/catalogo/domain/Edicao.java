package br.com.orbithub.catalogo.domain;

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
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edição temática do catálogo
 * Objetivo: Guardar a edição montada pelo curador: título, descrição, capa, critérios de inclusão, vigência, curador
 *           responsável, as startups na ordem escolhida e a situação.
 * Decisões: Rascunho → agendada (publicada antes do início) → vigente → arquivada. A rotina R10 move pela vigência;
 *           o curador também pode arquivar antes. Vigência em dias inteiros (horário de Brasília).
 */
@Entity
@Table(name = "edicao")
public class Edicao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Situação da edição
     * Objetivo: Enumerar o ciclo da edição.
     * Decisões: Só a vigente aparece para o investidor.
     */
    public enum Situacao { RASCUNHO, AGENDADA, VIGENTE, ARQUIVADA }

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String descricao;

    @Column(nullable = false, length = 1000)
    private String criterios;

    @Column(name = "capa_chave", length = 512)
    private String capaChave;

    @Column(name = "capa_tipo", length = 40)
    private String capaTipo;

    @Column(nullable = false)
    private LocalDate inicio;

    @Column(nullable = false)
    private LocalDate fim;

    @Column(name = "curador_id", nullable = false)
    private UUID curadorId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "edicao_startup", joinColumns = @JoinColumn(name = "edicao_id"))
    @OrderColumn(name = "ordem")
    @Column(name = "startup_id", nullable = false)
    private List<UUID> startups = new ArrayList<>();

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @Version
    private long versao;

    protected Edicao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Nova edição
     * Objetivo: Criar a edição em rascunho, com o curador responsável.
     * Decisões: Sem startups.
     */
    public static Edicao nova(Dados dados, UUID curador, Instant agora) {
        Edicao e = new Edicao();
        e.id = UUID.randomUUID();
        e.curadorId = curador;
        e.situacao = Situacao.RASCUNHO;
        e.criadaEm = agora;
        e.editar(dados, agora);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Editar
     * Objetivo: Trocar título, descrição, critérios e vigência.
     * Decisões: Arquivada não muda. Fim não pode vir antes do início.
     */
    public void editar(Dados d, Instant agora) {
        exigirNaoArquivada();
        if (d.fim().isBefore(d.inicio())) {
            throw new RegraDeNegocioException("vigencia_invalida", "O fim da vigência não pode ser antes do início.");
        }
        this.titulo = d.titulo().strip();
        this.descricao = d.descricao().strip();
        this.criterios = d.criterios().strip();
        this.inicio = d.inicio();
        this.fim = d.fim();
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Definir startups
     * Objetivo: Trocar a composição e a ordem das startups.
     * Decisões: Quem chama já conferiu as startups; aqui só se recusa repetição.
     */
    public void definirStartups(List<UUID> ordem, Instant agora) {
        exigirNaoArquivada();
        if (ordem.stream().distinct().count() != ordem.size()) {
            throw new RegraDeNegocioException("startup_repetida", "Uma startup aparece mais de uma vez na edição.");
        }
        this.startups.clear();
        this.startups.addAll(ordem);
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Publicar
     * Objetivo: Tirar a edição do rascunho: agendada se a vigência ainda não começou, vigente se já começou.
     * Decisões: Precisa de ao menos uma startup e de vigência que não terminou.
     */
    public void publicar(LocalDate hoje, Instant agora) {
        if (situacao != Situacao.RASCUNHO) {
            throw new RegraDeNegocioException("edicao_publicada", "A edição já foi publicada.");
        }
        if (startups.isEmpty()) {
            throw new RegraDeNegocioException("edicao_vazia", "Inclua ao menos uma startup antes de publicar.");
        }
        if (fim.isBefore(hoje)) {
            throw new RegraDeNegocioException("vigencia_encerrada", "A vigência já terminou. Ajuste as datas.");
        }
        this.situacao = inicio.isAfter(hoje) ? Situacao.AGENDADA : Situacao.VIGENTE;
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Seguir a vigência
     * Objetivo: Pôr no ar a agendada que começou e arquivar a que terminou.
     * Decisões: Usado pela R10. Devolve se mudou.
     */
    public boolean seguirVigencia(LocalDate hoje, Instant agora) {
        Situacao antes = situacao;
        if (situacao == Situacao.AGENDADA && !inicio.isAfter(hoje)) {
            situacao = Situacao.VIGENTE;
        }
        if ((situacao == Situacao.VIGENTE || situacao == Situacao.AGENDADA) && fim.isBefore(hoje)) {
            situacao = Situacao.ARQUIVADA;
        }
        if (situacao != antes) {
            atualizadaEm = agora;
            return true;
        }
        return false;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Arquivar
     * Objetivo: Tirar a edição do ar antes do fim da vigência.
     * Decisões: Sem volta.
     */
    public void arquivar(Instant agora) {
        exigirNaoArquivada();
        this.situacao = Situacao.ARQUIVADA;
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Trocar capa
     * Objetivo: Guardar a nova capa e devolver a chave da antiga, para apagar.
     * Decisões: A chave nunca sai na API.
     */
    public String trocarCapa(String chave, String tipo, Instant agora) {
        exigirNaoArquivada();
        String antiga = capaChave;
        this.capaChave = chave;
        this.capaTipo = tipo;
        this.atualizadaEm = agora;
        return antiga;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Exigir não arquivada
     * Objetivo: Recusar mudanças na edição arquivada.
     * Decisões: Mensagem única.
     */
    private void exigirNaoArquivada() {
        if (situacao == Situacao.ARQUIVADA) {
            throw new RegraDeNegocioException("edicao_arquivada", "A edição está arquivada e não muda mais.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
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
     * Cartão Jira: KAN-85
     * Título: Acesso a Titulo
     * Objetivo: Expor Titulo.
     * Decisões: Somente leitura.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Descricao
     * Objetivo: Expor Descricao.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Criterios
     * Objetivo: Expor Criterios.
     * Decisões: Somente leitura.
     */
    public String getCriterios() {
        return criterios;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a CapaChave
     * Objetivo: Expor CapaChave.
     * Decisões: Somente leitura.
     */
    public String getCapaChave() {
        return capaChave;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a CapaTipo
     * Objetivo: Expor CapaTipo.
     * Decisões: Somente leitura.
     */
    public String getCapaTipo() {
        return capaTipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Inicio
     * Objetivo: Expor Inicio.
     * Decisões: Somente leitura.
     */
    public LocalDate getInicio() {
        return inicio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a Fim
     * Objetivo: Expor Fim.
     * Decisões: Somente leitura.
     */
    public LocalDate getFim() {
        return fim;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a CuradorId
     * Objetivo: Expor CuradorId.
     * Decisões: Somente leitura.
     */
    public UUID getCuradorId() {
        return curadorId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
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
     * Cartão Jira: KAN-85
     * Título: Acesso a Startups
     * Objetivo: Expor Startups.
     * Decisões: Somente leitura.
     */
    public List<UUID> getStartups() {
        return List.copyOf(startups);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Acesso a AtualizadaEm
     * Objetivo: Expor AtualizadaEm.
     * Decisões: Somente leitura.
     */
    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Dados da edição
     * Objetivo: Levar título, descrição, critérios e vigência.
     * Decisões: Validação de tamanho na API.
     */
    public record Dados(String titulo, String descricao, String criterios, LocalDate inicio, LocalDate fim) {
    }
}
