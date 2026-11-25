package br.com.orbithub.catalogo.service;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.compartilhado.escalas.MaturidadeDoNegocio;
import br.com.orbithub.compartilhado.parametros.LeituraDeParametros;
import br.com.orbithub.conexoes.domain.Rodada;
import br.com.orbithub.conexoes.service.RodadasService;
import br.com.orbithub.empreendimentos.domain.CampoDaStartup;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import br.com.orbithub.investidores.domain.PerfilDeInvestimento;
import br.com.orbithub.investidores.service.AtuacaoService;
import br.com.orbithub.investidores.service.PerfilDeInvestimentoService;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-38, KAN-39
 * Título: Navegação do catálogo
 * Objetivo: Montar as listas do catálogo (seleção personalizada e novas oportunidades) com os filtros combinados, os
 *           critérios atendidos de cada startup e a marcação de favorita.
 * Decisões: Seleção personalizada: ordena pela quantidade de critérios atendidos e, no empate, pela publicação mais
 *           recente e pelo identificador (determinística); sem perfil, só pela data. Descartadas saem da seleção
 *           personalizada. Novas oportunidades: publicadas nos últimos catalogo.novas_dias, da mais recente. Filtros de
 *           rodada (estágio, instrumento, faixa) e critérios da rodada só valem para investidor verificado. Nunca há
 *           percentual. O bloqueio pela startup (KAN-67) entra com o Match (S7).
 */
@Service
public class NavegacaoService {

    static final String NOVAS = "catalogo.novas_dias";

