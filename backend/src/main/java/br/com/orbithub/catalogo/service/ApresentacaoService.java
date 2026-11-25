package br.com.orbithub.catalogo.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.service.RodadasService;
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.Fundador;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.domain.VisibilidadeDaStartup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.empreendimentos.service.IndicadoresDaStartupService;
import br.com.orbithub.empreendimentos.service.StartupsService;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.verificacao.domain.Selo;
import br.com.orbithub.verificacao.domain.SujeitoDaVerificacao;
import br.com.orbithub.verificacao.service.VerificacoesService;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 10:40
 * Cartão Jira: KAN-29, KAN-36
 * Título: Apresentação da startup em três páginas
 * Objetivo: Montar a apresentação da startup no catálogo (Apresentação, Resumo executivo e Conheça o empreendimento),
 *           só com o que a equipe marcou como público, para o investidor, para a pré-visualização da equipe e, completa,
 *           para o curador.
 * Decisões: Três modos. CATALOGO: startup publicada, com nome, segmento e estágio aprovados pela curadoria e os dados da
 *           rodada só para investidor verificado. PREVIA: a equipe vê a versão atual, escolhendo ver como investidor
 *           verificado ou não. COMPLETA: o curador vê todos os campos, públicos ou não. Campos vazios ou confidenciais
 *           somem, e blocos sem itens também. Valuation e termos do instrumento nunca entram.
 */
@Service
public class ApresentacaoService {

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-29
     * Título: Modo da apresentação
     * Objetivo: Dizer para quem a apresentação é montada.
     * Decisões: Ver o objetivo da classe.
     */
    public enum Modo { CATALOGO, PREVIA, COMPLETA }

    static final String AVISO_INDICADORES = "Declarado pelo empreendedor, não auditado pela plataforma.";
    static final String SEM_RODADA = "Sem rodada aberta no momento.";
    static final String RODADA_SO_VERIFICADOS = "Os dados da rodada aparecem só para investidores verificados.";
    static final String INDICADORES = "INDICADORES";
    static final String RODADA = "RODADA";
    static final String FUNDADORES = "FUNDADORES";
    static final DateTimeFormatter MES = DateTimeFormatter.ofPattern("MM/yyyy");

