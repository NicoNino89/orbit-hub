package br.com.orbithub.catalogo.service;

import br.com.orbithub.compartilhado.erro.RecursoNaoEncontradoException;
import br.com.orbithub.empreendimentos.domain.SituacaoDaStartup;
import br.com.orbithub.empreendimentos.domain.Startup;
import br.com.orbithub.empreendimentos.repository.StartupRepository;
import java.sql.Timestamp;
import java.time.Clock;
import java.time.Instant;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 11:03
 * Cartão Jira: KAN-41, KAN-42, KAN-43, KAN-44
 * Título: Favoritos, histórico e descartes
 * Objetivo: Salvar e remover favoritos, registrar as startups visualizadas, descartar e desfazer o descarte, e listar
 *           cada um, por pessoa.
 * Decisões: Tabelas simples (pessoa, startup, data) por JDBC. Nada notifica o empreendedor nem aparece para ele.
 *           Favoritar e descartar exigem startup publicada; a lista mostra como indisponível a que saiu do catálogo.
 *           Favoritar desfaz o descarte e vice-versa.
 */
@Service
public class MarcacoesDoInvestidor {

    static final String FAVORITO = "catalogo_favorito";
    static final String VISTA = "catalogo_visualizacao";
    static final String DESCARTE = "catalogo_descarte";
    static final Map<String, String> COLUNA = Map.of(FAVORITO, "salvo_em", VISTA, "vista_em", DESCARTE, "descartada_em");