    private final StartupRepository startups;
    private final ApresentacaoService apresentacao;
    private final RodadasService rodadas;
    private final ListasService listas;
    private final PerfilDeInvestimentoService perfis;
    private final AtuacaoService atuacao;
    private final MarcacoesDoInvestidor marcacoes;
    private final List<CriterioDeCompatibilidade> criterios;
    private final LeituraDeParametros parametros;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Criação do serviço
     * Objetivo: Receber os serviços e todos os critérios de compatibilidade registrados.
     * Decisões: A lista de critérios vem na ordem do @Order.
     */
    @SuppressWarnings("java:S107")
    public NavegacaoService(StartupRepository startups, ApresentacaoService apresentacao, RodadasService rodadas, ListasService listas,
                            PerfilDeInvestimentoService perfis, AtuacaoService atuacao, MarcacoesDoInvestidor marcacoes,
                            List<CriterioDeCompatibilidade> criterios, LeituraDeParametros parametros, Clock relogio) {
        this.startups = startups;
        this.apresentacao = apresentacao;
        this.rodadas = rodadas;
        this.listas = listas;
        this.perfis = perfis;
        this.atuacao = atuacao;
        this.marcacoes = marcacoes;
        this.criterios = List.copyOf(criterios);
        this.parametros = parametros;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-38, KAN-39
     * Título: Navegar
     * Objetivo: Devolver a lista pedida, filtrada e ordenada, com os critérios atendidos.
     * Decisões: O perfil é o da atuação (pessoa ou organização); só investidor tem perfil.
     */
    @Transactional(readOnly = true)
    public Resultado navegar(UUID eu, String organizacao, ListaDoCatalogo lista, Filtros filtros) {
        boolean verificado = apresentacao.investidorVerificado(eu, organizacao);
        Optional<PerfilDeInvestimento.Dados> perfil = perfil(eu, organizacao);
        Set<UUID> descartadas = marcacoes.ids(eu, MarcacoesDoInvestidor.Tipo.DESCARTADAS);
        Set<UUID> favoritas = marcacoes.ids(eu, MarcacoesDoInvestidor.Tipo.FAVORITOS);
        Instant desde = relogio.instant().minus(Duration.ofDays(parametros.inteiro(NOVAS)));
        Filtros f = verificado ? filtros : filtros.semRodada();
        List<Item> itens = new ArrayList<>();
        for (Startup s : startups.findBySituacaoOrderByNomeAprovadoAsc(SituacaoDaStartup.PUBLICADA)) {
            if (lista == ListaDoCatalogo.PERSONALIZADA && descartadas.contains(s.getId())) {
                continue;
            }
            if (lista == ListaDoCatalogo.NOVAS && (s.getPublicadaEm() == null || s.getPublicadaEm().isBefore(desde))) {
                continue;
            }
            Rodada rodada = rodadas.rodadaAberta(s.getId()).orElse(null);
            if (!f.aceita(s, rodada)) {
                continue;
            }
            List<String> atendidos = lista == ListaDoCatalogo.PERSONALIZADA && perfil.isPresent()
                    ? avaliar(perfil.get(), s, rodada, verificado) : List.of();
            itens.add(new Item(apresentacao.cartao(s), atendidos, rodada != null, favoritas.contains(s.getId()), s.getPublicadaEm()));
        }
        Comparator<Item> recentes = Comparator.comparing(Item::publicadaEm, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(i -> i.startup().id());
        itens.sort(lista == ListaDoCatalogo.PERSONALIZADA && perfil.isPresent()
                ? Comparator.comparingInt((Item i) -> -i.criterios().size()).thenComparing(recentes) : recentes);
        return new Resultado(lista, verificado, perfil.isPresent(), itens);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Avaliar critérios
     * Objetivo: Rodar cada estratégia e juntar as justificativas atendidas.
     * Decisões: Critérios da rodada pulados sem rodada aberta ou sem verificação.
     */
    private List<String> avaliar(PerfilDeInvestimento.Dados perfil, Startup s, Rodada rodada, boolean verificado) {
        String estagio = s.getEstagioAprovado();
        String modelo = s.campos().get(CampoDaStartup.MODELO);
        Rodada.Termos termos = rodada == null ? null : rodada.termos();
        CriterioDeCompatibilidade.Contexto c = new CriterioDeCompatibilidade.Contexto(perfil, s.getSegmentoAprovado(),
                listas.rotulo(Lista.SEGMENTO, s.getSegmentoAprovado()), estagio == null ? null : MaturidadeDoNegocio.valueOf(estagio),
                s.cadastro().localizacao(), modelo, modelo == null ? null : listas.rotulo(Lista.MODELO_NEGOCIO, modelo), termos,
                termos == null || termos.estagio() == null ? null : listas.rotulo(Lista.ESTAGIO_RODADA, termos.estagio()),
                termos == null || termos.instrumento() == null ? null : listas.rotulo(Lista.INSTRUMENTO, termos.instrumento()),
                rodada != null);
        List<String> atendidos = new ArrayList<>();
        for (CriterioDeCompatibilidade criterio : criterios) {
            if (criterio.daRodada() && (termos == null || !verificado)) {
                continue;
            }
            criterio.avaliar(c).ifPresent(atendidos::add);
        }
        return atendidos;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Perfil da atuação
     * Objetivo: Ler o perfil de investimento da pessoa ou da organização em nome de quem ela atua.
     * Decisões: Quem não é investidor ou não preencheu fica sem perfil (lista por data).
     */
    private Optional<PerfilDeInvestimento.Dados> perfil(UUID eu, String organizacao) {
        try {
            return perfis.ler(atuacao.paraVer(eu, organizacao)).perfil();
        } catch (RuntimeException e) {
            return Optional.empty();
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39
     * Título: Lista do catálogo
     * Objetivo: Enumerar as listas navegáveis.
     * Decisões: As edições temáticas têm rota própria (KAN-85).
     */
    public enum ListaDoCatalogo { PERSONALIZADA, NOVAS }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-38
     * Título: Filtros
     * Objetivo: Levar os filtros combináveis do catálogo.
     * Decisões: Nulo ou vazio = sem filtro. Faixa compara o valor alvo da rodada (ou o máximo, sem alvo).
     */
    public record Filtros(Set<String> segmentos, Set<MaturidadeDoNegocio> maturidades, Set<String> localizacoes, Boolean comRodada,
                          Set<String> estagios, Set<String> instrumentos, BigDecimal captacaoMinima, BigDecimal captacaoMaxima) {

        public static final Filtros NENHUM = new Filtros(Set.of(), Set.of(), Set.of(), null, Set.of(), Set.of(), null, null);

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:03
         * Cartão Jira: KAN-38
         * Título: Sem filtros de rodada
         * Objetivo: Tirar os filtros de rodada, para quem não é verificado.
         * Decisões: "Com rodada aberta" continua valendo.
         */
        Filtros semRodada() {
            return new Filtros(segmentos, maturidades, localizacoes, comRodada, Set.of(), Set.of(), null, null);
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:03
         * Cartão Jira: KAN-38
         * Título: Aceita
         * Objetivo: Dizer se a startup passa em todos os filtros.
         * Decisões: Filtros de rodada exigem rodada aberta.
         */
        boolean aceita(Startup s, Rodada rodada) {
            String estagio = s.getEstagioAprovado();
            if (!vazio(segmentos) && !segmentos.contains(s.getSegmentoAprovado())) {
                return false;
            }
            if (!vazio(maturidades) && (estagio == null || !maturidades.contains(MaturidadeDoNegocio.valueOf(estagio)))) {
                return false;
            }
            if (!vazio(localizacoes) && !localizacoes.contains(s.cadastro().localizacao())) {
                return false;
            }
            if (comRodada != null && comRodada != (rodada != null)) {
                return false;
            }
            boolean filtraRodada = !vazio(estagios) || !vazio(instrumentos) || captacaoMinima != null || captacaoMaxima != null;
            if (!filtraRodada) {
                return true;
            }
            if (rodada == null) {
                return false;
            }
            Rodada.Termos t = rodada.termos();
            BigDecimal valor = t.valorAlvo() != null ? t.valorAlvo() : t.valorMaximo();
            return (vazio(estagios) || estagios.contains(t.estagio()))
                    && (vazio(instrumentos) || instrumentos.contains(t.instrumento()))
                    && (captacaoMinima == null || (valor != null && valor.compareTo(captacaoMinima) >= 0))
                    && (captacaoMaxima == null || (valor != null && valor.compareTo(captacaoMaxima) <= 0));
        }

        /**
         * Autor: Nicholas Bruno Pimentel
         * Data: 25/11/2026
         * Hora: 11:03
         * Cartão Jira: KAN-38
         * Título: Vazio
         * Objetivo: Dizer se o filtro de conjunto está vazio.
         * Decisões: Nulo conta como vazio.
         */
        private static boolean vazio(Set<?> conjunto) {
            return conjunto == null || conjunto.isEmpty();
        }
    }

    public record Item(ApresentacaoService.Cartao startup, List<String> criterios, boolean rodadaAberta, boolean favorito,
                       Instant publicadaEm) {
    }

    public record Resultado(ListaDoCatalogo lista, boolean verificado, boolean comPerfil, List<Item> itens) {
    }
}
