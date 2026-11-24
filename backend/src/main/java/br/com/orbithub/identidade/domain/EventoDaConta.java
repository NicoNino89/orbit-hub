package br.com.orbithub.identidade.domain;

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
 * Hora: 01:40
 * Cartão Jira: KAN-16
 * Título: Evento no histórico da conta
 * Objetivo: Registrar mudanças importantes da conta: papéis ativados e desativados, troca de e-mail e de
 *           senha.
 * Decisões: Só inserido, nunca alterado. O detalhe não leva dado pessoal (o novo e-mail, por exemplo, não é
 *           guardado aqui).
 */
@Entity
@Table(name = "historico_conta")
public class EventoDaConta {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private Tipo evento;

    @Column(length = 120)
    private String detalhe;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a entidade.
     * Decisões: Protegido; a aplicação usa {@link #registrar}.
     */
    protected EventoDaConta() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Registrar evento
     * Objetivo: Criar o registro de um evento da conta.
     * Decisões: Data do servidor.
     */
    public static EventoDaConta registrar(UUID usuarioId, Tipo evento, String detalhe, Instant agora) {
        EventoDaConta registro = new EventoDaConta();
        registro.id = UUID.randomUUID();
        registro.usuarioId = usuarioId;
        registro.evento = evento;
        registro.detalhe = detalhe;
        registro.ocorridoEm = agora;
        return registro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do evento.
     * Decisões: Somente leitura.
     */
    public Tipo getEvento() {
        return evento;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Acesso ao detalhe
     * Objetivo: Expor o detalhe do evento (por exemplo, o papel).
     * Decisões: Pode ser nulo.
     */
    public String getDetalhe() {
        return detalhe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Acesso ao momento
     * Objetivo: Expor quando o evento ocorreu.
     * Decisões: Somente leitura.
     */
    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 01:40
     * Cartão Jira: KAN-16
     * Título: Tipos de evento
     * Objetivo: Enumerar os eventos registrados no histórico.
     * Decisões: Os de papel atendem o critério do KAN-16; e-mail e senha ajudam o suporte e o titular.
     */
    public enum Tipo {
        PAPEL_ATIVADO,
        PAPEL_DESATIVADO,
        EMAIL_ALTERADO,
        SENHA_ALTERADA
    }
}
