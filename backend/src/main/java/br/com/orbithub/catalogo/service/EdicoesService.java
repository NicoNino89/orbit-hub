package br.com.orbithub.catalogo.service;

import br.com.orbithub.auditoria.service.ContextoDeAuditoria;
import br.com.orbithub.catalogo.domain.Edicao;
import br.com.orbithub.catalogo.domain.MudancaNaEdicao;
import br.com.orbithub.catalogo.repository.EdicaoRepository;
import br.com.orbithub.catalogo.repository.MudancaNaEdicaoRepository;
import br.com.orbithub.compartilhado.armazenamento.ArmazenamentoArquivos;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.erro.RegraDeNegocioException;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.investidores.service.AtuacaoService;
import java.util.Set;
import br.com.orbithub.identidade.service.ConsultaDeContas;
import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-85
 * Título: Edições temáticas
 * Objetivo: Deixar o curador montar a edição (dados, capa, startups e ordem), publicar e arquivar, registrar o histórico
 *           com o curador responsável, e mostrar ao investidor as edições vigentes só com as startups que podem aparecer.
 * Decisões: Só entram startups publicadas que aceitam participar de edições; quem deixa de atender (despublicada,
 *           recusada ou que saiu das edições) some da leitura do investidor sem mexer na composição, e volta se voltar a
 *           atender. Sem fim informado, a vigência dura o período de catalogo.frequencia_edicoes. O bloqueio de
 *           investidor pela startup esconde a startup dele (KAN-67, 25/11/2026).
 */
@Service
public class EdicoesService {

    static final String FREQUENCIA = "catalogo.frequencia_edicoes";
    static final String CAPA_MAX = "catalogo.capa_max_mb";
    static final ZoneId BRASILIA = ZoneId.of("America/Sao_Paulo");

