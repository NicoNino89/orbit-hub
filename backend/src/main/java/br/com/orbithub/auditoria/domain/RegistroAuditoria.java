package br.com.orbithub.auditoria.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Objects;
import java.util.UUID;
import org.hibernate.annotations.Immutable;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 10:09
 * Cartão Jira: KAN-80
 * Título: Registro da trilha de auditoria
 * Objetivo: Representar uma ação da equipe interna: quem, o quê, quando, sobre qual objeto e com qual resultado.
 * Decisões: Entidade @Immutable e sem setters: o Hibernate nunca gera UPDATE, e o gatilho do banco recusa
 *           qualquer alteração ou exclusão. Textos longos são cortados no limite da coluna, para um detalhe
 *           grande não impedir o registro.
 */
@Entity
@Immutable
@Table(name = "registro_auditoria")
public class RegistroAuditoria {

    @Id
    private UUID id;

    @Column(name = "ator_id")
    private UUID atorId;

    @Column(name = "ator_nome", length = 120)
    private String atorNome;

    @Column(nullable = false, length = 120)
    private String acao;

    @Column(name = "objeto_tipo", length = 60)
    private String objetoTipo;

    @Column(name = "objeto_id", length = 80)
    private String objetoId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Resultado resultado;

    @Column(name = "status_http")
    private Integer statusHttp;

    @Column(length = 500)
    private String detalhe;

    @Column(length = 1000)
    private String justificativa;

    @Column(name = "ocorrido_em", nullable = false)
    private Instant ocorridoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o registro ao ler do banco.
     * Decisões: Protegido; a criação passa sempre por {@link #de}.
     */
    protected RegistroAuditoria() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Criação de um registro
     * Objetivo: Montar o registro de uma ação a partir dos dados coletados.
     * Decisões: Ação, resultado e momento são obrigatórios; o autor pode faltar quando a ação é do sistema
     *           (rotina automática).
     */
    public static RegistroAuditoria de(Acao dados, Instant agora) {
        Objects.requireNonNull(dados.acao(), "acao");
        Objects.requireNonNull(dados.resultado(), "resultado");
        RegistroAuditoria registro = new RegistroAuditoria();
        registro.id = UUID.randomUUID();
        registro.atorId = dados.atorId();
        registro.atorNome = cortar(dados.atorNome(), 120);
        registro.acao = cortar(dados.acao(), 120);
        registro.objetoTipo = cortar(dados.objetoTipo(), 60);
        registro.objetoId = cortar(dados.objetoId(), 80);
        registro.resultado = dados.resultado();
        registro.statusHttp = dados.statusHttp();
        registro.detalhe = cortar(dados.detalhe(), 500);
        registro.justificativa = cortar(dados.justificativa(), 1000);
        registro.ocorridoEm = Objects.requireNonNull(agora, "agora");
        return registro;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Corte de texto
     * Objetivo: Limitar um texto ao tamanho da coluna.
     * Decisões: Nulo continua nulo; texto em branco vira nulo.
     */
    static String cortar(String texto, int limite) {
        if (texto == null || texto.isBlank()) {
            return null;
        }
        String limpo = texto.strip();
        return limpo.length() > limite ? limpo.substring(0, limite) : limpo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao identificador
     * Objetivo: Expor o identificador do registro.
     * Decisões: Somente leitura.
     */
    public UUID getId() {
        return id;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao autor
     * Objetivo: Expor o id da conta que fez a ação.
     * Decisões: Nulo em ações do sistema.
     */
    public UUID getAtorId() {
        return atorId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao nome do autor
     * Objetivo: Expor o nome do autor no momento da ação.
     * Decisões: Cópia gravada no registro.
     */
    public String getAtorNome() {
        return atorNome;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso à ação
     * Objetivo: Expor o código da ação (por exemplo, DOCUMENTO_LEGAL_PUBLICADO).
     * Decisões: Somente leitura.
     */
    public String getAcao() {
        return acao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao tipo do objeto
     * Objetivo: Expor o tipo do objeto afetado (por exemplo, startup).
     * Decisões: Pode ser nulo em ações sem objeto (consultas gerais).
     */
    public String getObjetoTipo() {
        return objetoTipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao id do objeto
     * Objetivo: Expor o identificador do objeto afetado.
     * Decisões: Texto, porque nem todo objeto tem UUID (parâmetros usam a chave).
     */
    public String getObjetoId() {
        return objetoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao resultado
     * Objetivo: Dizer se a ação deu certo.
     * Decisões: SUCESSO ou FALHA.
     */
    public Resultado getResultado() {
        return resultado;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao status HTTP
     * Objetivo: Expor o status da resposta, quando a ação veio da API.
     * Decisões: Nulo em ações fora de uma requisição.
     */
    public Integer getStatusHttp() {
        return statusHttp;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao detalhe
     * Objetivo: Expor o resumo legível da ação.
     * Decisões: Sem dado pessoal sensível; quem grava decide o texto.
     */
    public String getDetalhe() {
        return detalhe;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso à justificativa
     * Objetivo: Expor a justificativa informada pelo autor.
     * Decisões: Obrigatória em acessos a dados confidenciais e em ações que a exigem.
     */
    public String getJustificativa() {
        return justificativa;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Acesso ao momento
     * Objetivo: Expor quando a ação aconteceu.
     * Decisões: Instante em UTC.
     */
    public Instant getOcorridoEm() {
        return ocorridoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Resultado de uma ação
     * Objetivo: Enumerar os resultados possíveis.
     * Decisões: Só dois: o motivo da falha fica no status e no detalhe.
     */
    public enum Resultado {
        SUCESSO,
        FALHA
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 10:09
     * Cartão Jira: KAN-80
     * Título: Dados de uma ação a registrar
     * Objetivo: Agrupar o que se sabe de uma ação antes de virar registro.
     * Decisões: Record imutável; os campos opcionais podem ser nulos.
     */
    public record Acao(UUID atorId, String atorNome, String acao, String objetoTipo, String objetoId,
                       Resultado resultado, Integer statusHttp, String detalhe, String justificativa) {
    }
}
