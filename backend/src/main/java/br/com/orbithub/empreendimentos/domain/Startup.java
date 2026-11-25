package br.com.orbithub.empreendimentos.domain;

import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.MapKeyEnumerated;
import jakarta.persistence.OrderColumn;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 24/11/2026
 * Hora: 22:33
 * Cartão Jira: KAN-21
 * Título: Startup
 * Objetivo: Representar o empreendimento com os dados cadastrais, as redes, o logotipo e os fundadores.
 * Decisões: CNPJ opcional: sem ele, a startup é "empresa ainda não constituída" e não abre rodada (KAN-59). Segmento e
 *           localização são códigos das listas. As regras de validação ficam no serviço; aqui só os dados.
 */
@Entity
@Table(name = "startup")
public class Startup {

    @Id
    private UUID id;

    @Column(nullable = false, length = 120)
    private String nome;

    @Column(name = "nome_fantasia", length = 120)
    private String nomeFantasia;

    @Column(name = "razao_social", length = 200)
    private String razaoSocial;

    @Column(length = 14)
    private String cnpj;

    @Column(nullable = false, length = 40)
    private String segmento;

    @Column(nullable = false, length = 40)
    private String localizacao;

    @Column(length = 200)
    private String site;

    @Column(name = "logotipo_chave", length = 512)
    private String logotipoChave;

    @Column(name = "logotipo_tipo", length = 40)
    private String logotipoTipo;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SituacaoDaStartup situacao;

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "startup_rede", joinColumns = @JoinColumn(name = "startup_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "rede", length = 20)
    @Column(name = "url", length = 300, nullable = false)
    private Map<RedeSocial, String> redes = new EnumMap<>(RedeSocial.class);

    @ElementCollection(fetch = FetchType.EAGER)
    @CollectionTable(name = "startup_fundador", joinColumns = @JoinColumn(name = "startup_id"))
    @OrderColumn(name = "ordem")
    private List<Fundador> fundadores = new ArrayList<>();

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "startup_campo", joinColumns = @JoinColumn(name = "startup_id"))
    @MapKeyEnumerated(EnumType.STRING)
    @MapKeyColumn(name = "campo", length = 60)
    @Column(name = "valor", nullable = false, columnDefinition = "TEXT")
    private Map<CampoDaStartup, String> campos = new EnumMap<>(CampoDaStartup.class);

    @Column(name = "enviada_em")
    private Instant enviadaEm;

    @Column(name = "publicada_em")
    private Instant publicadaEm;

    @Column(name = "aviso_inatividade_em")
    private Instant avisoInatividadeEm;