    private final EdicaoRepository edicoes;
    private final MudancaNaEdicaoRepository historico;
    private final StartupRepository startups;
    private final ApresentacaoService apresentacao;
    private final ArmazenamentoArquivos arquivos;
    private final LeituraDeParametros parametros;
    private final ConsultaDeContas contas;
    private final Clock relogio;
    private final BloqueiosDaStartup bloqueios;
    private final AtuacaoService atuacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Criação do serviço
     * Objetivo: Receber repositórios, apresentação, armazenamento, parâmetros, contas e relógio.
     * Decisões: Injeção por construtor.
     */
    @SuppressWarnings("java:S107")
    public EdicoesService(EdicaoRepository edicoes, MudancaNaEdicaoRepository historico, StartupRepository startups,
                          ApresentacaoService apresentacao, ArmazenamentoArquivos arquivos, LeituraDeParametros parametros,
                          ConsultaDeContas contas, Clock relogio, BloqueiosDaStartup bloqueios, AtuacaoService atuacao) {
        this.bloqueios = bloqueios;
        this.atuacao = atuacao;
        this.edicoes = edicoes;
        this.historico = historico;
        this.startups = startups;
        this.apresentacao = apresentacao;
        this.arquivos = arquivos;
        this.parametros = parametros;
        this.contas = contas;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Listar edições
     * Objetivo: Listar todas as edições para a curadoria.
     * Decisões: Pelo início mais recente.
     */
    @Transactional(readOnly = true)
    public List<Edicao> listar() {
        return edicoes.findAllByOrderByInicioDesc();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Detalhe para a curadoria
     * Objetivo: Mostrar a edição com todas as startups da composição (indicando quem não aparece para o investidor), o
     *           curador responsável e o histórico.
     * Decisões: Startup que saiu do catálogo continua na composição, marcada como oculta.
     */
    @Transactional(readOnly = true)
    public Detalhe detalhe(UUID id) {
        Edicao e = edicao(id);
        Map<UUID, Startup> porId = carregar(e.getStartups());
        List<StartupNaEdicao> itens = e.getStartups().stream().map(porId::get).filter(java.util.Objects::nonNull)
                .map(s -> new StartupNaEdicao(apresentacao.cartao(s), elegivel(s))).toList();
        return new Detalhe(e, contas.nome(e.getCuradorId()).orElse(null), itens, historico.findByEdicaoIdOrderByEmDesc(id).stream()
                .map(m -> new Mudanca(m.getAcao(), m.getDetalhe(), m.getPor() == null ? null : contas.nome(m.getPor()).orElse(null), m.getEm()))
                .toList());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Startups elegíveis
     * Objetivo: Listar as startups publicadas que aceitam edições, para o curador escolher.
     * Decisões: Pelo nome aprovado.
     */
    @Transactional(readOnly = true)
    public List<ApresentacaoService.Cartao> elegiveis() {
        return startups.findBySituacaoOrderByNomeAprovadoAsc(SituacaoDaStartup.PUBLICADA).stream()
                .filter(Startup::isParticipaEdicoes).map(apresentacao::cartao).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Criar edição
     * Objetivo: Abrir uma edição em rascunho com o curador responsável.
     * Decisões: Sem fim informado, usa a frequência padrão das edições.
     */
    @Transactional
    public Edicao criar(UUID curador, Edicao.Dados dados) {
        Edicao e = edicoes.save(Edicao.nova(comFim(dados), curador, relogio.instant()));
        registrar(e, "CRIADA", e.getTitulo(), curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Editar edição
     * Objetivo: Trocar título, descrição, critérios e vigência.
     * Decisões: Histórico com a vigência nova.
     */
    @Transactional
    public Edicao editar(UUID id, UUID curador, Edicao.Dados dados) {
        Edicao e = edicao(id);
        e.editar(comFim(dados), relogio.instant());
        e.seguirVigencia(hoje(), relogio.instant());
        registrar(e, "DADOS_ALTERADOS", e.getTitulo() + " · " + e.getInicio() + " a " + e.getFim(), curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Definir startups
     * Objetivo: Trocar a composição e a ordem das startups da edição.
     * Decisões: Startup nova na lista precisa estar publicada e aceitar edições; quem já estava pode continuar, mesmo
     *           oculta. O histórico guarda a ordem completa pelos nomes.
     */
    @Transactional
    public Edicao definirStartups(UUID id, UUID curador, List<UUID> ordem) {
        Edicao e = edicao(id);
        List<UUID> lista = ordem == null ? List.of() : ordem;
        Map<UUID, Startup> porId = carregar(lista);
        for (UUID s : lista) {
            Startup st = porId.get(s);
            if (st == null) {
                throw new RecursoNaoEncontradoException("Startup não encontrada.");
            }
            if (!e.getStartups().contains(s) && !elegivel(st)) {
                throw new RegraDeNegocioException("startup_inelegivel",
                        "A " + st.getNomeAprovado() + " não está publicada ou não participa de edições temáticas.");
            }
        }
        e.definirStartups(lista, relogio.instant());
        registrar(e, "COMPOSICAO_ALTERADA", lista.stream().map(s -> porId.get(s).getNomeAprovado()).collect(Collectors.joining(" · ")), curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Publicar edição
     * Objetivo: Tirar a edição do rascunho (agendada ou vigente, pela data).
     * Decisões: Regras na edição.
     */
    @Transactional
    public Edicao publicar(UUID id, UUID curador) {
        Edicao e = edicao(id);
        e.publicar(hoje(), relogio.instant());
        registrar(e, "PUBLICADA", e.getSituacao().name(), curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Arquivar edição
     * Objetivo: Tirar a edição do ar antes do fim.
     * Decisões: Sem volta.
     */
    @Transactional
    public Edicao arquivar(UUID id, UUID curador) {
        Edicao e = edicao(id);
        e.arquivar(relogio.instant());
        registrar(e, "ARQUIVADA", null, curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Trocar capa
     * Objetivo: Receber a imagem da capa.
     * Decisões: PNG, JPG ou WebP, até catalogo.capa_max_mb. A capa antiga é apagada.
     */
    @Transactional
    public Edicao trocarCapa(UUID id, UUID curador, byte[] conteudo) {
        long maximo = parametros.inteiro(CAPA_MAX) * 1024 * 1024;
        if (conteudo == null || conteudo.length == 0 || conteudo.length > maximo) {
            throw new RegraDeNegocioException("capa_tamanho", "A capa precisa ter até " + parametros.inteiro(CAPA_MAX) + " MB.");
        }
        String tipo = StartupsService.formatoDaImagem(conteudo)
                .orElseThrow(() -> new RegraDeNegocioException("capa_formato", "Envie a capa em PNG, JPG ou WebP."));
        Edicao e = edicao(id);
        String chave = "edicoes/" + id + "/capa-" + UUID.randomUUID() + "." + tipo.substring(tipo.indexOf('/') + 1);
        arquivos.gravar(chave, new ByteArrayInputStream(conteudo), conteudo.length, tipo);
        String antiga = e.trocarCapa(chave, tipo, relogio.instant());
        if (antiga != null) {
            arquivos.remover(antiga);
        }
        registrar(e, "CAPA_ALTERADA", null, curador);
        return e;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Ler capa
     * Objetivo: Abrir a imagem da capa, para a curadoria ou, se vigente, para qualquer usuário cadastrado.
     * Decisões: Edição sem capa ou fora do ar para o investidor responde 404.
     */
    @Transactional(readOnly = true)
    public Capa capa(UUID id, boolean equipe) {
        Edicao e = edicao(id);
        if (e.getCapaChave() == null || (!equipe && e.getSituacao() != Edicao.Situacao.VIGENTE)) {
            throw new RecursoNaoEncontradoException("Capa não encontrada.");
        }
        return new Capa(arquivos.ler(e.getCapaChave()), e.getCapaTipo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edições vigentes
     * Objetivo: Listar ao investidor as edições no ar, cada uma com as startups que podem aparecer, na ordem do curador.
     * Decisões: Edição sem nenhuma startup visível some da lista.
     */
    @Transactional(readOnly = true)
    public List<EdicaoNoCatalogo> vigentes(UUID eu, String organizacao) {
        Set<UUID> bloquearam = bloquearam(eu, organizacao);
        List<EdicaoNoCatalogo> lista = new ArrayList<>();
        for (Edicao e : edicoes.findBySituacaoInOrderByInicioDesc(EnumSet.of(Edicao.Situacao.VIGENTE))) {
            EdicaoNoCatalogo item = noCatalogo(e, bloquearam);
            if (!item.startups().isEmpty()) {
                lista.add(item);
            }
        }
        return lista;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edição no catálogo
     * Objetivo: Mostrar uma edição vigente ao investidor.
     * Decisões: Fora do ar responde 404.
     */
    @Transactional(readOnly = true)
    public EdicaoNoCatalogo vigente(UUID eu, String organizacao, UUID id) {
        Edicao e = edicao(id);
        if (e.getSituacao() != Edicao.Situacao.VIGENTE) {
            throw new RecursoNaoEncontradoException("Edição não encontrada.");
        }
        return noCatalogo(e, bloquearam(eu, organizacao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Seguir a vigência de todas
     * Objetivo: Pôr no ar as agendadas que começaram e arquivar as que terminaram.
     * Decisões: Usado pela R10. Devolve quantas mudaram.
     */
    @Transactional
    public int seguirVigencias() {
        int mudaram = 0;
        LocalDate hoje = hoje();
        for (Edicao e : edicoes.findBySituacaoInOrderByInicioDesc(EnumSet.of(Edicao.Situacao.AGENDADA, Edicao.Situacao.VIGENTE))) {
            if (e.seguirVigencia(hoje, relogio.instant())) {
                registrar(e, "VIGENCIA_" + e.getSituacao().name(), "Rotina R10", null);
                mudaram++;
            }
        }
        return mudaram;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Montar edição do catálogo
     * Objetivo: Juntar a edição às startups visíveis.
     * Decisões: Ordem do curador.
     */
    private EdicaoNoCatalogo noCatalogo(Edicao e, Set<UUID> bloquearam) {
        Map<UUID, Startup> porId = carregar(e.getStartups());
        List<ApresentacaoService.Cartao> visiveis = e.getStartups().stream().map(porId::get)
                .filter(s -> s != null && elegivel(s) && !bloquearam.contains(s.getId())).map(apresentacao::cartao).toList();
        return new EdicaoNoCatalogo(e.getId(), e.getTitulo(), e.getDescricao(), e.getCapaChave() != null, e.getInicio(), e.getFim(),
                visiveis);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:25
     * Cartão Jira: KAN-67
     * Título: Startups que bloquearam
     * Objetivo: Ler as startups que bloquearam quem está atuando.
     * Decisões: Pela atuação (pessoa ou organização).
     */
    private Set<UUID> bloquearam(UUID eu, String organizacao) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        return bloqueios.startupsQueBloquearam(a.pessoa(), a.organizacao());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Elegível
     * Objetivo: Dizer se a startup pode aparecer numa edição.
     * Decisões: Publicada e aceitando edições.
     */
    private static boolean elegivel(Startup s) {
        return s.getSituacao() == SituacaoDaStartup.PUBLICADA && s.isParticipaEdicoes();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Fim pela frequência
     * Objetivo: Completar o fim da vigência pelo período padrão das edições.
     * Decisões: Semanal 7, quinzenal 15, mensal 1 mês, bimestral 2 meses; o fim é o dia anterior.
     */
    private Edicao.Dados comFim(Edicao.Dados d) {
        if (d.inicio() == null) {
            throw new RegraDeNegocioException("vigencia_invalida", "Informe o início da vigência.");
        }
        if (d.fim() != null) {
            return d;
        }
        LocalDate fim = switch (parametros.opcao(FREQUENCIA)) {
            case "SEMANAL" -> d.inicio().plusDays(7);
            case "QUINZENAL" -> d.inicio().plusDays(15);
            case "BIMESTRAL" -> d.inicio().plusMonths(2);
            default -> d.inicio().plusMonths(1);
        };
        return new Edicao.Dados(d.titulo(), d.descricao(), d.criterios(), d.inicio(), fim.minusDays(1));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Registrar mudança
     * Objetivo: Gravar o histórico e a trilha de auditoria.
     * Decisões: Mesma transação.
     */
    private void registrar(Edicao e, String acao, String detalhe, UUID por) {
        historico.save(MudancaNaEdicao.nova(e.getId(), acao, detalhe, por, relogio.instant()));
        if (por != null) {
            ContextoDeAuditoria.descrever("EDICAO_" + acao, "edicao", e.getId(), detalhe);
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Carregar startups
     * Objetivo: Ler várias startups de uma vez, por identificador.
     * Decisões: Mapa para manter a ordem da edição.
     */
    private Map<UUID, Startup> carregar(List<UUID> ids) {
        return startups.findAllById(ids).stream().collect(Collectors.toMap(Startup::getId, Function.identity()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Hoje
     * Objetivo: Dar a data de hoje em Brasília.
     * Decisões: Vigência em dias.
     */
    private LocalDate hoje() {
        return LocalDate.ofInstant(relogio.instant(), BRASILIA);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Edição
     * Objetivo: Ler a edição pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Edicao edicao(UUID id) {
        return edicoes.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Edição não encontrada."));
    }

    public record StartupNaEdicao(ApresentacaoService.Cartao startup, boolean visivel) {
    }

    public record Mudanca(String acao, String detalhe, String por, java.time.Instant em) {
    }

    public record Detalhe(Edicao edicao, String curador, List<StartupNaEdicao> startups, List<Mudanca> historico) {
    }

    public record EdicaoNoCatalogo(UUID id, String titulo, String descricao, boolean temCapa, LocalDate inicio, LocalDate fim,
                                   List<ApresentacaoService.Cartao> startups) {
    }

    public record Capa(InputStream conteudo, String tipo) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Curador da edição
     * Objetivo: Devolver o nome do curador responsável.
     * Decisões: Vazio se a conta não existir mais.
     */
    public Optional<String> curador(Edicao e) {
        return contas.nome(e.getCuradorId());
    }
}
