package br.com.orbithub.compartilhado.seguranca;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 01:50
 * Cartão Jira: KAN-34
 * Título: Tentativa de acesso negado
 * Objetivo: Registrar quem tentou o quê e quando, quando o acesso foi recusado.
 * Decisões: Só inserido. Sem token, IP ou dado pessoal; o caminho vai sem parâmetros de busca.
 */
@Entity
@Table(name = "acesso_negado")
public class AcessoNegado {

    @Id
    private UUID id;

    @Column(name = "usuario_id")
    private UUID usuarioId;

    @Column(nullable = false, length = 10)
    private String metodo;

    @Column(nullable = false, length = 300)
    private String caminho;

    @Column(nullable = false)
    private int status;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #registrar}.
     */
    protected AcessoNegado() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Registrar tentativa
     * Objetivo: Criar o registro de uma tentativa recusada.
     * Decisões: Caminho cortado em 300 caracteres.
     */
    public static AcessoNegado registrar(UUID usuarioId, String metodo, String caminho, int status, Instant agora) {
        AcessoNegado registro = new AcessoNegado();
        registro.id = UUID.randomUUID();
        registro.usuarioId = usuarioId;
        registro.metodo = metodo;
        registro.caminho = caminho.length() > 300 ? caminho.substring(0, 300) : caminho;
        registro.status = status;
        registro.ocorridoEm = agora;
        return registro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Acesso à conta
     * Objetivo: Expor quem tentou (nulo sem token válido).
     * Decisões: Somente leitura.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Acesso ao caminho
     * Objetivo: Expor o caminho pedido.
     * Decisões: Somente leitura.
     */
    public String getCaminho() {
        return caminho;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:50
     * Cartão Jira: KAN-34
     * Título: Acesso ao status
     * Objetivo: Expor a resposta dada (401 ou 403).
     * Decisões: Somente leitura.
     */
    public int getStatus() {
        return status;
    }
}