    private static final List<PaginaModelo> MODELO = List.of(
            new PaginaModelo("apresentacao", "Apresentação", List.of(
                    new BlocoModelo("O negócio", List.of(CampoDaStartup.DESCRICAO_PRODUTO.name())),
                    new BlocoModelo("Problema", List.of(CampoDaStartup.PROBLEMA.name(), CampoDaStartup.QUEM_ENFRENTA.name())),
                    new BlocoModelo("Solução", List.of(CampoDaStartup.SOLUCAO.name(), CampoDaStartup.COMO_FUNCIONA.name())),
                    new BlocoModelo("Proposta de valor", List.of(CampoDaStartup.DIFERENCIAL.name())))),
            new PaginaModelo("resumo", "Resumo executivo", List.of(
                    new BlocoModelo("Mercado", List.of(CampoDaStartup.PUBLICO_ALVO.name(), CampoDaStartup.PERFIL_CLIENTES.name(),
                            CampoDaStartup.DIMENSAO_MERCADO.name(), CampoDaStartup.MERCADO_ENDERECAVEL.name(),
                            CampoDaStartup.CONCORRENTES.name(), CampoDaStartup.BARREIRAS_ENTRADA.name())),
                    new BlocoModelo("Diferenciais", List.of(CampoDaStartup.DIFERENCIAIS_COMPETITIVOS.name())),
                    new BlocoModelo("Modelo de negócio", List.of(CampoDaStartup.MODELO.name(), CampoDaStartup.GERACAO_RECEITA.name(),
                            CampoDaStartup.FONTES_FATURAMENTO.name(), CampoDaStartup.PRECIFICACAO.name(),
                            CampoDaStartup.RECEITAS_RECORRENTES.name(), CampoDaStartup.ESTRUTURA_CUSTOS.name())),
                    new BlocoModelo("Estágio", List.of(CampoDaStartup.ESTAGIO.name())),
                    new BlocoModelo("Indicadores", List.of(INDICADORES, CampoDaStartup.OUTROS_INDICADORES.name())),
                    new BlocoModelo("Necessidade de investimento", List.of(CampoDaStartup.DESTINO_PRODUTO.name(),
                            CampoDaStartup.DESTINO_COMERCIAL.name(), CampoDaStartup.DESTINO_EQUIPE.name(),
                            CampoDaStartup.DESTINO_OPERACAO.name(), CampoDaStartup.DESTINO_OUTROS.name(),
                            CampoDaStartup.PRAZO_USO_MESES.name(), CampoDaStartup.METAS_DO_INVESTIMENTO.name(), RODADA)))),
            new PaginaModelo("conheca", "Conheça o empreendimento", List.of(
                    new BlocoModelo("Trajetória", List.of(CampoDaStartup.EXPERIENCIA.name(), CampoDaStartup.FORMACAO.name())),
                    new BlocoModelo("Equipe", List.of(FUNDADORES, CampoDaStartup.COMPOSICAO_EQUIPE.name(),
                            CampoDaStartup.ESTRUTURA_OPERACIONAL.name(), CampoDaStartup.PARCEIROS_ESTRATEGICOS.name())),
                    new BlocoModelo("Produto", List.of(CampoDaStartup.TEM_MVP.name(), CampoDaStartup.FUNCIONALIDADES.name(),
                            CampoDaStartup.TECNOLOGIA.name(), CampoDaStartup.PROPRIEDADE_INTELECTUAL.name())),
                    new BlocoModelo("Estratégia comercial", List.of(CampoDaStartup.MODELO_COMERCIALIZACAO.name(),
                            CampoDaStartup.CANAIS_DISTRIBUICAO.name(), CampoDaStartup.ESTRATEGIA_EXPANSAO.name())),
                    new BlocoModelo("Potencial de crescimento", List.of(CampoDaStartup.POTENCIAL_CRESCIMENTO.name(),
                            CampoDaStartup.TENDENCIAS.name(), CampoDaStartup.OBJETIVOS_CURTO_PRAZO.name(),
                            CampoDaStartup.OBJETIVOS_MEDIO_PRAZO.name(), CampoDaStartup.OBJETIVOS_LONGO_PRAZO.name(),
                            CampoDaStartup.METAS.name(), CampoDaStartup.RISCOS.name(), CampoDaStartup.MITIGACAO.name())))));

