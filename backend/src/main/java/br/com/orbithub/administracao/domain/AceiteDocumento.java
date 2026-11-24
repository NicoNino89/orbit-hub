package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 00:40
 * Cartão Jira: KAN-92
 * Título: Aceite de uma versão
 * Objetivo: Registrar que um usuário aceitou uma versão específica de um documento, e quando.
 * Decisões: Aponta para a versão (e não para o tipo), para provar qual texto foi aceito (LGPD). Nunca é
 *           alterado nem apagado enquanto a conta existir.
 */
@Entity
@Table(name = "aceite_documento")
public class AceiteDocumento {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Column(name = "documento_id", nullable = false)
    private UUID documentoId;

    @Column(name = "aceito_em", nullable = false)
    private Instant aceitoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #registrar}.
     */
    protected AceiteDocumento() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Registro de aceite
     * Objetivo: Criar o registro de aceite de uma versão por um usuário.
     * Decisões: Data e hora do servidor, nunca do navegador.
     */
    public static AceiteDocumento registrar(UUID usuarioId, UUID documentoId, Instant agora) {
        AceiteDocumento aceite = new AceiteDocumento();
        aceite.id = UUID.randomUUID();
        aceite.usuarioId = usuarioId;
        aceite.documentoId = documentoId;
        aceite.aceitoEm = agora;
        return aceite;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao usuário
     * Objetivo: Expor quem aceitou.
     * Decisões: Somente leitura.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso à versão aceita
     * Objetivo: Expor qual versão foi aceita.
     * Decisões: Somente leitura.
     */
    public UUID getDocumentoId() {
        return documentoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 00:40
     * Cartão Jira: KAN-92
     * Título: Acesso ao momento do aceite
     * Objetivo: Expor quando foi aceito.
     * Decisões: Somente leitura.
     */
    public Instant getAceitoEm() {
        return aceitoEm;
    }
}
