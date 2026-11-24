package br.com.orbithub.verificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Pedido de verificação
 * Objetivo: Representar o pedido de uma pessoa ou organização para ter a identidade conferida, do rascunho à decisão.
 * Decisões: Nasce em rascunho (os documentos sobem um a um) e vai para análise no envio. A decisão (KAN-83) grava quem
 *           decidiu, o motivo da lista e a validade. Mudanças de situação só por métodos com nome de regra.
 */
@Entity
@Table(name = "pedido_verificacao")
public class PedidoDeVerificacao {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(name = "sujeito_tipo", nullable = false, length = 20)
    private SujeitoDaVerificacao sujeitoTipo;

    @Column(name = "sujeito_id", nullable = false)
    private UUID sujeitoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 25)
    private SituacaoDoPedido situacao;

    @Column(name = "criado_por", nullable = false)
    private UUID criadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "enviado_em")
    private Instant enviadoEm;

    @Column(name = "decidido_por")
    private UUID decididoPor;

    @Column(name = "decidido_em")
    private Instant decididoEm;

    @Column(length = 60)
    private String motivo;

    @Column(length = 1000)
    private String observacao;

    @Column(name = "valido_ate")
    private Instant validoAte;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o pedido ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #rascunho}.
     */
    protected PedidoDeVerificacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Novo rascunho
     * Objetivo: Abrir o pedido em rascunho para receber os documentos.
     * Decisões: Guarda quem abriu (numa organização, qual administrador).
     */
    public static PedidoDeVerificacao rascunho(SujeitoDaVerificacao tipo, UUID sujeito, UUID autor, Instant agora) {
        PedidoDeVerificacao p = new PedidoDeVerificacao();
        p.id = UUID.randomUUID();
        p.sujeitoTipo = tipo;
        p.sujeitoId = sujeito;
        p.situacao = SituacaoDoPedido.RASCUNHO;
        p.criadoPor = autor;
        p.criadoEm = agora;
        return p;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Enviar para análise
     * Objetivo: Tirar do rascunho (ou do complemento) e colocar na fila da verificação.
     * Decisões: A data de envio marca a posição na fila e o início do prazo de análise.
     */
    public void enviar(Instant agora) {
        if (situacao != SituacaoDoPedido.RASCUNHO && situacao != SituacaoDoPedido.PENDENTE_COMPLEMENTO) {
            throw new IllegalStateException("Pedido não está em rascunho nem aguardando complemento");
        }
        this.situacao = SituacaoDoPedido.EM_ANALISE;
        this.enviadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Aceita documentos
     * Objetivo: Dizer se ainda dá para incluir ou trocar documentos.
     * Decisões: No rascunho e quando a análise pediu complemento.
     */
    public boolean aceitaDocumentos() {
        return situacao == SituacaoDoPedido.RASCUNHO || situacao == SituacaoDoPedido.PENDENTE_COMPLEMENTO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Verificado agora
     * Objetivo: Dizer se o pedido dá o selo de verificado no instante informado.
     * Decisões: Situação verificada e ainda dentro da validade.
     */
    public boolean verificadoEm(Instant instante) {
        return situacao == SituacaoDoPedido.VERIFICADO && validoAte != null && instante.isBefore(validoAte);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao tipo de sujeito
     * Objetivo: Expor quem está sendo verificado (pessoa, organização...).
     * Decisões: Somente leitura.
     */
    public SujeitoDaVerificacao getSujeitoTipo() {
        return sujeitoTipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao sujeito
     * Objetivo: Expor o identificador do sujeito.
     * Decisões: Somente leitura.
     */
    public UUID getSujeitoId() {
        return sujeitoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso à situação
     * Objetivo: Expor a situação do pedido.
     * Decisões: Somente leitura.
     */
    public SituacaoDoPedido getSituacao() {
        return situacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao envio
     * Objetivo: Expor quando o pedido entrou na fila.
     * Decisões: Nulo em rascunho.
     */
    public Instant getEnviadoEm() {
        return enviadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao motivo
     * Objetivo: Expor o motivo da decisão.
     * Decisões: Nulo antes da decisão.
     */
    public String getMotivo() {
        return motivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso à observação
     * Objetivo: Expor a observação da análise para o usuário (por exemplo, o que falta no complemento).
     * Decisões: Nulo antes da decisão.
     */
    public String getObservacao() {
        return observacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso à validade
     * Objetivo: Expor até quando a verificação vale.
     * Decisões: Só em pedido verificado.
     */
    public Instant getValidoAte() {
        return validoAte;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso à criação
     * Objetivo: Expor quando o pedido foi aberto.
     * Decisões: Somente leitura.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Registrar decisão
     * Objetivo: Gravar a decisão da análise com autor, motivo, observação e validade.
     * Decisões: Usado pela fila de verificação (KAN-83); a regra de quem pode decidir fica lá.
     */
    public void decidir(SituacaoDoPedido resultado, UUID analista, String motivo, String observacao, Instant validoAte,
                        Instant agora) {
        if (situacao != SituacaoDoPedido.EM_ANALISE) {
            throw new IllegalStateException("Só pedido em análise recebe decisão");
        }
        this.situacao = resultado;
        this.decididoPor = analista;
        this.decididoEm = agora;
        this.motivo = motivo;
        this.observacao = observacao;
        this.validoAte = validoAte;
    }
}
