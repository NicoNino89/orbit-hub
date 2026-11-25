package br.com.orbithub.administracao.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 17:43
 * Cartão Jira: KAN-87
 * Título: Vínculo declarado por membro da equipe
 * Objetivo: Representar uma organização, startup ou pessoa com a qual o membro tem interesse (é membro, investe,
 *           trabalha, é parente).
 * Decisões: Organização e startup pelo CNPJ (já normalizado); pessoa pelo e-mail, em minúsculas. A descrição explica o
 *           vínculo, para a revisão periódica de acessos.
 */
@Entity
@Table(name = "vinculo_membro")
public class VinculoDeMembro {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Tipo do vínculo
     * Objetivo: Enumerar com o que o membro tem vínculo.
     * Decisões: Pessoa cobre parentes e sócios que usam a plataforma.
     */
    public enum Tipo {
        ORGANIZACAO,
        STARTUP,
        PESSOA
    }

    @Id
    private UUID id;

    @Column(name = "membro_id", nullable = false)
    private UUID membroId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Tipo tipo;

    @Column(length = 14)
    private String cnpj;

    @Column(length = 254)
    private String email;

    @Column(nullable = false, length = 200)
    private String descricao;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o vínculo ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected VinculoDeMembro() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Novo vínculo
     * Objetivo: Criar o vínculo com o alvo (CNPJ ou e-mail) e a descrição.
     * Decisões: A validação do CNPJ e do e-mail fica no serviço.
     */
    public static VinculoDeMembro novo(UUID membro, Tipo tipo, String cnpj, String email, String descricao, Instant agora) {
        VinculoDeMembro v = new VinculoDeMembro();
        v.id = UUID.randomUUID();
        v.membroId = membro;
        v.tipo = tipo;
        v.cnpj = tipo == Tipo.PESSOA ? null : cnpj;
        v.email = tipo == Tipo.PESSOA ? email.strip().toLowerCase(Locale.ROOT) : null;
        v.descricao = descricao.strip();
        v.criadoEm = agora;
        return v;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
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
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso ao membro
     * Objetivo: Expor quem declarou.
     * Decisões: Somente leitura.
     */
    public UUID getMembroId() {
        return membroId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso ao tipo
     * Objetivo: Expor o tipo do vínculo.
     * Decisões: Somente leitura.
     */
    public Tipo getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso ao CNPJ
     * Objetivo: Expor o CNPJ da organização ou startup.
     * Decisões: Nulo para pessoa.
     */
    public String getCnpj() {
        return cnpj;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso ao e-mail
     * Objetivo: Expor o e-mail da pessoa.
     * Decisões: Nulo para organização e startup.
     */
    public String getEmail() {
        return email;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso à descrição
     * Objetivo: Expor a explicação do vínculo.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:43
     * Cartão Jira: KAN-87
     * Título: Acesso à declaração
     * Objetivo: Expor quando o vínculo foi declarado.
     * Decisões: Somente leitura.
     */
    public Instant getCriadoEm() {
        return criadoEm;
    }
}
