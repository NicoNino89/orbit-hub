package br.com.orbithub.conexoes.domain;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:25
 * Cartão Jira: KAN-45, KAN-46, KAN-48, KAN-49
 * Título: Conexão entre investidor e startup
 * Objetivo: Guardar a manifestação de interesse (valor e condições preliminares, tipo de pedido e mensagem) e o ciclo da
 *           conexão, aceitando só as transições do diagrama do KAN-49.
 * Decisões: O investidor é a organização (se a pessoa atua por ela) ou a pessoa; a pessoa que agiu fica sempre
 *           registrada. Estados finais: recusada, expirada, retirada, investiu e encerrada. Investiu é marcado pelo
 *           registro do investimento (KAN-64, S9). O motivo de encerramento guarda também o lado que encerrou.
 */
@Entity
@Table(name = "conexao")
public class Conexao {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Situação da conexão
     * Objetivo: Enumerar os estados e dizer quais são finais e quais terminam mal.
     * Decisões: "Negativo" libera nova tentativa depois de conexao.nova_tentativa_dias.
     */
    public enum Situacao {
        PENDENTE, RECUSADA, EXPIRADA, RETIRADA, MATCH, EM_ANALISE, EM_NEGOCIACAO, INVESTIU, ENCERRADA;

        public boolean finalizada() {
            return this == RECUSADA || this == EXPIRADA || this == RETIRADA || this == INVESTIU || this == ENCERRADA;
        }

