package br.com.orbithub.investidores.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Instant;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:05
 * Cartão Jira: KAN-60
 * Título: Organização investidora
 * Objetivo: Representar um fundo, rede de anjos, CVC, family office, aceleradora ou outra entidade com várias pessoas.
 * Decisões: O tipo é um código da lista TIPO_INVESTIDOR (sem "investidor-anjo", que é pessoa física). O CNPJ chega
 *           já normalizado (14 dígitos) e não muda depois de criado: trocar o CNPJ é trocar de entidade.
 */
@Entity
@Table(name = "organizacao")
public class Organizacao {

    @Id
    private UUID id;

    @Column(nullable = false, length = 160)
    private String nome;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(nullable = false, length = 14)
    private String cnpj;

    @Column(length = 200)
    private String site;

    @Column(length = 2000)
    private String descricao;

    @Column(name = "criada_por", nullable = false)
    private UUID criadaPor;

    @Column(name = "criada_em", nullable = false)
    private Instant criadaEm;

    @Column(name = "atualizada_em", nullable = false)
    private Instant atualizadaEm;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a organização ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #nova}.
     */
    protected Organizacao() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Nova organização
     * Objetivo: Criar a organização com os dados informados pelo criador.
     * Decisões: Textos sem espaços nas pontas; site e descrição vazios viram nulo.
     */
    public static Organizacao nova(String nome, String tipo, String cnpj, String site, String descricao, UUID criador,
                                   Instant agora) {
        Organizacao o = new Organizacao();
        o.id = UUID.randomUUID();
        o.cnpj = cnpj;
        o.criadaPor = criador;
        o.criadaEm = agora;
        o.atualizar(nome, tipo, site, descricao, agora);
        return o;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Atualizar dados
     * Objetivo: Trocar nome, tipo, site e descrição.
     * Decisões: O CNPJ não muda.
     */
    public void atualizar(String nome, String tipo, String site, String descricao, Instant agora) {
        this.nome = nome.strip();
        this.tipo = tipo;
        this.site = vazioViraNulo(site);
        this.descricao = vazioViraNulo(descricao);
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Texto opcional
     * Objetivo: Tratar texto vazio como ausente.
     * Decisões: Tira espaços das pontas.
     */
    private static String vazioViraNulo(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
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
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao nome
     * Objetivo: Expor o nome da organização.
     * Decisões: Somente leitura.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao tipo
     * Objetivo: Expor o código do tipo de investidor.
     * Decisões: Somente leitura.
     */
    public String getTipo() {
        return tipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao CNPJ
     * Objetivo: Expor os 14 dígitos do CNPJ.
     * Decisões: Somente leitura.
     */
    public String getCnpj() {
        return cnpj;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao site
     * Objetivo: Expor o site, se houver.
     * Decisões: Somente leitura.
     */
    public String getSite() {
        return site;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à descrição
     * Objetivo: Expor a descrição, se houver.
     * Decisões: Somente leitura.
     */
    public String getDescricao() {
        return descricao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso ao criador
     * Objetivo: Expor quem criou a organização.
     * Decisões: Somente leitura.
     */
    public UUID getCriadaPor() {
        return criadaPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:05
     * Cartão Jira: KAN-60
     * Título: Acesso à criação
     * Objetivo: Expor quando a organização foi criada.
     * Decisões: Somente leitura.
     */
    public Instant getCriadaEm() {
        return criadaEm;
    }
}