    private final JdbcTemplate jdbc;
    private final StartupRepository startups;
    private final ApresentacaoService apresentacao;
    private final Clock relogio;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Criação
     * Objetivo: Receber o JDBC, as startups, a apresentação e o relógio.
     * Decisões: Injeção por construtor.
     */
    public MarcacoesDoInvestidor(JdbcTemplate jdbc, StartupRepository startups, ApresentacaoService apresentacao, Clock relogio) {
        this.jdbc = jdbc;
        this.startups = startups;
        this.apresentacao = apresentacao;
        this.relogio = relogio;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Favoritar
     * Objetivo: Salvar a startup publicada nos favoritos.
     * Decisões: Idempotente; tira do descarte.
     */
    @Transactional
    public void favoritar(UUID eu, UUID startupId) {
        exigirPublicada(startupId);
        gravar(FAVORITO, eu, startupId, false);
        apagar(DESCARTE, eu, startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Desfavoritar
     * Objetivo: Tirar a startup dos favoritos.
     * Decisões: Funciona mesmo para startup que saiu do catálogo.
     */
    @Transactional
    public void desfavoritar(UUID eu, UUID startupId) {
        apagar(FAVORITO, eu, startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-44
     * Título: Descartar
     * Objetivo: Tirar a startup publicada da seleção personalizada da pessoa.
     * Decisões: Idempotente; tira dos favoritos.
     */
    @Transactional
    public void descartar(UUID eu, UUID startupId) {
        exigirPublicada(startupId);
        gravar(DESCARTE, eu, startupId, false);
        apagar(FAVORITO, eu, startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-44
     * Título: Desfazer descarte
     * Objetivo: Devolver a startup à seleção personalizada.
     * Decisões: Sem efeito se não estava descartada.
     */
    @Transactional
    public void desfazerDescarte(UUID eu, UUID startupId) {
        apagar(DESCARTE, eu, startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-43
     * Título: Registrar visita
     * Objetivo: Guardar que a pessoa abriu a apresentação da startup.
     * Decisões: Uma linha por startup; a visita nova atualiza a data.
     */
    @Transactional
    public void registrarVisita(UUID eu, UUID startupId) {
        gravar(VISTA, eu, startupId, true);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-43
     * Título: Limpar histórico
     * Objetivo: Apagar todas as visitas da pessoa.
     * Decisões: Sem volta.
     */
    @Transactional
    public void limparHistorico(UUID eu) {
        jdbc.update("DELETE FROM " + VISTA + " WHERE usuario_id = ?", eu);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-42, KAN-43, KAN-44
     * Título: Listar marcações
     * Objetivo: Listar favoritos, visitas ou descartes, do mais recente ao mais antigo, com o cartão da startup e se ela
     *           ainda está no catálogo.
     * Decisões: Startup fora do catálogo aparece como indisponível, sem link.
     */
    @Transactional(readOnly = true)
    public List<Marcada> listar(UUID eu, Tipo tipo) {
        String tabela = tabela(tipo);
        List<Map<String, Object>> linhas = jdbc.queryForList("SELECT startup_id, " + COLUNA.get(tabela) + " AS em FROM " + tabela
                + " WHERE usuario_id = ? ORDER BY 2 DESC", eu);
        Map<UUID, Startup> porId = startups.findAllById(linhas.stream().map(l -> (UUID) l.get("startup_id")).toList()).stream()
                .collect(Collectors.toMap(Startup::getId, Function.identity()));
        return linhas.stream().map(l -> {
            Startup s = porId.get((UUID) l.get("startup_id"));
            Instant em = ((Timestamp) l.get("em")).toInstant();
            return new Marcada(apresentacao.cartao(s), s.getSituacao() == SituacaoDaStartup.PUBLICADA, em);
        }).toList();
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41, KAN-44
     * Título: Marcações de uma startup
     * Objetivo: Dizer se a startup está nos favoritos ou descartada, para os botões da apresentação.
     * Decisões: Duas consultas simples.
     */
    @Transactional(readOnly = true)
    public Estado estado(UUID eu, UUID startupId) {
        return new Estado(existe(FAVORITO, eu, startupId), existe(DESCARTE, eu, startupId));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-39, KAN-41
     * Título: Identificadores marcados
     * Objetivo: Devolver as startups favoritas ou descartadas da pessoa, para o catálogo filtrar e marcar.
     * Decisões: Conjunto.
     */
    @Transactional(readOnly = true)
    public Set<UUID> ids(UUID eu, Tipo tipo) {
        return new HashSet<>(jdbc.queryForList("SELECT startup_id FROM " + tabela(tipo) + " WHERE usuario_id = ?", UUID.class, eu));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Gravar
     * Objetivo: Inserir a marcação; com atualização, troca a data se já existir.
     * Decisões: ON CONFLICT do PostgreSQL.
     */
    private void gravar(String tabela, UUID eu, UUID startupId, boolean atualizar) {
        String coluna = COLUNA.get(tabela);
        jdbc.update("INSERT INTO " + tabela + " (usuario_id, startup_id, " + coluna + ") VALUES (?, ?, ?) ON CONFLICT (usuario_id, startup_id) "
                + (atualizar ? "DO UPDATE SET " + coluna + " = EXCLUDED." + coluna : "DO NOTHING"), eu, startupId,
                Timestamp.from(relogio.instant()));
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Apagar
     * Objetivo: Remover a marcação.
     * Decisões: Sem erro se não existir.
     */
    private void apagar(String tabela, UUID eu, UUID startupId) {
        jdbc.update("DELETE FROM " + tabela + " WHERE usuario_id = ? AND startup_id = ?", eu, startupId);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Existe
     * Objetivo: Dizer se a marcação existe.
     * Decisões: Contagem.
     */
    private boolean existe(String tabela, UUID eu, UUID startupId) {
        Integer n = jdbc.queryForObject("SELECT count(*) FROM " + tabela + " WHERE usuario_id = ? AND startup_id = ?", Integer.class,
                eu, startupId);
        return n != null && n > 0;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-41
     * Título: Exigir publicada
     * Objetivo: Recusar marcar startup fora do catálogo.
     * Decisões: 404, como na apresentação.
     */
    private void exigirPublicada(UUID startupId) {
        if (startups.findById(startupId).filter(s -> s.getSituacao() == SituacaoDaStartup.PUBLICADA).isEmpty()) {
            throw new RecursoNaoEncontradoException("Startup não encontrada.");
        }
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-42
     * Título: Tabela do tipo
     * Objetivo: Traduzir o tipo de marcação na tabela.
     * Decisões: Nomes fixos, nunca vindos da requisição.
     */
    private static String tabela(Tipo tipo) {
        return switch (tipo) {
            case FAVORITOS -> FAVORITO;
            case HISTORICO -> VISTA;
            case DESCARTADAS -> DESCARTE;
        };
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 11:03
     * Cartão Jira: KAN-42
     * Título: Tipo de marcação
     * Objetivo: Enumerar as listas pessoais do catálogo.
     * Decisões: Favoritos, histórico e descartadas.
     */
    public enum Tipo { FAVORITOS, HISTORICO, DESCARTADAS }

    public record Marcada(ApresentacaoService.Cartao startup, boolean disponivel, Instant em) {
    }

    public record Estado(boolean favorito, boolean descartada) {
    }
}
