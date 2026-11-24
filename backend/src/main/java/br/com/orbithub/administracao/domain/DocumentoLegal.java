package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Versão de documento legal ou aviso
 * Objetivo: Guardar uma versão publicada de um termo, política ou aviso, com a data de vigência.
 * Decisões: Imutável depois de publicada: corrigir um texto é publicar uma versão nova. Assim o registro
 *           de aceite sempre aponta para o texto exato que a pessoa leu.
 */
@Entity
@Table(name = "documento_legal")
public class DocumentoLegal {

    @Id
    private UUID id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDocumento tipo;

    @Column(nullable = false)
    private int versao;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, columnDefinition = "text")
    private String conteudo;

    @Column(name = "vigente_desde", nullable = false)
    private Instant vigenteDesde;

    @Column(name = "publicado_em", nullable = false)
    private Instant publicadoEm;

    @Column(name = "publicado_por")
    private UUID publicadoPor;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #publicar}.
     */
    protected DocumentoLegal() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Publicação de uma versão
     * Objetivo: Criar a versão informada de um documento, com vigência e autor da publicação.
     * Decisões: Título e conteúdo obrigatórios e sem espaços nas pontas.
     */
    public static DocumentoLegal publicar(TipoDocumento tipo, int versao, String titulo, String conteudo,
                                          Instant vigenteDesde, Instant agora, UUID publicadoPor) {
        if (titulo == null || titulo.isBlank() || conteudo == null || conteudo.isBlank()) {
            throw new IllegalArgumentException("Título e conteúdo são obrigatórios");
        }
        DocumentoLegal documento = new DocumentoLegal();
        documento.id = UUID.randomUUID();
        documento.tipo = tipo;
        documento.versao = versao;
        documento.titulo = titulo.strip();
        documento.conteudo = conteudo.strip();
        documento.vigenteDesde = vigenteDesde;
        documento.publicadoEm = agora;
        documento.publicadoPor = publicadoPor;
        return documento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Vigência iniciada
     * Objetivo: Dizer se a versão já está em vigor no instante informado.
     * Decisões: O próprio instante de início já conta como vigente.
     */
    public boolean emVigor(Instant agora) {
        return !vigenteDesde.isAfter(agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador da versão.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do documento.
     * Decisões: Somente leitura.
     */
    public TipoDocumento getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso à versão
     * Objetivo: Expor o número da versão.
     * Decisões: Somente leitura.
     */
    public int getVersao() {
        return versao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao título
     * Objetivo: Expor o título.
     * Decisões: Somente leitura.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao conteúdo
     * Objetivo: Expor o texto da versão.
     * Decisões: Somente leitura.
     */
    public String getConteudo() {
        return conteudo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso à vigência
     * Objetivo: Expor quando a versão entra (ou entrou) em vigor.
     * Decisões: Somente leitura.
     */
    public Instant getVigenteDesde() {
        return vigenteDesde;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso à data de publicação
     * Objetivo: Expor quando a versão foi publicada.
     * Decisões: Somente leitura.
     */
    public Instant getPublicadoEm() {
        return publicadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso a quem publicou
     * Objetivo: Expor o id da pessoa da equipe que publicou.
     * Decisões: Nulo nas versões iniciais, carregadas pela migração.
     */
    public UUID getPublicadoPor() {
        return publicadoPor;
    }
}
