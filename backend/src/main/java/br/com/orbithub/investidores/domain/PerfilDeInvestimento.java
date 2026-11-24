package br.com.orbithub.investidores.domain;

import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.Set;
import java.util.TreeSet;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 14:25
 * Cartão Jira: KAN-18
 * Título: Perfil de investimento
 * Objetivo: Representar como um investidor (pessoa ou organização) investe: tipo, ticket, liderança, follow-on,
 *           participação na gestão e instrumentos aceitos.
 * Decisões: O dono é a pessoa ou a organização, nunca os dois. Guarda quem fez a última alteração (numa organização,
 *           qual membro). As regras de validação ficam no serviço, que conhece as listas; aqui só os dados. A tese
 *           (segmentos, estágios da rodada, maturidades do negócio e localizações) faz parte do perfil (KAN-18).
 */
@Entity
@Table(name = "perfil_investimento")
public class PerfilDeInvestimento {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Liderança nas rodadas
     * Objetivo: Enumerar se o investidor lidera rodadas, só co-investe ou faz os dois.
     * Decisões: Usado na escolha do líder da rodada (KAN-59).
     */
    public enum Lideranca {
        LIDERA,
        SO_COINVESTE,
        AMBOS
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Participação na gestão
     * Objetivo: Enumerar se o investidor exige conselho, aceita ser observador ou não participa.
     * Decisões: Texto curto no banco.
     */
    public enum ParticipacaoNaGestao {
        EXIGE_CONSELHO,
        ACEITA_OBSERVADOR,
        NAO_PARTICIPA
    }

    @Id
    private UUID id;

    @Column(name = "pessoa_id")
    private UUID pessoaId;

    @Column(name = "organizacao_id")
    private UUID organizacaoId;

    @Column(nullable = false, length = 40)
    private String tipo;

    @Column(name = "tipo_outro", length = 120)
    private String tipoOutro;

    @Column(name = "ticket_minimo", nullable = false, precision = 15, scale = 2)
    private BigDecimal ticketMinimo;

    @Column(name = "ticket_maximo", nullable = false, precision = 15, scale = 2)
    private BigDecimal ticketMaximo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Lideranca lideranca;

    @Column(name = "follow_on", nullable = false)
    private boolean followOn;

    @Enumerated(EnumType.STRING)
    @Column(name = "participacao_gestao", nullable = false, length = 30)
    private ParticipacaoNaGestao participacaoNaGestao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_instrumento", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "instrumento", length = 40)
    private Set<String> instrumentos = new TreeSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_segmento", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "segmento", length = 40)
    private Set<String> segmentos = new TreeSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_estagio", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "estagio", length = 40)
    private Set<String> estagios = new TreeSet<>();

    @Enumerated(EnumType.STRING)
    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_maturidade", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "maturidade", length = 40)
    private Set<MaturidadeDoNegocio> maturidades = new TreeSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_localizacao", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "localizacao", length = 40)
    private Set<String> localizacoes = new TreeSet<>();


    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_modelo", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "modelo", length = 40)
    private Set<String> modelos = new TreeSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_modalidade", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "modalidade", length = 40)
    private Set<String> modalidades = new TreeSet<>();

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "perfil_investimento_interesse", joinColumns = @JoinColumn(name = "perfil_id"))
    @Column(name = "interesse", length = 40)
    private Set<String> interesses = new TreeSet<>();

    @Column(length = 40)
    private String horizonte;

    @Column(name = "perfil_risco", length = 40)
    private String perfilRisco;

    @Column(length = 40)
    private String experiencia;

    @Column(name = "atualizado_por", nullable = false)
    private UUID atualizadoPor;

    @Column(name = "criado_em", nullable = false)
    private Instant criadoEm;

    @Column(name = "atualizado_em", nullable = false)
    private Instant atualizadoEm;

