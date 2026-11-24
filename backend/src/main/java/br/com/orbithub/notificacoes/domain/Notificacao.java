package br.com.orbithub.notificacoes.domain;

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
 * Hora: 12:34
 * Cartão Jira: KAN-146
 * Título: Notificação
 * Objetivo: Representar um aviso para uma pessoa, com título, mensagem, link e se já foi lido.
 * Decisões: "naPlataforma" diz se o aviso aparece no sino (a pessoa pode ter escolhido só e-mail); o registro existe
 *           mesmo assim, porque é dele que o e-mail é montado.
 */
@Entity
@Table(name = "notificacao")
public class Notificacao {

    @Id
    private UUID id;

    @Column(name = "usuario_id", nullable = false)
    private UUID usuarioId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private TipoDeNotificacao tipo;

    @Column(nullable = false, length = 160)
    private String titulo;

    @Column(nullable = false, length = 1000)
    private String mensagem;

    @Column(length = 300)
    private String link;

    @Column(name = "na_plataforma", nullable = false)
    private boolean naPlataforma;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "lida_em")
    private Instant lidaEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a notificação ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #nova}.
     */
    protected Notificacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Nova notificação
     * Objetivo: Criar o aviso não lido.
     * Decisões: Textos cortados no tamanho das colunas; link só interno (começa com "/").
     */
    public static Notificacao nova(UUID usuarioId, TipoDeNotificacao tipo, String titulo, String mensagem, String link,
                                   boolean naPlataforma, Instant agora) {
        Notificacao n = new Notificacao();
        n.id = UUID.randomUUID();
        n.usuarioId = usuarioId;
        n.tipo = tipo;
        n.titulo = titulo.length() > 160 ? titulo.substring(0, 160) : titulo;
        n.mensagem = mensagem.length() > 1000 ? mensagem.substring(0, 1000) : mensagem;
        n.link = link != null && link.startsWith("/") && !link.startsWith("//") ? link : null;
        n.naPlataforma = naPlataforma;
        n.criadaEm = agora;
        return n;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Marcar como lida
     * Objetivo: Registrar que a pessoa viu o aviso.
     * Decisões: Idempotente: mantém a primeira leitura.
     */
    public void marcarLida(Instant agora) {
        if (lidaEm == null) {
            lidaEm = agora;
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
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
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso ao destinatário
     * Objetivo: Expor de quem é o aviso.
     * Decisões: Somente leitura.
     */
    public UUID getUsuarioId() {
        return usuarioId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do evento.
     * Decisões: Somente leitura.
     */
    public TipoDeNotificacao getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso ao título
     * Objetivo: Expor o título do aviso.
     * Decisões: Somente leitura.
     */
    public String getTitulo() {
        return titulo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso à mensagem
     * Objetivo: Expor o texto do aviso.
     * Decisões: Somente leitura.
     */
    public String getMensagem() {
        return mensagem;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso ao link
     * Objetivo: Expor o caminho da tela relacionada.
     * Decisões: Nulo quando não há.
     */
    public String getLink() {
        return link;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso à data
     * Objetivo: Expor quando o aviso foi criado.
     * Decisões: Somente leitura.
     */
    public Instant getCriadaEm() {
        return criadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 12:34
     * Cartão Jira: KAN-146
     * Título: Acesso à leitura
     * Objetivo: Expor quando o aviso foi lido.
     * Decisões: Nulo enquanto não lido.
     */
    public Instant getLidaEm() {
        return lidaEm;
    }
}