        public boolean negativa() {
            return finalizada() && this != INVESTIU;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-46
     * Título: Tipo de pedido
     * Objetivo: Enumerar o que o investidor pede junto com o interesse.
     * Decisões: Nenhum, informações adicionais ou apresentação.
     */
    public enum Pedido { NENHUM, INFORMACOES, APRESENTACAO }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Lado
     * Objetivo: Dizer quem fez a mudança: investidor, startup ou plataforma (rotina).
     * Decisões: Usado no histórico e no motivo do encerramento.
     */
    public enum Lado { INVESTIDOR, STARTUP, PLATAFORMA }

    private static final Map<Situacao, Set<Situacao>> TRANSICOES = Map.of(
            Situacao.PENDENTE, EnumSet.of(Situacao.RECUSADA, Situacao.EXPIRADA, Situacao.RETIRADA, Situacao.MATCH),
            Situacao.MATCH, EnumSet.of(Situacao.EM_ANALISE, Situacao.ENCERRADA),
            Situacao.EM_ANALISE, EnumSet.of(Situacao.EM_NEGOCIACAO, Situacao.ENCERRADA),
            Situacao.EM_NEGOCIACAO, EnumSet.of(Situacao.INVESTIU, Situacao.ENCERRADA));

    @Id
    private UUID id;

    @Column(name = "startup_id", nullable = false)
    private UUID startupId;

    @Column(name = "pessoa_id", nullable = false)
    private UUID pessoaId;

    @Column(name = "organizacao_id")
    private UUID organizacaoId;

    @Column(name = "investidor_chave", nullable = false)
    private UUID investidorChave;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Situacao situacao;

    @Column(name = "valor_preliminar", precision = 18, scale = 2)
    private BigDecimal valorPreliminar;

    @Column(length = 1000)
    private String condicoes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Pedido pedido;

    @Column(length = 1000)
    private String mensagem;

    @Column(length = 60)
    private String motivo;

    @Enumerated(EnumType.STRING)
    @Column(name = "motivo_lado", length = 20)
    private Lado motivoLado;

    @Column(nullable = false)
    private int lembretes;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @Version
    private long versao;

    protected Conexao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-45
     * Título: Nova manifestação
     * Objetivo: Criar a conexão pendente.
     * Decisões: A chave do investidor é a organização, se houver.
     */
    public static Conexao nova(UUID startupId, UUID pessoa, UUID organizacao, Interesse interesse, Instant agora) {
        Conexao c = new Conexao();
        c.id = UUID.randomUUID();
        c.startupId = startupId;
        c.pessoaId = pessoa;
        c.organizacaoId = organizacao;
        c.investidorChave = organizacao != null ? organizacao : pessoa;
        c.situacao = Situacao.PENDENTE;
        c.valorPreliminar = interesse.valor();
        c.condicoes = interesse.condicoes();
        c.pedido = interesse.pedido() == null ? Pedido.NENHUM : interesse.pedido();
        c.mensagem = interesse.mensagem();
        c.criadaEm = agora;
        c.atualizadaEm = agora;
        return c;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Mudar de situação
     * Objetivo: Aplicar uma transição do diagrama e devolver a situação anterior.
     * Decisões: Transição fora do diagrama é recusada.
     */
    public Situacao mudar(Situacao para, Instant agora) {
        if (!TRANSICOES.getOrDefault(situacao, Set.of()).contains(para)) {
            throw new RegraDeNegocioException("transicao_invalida", "A conexão está " + rotulo(situacao) + " e não pode passar para "
                    + rotulo(para) + ".");
        }
        Situacao anterior = situacao;
        situacao = para;
        atualizadaEm = agora;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Encerrar
     * Objetivo: Encerrar a conexão depois do Match, guardando o motivo e o lado.
     * Decisões: O motivo vem da lista MOTIVO_ENCERRAMENTO.
     */
    public Situacao encerrar(String codigoMotivo, Lado lado, Instant agora) {
        Situacao anterior = mudar(Situacao.ENCERRADA, agora);
        this.motivo = codigoMotivo;
        this.motivoLado = lado;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-48
     * Título: Recusar
     * Objetivo: Recusar a manifestação, guardando o motivo só para a plataforma.
     * Decisões: O investidor recebe mensagem neutra.
     */
    public Situacao recusar(String codigoMotivo, Instant agora) {
        Situacao anterior = mudar(Situacao.RECUSADA, agora);
        this.motivo = codigoMotivo;
        this.motivoLado = Lado.STARTUP;
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-105
     * Título: Registrar lembrete
     * Objetivo: Contar os lembretes enviados pela R2.
     * Decisões: Um por prazo da lista conexao.lembrete_dias.
     */
    public void registrarLembrete() {
        this.lembretes++;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Rótulo da situação
     * Objetivo: Escrever a situação em português, para as mensagens.
     * Decisões: Minúsculas.
     */
    public static String rotulo(Situacao s) {
        return switch (s) {
            case PENDENTE -> "pendente";
            case RECUSADA -> "recusada";
            case EXPIRADA -> "expirada";
            case RETIRADA -> "retirada";
            case MATCH -> "em Match";
            case EM_ANALISE -> "em análise";
            case EM_NEGOCIACAO -> "em negociação";
            case INVESTIU -> "com investimento";
            case ENCERRADA -> "encerrada";
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
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
     * Hora: 11:25
     * Cartão Jira: KAN-49
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
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a PessoaId
     * Objetivo: Expor PessoaId.
     * Decisões: Somente leitura.
     */
    public UUID getPessoaId() {
        return pessoaId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a OrganizacaoId
     * Objetivo: Expor OrganizacaoId.
     * Decisões: Somente leitura.
     */
    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a InvestidorChave
     * Objetivo: Expor InvestidorChave.
     * Decisões: Somente leitura.
     */
    public UUID getInvestidorChave() {
        return investidorChave;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
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
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a ValorPreliminar
     * Objetivo: Expor ValorPreliminar.
     * Decisões: Somente leitura.
     */
    public BigDecimal getValorPreliminar() {
        return valorPreliminar;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a Condicoes
     * Objetivo: Expor Condicoes.
     * Decisões: Somente leitura.
     */
    public String getCondicoes() {
        return condicoes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a Pedido
     * Objetivo: Expor Pedido.
     * Decisões: Somente leitura.
     */
    public Pedido getPedido() {
        return pedido;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a Mensagem
     * Objetivo: Expor Mensagem.
     * Decisões: Somente leitura.
     */
    public String getMensagem() {
        return mensagem;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a Motivo
     * Objetivo: Expor Motivo.
     * Decisões: Somente leitura.
     */
    public String getMotivo() {
        return motivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a MotivoLado
     * Objetivo: Expor MotivoLado.
     * Decisões: Somente leitura.
     */
    public Lado getMotivoLado() {
        return motivoLado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a Lembretes
     * Objetivo: Expor Lembretes.
     * Decisões: Somente leitura.
     */
    public int getLembretes() {
        return lembretes;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
     * Título: Acesso a CriadaEm
     * Objetivo: Expor CriadaEm.
     * Decisões: Somente leitura.
     */
    public Instant getCriadaEm() {
        return criadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-49
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
     * Hora: 11:25
     * Cartão Jira: KAN-45, KAN-46
     * Título: Dados do interesse
     * Objetivo: Levar o valor e as condições preliminares, o tipo de pedido e a mensagem.
     * Decisões: Todos opcionais.
     */
    public record Interesse(BigDecimal valor, String condicoes, Pedido pedido, String mensagem) {
    }
}