    @Version
    private long versao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie o perfil ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #novo}.
     */
    protected PerfilDeInvestimento() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Novo perfil
     * Objetivo: Criar o perfil vazio de uma pessoa ou de uma organização.
     * Decisões: Os dados vêm em seguida, por {@link #atualizar}.
     */
    public static PerfilDeInvestimento novo(UUID pessoaId, UUID organizacaoId, Instant agora) {
        PerfilDeInvestimento p = new PerfilDeInvestimento();
        p.id = UUID.randomUUID();
        p.pessoaId = organizacaoId == null ? pessoaId : null;
        p.organizacaoId = organizacaoId;
        p.criadoEm = agora;
        return p;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:04
     * Cartão Jira: KAN-19
     * Título: Atualizar
     * Objetivo: Gravar os dados de como o investidor investe e quem alterou.
     * Decisões: Os instrumentos substituem os anteriores; "tipo_outro" só fica com o tipo "Outros".
     */
    public void atualizar(Dados dados, UUID autor, Instant agora) {
        this.tipo = dados.tipo();
        this.tipoOutro = dados.tipoOutro();
        this.ticketMinimo = dados.ticketMinimo();
        this.ticketMaximo = dados.ticketMaximo();
        this.lideranca = dados.lideranca();
        this.followOn = dados.followOn();
        this.participacaoNaGestao = dados.participacaoNaGestao();
        this.instrumentos.clear();
        this.instrumentos.addAll(dados.instrumentos());
        substituir(this.segmentos, dados.segmentos());
        substituir(this.estagios, dados.estagios());
        substituir(this.maturidades, dados.maturidades());
        substituir(this.localizacoes, dados.localizacoes());
        Complementares c = dados.complementares() == null ? Complementares.VAZIAS : dados.complementares();
        substituir(this.modelos, c.modelos());
        substituir(this.modalidades, c.modalidades());
        substituir(this.interesses, c.interesses());
        this.horizonte = c.horizonte();
        this.perfilRisco = c.perfilRisco();
        this.experiencia = c.experiencia();
        this.atualizadoPor = autor;
        this.atualizadoEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:04
     * Cartão Jira: KAN-19
     * Título: Dados do perfil
     * Objetivo: Ler o conteúdo do perfil de uma vez, para a resposta da API e para a compatibilidade.
     * Decisões: Cópia dos instrumentos.
     */
    public Dados dados() {
        return new Dados(tipo, tipoOutro, ticketMinimo, ticketMaximo, lideranca, followOn, participacaoNaGestao,
                Set.copyOf(instrumentos), Set.copyOf(segmentos), Set.copyOf(estagios), Set.copyOf(maturidades),
                Set.copyOf(localizacoes), new Complementares(Set.copyOf(modelos), Set.copyOf(modalidades), horizonte,
                perfilRisco, experiencia, Set.copyOf(interesses)));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:25
     * Cartão Jira: KAN-18
     * Título: Substituir uma escolha múltipla
     * Objetivo: Trocar o conteúdo de uma coleção pelo novo, aceitando ausência.
     * Decisões: Nulo vira vazio.
     */
    private static <T> void substituir(Set<T> atual, Set<T> novo) {
        atual.clear();
        if (novo != null) {
            atual.addAll(novo);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
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
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Acesso à organização dona
     * Objetivo: Expor a organização dona do perfil, se houver.
     * Decisões: Nulo para perfil pessoal.
     */
    public UUID getOrganizacaoId() {
        return organizacaoId;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Acesso ao autor
     * Objetivo: Expor quem fez a última alteração.
     * Decisões: Somente leitura.
     */
    public UUID getAtualizadoPor() {
        return atualizadoPor;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 14:16
     * Cartão Jira: KAN-61
     * Título: Acesso à última alteração
     * Objetivo: Expor quando o perfil foi alterado.
     * Decisões: Somente leitura.
     */
    public Instant getAtualizadoEm() {
        return atualizadoEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:04
     * Cartão Jira: KAN-19
     * Título: Dados
     * Objetivo: Levar os campos do perfil.
     * Decisões: Valores em reais, com centavos. Inclui a tese. E as preferências complementares.
     */
    public record Dados(String tipo, String tipoOutro, BigDecimal ticketMinimo, BigDecimal ticketMaximo,
                        Lideranca lideranca, boolean followOn, ParticipacaoNaGestao participacaoNaGestao,
                        Set<String> instrumentos, Set<String> segmentos, Set<String> estagios,
                        Set<MaturidadeDoNegocio> maturidades, Set<String> localizacoes,
                        Complementares complementares) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 17:04
     * Cartão Jira: KAN-19
     * Título: Preferências complementares
     * Objetivo: Levar as preferências opcionais: modelos de negócio, modalidades, horizonte, perfil de risco,
     *           experiência e interesses estratégicos.
     * Decisões: Todas opcionais; códigos de listas padronizadas.
     */
    public record Complementares(Set<String> modelos, Set<String> modalidades, String horizonte, String perfilRisco,
                                 String experiencia, Set<String> interesses) {

        public static final Complementares VAZIAS = new Complementares(Set.of(), Set.of(), null, null, null, Set.of());
    }
}