    @Column(name = "lembrete_indicadores_em")
    private Instant lembreteIndicadoresEm;

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
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Construtor exigido pelo JPA
     * Objetivo: Permitir que o Hibernate instancie a startup ao ler do banco.
     * Decisões: Protegido; a criação passa por {@link #nova}.
     */
    protected Startup() {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Nova startup
     * Objetivo: Criar a startup em rascunho com os dados cadastrais.
     * Decisões: Quem cria fica registrado.
     */
    public static Startup nova(Cadastro cadastro, UUID criador, Instant agora) {
        Startup s = new Startup();
        s.id = UUID.randomUUID();
        s.situacao = SituacaoDaStartup.RASCUNHO;
        s.criadaPor = criador;
        s.criadaEm = agora;
        s.atualizarCadastro(cadastro, agora);
        return s;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Atualizar cadastro
     * Objetivo: Trocar os dados cadastrais, as redes e os fundadores.
     * Decisões: Textos opcionais vazios viram nulo; listas substituem as anteriores.
     */
    public void atualizarCadastro(Cadastro c, Instant agora) {
        this.nome = c.nome().strip();
        this.nomeFantasia = vazio(c.nomeFantasia());
        this.razaoSocial = vazio(c.razaoSocial());
        this.cnpj = c.cnpj();
        this.segmento = c.segmento();
        this.localizacao = c.localizacao();
        this.site = vazio(c.site());
        this.redes.clear();
        this.redes.putAll(c.redes());
        this.fundadores.clear();
        this.fundadores.addAll(c.fundadores());
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Preencher seção
     * Objetivo: Trocar os valores de uma seção pelos informados.
     * Decisões: Os campos da seção que não vierem (ou vierem vazios) são apagados; a validação fica no serviço.
     */
    public void preencherSecao(SecaoDaStartup secao, Map<CampoDaStartup, String> valores, Instant agora) {
        campos.keySet().removeIf(c -> c.getSecao() == secao);
        valores.forEach((c, v) -> {
            if (c.getSecao() == secao && v != null && !v.isBlank()) {
                campos.put(c, v.strip());
            }
        });
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:56
     * Cartão Jira: KAN-22
     * Título: Campos preenchidos
     * Objetivo: Devolver os valores das seções descritivas.
     * Decisões: Cópia, para quem lê não alterar a entidade.
     */
    public Map<CampoDaStartup, String> campos() {
        return campos.isEmpty() ? Map.of() : new EnumMap<>(campos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Trocar logotipo
     * Objetivo: Registrar onde está o logotipo novo.
     * Decisões: Devolve a chave antiga, para quem chama apagar o arquivo.
     */
    public String trocarLogotipo(String chave, String tipo, Instant agora) {
        String antiga = this.logotipoChave;
        this.logotipoChave = chave;
        this.logotipoTipo = tipo;
        this.atualizadaEm = agora;
        return antiga;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Empresa constituída
     * Objetivo: Dizer se a startup já tem CNPJ.
     * Decisões: Sem CNPJ, mostra o selo "empresa ainda não constituída" e não abre rodada.
     */
    public boolean constituida() {
        return cnpj != null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Texto opcional
     * Objetivo: Tratar texto vazio como ausente.
     * Decisões: Tira espaços das pontas.
     */
    private static String vazio(String texto) {
        return texto == null || texto.isBlank() ? null : texto.strip();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Dados cadastrais
     * Objetivo: Ler os dados cadastrais de uma vez.
     * Decisões: Cópias das listas.
     */
    public Cadastro cadastro() {
        return new Cadastro(nome, nomeFantasia, razaoSocial, cnpj, segmento, localizacao, site, Map.copyOf(redes),
                List.copyOf(fundadores));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
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
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso ao nome
     * Objetivo: Expor o nome da startup.
     * Decisões: Somente leitura.
     */
    public String getNome() {
        return nome;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso ao CNPJ
     * Objetivo: Expor os 14 dígitos do CNPJ.
     * Decisões: Nulo quando não constituída.
     */
    public String getCnpj() {
        return cnpj;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso à situação
     * Objetivo: Expor a situação no ciclo de vida.
     * Decisões: Somente leitura.
     */
    public SituacaoDaStartup getSituacao() {
        return situacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso ao logotipo
     * Objetivo: Expor a chave do logotipo no armazenamento.
     * Decisões: Nunca vai para a resposta da API.
     */
    public String getLogotipoChave() {
        return logotipoChave;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso ao tipo do logotipo
     * Objetivo: Expor o formato do logotipo.
     * Decisões: Somente leitura.
     */
    public String getLogotipoTipo() {
        return logotipoTipo;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Acesso à atualização
     * Objetivo: Expor a última alteração.
     * Decisões: Somente leitura.
     */
    public Instant getAtualizadaEm() {
        return atualizadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Lembrar dos indicadores
     * Objetivo: Dizer se a R8 deve avisar a equipe dos indicadores desatualizados agora.
     * Decisões: Avisa de novo só depois do intervalo informado, para não repetir todo dia.
     */
    public boolean deveLembrarIndicadores(Instant agora, Duration intervalo) {
        return lembreteIndicadoresEm == null || !lembreteIndicadoresEm.plus(intervalo).isAfter(agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:11
     * Cartão Jira: KAN-24
     * Título: Registrar lembrete
     * Objetivo: Guardar quando a equipe foi lembrada dos indicadores.
     * Decisões: Não muda a data de atualização da startup.
     */
    public void registrarLembreteDeIndicadores(Instant agora) {
        this.lembreteIndicadoresEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Enviar para revisão
     * Objetivo: Passar a startup para "em revisão", na fila da curadoria (KAN-66).
     * Decisões: Vale de rascunho, devolvida ou despublicada; devolve a situação anterior para o histórico.
     */
    public SituacaoDaStartup enviarParaRevisao(Instant agora) {
        return mudar(EnumSet.of(SituacaoDaStartup.RASCUNHO, SituacaoDaStartup.DEVOLVIDA, SituacaoDaStartup.DESPUBLICADA),
                SituacaoDaStartup.EM_REVISAO, agora, () -> this.enviadaEm = agora);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Cancelar envio
     * Objetivo: Tirar a startup da fila da curadoria antes da decisão.
     * Decisões: Volta para rascunho, ou para despublicada se já foi publicada antes.
     */
    public SituacaoDaStartup cancelarEnvio(Instant agora) {
        SituacaoDaStartup volta = publicadaEm == null ? SituacaoDaStartup.RASCUNHO : SituacaoDaStartup.DESPUBLICADA;
        return mudar(EnumSet.of(SituacaoDaStartup.EM_REVISAO), volta, agora, () -> this.enviadaEm = null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Despublicar
     * Objetivo: Tirar a startup publicada do catálogo.
     * Decisões: Só de publicada; o aviso de inatividade é limpo.
     */
    public SituacaoDaStartup despublicar(Instant agora) {
        return mudar(EnumSet.of(SituacaoDaStartup.PUBLICADA), SituacaoDaStartup.DESPUBLICADA, agora, () -> this.avisoInatividadeEm = null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Mudar situação
     * Objetivo: Conferir a situação de origem e aplicar a nova.
     * Decisões: Situação inválida: regra de negócio com a situação atual na mensagem. Não muda a data de atualização do cadastro.
     */
    private SituacaoDaStartup mudar(Set<SituacaoDaStartup> origens, SituacaoDaStartup destino, Instant agora, Runnable efeito) {
        if (!origens.contains(situacao)) {
            throw new RegraDeNegocioException("situacao_invalida",
                    "Não é possível fazer isso com a startup " + situacao.name().toLowerCase(Locale.ROOT).replace('_', ' ') + ".");
        }
        SituacaoDaStartup anterior = situacao;
        situacao = destino;
        efeito.run();
        return anterior;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Registrar atualização
     * Objetivo: Marcar que a equipe mexeu no conteúdo da startup (para a R8 saber que ela está ativa).
     * Decisões: Usado pelas partes do cadastro que não passam pelos dados cadastrais (indicadores).
     */
    public void registrarAtualizacao(Instant agora) {
        this.atualizadaEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Avisar inatividade
     * Objetivo: Guardar quando a equipe foi avisada de que a startup publicada está sem atualização.
     * Decisões: A R8 despublica se nada mudar em 30 dias.
     */
    public void registrarAvisoDeInatividade(Instant agora) {
        this.avisoInatividadeEm = agora;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Limpar aviso de inatividade
     * Objetivo: Esquecer o aviso quando a startup volta a ser atualizada.
     * Decisões: Chamado pela R8.
     */
    public void limparAvisoDeInatividade() {
        this.avisoInatividadeEm = null;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso ao aviso de inatividade
     * Objetivo: Expor quando a equipe foi avisada da inatividade.
     * Decisões: Nulo sem aviso.
     */
    public Instant getAvisoInatividadeEm() {
        return avisoInatividadeEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso ao envio
     * Objetivo: Expor quando a startup foi enviada para revisão.
     * Decisões: Nulo fora da revisão.
     */
    public Instant getEnviadaEm() {
        return enviadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 23:40
     * Cartão Jira: KAN-27
     * Título: Acesso à publicação
     * Objetivo: Expor quando a startup foi publicada pela última vez.
     * Decisões: Nulo se nunca foi publicada; quem publica é a curadoria (KAN-66).
     */
    public Instant getPublicadaEm() {
        return publicadaEm;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 24/11/2026
     * Hora: 22:33
     * Cartão Jira: KAN-21
     * Título: Cadastro
     * Objetivo: Levar os dados cadastrais da startup.
     * Decisões: CNPJ já normalizado (ou nulo).
     */
    public record Cadastro(String nome, String nomeFantasia, String razaoSocial, String cnpj, String segmento,
                           String localizacao, String site, Map<RedeSocial, String> redes, List<Fundador> fundadores) {
    }
}