    private final StartupRepository startups;
    private final StartupsService cadastro;
    private final IndicadoresDaStartupService indicadores;
    private final RodadasService rodadas;
    private final ListasService listas;
    private final VerificacoesService verificacoes;
    private final AtuacaoService atuacao;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Criação do serviço
     * Objetivo: Receber os serviços que fornecem os dados da startup, da rodada e da verificação.
     * Decisões: Injeção por construtor.
     */
    public ApresentacaoService(StartupRepository startups, StartupsService cadastro, IndicadoresDaStartupService indicadores,
                               RodadasService rodadas, ListasService listas, VerificacoesService verificacoes,
                               AtuacaoService atuacao) {
        this.startups = startups;
        this.cadastro = cadastro;
        this.indicadores = indicadores;
        this.rodadas = rodadas;
        this.listas = listas;
        this.verificacoes = verificacoes;
        this.atuacao = atuacao;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Apresentação no catálogo
     * Objetivo: Montar a apresentação de uma startup publicada para quem está navegando no catálogo.
     * Decisões: Só usuários cadastrados chegam aqui (a API exige login). Startup fora do catálogo responde 404. O
     *           investidor é verificado se a pessoa, ou a organização em nome de quem atua, tem o selo (KAN-62).
     */
    @Transactional(readOnly = true)
    public Apresentacao doCatalogo(UUID eu, String organizacao, UUID startupId) {
        Startup s = startup(startupId);
        if (s.getSituacao() != SituacaoDaStartup.PUBLICADA) {
            throw new RecursoNaoEncontradoException("Startup não encontrada.");
        }
        return montar(s, Modo.CATALOGO, investidorVerificado(eu, organizacao));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-29
     * Título: Pré-visualização da equipe
     * Objetivo: Mostrar à equipe como a startup aparece para o investidor, verificado ou não.
     * Decisões: Qualquer membro da equipe; usa a versão atual, já com as alterações ainda em revisão.
     */
    @Transactional(readOnly = true)
    public Apresentacao previa(UUID eu, UUID startupId, boolean comoVerificado) {
        cadastro.papel(eu, startupId);
        return montar(startup(startupId), Modo.PREVIA, comoVerificado);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-84
     * Título: Apresentação completa para a curadoria
     * Objetivo: Mostrar ao curador todos os campos da startup, nas três páginas.
     * Decisões: Ignora a visibilidade; a equipe interna confere tudo.
     */
    @Transactional(readOnly = true)
    public Apresentacao completa(UUID startupId) {
        return montar(startup(startupId), Modo.COMPLETA, true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Cartão da startup
     * Objetivo: Montar o resumo usado nas listas do catálogo (edições): nome, segmento, estágio e localização, se pública.
     * Decisões: Usa os valores aprovados.
     */
    @Transactional(readOnly = true)
    public Cartao cartao(Startup s) {
        Map<String, Boolean> vis = s.visibilidade();
        String estagio = s.getEstagioAprovado();
        return new Cartao(s.getId(), s.getNomeAprovado(), listas.rotulo(Lista.SEGMENTO, s.getSegmentoAprovado()),
                estagio == null ? null : MaturidadeDoNegocio.valueOf(estagio).getRotulo(),
                VisibilidadeDaStartup.publico(VisibilidadeDaStartup.Extra.LOCALIZACAO.name(), vis)
                        ? listas.rotulo(Lista.LOCALIZACAO, s.cadastro().localizacao()) : null,
                s.getLogotipoChave() != null);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Montar apresentação
     * Objetivo: Preencher o cabeçalho e as três páginas conforme o modo.
     * Decisões: Blocos e páginas vazios ficam de fora, sem espaço em branco.
     */
    private Apresentacao montar(Startup s, Modo modo, boolean verificado) {
        boolean catalogo = modo == Modo.CATALOGO;
        Map<String, Boolean> vis = s.visibilidade();
        Map<CampoDaStartup, String> campos = s.campos();
        Startup.Cadastro c = s.cadastro();
        String estagio = catalogo ? s.getEstagioAprovado() : campos.get(CampoDaStartup.ESTAGIO);
        List<Pagina> paginas = new ArrayList<>();
        for (PaginaModelo p : MODELO) {
            List<Bloco> blocos = new ArrayList<>();
            for (BlocoModelo b : p.blocos()) {
                List<Item> itens = new ArrayList<>();
                List<String> avisos = new ArrayList<>();
                for (String codigo : b.itens()) {
                    itensDo(codigo, s, modo, verificado, vis, campos, estagio, itens, avisos);
                }
                if (!itens.isEmpty() || !avisos.isEmpty()) {
                    blocos.add(new Bloco(b.titulo(), itens, avisos));
                }
            }
            paginas.add(new Pagina(p.id(), p.titulo(), blocos));
        }
        boolean mostra = modo == Modo.COMPLETA;
        Cabecalho cabecalho = new Cabecalho(s.getId(), catalogo ? s.getNomeAprovado() : c.nome(),
                listas.rotulo(Lista.SEGMENTO, catalogo ? s.getSegmentoAprovado() : c.segmento()),
                estagio == null ? null : MaturidadeDoNegocio.valueOf(estagio).getRotulo(),
                mostra || publico(VisibilidadeDaStartup.Extra.LOCALIZACAO.name(), vis) ? listas.rotulo(Lista.LOCALIZACAO, c.localizacao()) : null,
                mostra || publico(VisibilidadeDaStartup.Extra.SITE.name(), vis) ? c.site() : null,
                mostra || publico(VisibilidadeDaStartup.Extra.REDES_SOCIAIS.name(), vis)
                        ? c.redes().entrySet().stream().map(e -> new Rede(e.getKey().name(), e.getValue())).toList() : List.of(),
                s.getLogotipoChave() != null, !s.constituida(), verificacoes.selo(SujeitoDaVerificacao.STARTUP, s.getId()));
        return new Apresentacao(modo, verificado, cabecalho, paginas);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Itens de um código
     * Objetivo: Transformar um código do modelo (campo, indicadores, rodada ou fundadores) em itens e avisos.
     * Decisões: Na curadoria a rodada aparece sem a restrição de verificado.
     */
    @SuppressWarnings("java:S107")
    private void itensDo(String codigo, Startup s, Modo modo, boolean verificado, Map<String, Boolean> vis,
                         Map<CampoDaStartup, String> campos, String estagio, List<Item> itens, List<String> avisos) {
        boolean completa = modo == Modo.COMPLETA;
        switch (codigo) {
            case INDICADORES -> {
                if (!completa && !publico(VisibilidadeDaStartup.Extra.INDICADORES.name(), vis)) {
                    return;
                }
                int antes = itens.size();
                indicadores.atuais(s.getId()).stream().filter(i -> i.atual() != null).forEach(i -> itens.add(new Item(i.rotulo(),
                        numero(i.atual().valor()) + " (ref. " + MES.format(i.atual().referencia()) + ")"
                                + (i.desatualizado() ? " · desatualizado" : ""))));
                if (itens.size() > antes) {
                    avisos.add(AVISO_INDICADORES);
                }
            }
            case RODADA -> rodada(s, completa || publico(VisibilidadeDaStartup.Extra.DADOS_DA_RODADA.name(), vis), completa || verificado,
                    itens, avisos);
            case FUNDADORES -> {
                if (completa || publico(VisibilidadeDaStartup.Extra.FUNDADORES.name(), vis)) {
                    for (Fundador f : s.cadastro().fundadores()) {
                        itens.add(new Item("Fundador", f.nome() + (f.cargo() == null ? "" : " · " + f.cargo())));
                    }
                }
            }
            default -> {
                CampoDaStartup campo = CampoDaStartup.valueOf(codigo);
                String valor = campo == CampoDaStartup.ESTAGIO ? estagio : campos.get(campo);
                if (valor != null && (completa || publico(codigo, vis))) {
                    itens.add(new Item(campo.getRotulo(), formatar(campo, valor)));
                }
            }
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Dados da rodada
     * Objetivo: Mostrar estágio, instrumento e valores da rodada aberta, quando públicos e o investidor é verificado.
     * Decisões: Sem rodada aberta, avisa. Rodada aberta, mas dados confidenciais: nada aparece. Investidor não
     *           verificado: só o aviso de que os dados são restritos.
     */
    private void rodada(Startup s, boolean publica, boolean verificado, List<Item> itens, List<String> avisos) {
        Rodada r = rodadas.rodadaAberta(s.getId()).orElse(null);
        if (r == null) {
            avisos.add(SEM_RODADA);
            return;
        }
        if (!publica) {
            return;
        }
        if (!verificado) {
            avisos.add(RODADA_SO_VERIFICADOS);
            return;
        }
        Rodada.Termos t = r.termos();
        if (t.estagio() != null) {
            itens.add(new Item("Estágio da rodada", listas.rotulo(Lista.ESTAGIO_RODADA, t.estagio())));
        }
        if (t.instrumento() != null) {
            itens.add(new Item("Instrumento", listas.rotulo(Lista.INSTRUMENTO, t.instrumento())));
        }
        valor(itens, "Valor alvo", t.valorAlvo());
        valor(itens, "Valor mínimo", t.valorMinimo());
        valor(itens, "Valor máximo", t.valorMaximo());
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Valor em reais
     * Objetivo: Acrescentar um valor da rodada, se houver.
     * Decisões: Formato brasileiro.
     */
    private static void valor(List<Item> itens, String rotulo, BigDecimal valor) {
        if (valor != null) {
            itens.add(new Item(rotulo, NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(valor)));
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Número
     * Objetivo: Escrever um número no formato brasileiro.
     * Decisões: Até duas casas.
     */
    private static String numero(BigDecimal valor) {
        NumberFormat f = NumberFormat.getNumberInstance(Locale.forLanguageTag("pt-BR"));
        f.setMaximumFractionDigits(2);
        return f.format(valor);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Formatar valor
     * Objetivo: Escrever o valor do campo como o investidor lê.
     * Decisões: Lista e escala viram o rótulo; sim/não, percentual e meses ganham o texto.
     */
    private String formatar(CampoDaStartup campo, String valor) {
        return switch (campo.getTipo()) {
            case LISTA -> listas.rotulo(campo.getLista(), valor);
            case ESCALA -> MaturidadeDoNegocio.valueOf(valor).getRotulo();
            case SIM_NAO -> "SIM".equals(valor) ? "Sim" : "Não";
            case PERCENTUAL -> numero(new BigDecimal(valor)) + "%";
            case INTEIRO -> valor + " meses";
            default -> valor;
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: É público
     * Objetivo: Atalho para a regra de visibilidade.
     * Decisões: Ver VisibilidadeDaStartup.
     */
    private static boolean publico(String codigo, Map<String, Boolean> vis) {
        return VisibilidadeDaStartup.publico(codigo, vis);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-28
     * Título: Investidor verificado
     * Objetivo: Dizer se quem navega tem o selo de verificado, como pessoa ou pela organização em nome de quem atua.
     * Decisões: Sem a organização no cabeçalho, vale o selo da pessoa.
     */
    private boolean investidorVerificado(UUID eu, String organizacao) {
        AtuacaoService.Atuacao a = atuacao.paraVer(eu, organizacao);
        Selo selo = a.emNomeDaOrganizacao() ? verificacoes.selo(SujeitoDaVerificacao.ORGANIZACAO, a.organizacao())
                : verificacoes.selo(SujeitoDaVerificacao.PESSOA, eu);
        return selo == Selo.VERIFICADO;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Startup
     * Objetivo: Ler a startup pelo identificador.
     * Decisões: 404 se não existir.
     */
    private Startup startup(UUID id) {
        return startups.findById(id).orElseThrow(() -> new RecursoNaoEncontradoException("Startup não encontrada."));
    }

    record PaginaModelo(String id, String titulo, List<BlocoModelo> blocos) {
    }

    record BlocoModelo(String titulo, List<String> itens) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Apresentação
     * Objetivo: Levar o modo, se a visão é de verificado, o cabeçalho e as três páginas.
     * Decisões: Mesma forma nos três modos.
     */
    public record Apresentacao(Modo modo, boolean comoVerificado, Cabecalho cabecalho, List<Pagina> paginas) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-36
     * Título: Cabeçalho
     * Objetivo: Levar a identidade da startup: nome, segmento, estágio, localização, site, redes, logotipo e selos.
     * Decisões: Localização, site e redes nulos ou vazios quando confidenciais.
     */
    public record Cabecalho(UUID id, String nome, String segmento, String estagio, String localizacao, String site,
                            List<Rede> redes, boolean temLogotipo, boolean naoConstituida, Selo selo) {
    }

    public record Rede(String rede, String url) {
    }

    public record Pagina(String id, String titulo, List<Bloco> blocos) {
    }

    public record Bloco(String titulo, List<Item> itens, List<String> avisos) {
    }

    public record Item(String rotulo, String valor) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 10:40
     * Cartão Jira: KAN-85
     * Título: Cartão da startup
     * Objetivo: Levar o resumo da startup nas listas do catálogo.
     * Decisões: Localização nula quando confidencial.
     */
    public record Cartao(UUID id, String nome, String segmento, String estagio, String localizacao, boolean temLogotipo) {
    }
}
