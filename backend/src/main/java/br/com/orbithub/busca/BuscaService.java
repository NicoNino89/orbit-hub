package br.com.orbithub.busca;

import br.com.orbithub.administracao.domain.Lista;
import br.com.orbithub.administracao.service.ListasService;
import br.com.orbithub.empreendimentos.service.BloqueiosDaStartup;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Autor: Nicholas Bruno Pimentel
 * Data: 25/11/2026
 * Hora: 22:40
 * Cartão Jira: KAN-167
 * Título: Busca global
 * Objetivo: Buscar, de qualquer tela, startups publicadas, organizações investidoras e artigos da central de ajuda.
 * Decisões: Pelo nome (startup e organização) e pelo título (artigo), sem diferenciar maiúsculas nem acentos simples;
 *           no máximo 5 por grupo. Startups bloqueadas para quem busca ficam de fora. Nenhum dado confidencial sai.
 *           Conteúdos (cursos) entram na fase 2 (KAN-133).
 */
@Service
public class BuscaService {

    private static final int POR_GRUPO = 5;

    private final JdbcTemplate jdbc;
    private final BloqueiosDaStartup bloqueios;
    private final ListasService listas;

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167
     * Título: Criação
     * Objetivo: Receber os serviços.
     * Decisões: Injeção por construtor.
     */
    public BuscaService(JdbcTemplate jdbc, BloqueiosDaStartup bloqueios, ListasService listas) {
        this.jdbc = jdbc;
        this.bloqueios = bloqueios;
        this.listas = listas;
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167
     * Título: Buscar
     * Objetivo: Devolver os resultados agrupados por tipo.
     * Decisões: Termo com menos de 2 caracteres devolve vazio.
     */
    @Transactional(readOnly = true)
    public Resultado buscar(UUID eu, UUID organizacao, String termo) {
        String t = termo == null ? "" : termo.strip();
        if (t.length() < 2) {
            return new Resultado(List.of(), List.of(), List.of());
        }
        String padrao = "%" + t.toLowerCase().replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_") + "%";
        List<Achado> startups = jdbc.query("SELECT id, nome_aprovado, segmento_aprovado FROM startup WHERE situacao = 'PUBLICADA' AND "
                + "lower(unaccent_simples(nome_aprovado)) LIKE lower(unaccent_simples(?)) ORDER BY nome_aprovado LIMIT 20", (rs, i) ->
                new Achado(rs.getObject(1, UUID.class), rs.getString(2), listas.rotulo(Lista.SEGMENTO, rs.getString(3))), padrao).stream()
                .filter(a -> !bloqueios.bloqueia(a.id(), eu, organizacao)).limit(POR_GRUPO).toList();
        List<Achado> organizacoes = jdbc.query("SELECT g.id, g.nome, g.tipo FROM organizacao g WHERE EXISTS (SELECT 1 FROM organizacao_membro m "
                + "WHERE m.organizacao_id = g.id AND m.saiu_em IS NULL) AND lower(unaccent_simples(g.nome)) LIKE lower(unaccent_simples(?)) ORDER BY g.nome LIMIT ?",
                (rs, i) -> new Achado(rs.getObject(1, UUID.class), rs.getString(2), listas.rotulo(Lista.TIPO_INVESTIDOR, rs.getString(3))), padrao, POR_GRUPO);
        List<Achado> artigos = jdbc.query("SELECT id, titulo FROM artigo_ajuda WHERE publicado AND lower(unaccent_simples(titulo)) LIKE "
                + "lower(unaccent_simples(?)) ORDER BY titulo LIMIT ?", (rs, i) -> new Achado(rs.getObject(1, UUID.class), rs.getString(2), null), padrao, POR_GRUPO);
        return new Resultado(startups, organizacoes, artigos);
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167
     * Título: Dados da busca
     * Objetivo: Tipar os resultados.
     * Decisões: Detalhe = segmento da startup ou tipo da organização.
     */
    public record Achado(UUID id, String nome, String detalhe) {
    }

    /**
     * Autor: Nicholas Bruno Pimentel
     * Data: 25/11/2026
     * Hora: 22:40
     * Cartão Jira: KAN-167, KAN-170, KAN-171
     * Título: Resultado
     * Objetivo: Rotina auxiliar Resultado.
     * Decisões: Ver a documentação da classe.
     */
    public record Resultado(List<Achado> startups, List<Achado> organizacoes, List<Achado> artigos) {
    }
}
