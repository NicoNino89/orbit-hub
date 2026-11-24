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

    @Column(name = "responsavel_id")
    private UUID responsavelId;

    @Column(name = "assumido_em")
    private Instant assumidoEm;

    @Column(name = "prazo_complemento_ate")
    private Instant prazoComplementoAte;

    @Column(name = "aviso_revalidacao_em")
    private Instant avisoRevalidacaoEm;

    @Column(name = "encaminhado_moderacao", nullable = false)
    private boolean encaminhadoModeracao;

    @Column(name = "documentos_apagados_em")
    private Instant documentosApagadosEm;

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

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Assumir
     * Objetivo: Registrar o analista responsável pelo pedido em análise.
     * Decisões: Troca de responsável é permitida (redistribuição); a regra de quem pode assumir fica no serviço.
     */
    public void assumir(UUID analista, Instant agora) {
        if (situacao != SituacaoDoPedido.EM_ANALISE) {
            throw new IllegalStateException("Só pedido em análise pode ser assumido");
        }
        this.responsavelId = analista;
        this.assumidoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Pedir complemento
     * Objetivo: Devolver o pedido ao usuário com o que falta e o prazo para responder.
     * Decisões: O mesmo analista segue com o pedido quando ele voltar.
     */
    public void pedirComplemento(UUID analista, String motivo, String observacao, Instant prazo, Instant agora) {
        decidir(SituacaoDoPedido.PENDENTE_COMPLEMENTO, analista, motivo, observacao, null, agora);
        this.prazoComplementoAte = prazo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Encaminhar à moderação
     * Objetivo: Marcar o pedido recusado por indício de fraude como encaminhado à moderação.
     * Decisões: Marca só; o caso de moderação nasce com o KAN-71.
     */
    public void encaminharModeracao() {
        this.encaminhadoModeracao = true;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Encerrar por falta de complemento
     * Objetivo: Fechar o pedido cujo complemento não chegou no prazo.
     * Decisões: Só a partir de pendente de complemento.
     */
    public void encerrarPorFaltaDeComplemento(Instant agora) {
        if (situacao != SituacaoDoPedido.PENDENTE_COMPLEMENTO) {
            throw new IllegalStateException("Só pedido aguardando complemento pode ser encerrado");
        }
        this.situacao = SituacaoDoPedido.ENCERRADO;
        this.decididoEm = agora;
        this.decididoPor = null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Registrar aviso de revalidação
     * Objetivo: Guardar quando o usuário foi avisado de que a verificação vai vencer.
     * Decisões: Um aviso só por pedido.
     */
    public void marcarAvisoRevalidacao(Instant agora) {
        this.avisoRevalidacaoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Vencer
     * Objetivo: Passar a verificação vencida para a situação vencida.
     * Decisões: Só a partir de verificado.
     */
    public void vencer() {
        if (situacao != SituacaoDoPedido.VERIFICADO) {
            throw new IllegalStateException("Só verificação aprovada vence");
        }
        this.situacao = SituacaoDoPedido.VENCIDO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Registrar limpeza dos documentos
     * Objetivo: Guardar quando os documentos do pedido foram apagados pela guarda.
     * Decisões: O pedido e a decisão continuam no histórico.
     */
    public void marcarDocumentosApagados(Instant agora) {
        this.documentosApagadosEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso ao responsável
     * Objetivo: Expor o analista que assumiu o pedido.
     * Decisões: Nulo enquanto ninguém assumiu.
     */
    public UUID getResponsavelId() {
        return responsavelId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso ao prazo do complemento
     * Objetivo: Expor até quando o usuário pode enviar o complemento.
     * Decisões: Só em pendente de complemento.
     */
    public Instant getPrazoComplementoAte() {
        return prazoComplementoAte;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso ao aviso de revalidação
     * Objetivo: Expor quando o aviso de vencimento foi enviado.
     * Decisões: Nulo se ainda não avisou.
     */
    public Instant getAvisoRevalidacaoEm() {
        return avisoRevalidacaoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso ao encaminhamento
     * Objetivo: Dizer se o pedido foi encaminhado à moderação.
     * Decisões: Somente leitura.
     */
    public boolean isEncaminhadoModeracao() {
        return encaminhadoModeracao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso à decisão
     * Objetivo: Expor quando houve a decisão.
     * Decisões: Nulo antes da decisão.
     */
    public Instant getDecididoEm() {
        return decididoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso a quem decidiu
     * Objetivo: Expor o analista que decidiu.
     * Decisões: Nulo em encerramento automático.
     */
    public UUID getDecididoPor() {
        return decididoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso a quem abriu
     * Objetivo: Expor quem abriu o pedido.
     * Decisões: Somente leitura.
     */
    public UUID getCriadoPor() {
        return criadoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:33
     * Cartão Jira: KAN-83
     * Título: Acesso à limpeza
     * Objetivo: Expor quando os documentos foram apagados.
     * Decisões: Nulo enquanto guardados.
     */
    public Instant getDocumentosApagadosEm() {
        return documentosApagadosEm;
    }
}
