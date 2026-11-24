package br.com.orbithub.verificacao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:19
 * Cartão Jira: KAN-62
 * Título: Documento de verificação
 * Objetivo: Representar um documento enviado num pedido: o tipo exigido e onde o arquivo está guardado.
 * Decisões: Um por tipo em cada pedido (trocar substitui). O conteúdo fica no armazenamento; aqui só metadados. É dado
 *           pessoal: leitura só pelo próprio dono e pela equipe de verificação.
 */
@Entity
@Table(name = "documento_verificacao")
public class DocumentoDeVerificacao {

    @Id
    private UUID id;

    @Column(name = "pedido_id", nullable = false)
    private UUID pedidoId;

    @Column(nullable = false, length = 60)
    private String tipo;

    @Column(name = "chave_arquivo", nullable = false, length = 512)
    private String chaveArquivo;

    @Column(name = "nome_original", nullable = false, length = 200)
    private String nomeOriginal;

    @Column(name = "tipo_conteudo", nullable = false, length = 60)
    private String tipoConteudo;

    @Column(nullable = false)
    private long tamanho;

    @Column(name = "enviado_em", nullable = false)
    private Instant enviadoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o documento ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected DocumentoDeVerificacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Novo documento
     * Objetivo: Registrar um documento já gravado no armazenamento.
     * Decisões: Nome original cortado em 200 caracteres, só para exibição.
     */
    public static DocumentoDeVerificacao novo(UUID id, UUID pedidoId, String tipo, String chave, String nomeOriginal,
                                              String tipoConteudo, long tamanho, Instant agora) {
        DocumentoDeVerificacao d = new DocumentoDeVerificacao();
        d.id = id;
        d.pedidoId = pedidoId;
        d.tipo = tipo;
        d.chaveArquivo = chave;
        String nome = nomeOriginal == null || nomeOriginal.isBlank() ? "documento" : nomeOriginal.strip();
        d.nomeOriginal = nome.length() > 200 ? nome.substring(0, 200) : nome;
        d.tipoConteudo = tipoConteudo;
        d.tamanho = tamanho;
        d.enviadoEm = agora;
        return d;
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
     * Título: Acesso ao pedido
     * Objetivo: Expor o pedido do documento.
     * Decisões: Somente leitura.
     */
    public UUID getPedidoId() {
        return pedidoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao tipo
     * Objetivo: Expor o código do tipo de documento (lista DOCUMENTO_VERIFICACAO).
     * Decisões: Somente leitura.
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso à chave
     * Objetivo: Expor onde o arquivo está guardado.
     * Decisões: Nunca vai para a resposta da API.
     */
    public String getChaveArquivo() {
        return chaveArquivo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao nome
     * Objetivo: Expor o nome do arquivo enviado.
     * Decisões: Somente leitura.
     */
    public String getNomeOriginal() {
        return nomeOriginal;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao formato
     * Objetivo: Expor o tipo de conteúdo detectado.
     * Decisões: PDF, JPEG ou PNG.
     */
    public String getTipoConteudo() {
        return tipoConteudo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao tamanho
     * Objetivo: Expor o tamanho em bytes.
     * Decisões: Somente leitura.
     */
    public long getTamanho() {
        return tamanho;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:19
     * Cartão Jira: KAN-62
     * Título: Acesso ao envio
     * Objetivo: Expor quando o documento foi enviado.
     * Decisões: Somente leitura.
     */
    public Instant getEnviadoEm() {
        return enviadoEm;
    }
}
